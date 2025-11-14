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

        // Observe bank details API response
        observeBankDetails()

        // Observe Stripe config API response
        observeStripeConfig()

        // Observe order placement API response
        observeOrderPlace()
    }

    // Setup toggle between bank transfer and card payment
    private fun setupPaymentMethodSelection() {
        // Show bank layout when "Pay by Bank" is checked
        binding.payByBank.setOnCheckedChangeListener { _, isChecked ->
            binding.bankPaymentLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.cardPaymentLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // Show card layout when "Pay by Card" is checked
        binding.payByCard.setOnCheckedChangeListener { _, isChecked ->
            binding.bankPaymentLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.cardPaymentLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
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
            if (hasStripeConfig && stripeClientSecret != null && stripePublishableKey != null && paymentSheet != null) {
                presentPaymentSheet()
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

        // Hide manual card input fields since we're using Stripe PaymentSheet
        binding.cardholderNameLayout.visibility = View.GONE
        binding.cardNumberLayout.visibility = View.GONE
        binding.expiresLayout.visibility = View.GONE
        binding.cvvLayout.visibility = View.GONE

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
                             response.stripe_config != null && 
                             !response.stripe_config.publishable_key.isNullOrEmpty() &&
                             !response.stripe_config.client_secret.isNullOrEmpty()
            
            if (hasStripeConfig) {
                response?.stripe_config?.let { config ->
                    stripePublishableKey = config.publishable_key
                    stripeClientSecret = config.client_secret
                    
                    // Initialize Stripe with publishable key
                    stripePublishableKey?.let { key ->
                        try {
                            // Initialize PaymentConfiguration (safe to call multiple times)
                            PaymentConfiguration.init(applicationContext, key)
                            
                            // Initialize PaymentSheet after PaymentConfiguration is set
                            // Create new instance to ensure it's properly initialized
                            paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
                            
                            Log.d("StripePayment", "Stripe PaymentSheet initialized successfully")
                            Log.d("StripePayment", "Publishable Key: ${key.take(20)}...")
                            Log.d("StripePayment", "Client Secret: ${config.client_secret?.take(20) ?: "null"}...")
                        } catch (e: Exception) {
                            Log.e("StripePayment", "Failed to initialize Stripe", e)
                            hasStripeConfig = false
                            paymentSheet = null
                            Toast.makeText(this, "Failed to initialize payment system: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                paymentSheet = null
                stripePublishableKey = null
                stripeClientSecret = null
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
            paymentSheet = null
            stripePublishableKey = null
            stripeClientSecret = null
            Log.e("StripePayment", "Stripe config API error: $error")
            Toast.makeText(this, "Failed to load payment configuration: $error", Toast.LENGTH_SHORT).show()
            updatePaymentMethodVisibility()
        }

        // Observe network failure
        stripeConfigViewModel.onFailure.observe(this) { throwable ->
            hasStripeConfig = false
            paymentSheet = null
            stripePublishableKey = null
            stripeClientSecret = null
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
            if (!hasStripeConfig && !binding.payByBank.isChecked) {
                binding.payByBank.isChecked = true
            }
        } else {
            binding.payByBank.visibility = View.GONE
            binding.bankPaymentLayout.visibility = View.GONE
        }
        
        // Show/hide Pay by Card option
        if (hasStripeConfig) {
            binding.payByCard.visibility = View.VISIBLE
            binding.completePaymentButton.isEnabled = true
            // If card is available and bank is not, select card by default
            if (!hasBankDetails && !binding.payByCard.isChecked) {
                binding.payByCard.isChecked = true
            }
        } else {
            binding.payByCard.visibility = View.GONE
            binding.cardPaymentLayout.visibility = View.GONE
            binding.completePaymentButton.isEnabled = false
        }
        
        // If neither option is available, show error
        if (!hasBankDetails && !hasStripeConfig) {
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
                // If success, clear cart and navigate to success screen
                clearCartAndNavigateToSuccess()
            } ?: run {
                // Handle failure case
            }
        }

        // Observe loading state during API call
        orderPlaceViewModel.isLoading.observe(this) { isLoading ->
            // Disable/Enable button depending on loading
//            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.authorizePaymentButton.isEnabled = !isLoading
            binding.completePaymentButton.isEnabled = !isLoading && hasStripeConfig
        }

        // Observe API error
        orderPlaceViewModel.apiError.observe(this) { error ->
            // Handle API error
            Log.e("OrderPlace", "Order placement API error: $error")
            Toast.makeText(this, "Order failed: $error", Toast.LENGTH_SHORT).show()
            binding.authorizePaymentButton.isEnabled = true
            binding.completePaymentButton.isEnabled = hasStripeConfig
        }

        // Observe failure case
        orderPlaceViewModel.onFailure.observe(this) { throwable ->
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