package com.app.truewebapp.ui.component.main.cart

// Import required Android and project-specific classes
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.order.OrderRequest
import com.app.truewebapp.databinding.ActivityPaymentBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.viewmodel.BankDetailViewModel
import com.app.truewebapp.ui.viewmodel.OrderPlaceViewModel
import com.app.truewebapp.ui.viewmodel.StripeConfigViewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

// Activity responsible for handling payment process and placing an order
class PaymentActivity : AppCompatActivity() {

    // ViewBinding for accessing layout views safely
    private lateinit var binding: ActivityPaymentBinding

    // ViewModels for bank details, stripe config and order placement
    private lateinit var bankDetailViewModel: BankDetailViewModel
    private lateinit var stripeConfigViewModel: StripeConfigViewModel
    private lateinit var orderPlaceViewModel: OrderPlaceViewModel

    // Variables for payment and order details
    private var token = ""                       // Authorization token for API calls
    private var deliveryMethodId = ""            // Selected delivery method ID
    private var addressId = ""                   // Selected delivery address ID
    private var deliveryInstructions = ""        // Special delivery instructions
    private var totalAmount = 0.0                // Final payable amount after deductions
    private var couponDiscount = 0.0             // Discount amount from applied coupon
    private var couponId = ""                    // Applied coupon ID
    private var useWallet = false                // Whether wallet is used for payment
    private var walletDeduction = 0.0            // Amount deducted from wallet
    private var originalTotalAmount = 0.0        // Original cart total before deductions
    
    // Stripe payment variables
    private var paymentSheet: PaymentSheet? = null
    private var stripePublishableKey: String? = null
    private var stripeClientSecret: String? = null
    private var hasBankDetails = false
    private var hasStripeConfig = false
    private var isPaymentSheetInitialized = false

    // Lazy initialization of Cart DAO for database operations
    private val cartDao by lazy { CartDatabase.getInstance(this).cartDao() }

