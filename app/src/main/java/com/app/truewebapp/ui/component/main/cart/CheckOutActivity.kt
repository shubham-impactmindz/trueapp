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
    private val cartDao by lazy { let { CartDatabase.getInstance(it).cartDao() } }
    private var totalVatAmount = 0.0
    private var minOrderValue = 0.0
    private var deliveryFees = 0.0
    private var deliveryMethodId = 0
    private var addressId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckOutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        minOrderValue = intent.getStringExtra("minOrderValue")?.toDoubleOrNull()!!
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
                applyCoupon(matchedCoupon)
                showCouponAppliedUI()
            } else {
                Toast.makeText(this, "Invalid coupon code", Toast.LENGTH_SHORT).show()
            }
        }
        binding.clearCouponIcon.setOnClickListener {
            removeCoupon()
        }
//        binding.couponInput.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                val currentCode = s.toString().trim()
//                val matchedCoupon = couponsViewModel.couponsResponse.value?.data?.find {
//                    it.code.equals(currentCode, ignoreCase = true)
//                }
//
//                if (appliedCoupon?.code.equals(currentCode, ignoreCase = true)) {
//                    // Already applied, show "Applied" button
//                    showCouponAppliedUI()
//                } else if (matchedCoupon != null) {
//                    // Show apply button if code exists but not yet applied
//                    showApplyCouponUI()
//                } else {
//                    // No valid code, show apply button but discount is not valid
//                    removeCoupon() // remove any previously applied
//                }
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })

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

        val adapter = CouponAdapter(coupons, subtotalAmount, appliedCoupon?.code) { selectedCoupon ->
            applyCoupon(selectedCoupon)
            bottomSheetDialog.dismiss()
        }

        recyclerView.adapter = adapter
        bottomSheetDialog.show()
    }

    private fun applyCoupon(coupon: Data) {
        if (subtotalAmount >= coupon.min_cart_value.toDoubleOrNull()!!) {
            appliedCoupon = coupon
            binding.couponInput.setText(coupon.code)

            updateTotalPayment()
            showCouponAppliedUI()
        } else {
            Toast.makeText(
                this,
                "Cart total should be at least £${coupon.min_cart_value} to apply this coupon.",
                Toast.LENGTH_SHORT
            ).show()
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
                    val validCoupons = it.data.filter {
                        c -> subtotalAmount >= c.min_cart_value.toDoubleOrNull()!!
                    }
                    if (validCoupons.isNotEmpty()) {
                        applyCoupon(validCoupons.first())
                    }

                } else {
                    binding.textSeeAllCoupon.visibility = View.GONE
                }
            }
        }

        couponsViewModel.isLoading.observe(this) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {

            }
        }

        couponsViewModel.apiError.observe(this) {

        }

        couponsViewModel.onFailure.observe(this) {

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
                            it.company_addresses[0].company_country
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

                }
            }
        }

        companyAddressViewModel.isLoading.observe(this) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {

            }
        }

        companyAddressViewModel.apiError.observe(this) {

        }

        companyAddressViewModel.onFailure.observe(this) {

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
            deliveryFees = selectedOption.delivery_method_amount.toDoubleOrNull()!!
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
                option.company_country
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
                selectedOption.company_country
            )

            selectedAddress = addressParts.joinToString(", ")
            binding.tvAddress.text = selectedAddress
            addressId = selectedOption.user_company_address_id
        }
    }
}