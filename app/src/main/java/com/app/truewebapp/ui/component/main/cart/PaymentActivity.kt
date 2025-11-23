package com.app.truewebapp.ui.component.main.cart

// Import required Android and project-specific classes
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.data.dto.order.OrderRequest
import com.app.truewebapp.data.dto.stripe.AutomaticPaymentMethods
import com.app.truewebapp.data.dto.stripe.PaymentIntentRequest
import com.app.truewebapp.databinding.ActivityPaymentBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.viewmodel.BankDetailViewModel
import com.app.truewebapp.ui.viewmodel.OrderPlaceViewModel
import com.app.truewebapp.ui.viewmodel.PaymentIntentViewModel
import com.app.truewebapp.ui.viewmodel.StripeConfigViewModel
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

// Activity responsible for handling payment process and placing an order
class PaymentActivity : AppCompatActivity() {

    // ViewBinding for accessing layout views safely
    private lateinit var binding: ActivityPaymentBinding

    // ViewModels for bank details, stripe config, payment intent and order placement
    private lateinit var bankDetailViewModel: BankDetailViewModel
    private lateinit var stripeConfigViewModel: StripeConfigViewModel
    private lateinit var paymentIntentViewModel: PaymentIntentViewModel
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
    private var stripe: Stripe? = null
    private var stripePublishableKey: String? = null
    private var stripeSecretKey: String? = null
    private var stripeClientSecret: String? = null
    private var paymentIntentId: String? = null // Payment Intent ID extracted from client_secret
    private var paymentMethodId: String? = null // Payment Method ID (will be extracted from payment result)
    private var customerId: String? = null // Stripe Customer ID
    private var receiptEmail: String? = null // Receipt email
    private var paymentDescription: String? = null // Payment description
    private var paymentMetadata: String? = null // Payment metadata as JSON string
    private var hasBankDetails = false
    private var hasStripeConfig = false

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
        
        // Setup expiry field formatting
        setupExpiryFieldFormatting()
        
        // Setup card number formatting
        setupCardNumberFormatting()

        // Extract data passed via Intent from previous activity
        extractIntentData()

        // Update payment UI with correct values
        updatePaymentUI()

        // Initially disable payment button until Stripe config is ready
        binding.completePaymentButton.isEnabled = false

        // Stripe will be initialized when publishable key is available

        // Observe bank details API response
        observeBankDetails()

        // Observe Stripe config API response
        observeStripeConfig()

        // Observe payment intent API response
        observePaymentIntent()

