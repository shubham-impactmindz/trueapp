package com.app.truewebapp.ui.component.main.dashboard

import android.os.Bundle
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
            binding.tvTotal.text = "£ ${it.summary.subtotal}"
            binding.tvWalletDiscount.text = "£ ${it.summary.wallet_discount}"
            binding.tvCouponDiscount.text = "£ ${it.summary.coupon_discount}"
            binding.tvDelivery.text = "£ ${it.summary.delivery_cost}"
            binding.tvVat.text = "£ ${it.summary.vat}"
            binding.tvTotalPayment.text = "£ ${it.summary.payment_total}"

            adapter = ItemListAdapter(cdnURL)
            binding.orderItemsRecycler.layoutManager = LinearLayoutManager(this)
            binding.orderItemsRecycler.adapter = adapter
            adapter.setItems(it.items)
            if (it.payment_status.lowercase() == "pending"){
                binding.tvPaymentStatus.text = "Payment Pending"
            }
        }

        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}