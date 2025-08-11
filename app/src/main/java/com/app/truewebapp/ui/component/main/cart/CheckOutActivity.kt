package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
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
import com.app.truewebapp.data.dto.order.OrderRequest
import com.app.truewebapp.databinding.ActivityCheckOutBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.viewmodel.CompanyAddressViewModel
import com.app.truewebapp.ui.viewmodel.CouponsViewModel
import com.app.truewebapp.ui.viewmodel.OrderPlaceViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckOutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckOutBinding
    private lateinit var companyAddressViewModel: CompanyAddressViewModel
    private lateinit var couponsViewModel: CouponsViewModel
    private lateinit var orderPlaceViewModel: OrderPlaceViewModel

    private var token = ""
    private var appliedCoupon: Data? = null
    private var subtotalAmount = 0.0
    private var totalVatAmount = 0.0
    private var minOrderValue = 0.0
    private var deliveryFees = 0.0
    private var deliveryMethodId = 0
    private var addressId = 0
    private var walletBalanceAmount = 0.0
    private var isWalletSelected = false
    private var couponBottomSheet: BottomSheetDialog? = null

    private val cartDao by lazy { CartDatabase.getInstance(this).cartDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize values from intent - always display wallet balance
        minOrderValue = intent.getStringExtra("minOrderValue")?.toDoubleOrNull() ?: 0.0
        walletBalanceAmount = intent.getStringExtra("walletBalance")?.toDoubleOrNull() ?: 0.0
        binding.tvWalletBalance.text = formatCurrency(walletBalanceAmount) // Initialize wallet balance display
        binding.tvWalletDiscount.text = formatCurrency(0.0) // Initialize discount to 0.0

        setupWindowInsets()
        setupClickListeners()
        initializeViewModels()
        observeViewModels()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        binding.walletToggle.setOnCheckedChangeListener { _, isChecked ->
            isWalletSelected = isChecked
            updateTotalPayment()
        }

        binding.textPayment.setOnClickListener {
            binding.textPayment.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            handlePaymentButtonClick()
        }

        binding.textPlaceOrder.setOnClickListener {
            binding.textPlaceOrder.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            placeOrder()
        }

        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            finish()
        }

        binding.couponInput.setOnClickListener {
            binding.couponInput.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            showCouponsBottomSheet()
        }

        binding.applyButton.setOnClickListener {
            binding.applyButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            applyCouponFromInput()
        }

        binding.clearCouponIcon.setOnClickListener {
            binding.clearCouponIcon.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            removeCoupon()
        }
    }

    private fun initializeViewModels() {
        companyAddressViewModel = ViewModelProvider(this)[CompanyAddressViewModel::class.java]
        couponsViewModel = ViewModelProvider(this)[CouponsViewModel::class.java]
        orderPlaceViewModel = ViewModelProvider(this)[OrderPlaceViewModel::class.java]

        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences.getString("token", "").orEmpty()}"

        companyAddressViewModel.companyAddress(token)
        couponsViewModel.coupons(token)
    }

    private fun observeViewModels() {
        observeCartItems()
        observeDeliveryMethods()
        observeCoupons()
        observeOrderPlace()
    }

    private fun handlePaymentButtonClick() {
        val paymentDetails = calculatePaymentDetails()

        if (isWalletSelected && walletBalanceAmount <= 0) {
            Toast.makeText(this, "Wallet balance is zero, please deselect wallet option", Toast.LENGTH_SHORT).show()
            return
        }

        if (paymentDetails.finalAmount <= 0) {
            // If amount is zero after deductions, just place the order directly
            placeOrder()
            return
        }

        navigateToPaymentActivity(paymentDetails)
    }

    private fun placeOrder() {
        if (addressId == 0) {
            Toast.makeText(this, "Please select a delivery address", Toast.LENGTH_SHORT).show()
            return
        }

        if (deliveryMethodId == 0) {
            Toast.makeText(this, "Please select a delivery method", Toast.LENGTH_SHORT).show()
            return
        }

        val paymentDetails = calculatePaymentDetails()
        val deliveryInstructions = binding.deliveryInstructionsInput.text?.toString()?.trim() ?: ""

        val orderRequest = OrderRequest(
             paymentDetails.walletDeduction.toString(),
            paymentDetails.couponDiscount.toString(),
            addressId.toString(),
            deliveryMethodId.toString(),
            deliveryInstructions,
            appliedCoupon?.coupon_id?.toString() ?: ""
        )

        orderPlaceViewModel.orderPlace(token, orderRequest)
        binding.textPlaceOrder.isEnabled = false
        binding.textPayment.isEnabled = false
    }

    private fun navigateToPaymentActivity(paymentDetails: PaymentDetails) {
        Intent(this, PaymentActivity::class.java).apply {
            putExtra("deliveryMethodId", deliveryMethodId.toString())
            putExtra("addressId", addressId.toString())
            putExtra("deliveryInstructions", binding.deliveryInstructionsInput.text?.toString()?.trim() ?: "")
            // Pass the final amount after all deductions
            putExtra("totalAmount", paymentDetails.finalAmount)
            // Pass the original total before any deductions
            putExtra("originalCartTotal", paymentDetails.originalTotal)
            putExtra("couponDiscountAmount", paymentDetails.couponDiscount)
            putExtra("walletDeductionAmount", paymentDetails.walletDeduction)
            putExtra("couponId", appliedCoupon?.coupon_id.toString())
            putExtra("useWallet", isWalletSelected)
            startActivity(this)
        }
    }

    private fun calculatePaymentDetails(): PaymentDetails {
        // Calculate total before any deductions
        val totalBeforeDeductions = subtotalAmount + totalVatAmount +
                (if (subtotalAmount < minOrderValue) deliveryFees else 0.0)

        // Calculate coupon discount based on applied coupon
        val couponAmount = appliedCoupon?.let { coupon ->
            when (coupon.discount_type.lowercase()) {
                "fixed" -> coupon.discount_value.toDoubleOrNull() ?: 0.0
                "percent" -> (subtotalAmount * (coupon.discount_value.toDoubleOrNull() ?: 0.0) / 100.0)
                else -> 0.0
            }
        } ?: 0.0

        // Calculate amount after coupon
        val amountAfterCoupon = (totalBeforeDeductions - couponAmount).coerceAtLeast(0.0)

        // Calculate wallet deduction
        val walletDeduction = if (isWalletSelected) {
            minOf(walletBalanceAmount, amountAfterCoupon)
        } else {
            0.0
        }

        // Calculate final amount to pay
        val finalAmountToPay = (amountAfterCoupon - walletDeduction).coerceAtLeast(0.0)

        return PaymentDetails(
            subtotal = subtotalAmount,
            vat = totalVatAmount,
            deliveryFee = if (subtotalAmount < minOrderValue) deliveryFees else 0.0,
            couponDiscount = couponAmount,
            walletDeduction = walletDeduction,
            finalAmount = finalAmountToPay,
            originalTotal = totalBeforeDeductions
        )
    }

    private fun updateTotalPayment() {
        val paymentDetails = calculatePaymentDetails()

        // Always update wallet balance display
        binding.tvWalletBalance.text = formatCurrency(walletBalanceAmount)

        // Update other UI elements
        binding.tvTotal.text = formatCurrency(paymentDetails.subtotal)
        binding.tvVat.text = formatCurrency(paymentDetails.vat)
        binding.tvDelivery.text = if (paymentDetails.deliveryFee > 0)
            formatCurrency(paymentDetails.deliveryFee) else "FREE"
        binding.tvCouponDiscount.text = formatCurrency(paymentDetails.couponDiscount)

        // Update wallet deduction display
        if (isWalletSelected && paymentDetails.walletDeduction > 0) {
            binding.tvWalletDiscount.text = "-${formatCurrency(paymentDetails.walletDeduction)}"
        } else {
            binding.tvWalletDiscount.text = formatCurrency(0.0)
        }

        binding.tvTotalPayment.text = formatCurrency(paymentDetails.finalAmount)

        // Update wallet UI visibility
        updateWalletUI(paymentDetails)
    }

    private fun updateWalletUI(paymentDetails: PaymentDetails) {
        if (isWalletSelected) {
            if (walletBalanceAmount > 0) {
                if (walletBalanceAmount >= paymentDetails.originalTotal) {
                    binding.walletDeductionMessage.text = "Your wallet balance will cover the full order amount"
                    binding.remainingAmountMessage.visibility = View.GONE
                    binding.textPlaceOrder.visibility = View.VISIBLE
                    binding.textPayment.visibility = View.GONE
                } else {
                    binding.walletDeductionMessage.text =
                        "${formatCurrency(paymentDetails.walletDeduction)} will be deducted from your wallet"
                    binding.remainingAmountMessage.text =
                        "Remaining amount to pay: ${formatCurrency(paymentDetails.finalAmount)}"
                    binding.remainingAmountMessage.visibility = View.VISIBLE
                    binding.textPlaceOrder.visibility = View.GONE
                    binding.textPayment.visibility = View.VISIBLE
                }
                binding.walletDeductionMessage.visibility = View.VISIBLE
            } else {
                binding.walletDeductionMessage.text = "Wallet balance is £0.00"
                binding.walletDeductionMessage.visibility = View.VISIBLE
                binding.remainingAmountMessage.visibility = View.GONE
                binding.textPlaceOrder.visibility = View.GONE
                binding.textPayment.visibility = View.VISIBLE
            }
        } else {
            binding.walletDeductionMessage.visibility = View.GONE
            binding.remainingAmountMessage.visibility = View.GONE
            binding.textPlaceOrder.visibility = View.GONE
            binding.textPayment.visibility = View.VISIBLE
        }
    }

    private fun observeCartItems() {
        lifecycleScope.launch {
            cartDao.getAllItems().collectLatest { cartItems ->
                if (cartItems.isNotEmpty()) {
                    updateTotalAmount(cartItems)
                } else {
                    subtotalAmount = 0.0
                    totalVatAmount = 0.0
                    removeCoupon()
                    updateTotalPayment()
                }
            }
        }
    }

    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        val totalQuantity = cartItems.sumOf { it.quantity }
        binding.tvUnitNo.text = totalQuantity.toString()
        binding.tvSkuNo.text = cartItems.size.toString()

        subtotalAmount = 0.0
        totalVatAmount = 0.0

        cartItems.forEach { item ->
            val itemTotal = item.price * item.quantity
            subtotalAmount += itemTotal

            if (item.taxable == 1) {
                totalVatAmount += itemTotal * 0.2
            }
        }

        binding.tvTotal.text = formatCurrency(subtotalAmount)
        reEvaluateAndApplyCoupon()
        updateTotalPayment()
    }

    private fun observeDeliveryMethods() {
        companyAddressViewModel.companyAddressResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let {
                if (it.delivery_methods.isNotEmpty()) {
                    createDeliveryMethodRadioButtons(it.delivery_methods)
                }
                if (it.company_addresses.isNotEmpty()) {
                    createAddressRadioButtons(it.company_addresses)
                }
            }
        }
    }

    private fun createDeliveryMethodRadioButtons(methods: List<DeliveryMethods>) {
        binding.deliveryMethodRadioGroup.removeAllViews()

        methods.forEachIndexed { index, method ->
            RadioButton(this).apply {
                id = View.generateViewId()
                text = method.delivery_method_name
                tag = method
                textSize = 14f
                typeface = Typeface.DEFAULT
                setPadding(8, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(10, 10, 0, 10) }

                binding.deliveryMethodRadioGroup.addView(this)

                if (index == 0) {
                    isChecked = true
                    binding.tvDeliveryMethod.text = method.delivery_method_name
                    deliveryMethodId = method.delivery_method_id
                    deliveryFees = method.delivery_method_amount.toDoubleOrNull() ?: 0.0
                    updateTotalPayment()
                }
            }
        }

        binding.deliveryMethodRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = findViewById<RadioButton>(checkedId)
            val deliveryMethod = selectedRadioButton?.tag as? DeliveryMethods

            deliveryMethod?.let { method ->
                binding.tvDeliveryMethod.text = method.delivery_method_name
                deliveryMethodId = method.delivery_method_id
                deliveryFees = method.delivery_method_amount.toDoubleOrNull() ?: 0.0
                updateTotalPayment()
            }
        }
    }

    private fun createAddressRadioButtons(addresses: List<CompanyAddresses>) {
        binding.dispatchToRadioGroup.removeAllViews()

        addresses.forEachIndexed { index, address ->
            // Line 1: Company Name
            val line1 = address.user_company_name

            // Line 2: Address 1 and Address 2
            val line2Parts = mutableListOf<String>()
            address.company_address1?.let { line2Parts.add(it) }
            address.company_address2?.takeIf { it.isNotBlank() }?.let { line2Parts.add(it) }
            val line2 = line2Parts.joinToString(", ")

            // Line 3: City, Country, and Postcode
            val line3Parts = mutableListOf<String>()
            address.company_city?.let { line3Parts.add(it) }
            address.company_country?.let { line3Parts.add(it) }
            address.company_postcode?.let { line3Parts.add(it) }
            val line3 = line3Parts.joinToString(", ")

            val fullAddress = "$line1\n$line2\n$line3"

            RadioButton(this).apply {
                id = View.generateViewId()
                text = fullAddress
                tag = address
                textSize = 14f
                typeface = Typeface.DEFAULT
                setPadding(8, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(10, 10, 0, 10) }

                binding.dispatchToRadioGroup.addView(this)

                if (index == 0) {
                    isChecked = true
                    binding.tvAddress.text = fullAddress
                    addressId = address.user_company_address_id
                }
            }
        }


        binding.dispatchToRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = findViewById<RadioButton>(checkedId)
            val address = selectedRadioButton?.tag as? CompanyAddresses

            address?.let {
                val addressParts = listOfNotNull(
                    it.user_company_name,
                    it.company_address1,
                    it.company_address2?.takeIf { addr -> addr.isNotBlank() },
                    it.company_city,
                    it.company_country,
                    it.company_postcode
                )
                binding.tvAddress.text = addressParts.joinToString(", ")
                addressId = it.user_company_address_id
            }
        }
    }

    private fun observeCoupons() {
        couponsViewModel.couponsResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
//                    binding.textSeeAllCoupon.visibility = View.VISIBLE
                    if (it.data.isNotEmpty()) {
                        reEvaluateAndApplyCoupon()
                    } else {
                        removeCoupon()
                    }
                } else {
//                    binding.textSeeAllCoupon.visibility = View.GONE
                    removeCoupon()
                }
            }
        }
    }

    private fun reEvaluateAndApplyCoupon() {
        val coupons = couponsViewModel.couponsResponse.value?.data ?: emptyList()
        val eligibleCoupons = coupons.filter {
            it.can_be_applied == true &&
                    (it.min_cart_value.toDoubleOrNull()?.let { min -> subtotalAmount >= min } == true)
        }

        if (eligibleCoupons.isNotEmpty()) {
            if (appliedCoupon == null || !eligibleCoupons.contains(appliedCoupon)) {
                eligibleCoupons.maxByOrNull { coupon ->
                    when (coupon.discount_type.lowercase()) {
                        "fixed" -> coupon.discount_value.toDoubleOrNull() ?: 0.0
                        "percent" -> (subtotalAmount * (coupon.discount_value.toDoubleOrNull() ?: 0.0) / 100.0)
                        else -> 0.0
                    }
                }?.let { applyCoupon(it) }
            }
        } else {
            removeCoupon()
        }
    }

    private fun applyCoupon(coupon: Data) {
        appliedCoupon = coupon
        binding.couponInput.text = coupon.code
        updateTotalPayment()
        showCouponAppliedUI()
//        Toast.makeText(this, "Coupon applied successfully", Toast.LENGTH_SHORT).show() // Add feedback
    }

    private fun applyCouponFromInput() {
        val enteredCode = binding.couponInput.text.toString().trim()
        if (enteredCode.isEmpty()) {
            Toast.makeText(this, "Please enter a coupon code", Toast.LENGTH_SHORT).show()
            return
        }

        couponsViewModel.couponsResponse.value?.data?.find {
            it.code.equals(enteredCode, ignoreCase = true)
        }?.let { coupon ->
            if (coupon.can_be_applied == true) {
                if (coupon.min_cart_value.toDoubleOrNull()?.let { subtotalAmount >= it } == true) {
                    applyCoupon(coupon)
                } else {
                    Toast.makeText(
                        this,
                        "Minimum order value for this coupon is £${coupon.min_cart_value}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this, "This coupon cannot be applied", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Invalid coupon code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeCoupon() {
        appliedCoupon = null
        binding.couponInput.text = ""
        binding.tvCouponDiscount.text = formatCurrency(0.0)
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

    private fun showCouponsBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottomsheet_coupons, null)
        couponBottomSheet = BottomSheetDialog(this).apply {
            setContentView(bottomSheetView)
        }

        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.couponRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@CheckOutActivity)
            adapter = CouponAdapter(
                couponsViewModel.couponsResponse.value?.data ?: emptyList(),
                subtotalAmount,
                appliedCoupon?.code
            ) { selectedCoupon ->
                applyCoupon(selectedCoupon)
                couponBottomSheet?.dismiss() // Dismiss the bottom sheet after selection
            }
        }

        couponBottomSheet?.show()
    }

    private fun observeOrderPlace() {
        orderPlaceViewModel.orderPlaceResponse.observe(this) { response ->
            binding.textPlaceOrder.isEnabled = true
            binding.textPayment.isEnabled = true

            response?.let {
                if (it.status) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            cartDao.clearCart()
                        }
                        startActivity(Intent(this@CheckOutActivity, OrderSuccessActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this, it.message ?: "Order failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        orderPlaceViewModel.isLoading.observe(this) { isLoading ->

        }
    }

    private fun formatCurrency(amount: Double): String {
        return "£${"%.2f".format(amount)}"
    }

    data class PaymentDetails(
        val subtotal: Double,
        val vat: Double,
        val deliveryFee: Double,
        val couponDiscount: Double,
        val walletDeduction: Double,
        val finalAmount: Double,
        val originalTotal: Double
    )
}