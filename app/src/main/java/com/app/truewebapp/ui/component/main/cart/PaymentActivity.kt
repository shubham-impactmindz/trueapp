package com.app.truewebapp.ui.component.main.cart

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

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var bankDetailViewModel: BankDetailViewModel
    private lateinit var orderPlaceViewModel: OrderPlaceViewModel
    private var token = ""
    private var deliveryMethodId = ""
    private var addressId = ""
    private var deliveryInstructions = ""
    private var totalAmount = 0.0
    private var couponDiscount = 0.0
    private var couponId = ""
    private var useWallet = false
    private var walletDeduction = 0.0
    private var originalTotalAmount = 0.0
    private var finalTotal = 0.0

    // Lazy initialization for cartDao
    private val cartDao by lazy { CartDatabase.getInstance(this).cartDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system bars insets (status + nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        initializeViewModels()
        setupPaymentMethodSelection()
        setupClickListeners()
        extractIntentData()
        updatePaymentUI()
        observeBankDetails()
        observeOrderPlace()
    }

    private fun setupPaymentMethodSelection() {
        binding.payByBank.setOnCheckedChangeListener { _, isChecked ->
            binding.bankPaymentLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.cardPaymentLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        binding.payByCard.setOnCheckedChangeListener { _, isChecked ->
            binding.bankPaymentLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.cardPaymentLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            finish()
        }

        binding.authorizePaymentButton.setOnClickListener {
            binding.authorizePaymentButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            placeOrder()
        }

        binding.completePaymentButton.setOnClickListener {
            binding.completePaymentButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            navigateToOrderSuccess()
        }
    }

    private fun extractIntentData() {
        deliveryMethodId = intent.getStringExtra("deliveryMethodId").orEmpty()
        addressId = intent.getStringExtra("addressId").orEmpty()
        // This is the amount after all deductions (final amount to pay)
        totalAmount = intent.getDoubleExtra("totalAmount", 0.0)
        // This is the original total before any deductions
        originalTotalAmount = intent.getDoubleExtra("originalCartTotal", 0.0)
        deliveryInstructions = intent.getStringExtra("deliveryInstructions").orEmpty()
        couponId = intent.getStringExtra("couponId").orEmpty()
        couponDiscount = intent.getDoubleExtra("couponDiscountAmount",0.0)
        useWallet = intent.getBooleanExtra("useWallet", false)
        walletDeduction = intent.getDoubleExtra("walletDeductionAmount", 0.0)

        Log.d("PaymentDebug", "Original Total: $originalTotalAmount")
        Log.d("PaymentDebug", "Wallet Deduction: $walletDeduction")
        Log.d("PaymentDebug", "Final Amount: $totalAmount")
    }

    private fun updatePaymentUI() {
        // Show the final amount to pay (after all deductions)
        val formattedAmount = formatCurrency(totalAmount)
        binding.authorizePaymentButton.text = "Proceed Payment - $formattedAmount"

        // Show wallet deduction information if applicable
        if (useWallet && walletDeduction > 0) {
            binding.walletDeductionLayout.visibility = View.VISIBLE
            binding.tvWalletDeduction.text = formatCurrency(walletDeduction)
            binding.tvOriginalAmount.text = formatCurrency(originalTotalAmount)
            // Show the remaining amount to pay
            binding.tvFinalAmount.text = formattedAmount
        } else {
            binding.walletDeductionLayout.visibility = View.GONE
        }
    }

    private fun placeOrder() {
        // Clean coupon discount amount (remove currency symbol if present)

        val orderRequest = OrderRequest(
            wallet_discount = walletDeduction.toString(),
            coupon_discount = couponDiscount.toString(),
            user_company_address_id = addressId,
            delivery_method_id = deliveryMethodId,
            delivery_instructions = deliveryInstructions,
            couponId
        )

        orderPlaceViewModel.orderPlace(token, orderRequest)
    }

    private fun navigateToOrderSuccess() {
        val intent = Intent(this, OrderSuccessActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initializeViewModels() {
        bankDetailViewModel = ViewModelProvider(this)[BankDetailViewModel::class.java]
        orderPlaceViewModel = ViewModelProvider(this)[OrderPlaceViewModel::class.java]
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences.getString("token", "").orEmpty()}"
        bankDetailViewModel.bankDetails(token)
    }

    private fun observeBankDetails() {
        bankDetailViewModel.bankDetailResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let { bankDetail ->
                binding.tvBankHolderName.text = bankDetail.bank_detail.company_name
                binding.tvBankName.text = bankDetail.bank_detail.bank_name
                binding.tvAccountNumber.text = bankDetail.bank_detail.account_number
                binding.tvSortCode.text = bankDetail.bank_detail.sort_code
            }
        }

        bankDetailViewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
        }

        bankDetailViewModel.apiError.observe(this) { error ->
            // Handle API errors
        }

        bankDetailViewModel.onFailure.observe(this) { throwable ->
            // Handle failures
        }
    }

    private fun observeOrderPlace() {
        orderPlaceViewModel.orderPlaceResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let {
                clearCartAndNavigateToSuccess()
            } ?: run {
                // Handle order placement failure
            }
        }

        orderPlaceViewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator
//            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.authorizePaymentButton.isEnabled = !isLoading
        }

        orderPlaceViewModel.apiError.observe(this) { error ->
            // Show error message
        }

        orderPlaceViewModel.onFailure.observe(this) { throwable ->
            // Show error message
        }
    }

    private fun clearCartAndNavigateToSuccess() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                cartDao.clearCart()
            }
            navigateToOrderSuccess()
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.UK).format(amount)
    }
}