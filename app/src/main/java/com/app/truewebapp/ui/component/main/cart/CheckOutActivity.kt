package com.app.truewebapp.ui.component.main.cart // Package declaration for organizing project structure

// Importing necessary Android and project-specific libraries
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.app.truewebapp.ui.component.main.account.AddAddressActivity
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

// Main checkout activity that manages placing orders, applying coupons, and handling wallet balance
class CheckOutActivity : AppCompatActivity() {

    // View binding instance for accessing views
    private lateinit var binding: ActivityCheckOutBinding

    // ViewModels for API communication
    private lateinit var companyAddressViewModel: CompanyAddressViewModel
    private lateinit var couponsViewModel: CouponsViewModel
    private lateinit var orderPlaceViewModel: OrderPlaceViewModel

    // Variables for order and wallet management
    private var token = "" // Stores user authentication token
    private var appliedCoupon: Data? = null // Stores currently applied coupon
    private var subtotalAmount = 0.0 // Subtotal amount before VAT and discounts
    private var totalVatAmount = 0.0 // Total VAT applied
    private var minOrderValue = 0.0 // Minimum value required for free delivery
    private var deliveryFees = 0.0 // Delivery charges
    private var deliveryMethodId = 0 // Selected delivery method ID
    private var addressId = 0 // Selected delivery address ID
    private var walletBalanceAmount = 0.0 // User's wallet balance
    private var isWalletSelected = false // Whether wallet payment option is selected
    private var couponBottomSheet: BottomSheetDialog? = null // Bottom sheet for coupons
    private lateinit var addAddressLauncher: ActivityResultLauncher<Intent>

    // Lazy initialization of Cart DAO from Room database
    private val cartDao by lazy { CartDatabase.getInstance(this).cartDao() }

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using view binding
        binding = ActivityCheckOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve minimum order value and wallet balance from Intent extras
        minOrderValue = intent.getStringExtra("minOrderValue")?.toDoubleOrNull() ?: 0.0
        walletBalanceAmount = intent.getStringExtra("walletBalance")?.toDoubleOrNull() ?: 0.0

        // Initialize wallet-related UI elements
        binding.tvWalletBalance.text = formatCurrency(walletBalanceAmount) // Show wallet balance
        binding.tvWalletDiscount.text = formatCurrency(0.0) // Start with no wallet discount