        // Observe order placement API response
        observeOrderPlace()
    }
    
    // Initialize Stripe instance when publishable key is available
    private fun initializeStripe() {
        stripePublishableKey?.let { key ->
            try {
                stripe = Stripe(applicationContext, key)
                Log.d("StripePayment", "Stripe initialized with publishable key")
        } catch (e: Exception) {
                Log.e("StripePayment", "Failed to initialize Stripe", e)
                stripe = null
            }
        }
    }

    // Track selected payment method
    private var selectedPaymentMethod: String? = null

    // Setup toggle between bank transfer and card payment using custom radio buttons
    private fun setupPaymentMethodSelection() {
        // Initially hide all UI
        hideAllPaymentUI()
        selectedPaymentMethod = null

        // Pay By Card click listener
        binding.payByCardLayout.setOnClickListener {
            selectPaymentMethod("card")
        }

        // Pay By Bank click listener
        binding.payByBankLayout.setOnClickListener {
            selectPaymentMethod("bank")
        }
    }

    // Select payment method and update UI
    private fun selectPaymentMethod(method: String) {
        selectedPaymentMethod = method
        
        when (method) {
            "card" -> {
                // Update radio button indicators
                binding.payByCardRadio.setImageResource(R.drawable.radio_button_selected)
                binding.payByBankRadio.setImageResource(R.drawable.radio_button_unselected)
                
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
            "bank" -> {
                // Update radio button indicators
                binding.payByBankRadio.setImageResource(R.drawable.radio_button_selected)
                binding.payByCardRadio.setImageResource(R.drawable.radio_button_unselected)
                
                // Show bank UI, hide card UI
                binding.bankPaymentLayout.visibility = View.VISIBLE
                binding.cardPaymentLayout.visibility = View.GONE
                binding.cardholderNameLayout.visibility = View.GONE
                binding.cardNumberLayout.visibility = View.GONE
                binding.expiresLayout.visibility = View.GONE
                binding.cvvLayout.visibility = View.GONE
                binding.authorizePaymentButton.visibility = View.VISIBLE
                binding.completePaymentButton.visibility = View.GONE
            }
        }
    }

    // Hide all payment UI elements
    private fun hideAllPaymentUI() {
        binding.bankPaymentLayout.visibility = View.GONE
        binding.cardPaymentLayout.visibility = View.GONE
        binding.authorizePaymentButton.visibility = View.GONE
        binding.completePaymentButton.visibility = View.GONE
        binding.cardholderNameLayout.visibility = View.GONE
        binding.cardNumberLayout.visibility = View.GONE
        binding.expiresLayout.visibility = View.GONE
        binding.cvvLayout.visibility = View.GONE
    }


    // Setup expiry field formatting to automatically add "/" after 2 digits
    private fun setupExpiryFieldFormatting() {
        binding.expires.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                // Remove any existing "/" to avoid duplicates
                val digitsOnly = text.replace("/", "")
                
                // If we have 2 or more digits, format as MM/YY
                if (digitsOnly.length >= 2) {
                    val formatted = if (digitsOnly.length == 2) {
                        "${digitsOnly.substring(0, 2)}/"
                    } else {
                        "${digitsOnly.substring(0, 2)}/${digitsOnly.substring(2)}"
                    }
                    
                    if (formatted != text) {
                        binding.expires.removeTextChangedListener(this)
                        binding.expires.setText(formatted)
                        binding.expires.setSelection(formatted.length)
                        binding.expires.addTextChangedListener(this)
                    }
                }
            }
        })
    }
    
    // Setup card number formatting to add spaces every 4 digits (max 16 digits)
    private fun setupCardNumberFormatting() {
        binding.cardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                // Remove all spaces to get only digits
                val digitsOnly = text.replace(" ", "")
                
                // Limit to 16 digits
                val limitedDigits = if (digitsOnly.length > 16) {
                    digitsOnly.substring(0, 16)
                } else {
                    digitsOnly
                }
                
                // Format with spaces every 4 digits
                val formatted = StringBuilder()
                for (i in limitedDigits.indices) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ")
                    }
                    formatted.append(limitedDigits[i])
                }
                
                // Only update if the formatted text is different
                if (formatted.toString() != text) {
                    binding.cardNumber.removeTextChangedListener(this)
                    binding.cardNumber.setText(formatted.toString())
                    binding.cardNumber.setSelection(formatted.length)
                    binding.cardNumber.addTextChangedListener(this)
                }
            }
        })
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
            if (selectedPaymentMethod != "card") {
                Toast.makeText(this, "Please select card payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Validate card input fields
            if (!validateCardInputs()) {
                return@setOnClickListener
            }
            
            if (hasStripeConfig && stripePublishableKey != null && stripe != null) {
                // Step 1: Create PaymentIntent first
                createPaymentIntent()
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

        // Initially hide all payment UI - will be shown when user selects a payment method
        hideAllPaymentUI()

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
        // Show loader for bank payment
        showLoader("Creating Order...")
        
        // Convert amount from pounds to cents
        val amountInCents = (totalAmount * 100).toInt()
        
        if (selectedPaymentMethod == "bank") {
            val orderRequest = OrderRequest(
                user_company_address_id = addressId,
                delivery_method_id = deliveryMethodId,
                coupon_discount = couponDiscount.takeIf { it > 0 },
                wallet_discount = walletDeduction.takeIf { it > 0 },
                delivery_instructions = deliveryInstructions.takeIf { it.isNotEmpty() },
                coupon_id = couponId.takeIf { it.isNotEmpty() },
                pay_by_bank = true,
                payment_status = "pending",
                payment_provider = "bank_transfer",
                payment_reference = null,
                payment_intent_id = "", // Empty string for bank payments
                payment_method_id = null,
                customer_id = null,
                currency = "gbp",
                amount = amountInCents,
                status = "pending",
                receipt_email = null,
                description = null,
                metadata = null,
                raw_payload = null
            )
            // Call ViewModel to place order
            orderPlaceViewModel.orderPlace(token, orderRequest)
        } else {
            // For card payments, this should not be called directly
            // Use placeOrderWithPayment() instead after payment confirmation
            Toast.makeText(this, "Please complete card payment first", Toast.LENGTH_SHORT).show()
        }
    }

    // Place order with payment details (for card payments after payment intent success)
    private fun placeOrderWithPayment() {
        // Show "Completing order" loader
        showLoader("Completing order")
        
        // Convert amount from pounds to cents
        val amountInCents = (totalAmount * 100).toInt()
        
        // Ensure payment_intent_id is not null (use empty string if null)
        val piId = paymentIntentId ?: ""
        
            val orderRequest = OrderRequest(
            user_company_address_id = addressId,
            delivery_method_id = deliveryMethodId,
            coupon_discount = couponDiscount.takeIf { it > 0 },
            wallet_discount = walletDeduction.takeIf { it > 0 },
            delivery_instructions = deliveryInstructions.takeIf { it.isNotEmpty() },
            coupon_id = couponId.takeIf { it.isNotEmpty() },
            pay_by_bank = false,
            payment_status = "paid",
            payment_provider = "stripe",
            payment_reference = piId,
            payment_intent_id = piId,
            payment_method_id = paymentMethodId,
            customer_id = customerId,
            currency = "gbp",
            amount = amountInCents,
            status = "succeeded",
            receipt_email = receiptEmail,
            description = paymentDescription,
            metadata = paymentMetadata,
            raw_payload = null // Can be set if raw payment response is needed
        )
            orderPlaceViewModel.orderPlace(token, orderRequest)
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
        paymentIntentViewModel = ViewModelProvider(this)[PaymentIntentViewModel::class.java]
        orderPlaceViewModel = ViewModelProvider(this)[OrderPlaceViewModel::class.java]

        // Retrieve token from SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences.getString("token", "").orEmpty()}"

        // Fetch bank details for payment
        bankDetailViewModel.bankDetails(token)
        
        // Fetch Stripe config for card payment (only for publishable key, not client_secret)
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
                    stripeSecretKey = config.secret_key
                    // Note: We no longer use client_secret from StripeConfig
                    // Instead, we create PaymentIntent via /api/payment/create-intent API
                    
                    // Log configuration for debugging
                    Log.d("StripePayment", "Stripe Config received:")
                    Log.d("StripePayment", "  Provider: ${config.provider}")
                    Log.d("StripePayment", "  Test Mode: ${config.test_mode}")
                    Log.d("StripePayment", "  Publishable Key: ${config.publishable_key?.take(20)}...")
                    Log.d("StripePayment", "  Secret Key: ${config.secret_key?.take(20) ?: "null"}...")
                    
                    // Initialize Stripe with publishable key
                    stripePublishableKey?.let { key ->
                        try {
                            // Initialize PaymentConfiguration (safe to call multiple times)
                            PaymentConfiguration.init(applicationContext, key)
                            
                            // Initialize Stripe instance
                            initializeStripe()
                            
                            Log.d("StripePayment", "Stripe configured successfully")
                                Log.d("StripePayment", "Publishable Key: ${key.take(20)}...")
                                Log.d("StripePayment", "Test Mode: ${config.test_mode}")
                        } catch (e: Exception) {
                            Log.e("StripePayment", "Failed to configure Stripe", e)
                            hasStripeConfig = false
                            Toast.makeText(this, "Failed to configure payment system: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                stripePublishableKey = null
                stripeSecretKey = null
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
            stripeSecretKey = null
            // Don't set paymentSheet to null - keep it initialized for potential retry
            Log.e("StripePayment", "Stripe config API error: $error")
            Toast.makeText(this, "Failed to load payment configuration: $error", Toast.LENGTH_SHORT).show()
            updatePaymentMethodVisibility()
        }

        // Observe network failure
        stripeConfigViewModel.onFailure.observe(this) { throwable ->
            hasStripeConfig = false
            stripePublishableKey = null
            stripeSecretKey = null
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
            binding.payByBankLayout.visibility = View.VISIBLE
            // If bank is available and card is not, select bank by default
            if (!hasStripeConfig) {
                selectPaymentMethod("bank")
            }
        } else {
            binding.payByBankLayout.visibility = View.GONE
            binding.bankPaymentLayout.visibility = View.GONE
            // If bank is not available and bank was selected, switch to card if available
            if (selectedPaymentMethod == "bank" && hasStripeConfig) {
                selectPaymentMethod("card")
            }
        }

        // Show/hide Pay by Card option
        if (hasStripeConfig) {
            binding.payByCardLayout.visibility = View.VISIBLE
            binding.completePaymentButton.isEnabled = true
            // If card is available and bank is not, select card by default
            if (!hasBankDetails) {
                selectPaymentMethod("card")
            }
        } else {
            binding.payByCardLayout.visibility = View.GONE
            binding.cardPaymentLayout.visibility = View.GONE
            binding.completePaymentButton.isEnabled = false
            // Hide card input fields when card option is not available
            binding.cardholderNameLayout.visibility = View.GONE
            binding.cardNumberLayout.visibility = View.GONE
            binding.expiresLayout.visibility = View.GONE
            binding.cvvLayout.visibility = View.GONE
            // If card is not available and card was selected, switch to bank if available
            if (selectedPaymentMethod == "card" && hasBankDetails) {
                selectPaymentMethod("bank")
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

    
    // Show loader with custom message
    private fun showLoader(message: String) {
        binding.loadingText.text = message
        binding.loadingOverlayBackground.visibility = View.VISIBLE
        binding.loadingOverlay.visibility = View.VISIBLE
    }
    
    // Hide loader
    private fun hideLoader() {
        binding.loadingOverlayBackground.visibility = View.GONE
        binding.loadingOverlay.visibility = View.GONE
    }
    
    // Create PaymentIntent by calling the API
    private fun createPaymentIntent() {
        // Show "Processing payment" loader
        showLoader("Processing payment")
        
        // Amount should be in main currency unit (pounds for GBP)
        // totalAmount is already in pounds (e.g., 257.87)
        
        // Get user details from SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        val userDetailsJson = preferences.getString("userResponse", "")
        var userEmail: String? = null
        var userId: String? = null
        
        try {
            if (!userDetailsJson.isNullOrEmpty()) {
                val gson = com.google.gson.Gson()
                val loginResponse = gson.fromJson(userDetailsJson, LoginResponse::class.java)
                userEmail = loginResponse.user_detail?.email
                userId = loginResponse.user_detail?.id?.toString()
            }
        } catch (e: Exception) {
            Log.e("PaymentIntent", "Failed to parse user details", e)
        }
        
        // Generate dummy orderId (format: ORD-123)
        val dummyOrderId = "ORD-${System.currentTimeMillis().toString().takeLast(6)}"
        
        // Prepare metadata with orderId and userId
        val metadata = mutableMapOf<String, String>()
        metadata["orderId"] = dummyOrderId
        if (userId != null) {
            metadata["userId"] = userId
        }
        
        // Store payment details for order request
        customerId = null // Can be set if customer ID is available
        receiptEmail = userEmail
        paymentDescription = "Order $dummyOrderId"
        // Convert metadata map to JSON string
        paymentMetadata = try {
            val gson = com.google.gson.Gson()
            gson.toJson(metadata)
        } catch (e: Exception) {
            null
        }
        
        // Create PaymentIntent request
        val paymentIntentRequest = PaymentIntentRequest(
            amount = totalAmount, // Amount in pounds (e.g., 257.87)
            currency = "gbp",
            automatic_payment_methods = AutomaticPaymentMethods(enabled = true),
            metadata = metadata,
            description = paymentDescription,
            customerId = customerId,
            receipt_email = receiptEmail
        )
        
        Log.d("PaymentIntent", "Creating PaymentIntent with amount: $totalAmount, metadata: orderId=$dummyOrderId, userId=$userId")
        
        // Call API to create PaymentIntent
        paymentIntentViewModel.createPaymentIntent(token, paymentIntentRequest)
    }
    
    // Observe LiveData for Payment Intent creation
    private fun observePaymentIntent() {
        // Observe API response for payment intent
        paymentIntentViewModel.paymentIntentResponse.observe(this) { response ->
            if (response.client_secret != null) {
                stripeClientSecret = response.client_secret
                
                // Extract Payment Intent ID from client_secret (format: pi_xxx_secret_yyy)
                paymentIntentId = response.payment_intent_id
                    ?: stripeClientSecret?.substringBefore("_secret_")
                
                Log.d("StripePayment", "PaymentIntent created: $paymentIntentId")
                Log.d("StripePayment", "Client Secret: ${stripeClientSecret?.take(30)}...")
                
                // Step 2: Confirm payment with card details from UI
                confirmPaymentWithCard()
            } else {
                hideLoader()
                Toast.makeText(this, "Failed to create payment intent", Toast.LENGTH_SHORT).show()
                Log.e("StripePayment", "PaymentIntent creation failed: no client_secret")
            }
        }

        // Observe loading state
        paymentIntentViewModel.isLoading.observe(this) { isLoading ->
            // Keep loader visible with "Processing payment" message
            binding.completePaymentButton.isEnabled = !isLoading && hasStripeConfig
        }

        // Observe API error
        paymentIntentViewModel.apiError.observe(this) { error ->
            hideLoader()
            Log.e("StripePayment", "PaymentIntent API error: $error")
            Toast.makeText(this, "Failed to create payment intent: $error", Toast.LENGTH_SHORT).show()
            binding.completePaymentButton.isEnabled = hasStripeConfig
        }

        // Observe network failure
        paymentIntentViewModel.onFailure.observe(this) { throwable ->
            hideLoader()
            Log.e("StripePayment", "PaymentIntent network failure", throwable)
            Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show()
            binding.completePaymentButton.isEnabled = hasStripeConfig
        }
    }

    // Validate card input fields
    private fun validateCardInputs(): Boolean {
        val cardholderName = binding.cardholderName.text?.toString()?.trim() ?: ""
        val cardNumber = binding.cardNumber.text?.toString()?.trim()?.replace(" ", "") ?: ""
        val expires = binding.expires.text?.toString()?.trim() ?: ""
        val cvv = binding.cvv.text?.toString()?.trim() ?: ""
        
        if (cardholderName.isEmpty()) {
            binding.cardholderName.error = "Cardholder name is required"
            return false
        }
        
        // Card number should be 16 digits (after removing spaces)
        if (cardNumber.isEmpty() || cardNumber.length != 16) {
            binding.cardNumber.error = "Valid 16-digit card number is required"
            return false
        }
        
        // Validate expiry format (MM/YY)
        if (expires.isEmpty() || !expires.matches(Regex("\\d{2}/\\d{2}"))) {
            binding.expires.error = "Valid expiry date (MM/YY) is required"
            return false
        }
        
        // CVV should be exactly 3 digits
        if (cvv.isEmpty() || cvv.length != 3) {
            binding.cvv.error = "Valid 3-digit CVV is required"
            return false
        }
        
        return true
    }
    
    // Confirm payment with card details from UI
    private fun confirmPaymentWithCard() {
        // Show "Confirming payment" loader
        showLoader("Confirming payment")
        
        val cardNumber = binding.cardNumber.text?.toString()?.trim()?.replace(" ", "") ?: ""
        val expires = binding.expires.text?.toString()?.trim() ?: ""
        val cvv = binding.cvv.text?.toString()?.trim() ?: ""
        val cardholderName = binding.cardholderName.text?.toString()?.trim() ?: ""
        
        if (stripeClientSecret == null || stripe == null) {
            hideLoader()
            Toast.makeText(this, "Payment configuration error", Toast.LENGTH_SHORT).show()
                return
            }

        try {
            // Parse expiry date (MM/YY format)
            val expiryParts = expires.split("/")
            if (expiryParts.size != 2) {
                Toast.makeText(this, "Invalid expiry date format", Toast.LENGTH_SHORT).show()
                return
            }
            
            val expiryMonth = expiryParts[0].toIntOrNull()
            val expiryYear = expiryParts[1].toIntOrNull()
            
            if (expiryMonth == null || expiryYear == null || expiryMonth < 1 || expiryMonth > 12) {
                Toast.makeText(this, "Invalid expiry date", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Create PaymentMethodCreateParams from card details using Builder
            val cardParams = PaymentMethodCreateParams.Card.Builder()
                .setNumber(cardNumber)
                .setExpiryMonth(expiryMonth)
                .setExpiryYear(2000 + expiryYear) // Convert YY to YYYY
                .setCvc(cvv)
                .build()

            // Create payment method params
            val paymentMethodParams = PaymentMethodCreateParams.create(cardParams)
            
            // Create ConfirmPaymentIntentParams
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodParams,
                clientSecret = stripeClientSecret!!
            )
            
            // Confirm payment - this will handle 3DS if required
            stripe?.confirmPayment(this, confirmParams)
            
        } catch (e: Exception) {
            Log.e("StripePayment", "Error confirming payment", e)
            Toast.makeText(this, "Payment error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Override onActivityResult to handle 3DS authentication
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle Stripe payment result using PaymentIntentResult
        stripe?.onPaymentResult(
            requestCode,
            data,
            object : ApiResultCallback<PaymentIntentResult> {
                override fun onSuccess(result: PaymentIntentResult) {
                    val status = result.intent.status
                    
                    when (status) {
                        StripeIntent.Status.Succeeded -> {
                            Log.d("StripePayment", "Payment successful")
                            paymentIntentId = result.intent.id
                            paymentMethodId = result.intent.paymentMethodId
                            // Show "Finalising order" loader
                            showLoader("Finalising order")
                            handlePaymentSuccess()
                        }
                        StripeIntent.Status.RequiresPaymentMethod -> {
                            hideLoader()
                            Log.e("StripePayment", "Payment failed: requires payment method")
                            Toast.makeText(this@PaymentActivity, "Payment failed: Please try a different payment method", Toast.LENGTH_LONG).show()
                            handlePaymentFailure()
                        }
                        StripeIntent.Status.RequiresAction -> {
                            Log.d("StripePayment", "Payment requires action (3DS)")
                            // Keep "Confirming payment" loader visible during 3DS
                            // 3DS authentication should be handled automatically by Stripe SDK
                        }
                        else -> {
                            hideLoader()
                            val errorMessage = result.intent.lastPaymentError?.message
                                ?: "Payment failed with status: $status"
                            Log.e("StripePayment", errorMessage)
                            Toast.makeText(this@PaymentActivity, errorMessage, Toast.LENGTH_LONG).show()
                            handlePaymentFailure()
                        }
                    }
                }
                
                override fun onError(e: Exception) {
                    hideLoader()
                    Log.e("StripePayment", "Stripe error", e)
                    Toast.makeText(this@PaymentActivity, "Stripe error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    handlePaymentFailure()
                }
            }
        )
    }
    
    // Handle payment success
    private fun handlePaymentSuccess() {
        Log.d("StripePayment", "Payment Intent ID: $paymentIntentId")
        Log.d("StripePayment", "Payment Method ID: $paymentMethodId")
        
        // Place order with payment details
        placeOrderWithPayment()
    }
    
    // Handle payment failure
    private fun handlePaymentFailure() {
        hideLoader()
        // Payment failure is already handled with Toast in the callback
    }
    
    // Extract PaymentIntent from a Bundle recursively
    private fun extractPaymentIntentFromBundle(bundle: Bundle): PaymentIntent? {
        try {
            Log.d("StripePayment", "Extracting from Bundle with keys: ${bundle.keySet()}")
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                Log.d("StripePayment", "Checking key: $key, type: ${value?.javaClass?.name}")
                
                if (value is PaymentIntent) {
                    Log.d("StripePayment", "Found PaymentIntent directly")
                    return value
                }
                // Recursively check nested Bundles
                if (value is Bundle) {
                    val nested = extractPaymentIntentFromBundle(value)
                    if (nested != null) return nested
                }
                // Check if it's a PaymentIntentResult
                if (value != null && value.javaClass.name.contains("PaymentIntentResult")) {
                    try {
                        Log.d("StripePayment", "Found PaymentIntentResult, extracting PaymentIntent")
                        val intentMethod = value.javaClass.getMethod("getIntent")
                        val paymentIntent = intentMethod.invoke(value) as? PaymentIntent
                        if (paymentIntent != null) {
                            Log.d("StripePayment", "Successfully extracted PaymentIntent from PaymentIntentResult")
                            return paymentIntent
                        }
                    } catch (e: Exception) {
                        Log.e("StripePayment", "Error extracting from PaymentIntentResult", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StripePayment", "Error extracting from Bundle", e)
        }
        return null
    }
    
    // Extract PaymentIntent directly from Intent extras as fallback
    private fun extractPaymentIntentFromIntent(data: Intent?) {
        try {
            if (data == null) {
                Toast.makeText(this, "Payment result data is null", Toast.LENGTH_LONG).show()
                return
            }
            
            // Try common Stripe result keys
            val possibleKeys = listOf(
                "com.stripe.android.payments.result",
                "stripe_payment_intent_result",
                "payment_intent"
            )
            
            var paymentIntent: PaymentIntent? = null
            
            // First, try to get PaymentIntent as Parcelable
            for (key in possibleKeys) {
                @Suppress("DEPRECATION")
                val parcelable = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelableExtra(key, PaymentIntent::class.java)
                } else {
                    data.getParcelableExtra<PaymentIntent>(key)
                }
                if (parcelable != null) {
                    paymentIntent = parcelable
                    break
                }
            }
            
            // If not found, check all extras, especially "extra_args" which Stripe uses
            if (paymentIntent == null) {
                val extras = data.extras
                if (extras != null) {
                    // First, check "extra_args" specifically (Stripe often uses this)
                    val extraArgs = extras.get("extra_args")
                    if (extraArgs is Bundle) {
                        paymentIntent = extractPaymentIntentFromBundle(extraArgs)
                    }
                    
                    // If still not found, check all extras recursively
                    if (paymentIntent == null) {
                        for (key in extras.keySet()) {
                            val value = extras.get(key)
                            if (value is PaymentIntent) {
                                paymentIntent = value
                                break
                            }
                            // Check if it's a Bundle containing PaymentIntent
                            if (value is Bundle) {
                                paymentIntent = extractPaymentIntentFromBundle(value)
                                if (paymentIntent != null) break
                            }
                            // Check if it's a PaymentIntentResult (using reflection)
                            if (value != null && value.javaClass.name.contains("PaymentIntentResult")) {
                                try {
                                    val intentMethod = value.javaClass.getMethod("getIntent")
                                    paymentIntent = intentMethod.invoke(value) as? PaymentIntent
                                    if (paymentIntent != null) break
                                } catch (e: Exception) {
                                    Log.d("StripePayment", "Could not extract from PaymentIntentResult: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
            
            if (paymentIntent != null) {
                handlePaymentResult(paymentIntent)
            } else {
                Toast.makeText(this, "Could not extract payment result", Toast.LENGTH_LONG).show()
                Log.e("StripePayment", "Could not find PaymentIntent in Intent. Extras keys: ${data.extras?.keySet()}")
            }
        } catch (e: Exception) {
            Log.e("StripePayment", "Error extracting PaymentIntent from Intent", e)
            Toast.makeText(this, "Payment processing error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    // Handle payment result from Stripe (including 3DS)
    private fun handlePaymentResult(paymentIntent: PaymentIntent) {
        // Check payment status using string comparison for reliability
        val statusString = paymentIntent.status?.toString() ?: ""
        
        when {
            statusString.contains("Succeeded", ignoreCase = true) || 
            paymentIntent.status == com.stripe.android.model.StripeIntent.Status.Succeeded -> {
                // Payment succeeded
                paymentIntentId = paymentIntent.id
                paymentMethodId = paymentIntent.paymentMethodId
                
                Log.d("StripePayment", "Payment successful")
                Log.d("StripePayment", "Payment Intent ID: $paymentIntentId")
                Log.d("StripePayment", "Payment Method ID: $paymentMethodId")
                
                // Place order with payment details
                placeOrderWithPayment()
            }
            statusString.contains("RequiresAction", ignoreCase = true) -> {
                // 3DS authentication required - handled automatically by Stripe SDK
                Log.d("StripePayment", "3DS authentication required")
            }
            statusString.contains("RequiresPaymentMethod", ignoreCase = true) -> {
                Toast.makeText(this, "Payment failed: Please try a different payment method", Toast.LENGTH_LONG).show()
                Log.e("StripePayment", "Payment requires a different payment method")
            }
            else -> {
                val errorMessage = paymentIntent.lastPaymentError?.message
                    ?: "Payment failed"
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                Log.e("StripePayment", "Payment failed: $errorMessage, Status: $statusString")
            }
        }
    }

    // Observe LiveData for Order Placement
    private fun observeOrderPlace() {
        orderPlaceViewModel.orderPlaceResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let {
                // Hide loading overlay and background on success
                hideLoader()
                // If success, clear cart and navigate to success screen
                clearCartAndNavigateToSuccess()
            } ?: run {
                // Hide loading overlay and background on failure
                hideLoader()
                // Handle failure case
            }
        }

        // Observe loading state during API call
        orderPlaceViewModel.isLoading.observe(this) { isLoading ->
            // Keep loader visible with "Completing order" message during order placement
            // Disable/Enable button depending on loading
            binding.authorizePaymentButton.isEnabled = !isLoading
            binding.completePaymentButton.isEnabled = !isLoading && hasStripeConfig
        }

        // Observe API error
        orderPlaceViewModel.apiError.observe(this) { error ->
            // Hide loading overlay and background on error
            hideLoader()
            // Handle API error
            Log.e("OrderPlace", "Order placement API error: $error")
            Toast.makeText(this, "Order failed: $error", Toast.LENGTH_SHORT).show()
            binding.authorizePaymentButton.isEnabled = true
            binding.completePaymentButton.isEnabled = hasStripeConfig
        }

        // Observe failure case
        orderPlaceViewModel.onFailure.observe(this) { throwable ->
            // Hide loading overlay and background on failure
            hideLoader()
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