    // Lifecycle method called when activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adjust padding for system UI (status bar and navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,            // Apply top padding for status bar
                bottom = systemBars.bottom       // Apply bottom padding for nav bar
            )
            insets
        }

        // Initialize ViewModels
        initializeViewModels()

        // Setup payment method selection toggle (Bank / Card)
        setupPaymentMethodSelection()

        // Setup UI click listeners for buttons
        setupClickListeners()

        // Extract data passed via Intent from previous activity
        extractIntentData()

        // Update payment UI with correct values
        updatePaymentUI()

        // Initially disable payment button until Stripe config is ready
        binding.completePaymentButton.isEnabled = false

        // Initialize PaymentSheet early (before activity is RESUMED)
        // This must be done before STARTED state for Activity Result API registration
        initializePaymentSheet()

        // Observe bank details API response
        observeBankDetails()

        // Observe Stripe config API response
        observeStripeConfig()

        // Observe order placement API response
        observeOrderPlace()
    }
    
    // Initialize PaymentSheet early to avoid lifecycle issues
    private fun initializePaymentSheet() {
        try {
            // Initialize PaymentSheet with a callback - it will be configured later with publishable key
            paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
            isPaymentSheetInitialized = true
            Log.d("StripePayment", "PaymentSheet initialized early")
        } catch (e: Exception) {
            Log.e("StripePayment", "Failed to initialize PaymentSheet early", e)
            isPaymentSheetInitialized = false
        }
    }

    // Setup toggle between bank transfer and card payment
    // Replace your setupPaymentMethodSelection() method with this:
    private fun setupPaymentMethodSelection() {
        // Set initial state
        updatePaymentMethodUI(binding.paymentMethodRadioGroup.checkedRadioButtonId)

        // Listener: Update UI when selection changes
        binding.paymentMethodRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            Log.d("PaymentDebug", "RadioGroup selection changed: $checkedId")
            updatePaymentMethodUI(checkedId)

            // Force single selection by clearing any potential double selection
            binding.root.post {
                val bankChecked = binding.payByBank.isChecked
                val cardChecked = binding.payByCard.isChecked

                if (bankChecked && cardChecked) {
                    Log.w("PaymentDebug", "Both radio buttons checked - fixing selection")
                    // If both are checked, clear and re-check the intended one
                    group.clearCheck()
                    binding.root.postDelayed({
                        group.check(checkedId)
                    }, 50)
                }
            }
        }

        // Add individual click listeners as backup
        binding.payByCard.setOnClickListener {
            Log.d("PaymentDebug", "PayByCard clicked directly")
            if (!binding.payByCard.isChecked) {
                binding.paymentMethodRadioGroup.check(binding.payByCard.id)
            }
            updatePaymentMethodUI(binding.payByCard.id)
        }

        binding.payByBank.setOnClickListener {
            Log.d("PaymentDebug", "PayByBank clicked directly")
            if (!binding.payByBank.isChecked) {
                binding.paymentMethodRadioGroup.check(binding.payByBank.id)
            }
            updatePaymentMethodUI(binding.payByBank.id)
        }
    }

    // Update your updatePaymentMethodUI method to be more defensive:
    private fun updatePaymentMethodUI(checkedId: Int) {
        Log.d("PaymentDebug", "Updating UI for checkedId: $checkedId")

        when (checkedId) {
            binding.payByBank.id -> {
                Log.d("PaymentDebug", "Showing bank layout, hiding card layout")
                // Ensure bank is checked and card is unchecked
                binding.payByBank.isChecked = true
                binding.payByCard.isChecked = false

                // Show bank UI, hide card UI
                binding.bankPaymentLayout.visibility = View.VISIBLE
                binding.cardPaymentLayout.visibility = View.GONE
                binding.authorizePaymentButton.visibility = View.VISIBLE
                binding.completePaymentButton.visibility = View.GONE

                // Hide card input fields
                binding.cardholderNameLayout.visibility = View.GONE
                binding.cardNumberLayout.visibility = View.GONE
                binding.expiresLayout.visibility = View.GONE
                binding.cvvLayout.visibility = View.GONE
            }
            binding.payByCard.id -> {
                Log.d("PaymentDebug", "Showing card layout, hiding bank layout")
                // Ensure card is checked and bank is unchecked
                binding.payByCard.isChecked = true
                binding.payByBank.isChecked = false

                // Show card UI, hide bank UI
                binding.cardPaymentLayout.visibility = View.VISIBLE
                binding.bankPaymentLayout.visibility = View.GONE
                binding.cardholderNameLayout.visibility = View.VISIBLE
                binding.cardNumberLayout.visibility = View.VISIBLE
                binding.expiresLayout.visibility = View.VISIBLE
                binding.cvvLayout.visibility = View.VISIBLE
                binding.completePaymentButton.visibility = View.VISIBLE
                binding.authorizePaymentButton.visibility = View.GONE
            }
            else -> {
                Log.d("PaymentDebug", "No selection - hiding both layouts")
                // Nothing selected: hide both
                binding.bankPaymentLayout.visibility = View.GONE
                binding.cardPaymentLayout.visibility = View.GONE
                binding.authorizePaymentButton.visibility = View.GONE
                binding.completePaymentButton.visibility = View.GONE
            }
        }
    }

    // Setup click listeners for back, authorize payment, and complete payment
    private fun setupClickListeners() {
        // Back button → close activity with haptic feedback
        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional system flag
            )
            finish()
        }

        // Authorize Payment button → triggers order placement
        binding.authorizePaymentButton.setOnClickListener {
            binding.authorizePaymentButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            placeOrder()
        }

        // Complete Payment button → triggers Stripe payment
        binding.completePaymentButton.setOnClickListener {
            binding.completePaymentButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            if (hasStripeConfig && stripePublishableKey != null && paymentSheet != null) {
                // If we don't have a client secret, we need to create a PaymentIntent first
                // For now, we'll try to present the sheet - backend should provide client_secret
                if (stripeClientSecret != null) {
                    presentPaymentSheet()
                } else {
                    Toast.makeText(this, "Payment intent not ready. Please wait...", Toast.LENGTH_SHORT).show()
                    // Retry fetching Stripe config to get client_secret
                    stripeConfigViewModel.stripeConfig(token)
                }
            } else {
                Toast.makeText(this, "Payment configuration not ready. Please wait...", Toast.LENGTH_SHORT).show()
                // Retry fetching Stripe config
                stripeConfigViewModel.stripeConfig(token)
            }
        }
    }

    // Extract data from Intent extras sent by previous screen
    private fun extractIntentData() {
        deliveryMethodId = intent.getStringExtra("deliveryMethodId").orEmpty()
        addressId = intent.getStringExtra("addressId").orEmpty()
        totalAmount = intent.getDoubleExtra("totalAmount", 0.0)                 // Final payable
        originalTotalAmount = intent.getDoubleExtra("originalCartTotal", 0.0)   // Original total
        deliveryInstructions = intent.getStringExtra("deliveryInstructions").orEmpty()
        couponId = intent.getStringExtra("couponId").orEmpty()
        couponDiscount = intent.getDoubleExtra("couponDiscountAmount",0.0)
        useWallet = intent.getBooleanExtra("useWallet", false)
        walletDeduction = intent.getDoubleExtra("walletDeductionAmount", 0.0)

        // Debug logs for validation
        Log.d("PaymentDebug", "Original Total: $originalTotalAmount")
        Log.d("PaymentDebug", "Wallet Deduction: $walletDeduction")
        Log.d("PaymentDebug", "Final Amount: $totalAmount")
    }

    // Update UI with payment-related values
    private fun updatePaymentUI() {
        // Format final amount to currency string
        val formattedAmount = formatCurrency(totalAmount)

        // Update button text with payable amount
        binding.authorizePaymentButton.text = "Proceed Payment - $formattedAmount"
        binding.completePaymentButton.text = "Proceed Payment - $formattedAmount"

        // Initially set payment layout visibility based on default selection
        // Default is "Pay by Bank" (checked in XML), so show bank layout initially
        if (binding.payByBank.isChecked) {
            binding.bankPaymentLayout.visibility = View.VISIBLE
            binding.cardPaymentLayout.visibility = View.GONE
            binding.authorizePaymentButton.visibility = View.VISIBLE
            binding.completePaymentButton.visibility = View.GONE
            // Hide card input fields when bank is selected
            binding.cardholderNameLayout.visibility = View.GONE
            binding.cardNumberLayout.visibility = View.GONE
            binding.expiresLayout.visibility = View.GONE
            binding.cvvLayout.visibility = View.GONE
        } else if (binding.payByCard.isChecked) {
            binding.cardPaymentLayout.visibility = View.VISIBLE
            binding.bankPaymentLayout.visibility = View.GONE
            binding.completePaymentButton.visibility = View.VISIBLE
            binding.authorizePaymentButton.visibility = View.GONE
            // Show card input fields when card is selected
            binding.cardholderNameLayout.visibility = View.VISIBLE
            binding.cardNumberLayout.visibility = View.VISIBLE
            binding.expiresLayout.visibility = View.VISIBLE
            binding.cvvLayout.visibility = View.VISIBLE
        }

        // If wallet deduction is applied, update wallet section
        if (useWallet && walletDeduction > 0) {
            binding.walletDeductionLayout.visibility = View.VISIBLE
            binding.tvWalletDeduction.text = formatCurrency(walletDeduction)   // Deduction shown
            binding.tvOriginalAmount.text = formatCurrency(originalTotalAmount) // Original total shown
            binding.tvFinalAmount.text = formattedAmount                        // Remaining payable
        } else {
            binding.walletDeductionLayout.visibility = View.GONE
        }
    }

    // Create OrderRequest object and trigger API call
    private fun placeOrder() {
        if (binding.payByBank.isChecked) {
            val orderRequest = OrderRequest(
                wallet_discount = walletDeduction.toString(),   // Wallet deduction value
                coupon_discount = couponDiscount.toString(),    // Coupon discount value
                user_company_address_id = addressId,            // Delivery address ID
                delivery_method_id = deliveryMethodId,          // Delivery method ID
                delivery_instructions = deliveryInstructions,   // Special instructions
                couponId = couponId, // Coupon ID
                pay_by_bank = true
            )
            // Call ViewModel to place order
            orderPlaceViewModel.orderPlace(token, orderRequest)
        }else {
            val orderRequest = OrderRequest(
                wallet_discount = walletDeduction.toString(),   // Wallet deduction value
                coupon_discount = couponDiscount.toString(),    // Coupon discount value
                user_company_address_id = addressId,            // Delivery address ID
                delivery_method_id = deliveryMethodId,          // Delivery method ID
                delivery_instructions = deliveryInstructions,   // Special instructions
                couponId = couponId, // Coupon ID
                pay_by_bank = false
            )
            // Call ViewModel to place order
            orderPlaceViewModel.orderPlace(token, orderRequest)
        }

    }

    // Navigate to order success screen
    private fun navigateToOrderSuccess() {
        val intent = Intent(this, OrderSuccessActivity::class.java)
        startActivity(intent)
        finish() // Close current activity
    }

    // Initialize ViewModels and fetch bank details and Stripe config
    private fun initializeViewModels() {
        bankDetailViewModel = ViewModelProvider(this)[BankDetailViewModel::class.java]
        stripeConfigViewModel = ViewModelProvider(this)[StripeConfigViewModel::class.java]
        orderPlaceViewModel = ViewModelProvider(this)[OrderPlaceViewModel::class.java]

        // Retrieve token from SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences.getString("token", "").orEmpty()}"

        // Fetch bank details for payment
        bankDetailViewModel.bankDetails(token)
        
        // Fetch Stripe config for card payment
        stripeConfigViewModel.stripeConfig(token)
    }

    // Observe LiveData for Bank Details
    private fun observeBankDetails() {
        // Observe API response for bank details
        bankDetailViewModel.bankDetailResponse.observe(this) { response ->
            hasBankDetails = response?.status == true && response.bank_detail != null
            if (hasBankDetails) {
                response?.let { bankDetail ->
                    // Bind data to UI if response is successful
                    binding.tvBankHolderName.text = bankDetail.bank_detail.company_name
                    binding.tvBankName.text = bankDetail.bank_detail.bank_name
                    binding.tvAccountNumber.text = bankDetail.bank_detail.account_number
                    binding.tvSortCode.text = bankDetail.bank_detail.sort_code
                }
            }
            updatePaymentMethodVisibility()
        }

        // Observe loading state
        bankDetailViewModel.isLoading.observe(this) { isLoading ->
            // Can show/hide loading indicator if needed
        }

        // Observe API error
        bankDetailViewModel.apiError.observe(this) { error ->
            hasBankDetails = false
            updatePaymentMethodVisibility()
        }

        // Observe network failure
        bankDetailViewModel.onFailure.observe(this) { throwable ->
            hasBankDetails = false
            updatePaymentMethodVisibility()
        }
    }
    
    // Observe LiveData for Stripe Config
    private fun observeStripeConfig() {
        // Observe API response for Stripe config
        stripeConfigViewModel.stripeConfigResponse.observe(this) { response ->
            hasStripeConfig = response?.status == true && 
                             response.data != null && 
                             !response.data.publishable_key.isNullOrEmpty()
            
            if (hasStripeConfig) {
                response?.data?.let { config ->
                    stripePublishableKey = config.publishable_key
                    // Use client_secret if provided by backend
                    // Note: client_secret is the PaymentIntent client secret from Stripe
                    // If not provided, backend should create PaymentIntent and return it
                    stripeClientSecret = config.client_secret
                    
                    // Log configuration for debugging
                    Log.d("StripePayment", "Stripe Config received:")
                    Log.d("StripePayment", "  Provider: ${config.provider}")
                    Log.d("StripePayment", "  Test Mode: ${config.test_mode}")
                    Log.d("StripePayment", "  Publishable Key: ${config.publishable_key?.take(20)}...")
                    Log.d("StripePayment", "  Client Secret: ${if (config.client_secret != null) "Provided" else "Not provided - backend needs to create PaymentIntent"}")
                    
                    // Initialize Stripe with publishable key
                    stripePublishableKey?.let { key ->
                        try {
                            // Initialize PaymentConfiguration (safe to call multiple times)
                            PaymentConfiguration.init(applicationContext, key)
                            
                            // PaymentSheet was already initialized in onCreate, just log success
                            if (isPaymentSheetInitialized && paymentSheet != null) {
                                Log.d("StripePayment", "Stripe PaymentSheet configured successfully")
                                Log.d("StripePayment", "Publishable Key: ${key.take(20)}...")
                                Log.d("StripePayment", "Secret Key: ${config.secret_key?.take(20) ?: "null"}...")
                                Log.d("StripePayment", "Test Mode: ${config.test_mode}")
                            } else {
                                // Fallback: try to initialize if not already done
                                try {
                                    paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
                                    isPaymentSheetInitialized = true
                                    Log.d("StripePayment", "PaymentSheet initialized in fallback")
                                } catch (e: Exception) {
                                    Log.e("StripePayment", "Failed to initialize PaymentSheet in fallback", e)
                                    hasStripeConfig = false
                                    paymentSheet = null
                                    Toast.makeText(this, "Failed to initialize payment system: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("StripePayment", "Failed to configure Stripe", e)
                            hasStripeConfig = false
                            Toast.makeText(this, "Failed to configure payment system: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                stripePublishableKey = null
                stripeClientSecret = null
                // Don't set paymentSheet to null here as it's already initialized
            }
            updatePaymentMethodVisibility()
        }

        // Observe loading state
        stripeConfigViewModel.isLoading.observe(this) { isLoading ->
            // Can show/hide loading indicator if needed
        }

        // Observe API error
        stripeConfigViewModel.apiError.observe(this) { error ->
            hasStripeConfig = false
            stripePublishableKey = null
            stripeClientSecret = null
            // Don't set paymentSheet to null - keep it initialized for potential retry
            Log.e("StripePayment", "Stripe config API error: $error")
            Toast.makeText(this, "Failed to load payment configuration: $error", Toast.LENGTH_SHORT).show()
            updatePaymentMethodVisibility()
        }

        // Observe network failure
        stripeConfigViewModel.onFailure.observe(this) { throwable ->
            hasStripeConfig = false
            stripePublishableKey = null
            stripeClientSecret = null
            // Don't set paymentSheet to null - keep it initialized for potential retry
            Log.e("StripePayment", "Stripe config network failure", throwable)
            Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show()
            updatePaymentMethodVisibility()
        }
    }

    // Update payment method visibility based on API responses
    private fun updatePaymentMethodVisibility() {
        // Show/hide Pay by Bank option
        if (hasBankDetails) {
            binding.payByBank.visibility = View.VISIBLE
            // If bank is available and card is not, select bank by default
            if (!hasStripeConfig) {
                binding.payByBank.isChecked = true
                // Show bank layout and button
                binding.bankPaymentLayout.visibility = View.VISIBLE
                binding.cardPaymentLayout.visibility = View.GONE
                binding.authorizePaymentButton.visibility = View.VISIBLE
                binding.completePaymentButton.visibility = View.GONE
            }
        } else {
            binding.payByBank.visibility = View.GONE
            binding.bankPaymentLayout.visibility = View.GONE
            // If bank is not available and bank was selected, switch to card if available
            if (binding.payByBank.isChecked && hasStripeConfig) {
                binding.payByCard.isChecked = true
            }
        }

        // Show/hide Pay by Card option
        if (hasStripeConfig) {
            binding.payByCard.visibility = View.VISIBLE
            binding.completePaymentButton.isEnabled = true
            // If card is available and bank is not, select card by default
            if (!hasBankDetails) {
                binding.payByCard.isChecked = true
                // Show card layout, input fields, and button
                binding.cardPaymentLayout.visibility = View.VISIBLE
                binding.bankPaymentLayout.visibility = View.GONE
                binding.cardholderNameLayout.visibility = View.VISIBLE
                binding.cardNumberLayout.visibility = View.VISIBLE
                binding.expiresLayout.visibility = View.VISIBLE
                binding.cvvLayout.visibility = View.VISIBLE
                binding.completePaymentButton.visibility = View.VISIBLE
                binding.authorizePaymentButton.visibility = View.GONE
            }
        } else {
            binding.payByCard.visibility = View.GONE
            binding.cardPaymentLayout.visibility = View.GONE
            binding.completePaymentButton.isEnabled = false
            // Hide card input fields when card option is not available
            binding.cardholderNameLayout.visibility = View.GONE
            binding.cardNumberLayout.visibility = View.GONE
            binding.expiresLayout.visibility = View.GONE
            binding.cvvLayout.visibility = View.GONE
            // If card is not available and card was selected, switch to bank if available
            if (binding.payByCard.isChecked && hasBankDetails) {
                binding.payByBank.isChecked = true
            }
        }

        // If neither option is available, show error
        if (!hasBankDetails && !hasStripeConfig) {
            binding.bankPaymentLayout.visibility = View.GONE
            binding.cardPaymentLayout.visibility = View.GONE
            binding.authorizePaymentButton.visibility = View.GONE
            binding.completePaymentButton.visibility = View.GONE
            Toast.makeText(this, "No payment methods available", Toast.LENGTH_LONG).show()
        }
    }
    
    // Present Stripe Payment Sheet
    private fun presentPaymentSheet() {
        try {
            val clientSecret = stripeClientSecret
            val sheet = paymentSheet
            
            if (clientSecret == null || sheet == null) {
                Toast.makeText(this, "Payment configuration not ready", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Validate client secret format (should start with "pi_")
            if (!clientSecret.startsWith("pi_") && !clientSecret.startsWith("seti_")) {
                Log.e("StripePayment", "Invalid client secret format: $clientSecret")
                Toast.makeText(this, "Invalid payment configuration", Toast.LENGTH_SHORT).show()
                return
            }
            
            val paymentSheetConfiguration = PaymentSheet.Configuration.Builder("TrueWebApp")
                .build()
            
            sheet.presentWithPaymentIntent(
                clientSecret,
                paymentSheetConfiguration
            )
            
            Log.d("StripePayment", "Payment sheet presented successfully")
        } catch (e: Exception) {
            Log.e("StripePayment", "Error presenting payment sheet", e)
            Toast.makeText(this, "Failed to open payment screen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Handle Stripe Payment Sheet result
    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                // Payment succeeded - place order
                val orderRequest = OrderRequest(
                    wallet_discount = walletDeduction.toString(),
                    coupon_discount = couponDiscount.toString(),
                    user_company_address_id = addressId,
                    delivery_method_id = deliveryMethodId,
                    delivery_instructions = deliveryInstructions,
                    couponId = couponId,
                    pay_by_bank = false
                )
                orderPlaceViewModel.orderPlace(token, orderRequest)
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(this, "Payment failed: ${paymentResult.error.message}", Toast.LENGTH_LONG).show()
                Log.e("StripePayment", "Payment failed", paymentResult.error)
            }
        }
    }

    // Observe LiveData for Order Placement
    private fun observeOrderPlace() {
        orderPlaceViewModel.orderPlaceResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let {
                // Hide loading overlay and background on success
                binding.loadingOverlayBackground.visibility = View.GONE
                binding.loadingOverlay.visibility = View.GONE
                // If success, clear cart and navigate to success screen
                clearCartAndNavigateToSuccess()
            } ?: run {
                // Hide loading overlay and background on failure
                binding.loadingOverlayBackground.visibility = View.GONE
                binding.loadingOverlay.visibility = View.GONE
                // Handle failure case
            }
        }

        // Observe loading state during API call
        orderPlaceViewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading overlay and background
            if (isLoading) {
                binding.loadingOverlayBackground.visibility = View.VISIBLE
                binding.loadingOverlay.visibility = View.VISIBLE
            } else {
                binding.loadingOverlayBackground.visibility = View.GONE
                binding.loadingOverlay.visibility = View.GONE
            }
            // Disable/Enable button depending on loading
            binding.authorizePaymentButton.isEnabled = !isLoading
            binding.completePaymentButton.isEnabled = !isLoading && hasStripeConfig
        }

        // Observe API error
        orderPlaceViewModel.apiError.observe(this) { error ->
            // Hide loading overlay and background on error
            binding.loadingOverlayBackground.visibility = View.GONE
            binding.loadingOverlay.visibility = View.GONE
            // Handle API error
            Log.e("OrderPlace", "Order placement API error: $error")
            Toast.makeText(this, "Order failed: $error", Toast.LENGTH_SHORT).show()
            binding.authorizePaymentButton.isEnabled = true
            binding.completePaymentButton.isEnabled = hasStripeConfig
        }

        // Observe failure case
        orderPlaceViewModel.onFailure.observe(this) { throwable ->
            // Hide loading overlay and background on failure
            binding.loadingOverlayBackground.visibility = View.GONE
            binding.loadingOverlay.visibility = View.GONE
            // Handle throwable error
            Log.e("OrderPlace", "Order placement failed", throwable)
            Toast.makeText(this, "Order failed: ${throwable.message}", Toast.LENGTH_SHORT).show()
            binding.authorizePaymentButton.isEnabled = true
            binding.completePaymentButton.isEnabled = hasStripeConfig
        }
    }

    // Clear cart data from database and navigate to success screen
    private fun clearCartAndNavigateToSuccess() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                cartDao.clearCart()  // Clear cart from local database in background thread
            }
            navigateToOrderSuccess() // Navigate after clearing cart
        }
    }

    // Format given amount into UK currency format (£)
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.UK).format(amount)
    }
}