        // Register launcher to handle results from AddAddressActivity
        addAddressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Check if address was added and refresh list
                val isAdded = result.data?.getBooleanExtra("address_added", false) ?: false
                if (isAdded) {
                    companyAddressViewModel.companyAddress(token)
                }
            }
        }
        // Set up system window insets for proper padding
        setupWindowInsets()

        // Register all click listeners
        setupClickListeners()

        // Initialize ViewModels and fetch API data
        initializeViewModels()

        // Start observing ViewModel LiveData
        observeViewModels()
    }

    // Handle system bars (status & navigation bar) for padding adjustments
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
    }

    // Set up all button and toggle click listeners
    private fun setupClickListeners() {
        // Wallet toggle button
        binding.walletToggle.setOnCheckedChangeListener { _, isChecked ->
            isWalletSelected = isChecked
            updateTotalPayment()
        }

        // Payment button
        binding.textPayment.setOnClickListener {
            binding.textPayment.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY, // Provide haptic feedback
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            handlePaymentButtonClick()
        }

        // Payment button
        binding.llAddAddress.setOnClickListener {
            binding.llAddAddress.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY, // Provide haptic feedback
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            // Launch AddAddressActivity for result
            val intent = Intent(this, AddAddressActivity::class.java)
            addAddressLauncher.launch(intent)
        }

        // Place order button
        binding.textPlaceOrder.setOnClickListener {
            binding.textPlaceOrder.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            placeOrder()
        }

        // Back button
        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            finish() // Close activity
        }

        // Coupon input click
        binding.couponInput.setOnClickListener {
            binding.couponInput.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            showCouponsBottomSheet()
        }

        // Apply coupon button
        binding.applyButton.setOnClickListener {
            binding.applyButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            applyCouponFromInput()
        }

        // Clear coupon button
        binding.clearCouponIcon.setOnClickListener {
            binding.clearCouponIcon.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            removeCoupon()
        }
    }

    // Initialize ViewModels and API calls
    private fun initializeViewModels() {
        // Get ViewModels from provider
        companyAddressViewModel = ViewModelProvider(this)[CompanyAddressViewModel::class.java]
        couponsViewModel = ViewModelProvider(this)[CouponsViewModel::class.java]
        orderPlaceViewModel = ViewModelProvider(this)[OrderPlaceViewModel::class.java]

        // Get token from shared preferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences.getString("token", "").orEmpty()}"

        // Call API endpoints
        companyAddressViewModel.companyAddress(token)
        couponsViewModel.coupons(token)
    }

    // Attach observers to ViewModels
    private fun observeViewModels() {
        observeCartItems()
        observeDeliveryMethods()
        observeCoupons()
        observeOrderPlace()
    }

    // Handle payment button click
    private fun handlePaymentButtonClick() {
        val paymentDetails = calculatePaymentDetails()

        // Prevent wallet selection if balance is zero
        if (isWalletSelected && walletBalanceAmount <= 0) {
            Toast.makeText(this, "Wallet balance is zero, please deselect wallet option", Toast.LENGTH_SHORT).show()
            return
        }

        // Place order directly if amount is zero
        if (paymentDetails.finalAmount <= 0) {
            placeOrder()
            return
        }

        // Navigate to payment screen
        navigateToPaymentActivity(paymentDetails)
    }

    // Place an order request
    private fun placeOrder() {
        // Ensure delivery address is selected
        if (addressId == 0) {
            Toast.makeText(this, "Please select a delivery address", Toast.LENGTH_SHORT).show()
            return
        }

        // Ensure delivery method is selected
        if (deliveryMethodId == 0) {
            Toast.makeText(this, "Please select a delivery method", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare order request object
        val paymentDetails = calculatePaymentDetails()
        val deliveryInstructions = binding.deliveryInstructionsInput.text?.toString()?.trim() ?: ""
        val orderRequest = OrderRequest(
            paymentDetails.walletDeduction.toString(),
            paymentDetails.couponDiscount.toString(),
            addressId.toString(),
            deliveryMethodId.toString(),
            deliveryInstructions,
            appliedCoupon?.coupon_id?.toString() ?: "",
            false
        )

        // Call API to place order
        orderPlaceViewModel.orderPlace(token, orderRequest)

        // Disable buttons to avoid duplicate clicks
        binding.textPlaceOrder.isEnabled = false
        binding.textPayment.isEnabled = false
    }

    // Navigate to payment screen
    private fun navigateToPaymentActivity(paymentDetails: PaymentDetails) {
        // Ensure address and delivery method are selected
        if (addressId == 0) {
            Toast.makeText(this, "Please select a delivery address", Toast.LENGTH_SHORT).show()
            return
        }
        if (deliveryMethodId == 0) {
            Toast.makeText(this, "Please select a delivery method", Toast.LENGTH_SHORT).show()
            return
        }

        // Start payment activity with necessary extras
        Intent(this, PaymentActivity::class.java).apply {
            putExtra("deliveryMethodId", deliveryMethodId.toString())
            putExtra("addressId", addressId.toString())
            putExtra("deliveryInstructions", binding.deliveryInstructionsInput.text?.toString()?.trim() ?: "")
            putExtra("totalAmount", paymentDetails.finalAmount)
            putExtra("originalCartTotal", paymentDetails.originalTotal)
            putExtra("couponDiscountAmount", paymentDetails.couponDiscount)
            putExtra("walletDeductionAmount", paymentDetails.walletDeduction)
            putExtra("couponId", appliedCoupon?.coupon_id.toString())
            putExtra("useWallet", isWalletSelected)
            startActivity(this)
        }
    }
    // Function to calculate all payment-related details such as subtotal, VAT, coupon, wallet deduction, etc.
    private fun calculatePaymentDetails(): PaymentDetails {
        // Calculate total before any deductions (subtotal + VAT + delivery fee if applicable)
        val totalBeforeDeductions = subtotalAmount + totalVatAmount +
                (if (subtotalAmount < minOrderValue) deliveryFees else 0.0)

        // Calculate discount from applied coupon (fixed amount or percentage)
        val couponAmount = appliedCoupon?.let { coupon ->
            when (coupon.discount_type.lowercase()) {
                "fixed" -> coupon.discount_value.toDoubleOrNull() ?: 0.0 // Fixed discount value
                "percent" -> (subtotalAmount * (coupon.discount_value.toDoubleOrNull() ?: 0.0) / 100.0) // Percentage discount
                else -> 0.0
            }
        } ?: 0.0

        // Final amount after applying coupon (cannot go below 0)
        val amountAfterCoupon = (totalBeforeDeductions - couponAmount).coerceAtLeast(0.0)

        // Deduction from wallet if selected (cannot exceed amountAfterCoupon)
        val walletDeduction = if (isWalletSelected) {
            minOf(walletBalanceAmount, amountAfterCoupon)
        } else {
            0.0
        }

        // Final amount after wallet deduction (cannot go below 0)
        val finalAmountToPay = (amountAfterCoupon - walletDeduction).coerceAtLeast(0.0)

        // Return details in PaymentDetails data class
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

    // Function to update UI values with the latest payment calculations
    private fun updateTotalPayment() {
        // Get latest payment details
        val paymentDetails = calculatePaymentDetails()

        // Always update wallet balance text
        binding.tvWalletBalance.text = formatCurrency(walletBalanceAmount)

        // Update subtotal, VAT, delivery fee, and coupon discount
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

        // Final total payable
        binding.tvTotalPayment.text = formatCurrency(paymentDetails.finalAmount)

        // Update wallet-related messages and visibility
        updateWalletUI(paymentDetails)
    }

    // Function to handle wallet-related UI logic depending on wallet balance and selection
    private fun updateWalletUI(paymentDetails: PaymentDetails) {
        if (isWalletSelected) {
            if (walletBalanceAmount > 0) {
                if (walletBalanceAmount >= paymentDetails.originalTotal) {
                    // Wallet covers full amount
                    binding.walletDeductionMessage.text = "Your wallet balance will cover the full order amount"
                    binding.remainingAmountMessage.visibility = View.GONE
                    binding.textPlaceOrder.visibility = View.VISIBLE
                    binding.textPayment.visibility = View.GONE
                } else {
                    // Partial wallet coverage
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
                // Wallet balance is 0
                binding.walletDeductionMessage.text = "Wallet balance is £0.00"
                binding.walletDeductionMessage.visibility = View.VISIBLE
                binding.remainingAmountMessage.visibility = View.GONE
                binding.textPlaceOrder.visibility = View.GONE
                binding.textPayment.visibility = View.VISIBLE
            }
        } else {
            // Wallet not selected → hide wallet-related UI
            binding.walletDeductionMessage.visibility = View.GONE
            binding.remainingAmountMessage.visibility = View.GONE
            binding.textPlaceOrder.visibility = View.GONE
            binding.textPayment.visibility = View.VISIBLE
        }
    }

    // Observe cart items in real time from database
    private fun observeCartItems() {
        lifecycleScope.launch {
            cartDao.getAllItems().collectLatest { cartItems ->
                if (cartItems.isNotEmpty()) {
                    // Update totals if items exist
                    updateTotalAmount(cartItems)
                } else {
                    // Reset values if cart empty
                    subtotalAmount = 0.0
                    totalVatAmount = 0.0
                    removeCoupon()
                    updateTotalPayment()
                }
            }
        }
    }

    // Calculate totals (subtotal and VAT) from list of cart items
    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        // Sum of item quantities
        val totalQuantity = cartItems.sumOf { it.quantity }
        binding.tvUnitNo.text = totalQuantity.toString()
        binding.tvSkuNo.text = cartItems.size.toString()

        // Reset totals
        subtotalAmount = 0.0
        totalVatAmount = 0.0

        // Loop through cart items and calculate totals
        cartItems.forEach { item ->
            val itemTotal = item.price * item.quantity
            subtotalAmount += itemTotal

            // Apply VAT (20%) if taxable
            if (item.taxable == 1) {
                totalVatAmount += itemTotal * 0.2
            }
        }

        // Update subtotal in UI
        binding.tvTotal.text = formatCurrency(subtotalAmount)

        // Re-check coupon eligibility
        reEvaluateAndApplyCoupon()

        // Update full payment breakdown
        updateTotalPayment()
    }

    // Observe delivery methods API response
    private fun observeDeliveryMethods() {
        companyAddressViewModel.companyAddressResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let {
                if (it.delivery_methods.isNotEmpty()) {
                    // Create dynamic radio buttons for delivery methods
                    createDeliveryMethodRadioButtons(it.delivery_methods)
                } else {
                    // No delivery methods available
                    binding.tvDeliveryMethod.text = ""
                    deliveryMethodId = 0
                    deliveryFees = 0.0
                }
                if (it.company_addresses.isNotEmpty()) {
                    // Create address radio buttons
                    createAddressRadioButtons(it.company_addresses)
                } else {
                    binding.tvAddress.text = ""
                    addressId = 0
                }
            }
        }
    }

    // Dynamically create radio buttons for delivery methods
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

                // Add radio button to group
                binding.deliveryMethodRadioGroup.addView(this)

                // Select first method by default
                if (index == 0) {
                    isChecked = true
                    binding.tvDeliveryMethod.text = method.delivery_method_name
                    deliveryMethodId = method.delivery_method_id
                    deliveryFees = method.delivery_method_amount.toDoubleOrNull() ?: 0.0
                    updateTotalPayment()
                }
            }
        }

        // Handle user selection of delivery method
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

    // Dynamically create radio buttons for delivery addresses
    private fun createAddressRadioButtons(addresses: List<CompanyAddresses>) {
        binding.dispatchToRadioGroup.removeAllViews()

        addresses.forEachIndexed { index, address ->
            // Build address in multiple lines
            val line1 = address.user_company_name
            val line2Parts = mutableListOf<String>()
            address.company_address1.let { line2Parts.add(it) }
            address.company_address2?.takeIf { it.isNotBlank() }?.let { line2Parts.add(it) }
            val line2 = line2Parts.joinToString(", ")

            val line3Parts = mutableListOf<String>()
            address.company_city.let { line3Parts.add(it) }
            address.company_country.let { line3Parts.add(it) }
            address.company_postcode.let { line3Parts.add(it) }
            val line3 = line3Parts.joinToString(", ")

            val fullAddress = "$line1\n$line2\n$line3"

            // Create radio button for address
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

                // Select first valid address by default
                if (index == 0) {
                    if (address.company_address1.isNotEmpty()) {
                        isChecked = true
                        binding.tvAddress.text = fullAddress
                        addressId = address.user_company_address_id
                    } else {
                        isChecked = false
                        binding.tvAddress.text = ""
                        addressId = 0
                    }
                }
            }
        }

        // Handle user selection of address
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

    // Observe available coupons
    private fun observeCoupons() {
        couponsViewModel.couponsResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    if (it.data.isNotEmpty()) {
                        reEvaluateAndApplyCoupon()
                    } else {
                        removeCoupon()
                    }
                } else {
                    removeCoupon()
                }
            }
        }
    }

    // Re-check and apply best eligible coupon
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

    // Apply coupon and update UI
    private fun applyCoupon(coupon: Data) {
        appliedCoupon = coupon
        binding.couponInput.text = coupon.code
        updateTotalPayment()
        showCouponAppliedUI()
    }

    // Apply coupon entered manually by user
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

    // Remove coupon and reset UI
    private fun removeCoupon() {
        appliedCoupon = null
        binding.couponInput.text = ""
        binding.tvCouponDiscount.text = formatCurrency(0.0)
        updateTotalPayment()
        showApplyCouponUI()
    }

    // Show coupon applied UI state
    private fun showCouponAppliedUI() {
        binding.applyButton.visibility = View.GONE
        binding.appliedButton.visibility = View.VISIBLE
        binding.clearCouponIcon.visibility = View.VISIBLE
    }

    // Show default apply coupon UI state
    private fun showApplyCouponUI() {
        binding.applyButton.visibility = View.VISIBLE
        binding.appliedButton.visibility = View.GONE
        binding.clearCouponIcon.visibility = View.GONE
    }

    // Show bottom sheet listing all available coupons
    private fun showCouponsBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottomsheet_coupons, null)
        couponBottomSheet = BottomSheetDialog(this).apply {
            setContentView(bottomSheetView)
        }

        bottomSheetView.findViewById<RecyclerView>(R.id.couponRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@CheckOutActivity)
            adapter = CouponAdapter(
                couponsViewModel.couponsResponse.value?.data ?: emptyList(),
                subtotalAmount,
                appliedCoupon?.code
            ) { selectedCoupon ->
                applyCoupon(selectedCoupon)
                couponBottomSheet?.dismiss() // Close after selection
            }
        }

        couponBottomSheet?.show()
    }

    // Observe order placement API response
    private fun observeOrderPlace() {
        orderPlaceViewModel.orderPlaceResponse.observe(this) { response ->
            // Enable buttons back after response
            binding.textPlaceOrder.isEnabled = true
            binding.textPayment.isEnabled = true

            response?.let {
                if (it.status) {
                    // Success → clear cart and show success screen
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            cartDao.clearCart()
                        }
                        startActivity(Intent(this@CheckOutActivity, OrderSuccessActivity::class.java))
                        finish()
                    }
                } else {
                    // Failure → show error message
                    Toast.makeText(this, it.message ?: "Order failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Optional loading observer (could show progress bar if needed)
        orderPlaceViewModel.isLoading.observe(this) { isLoading ->

        }
    }

    // Utility function to format currency with pound symbol
    private fun formatCurrency(amount: Double): String {
        return "£${"%.2f".format(amount)}"
    }

    // Data class holding all payment-related details
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