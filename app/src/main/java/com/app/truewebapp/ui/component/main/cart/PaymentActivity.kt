package com.app.truewebapp.ui.component.main.cart

// Import required Android and project-specific classes
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

// Activity responsible for handling payment process and placing an order
class PaymentActivity : AppCompatActivity() {

    // ViewBinding for accessing layout views safely
    private lateinit var binding: ActivityPaymentBinding

    // ViewModels for bank details and order placement
    private lateinit var bankDetailViewModel: BankDetailViewModel
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

        // Observe bank details API response
        observeBankDetails()

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

        // Complete Payment button → navigates to success screen directly
        binding.completePaymentButton.setOnClickListener {
            binding.completePaymentButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            navigateToOrderSuccess()
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

    // Initialize ViewModels and fetch bank details
    private fun initializeViewModels() {
        bankDetailViewModel = ViewModelProvider(this)[BankDetailViewModel::class.java]
        orderPlaceViewModel = ViewModelProvider(this)[OrderPlaceViewModel::class.java]

        // Retrieve token from SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences.getString("token", "").orEmpty()}"

        // Fetch bank details for payment
        bankDetailViewModel.bankDetails(token)
    }

    // Observe LiveData for Bank Details
    private fun observeBankDetails() {
        // Observe API response for bank details
        bankDetailViewModel.bankDetailResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let { bankDetail ->
                // Bind data to UI if response is successful
                binding.tvBankHolderName.text = bankDetail.bank_detail.company_name
                binding.tvBankName.text = bankDetail.bank_detail.bank_name
                binding.tvAccountNumber.text = bankDetail.bank_detail.account_number
                binding.tvSortCode.text = bankDetail.bank_detail.sort_code
            }
        }

        // Observe loading state
        bankDetailViewModel.isLoading.observe(this) { isLoading ->
            // Can show/hide loading indicator if needed
        }

        // Observe API error
        bankDetailViewModel.apiError.observe(this) { error ->
            // Handle error scenario
        }

        // Observe network failure
        bankDetailViewModel.onFailure.observe(this) { throwable ->
            // Handle throwable case
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
        }

        // Observe API error
        orderPlaceViewModel.apiError.observe(this) { error ->
            // Handle API error
        }

        // Observe failure case
        orderPlaceViewModel.onFailure.observe(this) { throwable ->
            // Handle throwable error
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