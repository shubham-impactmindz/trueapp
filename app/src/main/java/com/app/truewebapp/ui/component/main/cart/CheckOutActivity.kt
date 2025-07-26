package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.company_address.CompanyAddresses
import com.app.truewebapp.data.dto.company_address.DeliveryMethods
import com.app.truewebapp.data.dto.coupons.Data
import com.app.truewebapp.databinding.ActivityCheckOutBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.viewmodel.CompanyAddressViewModel
import com.app.truewebapp.ui.viewmodel.CouponsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CheckOutActivity : AppCompatActivity() {

    lateinit var binding: ActivityCheckOutBinding
    private lateinit var companyAddressViewModel: CompanyAddressViewModel
    private lateinit var couponsViewModel: CouponsViewModel
    private var token = ""
    private var appliedCoupon: Data? = null
    private var subtotalAmount = 0.0
    private var selectedAddress = ""
    // Lazy initialization for cartDao, ensures context is available
    private val cartDao by lazy { CartDatabase.getInstance(this).cartDao() } // Corrected context passing
    private var totalVatAmount = 0.0
    private var minOrderValue = 0.0
    private var deliveryFees = 0.0
    private var deliveryMethodId = 0
    private var addressId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckOutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        minOrderValue = intent.getStringExtra("minOrderValue")?.toDoubleOrNull() ?: 0.0 // Added null safety default

        // Handle system bars insets (status + nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        binding.textPayment.setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra("deliveryMethodId",deliveryMethodId.toString())
            intent.putExtra("addressId",addressId.toString())
            val instructions = binding.deliveryInstructionsInput.text?.toString()?.trim()
            intent.putExtra("deliveryInstructions", if (instructions.isNullOrEmpty()) "" else instructions)
            intent.putExtra("totalAmount",binding.tvTotalPayment.text)
            intent.putExtra("couponDiscount",binding.tvCouponDiscount.text)
            intent.putExtra("couponId",appliedCoupon?.coupon_id.toString())
            startActivity(intent)
        }

        binding.backLayout.setOnClickListener {
            finish()
        }
        binding.textSeeAllCoupon.setOnClickListener {
            showCouponsBottomSheet()
        }
        binding.applyButton.setOnClickListener {
            val enteredCode = binding.couponInput.text.toString().trim()
            val matchedCoupon = couponsViewModel.couponsResponse.value?.data?.find {
                it.code.equals(enteredCode, ignoreCase = true)
            }

            if (matchedCoupon != null) {
                // Manually entered coupon code must also pass the eligibility checks
                val isEligible = matchedCoupon.can_be_applied == true &&
                        (matchedCoupon.min_cart_value.toDoubleOrNull()?.let { subtotalAmount >= it } == true)

                if (isEligible) {
                    applyCoupon(matchedCoupon)
                    showCouponAppliedUI()
                } else {
                    Toast.makeText(this, "Coupon is not eligible for your current cart.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Invalid coupon code", Toast.LENGTH_SHORT).show()
            }
        }
        binding.clearCouponIcon.setOnClickListener {
            removeCoupon()
        }

        initializeViewModels()
        observeDeliveryMethods()
        observeCoupons()
        observeCartItems()
    }


    private fun showCouponsBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottomsheet_coupons, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.couponRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val coupons = couponsViewModel.couponsResponse.value?.data ?: emptyList()

        // Pass the subtotal and appliedCode to the adapter for accurate display
        val adapter = CouponAdapter(coupons, subtotalAmount, appliedCoupon?.code) { selectedCoupon ->
            applyCoupon(selectedCoupon)
            bottomSheetDialog.dismiss()
        }

        recyclerView.adapter = adapter
        bottomSheetDialog.show()
    }

    private fun applyCoupon(coupon: Data) {
        // This check is already done in `observeCoupons` and `showCouponsBottomSheet` but good to keep as a final guard
        val isEligible = coupon.can_be_applied == true &&
                (coupon.min_cart_value.toDoubleOrNull()?.let { subtotalAmount >= it } == true)

        if (isEligible) {
            appliedCoupon = coupon
            binding.couponInput.setText(coupon.code)
            updateTotalPayment()
            showCouponAppliedUI()
        } else {
            // This toast might be redundant if the button is disabled or already filtered,
            // but it provides explicit feedback for manually entered invalid coupons.
            Toast.makeText(
                this,
                "Coupon '${coupon.code}' is not eligible for your current cart. Min cart value: £${coupon.min_cart_value}.",
                Toast.LENGTH_LONG
            ).show()
            removeCoupon() // Remove any previously applied coupon if the new one is invalid
        }
    }

    private fun removeCoupon() {
        appliedCoupon = null
        binding.couponInput.text?.clear()
        binding.tvCouponDiscount.text = "£ 0.00"
        updateTotalPayment()
        showApplyCouponUI()
    }

    private fun showCouponAppliedUI() {
        binding.applyButton.visibility = View.GONE
        binding.appliedButton.visibility = View.VISIBLE
        binding.clearCouponIcon.visibility = View.VISIBLE
    }

    private fun showApplyCouponUI() {
        binding.applyButton.visibility = View.VISIBLE
        binding.appliedButton.visibility = View.GONE
        binding.clearCouponIcon.visibility = View.GONE
    }


    private fun observeCartItems() {
        this.lifecycleScope.launch {
            cartDao.getAllItems().collectLatest { cartItems ->
                Log.d("CartFragment", "Received ${cartItems.size} items from database Flow.")
                // Update the adapter's data


                if (cartItems.isNotEmpty()) {
                    updateTotalAmount(cartItems) // Update total price
                } else {
                    subtotalAmount = 0.0 // Reset subtotal if cart is empty
                    totalVatAmount = 0.0 // Reset VAT if cart is empty
                    removeCoupon() // Clear any applied coupon if cart becomes empty
                    updateTotalPayment() // Recalculate total with no items/coupon
                }
            }
        }
    }

    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        val totalQuantity = cartItems.sumOf { it.quantity }
        binding.tvUnitNo.text = "$totalQuantity"
        binding.tvSkuNo.text = "${cartItems.size}"

        subtotalAmount = 0.0
        totalVatAmount = 0.0

        for (item in cartItems) {
            val itemTotal = item.price * item.quantity
            subtotalAmount += itemTotal

            if (item.taxable == 1) {
                val vatForItem = itemTotal * 0.2
                totalVatAmount += vatForItem
            }
        }

        binding.tvTotal.text = "£ %.2f".format(subtotalAmount)

        // After updating subtotalAmount, re-evaluate and apply the best coupon if needed
        // This ensures if cart value changes, the coupon is re-checked for eligibility
        reEvaluateAndApplyCoupon()
        updateTotalPayment()
    }

    private fun updateTotalPayment() {
        var discount = 0.0
        val discountType = appliedCoupon?.discount_type?.lowercase()

        // Apply discount on subtotal (VAT already calculated earlier per-item)
        val discountedSubtotal = when (discountType) {
            "fixed" -> {
                discount = appliedCoupon?.discount_value?.toDoubleOrNull() ?: 0.0
                binding.tvCouponDiscount.text = "£ %.2f".format(discount)
                (subtotalAmount - discount).coerceAtLeast(0.0)
            }
            "percent" -> {
                val percent = appliedCoupon?.discount_value?.toDoubleOrNull() ?: 0.0
                discount = (subtotalAmount * percent / 100.0)
                binding.tvCouponDiscount.text = "£ %.2f".format(discount)
                (subtotalAmount - discount).coerceAtLeast(0.0)
            }
            else -> {
                binding.tvCouponDiscount.text = "£ 0.00"
                subtotalAmount
            }
        }

        // Do NOT recalculate VAT here. It's already correct from updateTotalAmount()
        // totalVatAmount already includes per-item VAT on taxable items

        // Determine delivery fee based on original subtotal
        val fee = if (subtotalAmount < minOrderValue) {
            deliveryFees
        } else 0.0

        // Show delivery text
        binding.tvDelivery.text = if (fee > 0) "£%.2f".format(fee) else "FREE"

        // Final total calculation in correct order:
        val finalTotal = discountedSubtotal + totalVatAmount + fee

        // Update UI
        binding.tvTotalPayment.text = "£ %.2f".format(finalTotal)
        binding.tvVat.text = "£ %.2f".format(totalVatAmount)
    }

    // New helper function to re-evaluate and apply the best eligible coupon
    private fun reEvaluateAndApplyCoupon() {
        val allAvailableCoupons = couponsViewModel.couponsResponse.value?.data ?: emptyList()

        // Filter for coupons that are eligible AND can_be_applied
        val eligibleAndCanBeAppliedCoupons = allAvailableCoupons.filter { coupon ->
            coupon.can_be_applied == true &&
                    (coupon.min_cart_value.toDoubleOrNull()?.let { subtotalAmount >= it } == true)
        }

        if (eligibleAndCanBeAppliedCoupons.isNotEmpty()) {
            // If an old coupon was applied, check if it's still eligible and can be applied
            val currentAppliedStillValid = appliedCoupon != null &&
                    eligibleAndCanBeAppliedCoupons.contains(appliedCoupon)

            if (!currentAppliedStillValid) {
                // If the current applied coupon is no longer valid, or no coupon was applied,
                // find the 'best' coupon to apply automatically.
                // "Best" is subjective; here, we pick the one with the highest fixed discount,
                // or highest percentage discount. You might need more complex logic.
                val bestCoupon = eligibleAndCanBeAppliedCoupons.maxByOrNull { coupon ->
                    when (coupon.discount_type.lowercase()) {
                        "fixed" -> coupon.discount_value.toDoubleOrNull() ?: 0.0
                        "percent" -> (subtotalAmount * (coupon.discount_value.toDoubleOrNull() ?: 0.0) / 100.0)
                        else -> 0.0
                    }
                }
                bestCoupon?.let {
                    applyCoupon(it)
                } ?: run {
                    // No "best" coupon found (e.g., if all coupons are only 0 discount)
                    // If appliedCoupon was just invalidated, this will ensure it's removed.
                    // If no coupon was applied, no action needed.
                    if (appliedCoupon != null) {
                        removeCoupon()
                    }
                }
            }
            // If currentAppliedStillValid is true, do nothing, keep the current coupon
        } else {
            // No eligible coupons found, ensure no coupon is applied
            removeCoupon()
        }
    }


    private fun initializeViewModels() {
        companyAddressViewModel = ViewModelProvider(this)[CompanyAddressViewModel::class.java]
        couponsViewModel = ViewModelProvider(this)[CouponsViewModel::class.java]
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"
        companyAddressViewModel.companyAddress(token)
        couponsViewModel.coupons(token)
    }

    private fun observeCoupons() {
        couponsViewModel.couponsResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    binding.textSeeAllCoupon.visibility = View.VISIBLE
                    if (it.data.isNotEmpty()) { // Ensure data is not empty before filtering
                        val eligibleAndCanBeAppliedCoupons = it.data.filter { c ->
                            c.can_be_applied == true &&
                                    (c.min_cart_value.toDoubleOrNull()?.let { subtotalAmount >= it } == true)
                        }

                        if (eligibleAndCanBeAppliedCoupons.isNotEmpty()) {
                            // Automatically apply the first eligible and can_be_applied coupon
                            // You might want to apply the "best" one based on your business logic
                            // For simplicity, let's keep it applying the first one for now.
                            // If you want "best" coupon, call reEvaluateAndApplyCoupon() here.
                            applyCoupon(eligibleAndCanBeAppliedCoupons.first())
                        } else {
                            // If no valid coupons, ensure none are applied automatically
                            removeCoupon()
                        }
                    } else {
                        // No coupons returned from API, so no coupon to apply
                        removeCoupon()
                    }
                } else {
                    binding.textSeeAllCoupon.visibility = View.GONE
                    removeCoupon() // No coupons available, remove any applied
                }
            }
        }

        couponsViewModel.isLoading.observe(this) {
            // Handle loading state, e.g., show/hide a progress bar
        }

        couponsViewModel.apiError.observe(this) {
            // Handle API errors
            Toast.makeText(this, "API Error: $it", Toast.LENGTH_SHORT).show()
        }

        couponsViewModel.onFailure.observe(this) {
            // Handle general failures (e.g., network issues)
            Toast.makeText(this, "Network Error: ${it?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeDeliveryMethods() {
        companyAddressViewModel.companyAddressResponse.observe(this) { response ->
            response?.let {

                if (it.status) {

                    if (it.delivery_methods.isNotEmpty()) {
                        binding.tvDeliveryMethod.text = it.delivery_methods[0].delivery_method_name
                        createRadioButtons(binding.deliveryMethodRadioGroup, it.delivery_methods)
                    }
                    if (it.company_addresses.isNotEmpty()) {
                        val addressParts = listOfNotNull(
                            it.company_addresses[0].user_company_name,
                            it.company_addresses[0].company_address1,
                            it.company_addresses[0].company_address2?.takeIf { it.isNotBlank() },  // Include only if not blank
                            it.company_addresses[0].company_city,
                            it.company_addresses[0].company_country,
                            it.company_addresses[0].company_postcode,
                        )

                        val fullAddress = addressParts.joinToString(", ")

                        selectedAddress = fullAddress
                        binding.tvAddress.text = selectedAddress
                        // Create RadioButtons dynamically
                        createRadioButtonsAddress(
                            binding.dispatchToRadioGroup,
                            it.company_addresses
                        )
                    }
                } else {
                    // Handle case where status is false
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        companyAddressViewModel.isLoading.observe(this) {
            // Show shimmer while loading, hide ViewPager
        }

        companyAddressViewModel.apiError.observe(this) {
            Toast.makeText(this, "API Error: $it", Toast.LENGTH_SHORT).show()
        }

        companyAddressViewModel.onFailure.observe(this) {
            Toast.makeText(this, "Network Error: ${it?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createRadioButtons(radioGroup: RadioGroup, options: List<DeliveryMethods>) {
        radioGroup.removeAllViews() // Clear previous buttons

        options.forEachIndexed { index, option ->
            val radioButton = RadioButton(this).apply {
                id = View.generateViewId() // Ensure each button has a unique ID
                text = option.delivery_method_name
                tag = option // Store the object for later retrieval
                textSize = 14f
                typeface = Typeface.DEFAULT
                setPadding(8, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(10, 10, 0, 10)
                }
            }

            radioGroup.addView(radioButton)

            if (index == 0) {
                radioButton.isChecked = true
                binding.tvDeliveryMethod.text = option.delivery_method_name
                deliveryMethodId = option.delivery_method_id
                deliveryFees = option.delivery_method_amount.toDoubleOrNull() ?: 0.0
                updateTotalPayment()
            }
        }

        // Single listener for the group
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedButton = group.findViewById<RadioButton>(checkedId)
            val selectedOption = selectedButton.tag as DeliveryMethods
            binding.tvDeliveryMethod.text = selectedOption.delivery_method_name
            deliveryFees = selectedOption.delivery_method_amount.toDoubleOrNull() ?: 0.0
            deliveryMethodId = selectedOption.delivery_method_id
            updateTotalPayment()
        }
    }

    private fun createRadioButtonsAddress(radioGroup: RadioGroup, options: List<CompanyAddresses>) {
        radioGroup.removeAllViews() // Clear any existing buttons

        for ((index, option) in options.withIndex()) {
            val addressParts = listOfNotNull(
                option.user_company_name,
                option.company_address1,
                option.company_address2?.takeIf { it.isNotBlank() },
                option.company_city,
                option.company_country,
                option.company_postcode,
            )
            val fullAddress = addressParts.joinToString(", ")

            val radioButton = RadioButton(this).apply {
                id = View.generateViewId()
                text = fullAddress
                tag = option // Store object for later use
                textSize = 14f
                typeface = Typeface.DEFAULT
                setPadding(8, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(10, 10, 0, 10)
                }
            }

            radioGroup.addView(radioButton)

            // Set first button as checked and update address
            if (index == 0) {
                radioButton.isChecked = true
                selectedAddress = fullAddress
                binding.tvAddress.text = selectedAddress
                addressId = option.user_company_address_id
            }
        }

        // Set listener once after all buttons are added
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedButton = group.findViewById<RadioButton>(checkedId)
            val selectedOption = selectedButton.tag as CompanyAddresses

            val addressParts = listOfNotNull(
                selectedOption.user_company_name,
                selectedOption.company_address1,
                selectedOption.company_address2?.takeIf { it.isNotBlank() },
                selectedOption.company_city,
                selectedOption.company_country,
                selectedOption.company_postcode
            )

            selectedAddress = addressParts.joinToString(", ")
            binding.tvAddress.text = selectedAddress
            addressId = selectedOption.user_company_address_id
        }
    }
}