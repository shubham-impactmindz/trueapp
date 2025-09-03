package com.app.truewebapp.ui.component.main.dashboard

// Importing required Android and Jetpack libraries
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.data.dto.order.Orders
import com.app.truewebapp.databinding.ActivityOrderDetailBinding

// Activity class to display order details
class OrderDetailActivity : AppCompatActivity() {

    // View binding reference for activity layout
    lateinit var binding: ActivityOrderDetailBinding

    // Adapter for displaying order items in RecyclerView
    lateinit var adapter: ItemListAdapter

    // Lifecycle method called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)

        // Set the inflated layout as the content view
        setContentView(binding.root)

        // Handle system UI insets (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Get the system bar insets (top and bottom padding)
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding to the view dynamically to avoid UI overlap
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            // Return the insets after applying padding
            insets
        }

        // Retrieve "order_data" object passed via Intent (Parcelable object)
        val order = intent.getParcelableExtra<Orders>("order_data")

        // Retrieve CDN URL string passed via Intent
        val cdnURL = intent.getStringExtra("cdnURL")

        // Execute logic only if order object is not null
        order?.let {
            // Populate order summary values into respective text views
            binding.tvUnitNo.text = it.units.toString()          // Number of units
            binding.tvSkuNo.text = it.skus.toString()            // Number of SKUs
            binding.tvDeliveryMethod.text = it.delivery.method   // Delivery method
            binding.tvAddress.text = it.delivery.address         // Delivery address

            // Safely extract summary fields with fallback values (0.0 if null)
            val subtotal = it.summary.subtotal ?: 0.0
            val walletDiscount = it.summary.wallet_discount ?: 0.0
            val couponDiscount = it.summary.coupon_discount ?: 0.0
            val deliveryCost = it.summary.delivery_cost ?: 0.0
            val vat = it.summary.vat ?: 0.0

            // Calculate total payment
            val totalPayment = (subtotal + vat + deliveryCost) - (walletDiscount + couponDiscount)

            // Set values to UI with formatting to 2 decimal places
            binding.tvTotal.text = "£${"%.2f".format(subtotal)}"
            binding.tvWalletDiscount.text = "£${"%.2f".format(walletDiscount)}"
            binding.tvCouponDiscount.text = "£${"%.2f".format(couponDiscount)}"
            binding.tvDelivery.text = "£${"%.2f".format(deliveryCost)}"
            binding.tvVat.text = "£${"%.2f".format(vat)}"
            binding.tvTotalPayment.text = "£${"%.2f".format(totalPayment)}"

            // Initialize adapter with CDN URL
            adapter = ItemListAdapter(cdnURL)

            // Set layout manager for RecyclerView (vertical list)
            binding.orderItemsRecycler.layoutManager = LinearLayoutManager(this)

            // Attach adapter to RecyclerView
            binding.orderItemsRecycler.adapter = adapter

            // Populate items inside RecyclerView using adapter
            adapter.setItems(it.items)

            // Update payment status text view based on order status
            if (it.payment_status.lowercase() == "pending") {
                binding.tvPaymentStatus.text = "Payment Pending"
            } else if (it.payment_status.lowercase() == "paid") {
                binding.tvPaymentStatus.text = "Paid"
            }
        }

        // Handle back button (UI click listener)
        binding.backLayout.setOnClickListener {
            // Provide haptic feedback on click
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag to ignore global settings
            )
            // Finish the activity and return to previous screen
            finish()
        }
    }
}