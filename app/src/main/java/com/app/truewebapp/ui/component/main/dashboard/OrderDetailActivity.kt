package com.app.truewebapp.ui.component.main.dashboard

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.data.dto.order.Orders
import com.app.truewebapp.databinding.ActivityOrderDetailBinding

class OrderDetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityOrderDetailBinding
    lateinit var adapter: ItemListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
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
        val order = intent.getParcelableExtra<Orders>("order_data")
        val cdnURL = intent.getStringExtra("cdnURL")
        order?.let {
            binding.tvUnitNo.text = it.units.toString()
            binding.tvSkuNo.text = it.skus.toString()
            binding.tvDeliveryMethod.text = it.delivery.method
            binding.tvAddress.text = it.delivery.address
            val subtotal = it.summary.subtotal ?: 0.0
            val walletDiscount = it.summary.wallet_discount ?: 0.0
            val couponDiscount = it.summary.coupon_discount ?: 0.0
            val deliveryCost = it.summary.delivery_cost ?: 0.0
            val vat = it.summary.vat ?: 0.0

            val totalPayment = (subtotal + vat + deliveryCost) - (walletDiscount + couponDiscount)

            binding.tvTotal.text = "£${"%.2f".format(subtotal)}"
            binding.tvWalletDiscount.text = "£${"%.2f".format(walletDiscount)}"
            binding.tvCouponDiscount.text = "£${"%.2f".format(couponDiscount)}"
            binding.tvDelivery.text = "£${"%.2f".format(deliveryCost)}"
            binding.tvVat.text = "£${"%.2f".format(vat)}"
            binding.tvTotalPayment.text = "£${"%.2f".format(totalPayment)}"

            adapter = ItemListAdapter(cdnURL)
            binding.orderItemsRecycler.layoutManager = LinearLayoutManager(this)
            binding.orderItemsRecycler.adapter = adapter
            adapter.setItems(it.items)
            if (it.payment_status.lowercase() == "pending"){
                binding.tvPaymentStatus.text = "Payment Pending"
            }else if(it.payment_status.lowercase() == "paid"){
                binding.tvPaymentStatus.text = "Paid"
            }
        }

        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            finish()
        }
    }
}