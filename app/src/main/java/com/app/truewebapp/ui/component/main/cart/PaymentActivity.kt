package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.os.Bundle
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

class PaymentActivity : AppCompatActivity() {

    lateinit var binding: ActivityPaymentBinding
    private lateinit var bankDetailViewModel: BankDetailViewModel
    private lateinit var orderPlaceViewModel: OrderPlaceViewModel
    private var token = ""
    private var deliveryMethodId = ""
    private var addressId = ""
    private var deliveryInstructions = ""
    private var totalAmount = ""
    private var couponDiscount = ""
    private var couponId = ""
    // Lazy initialization for cartDao, ensures context is available
    private val cartDao by lazy { let { CartDatabase.getInstance(it).cartDao() } }

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


        binding.payByBank.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.bankPaymentLayout.visibility = View.VISIBLE
                binding.cardPaymentLayout.visibility = View.GONE
            }
        }

        binding.backLayout.setOnClickListener {
            finish()
        }

        binding.payByCard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.bankPaymentLayout.visibility = View.GONE
                binding.cardPaymentLayout.visibility = View.VISIBLE
            }
        }

        binding.authorizePaymentButton.setOnClickListener {
            if (couponDiscount.contains("£")){
                orderPlaceViewModel.orderPlace(token, OrderRequest("0.0",couponDiscount.split(" ")[1],addressId,deliveryMethodId, deliveryInstructions,couponId))
            } else{
                orderPlaceViewModel.orderPlace(token, OrderRequest("0.0",couponDiscount,addressId,deliveryMethodId, deliveryInstructions,couponId))

            }
        }
        binding.completePaymentButton.setOnClickListener {
            val intent = Intent(this, OrderSuccessActivity::class.java)
            startActivity(intent)
        }
        initializeViewModels()
        observeBankDetails()
        observeOrderPlace()
        deliveryMethodId = intent.getStringExtra("deliveryMethodId")?: ""
        addressId = intent.getStringExtra("addressId")?: ""
        totalAmount = intent.getStringExtra("totalAmount") ?: "0.0"
        deliveryInstructions = intent.getStringExtra("deliveryInstructions") ?: ""
        couponId = intent.getStringExtra("couponId") ?: ""
        couponDiscount = intent.getStringExtra("couponDiscount") ?: "0.0"
        binding.authorizePaymentButton.text = "Proceed Payment - ${totalAmount}"
    }

    private fun initializeViewModels() {
        bankDetailViewModel = ViewModelProvider(this)[BankDetailViewModel::class.java]
        orderPlaceViewModel = ViewModelProvider(this)[OrderPlaceViewModel::class.java]
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"
        bankDetailViewModel.bankDetails(token)
    }

    private fun observeBankDetails() {
        bankDetailViewModel.bankDetailResponse.observe(this) { response ->
            response?.let {

                if (it.status) {
                    binding.tvBankHolderName.text = it.bank_detail.account_holder_name
                    binding.tvAccountNumber.text = it.bank_detail.account_number
                    binding.tvSortCode.text = it.bank_detail.sort_code

                } else {

                }
            }
        }

        bankDetailViewModel.isLoading.observe(this) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {

            }
        }

        bankDetailViewModel.apiError.observe(this) {

        }

        bankDetailViewModel.onFailure.observe(this) {

        }
    }

    private fun observeOrderPlace() {
        orderPlaceViewModel.orderPlaceResponse.observe(this) { response ->
            response?.let {

                if (it.status) {

                    lifecycleScope.launch {
                        // Step 1: Fetch cart items once from DB
                        val cartItems = withContext(Dispatchers.IO) {
                            cartDao.clearCart()
                        }
                    }


                    val intent = Intent(this, OrderSuccessActivity::class.java)
                    startActivity(intent)


                } else {

                }
            }
        }

        orderPlaceViewModel.isLoading.observe(this) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {

            }
        }

        orderPlaceViewModel.apiError.observe(this) {

        }

        orderPlaceViewModel.onFailure.observe(this) {

        }
    }
}
