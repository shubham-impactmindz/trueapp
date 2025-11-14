package com.app.truewebapp.ui.component.main.dashboard

// Importing required Android and Jetpack libraries
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.bank.BankDetail
import com.app.truewebapp.data.dto.order.Orders
import com.app.truewebapp.databinding.ActivityOrderDetailBinding
import com.app.truewebapp.ui.viewmodel.BankDetailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

// Activity class to display order details
class OrderDetailActivity : AppCompatActivity() {

    // View binding reference for activity layout
    lateinit var binding: ActivityOrderDetailBinding

    // Adapter for displaying order items in RecyclerView
    lateinit var adapter: ItemListAdapter
    
    // ViewModel for bank details
    private lateinit var bankDetailViewModel: BankDetailViewModel
    
    // Token for API calls
    private var token = ""
    
    // Order data
    private var order: Orders? = null

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
        order = intent.getParcelableExtra<Orders>("order_data")

        // Retrieve CDN URL string passed via Intent
        val cdnURL = intent.getStringExtra("cdnURL")
        
        // Initialize ViewModel and token
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences.getString("token", "").orEmpty()}"
        bankDetailViewModel = ViewModelProvider(this)[BankDetailViewModel::class.java]
        
        // Observe bank details
        observeBankDetails()

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
            binding.tvWalletDiscount.text = "- £${"%.2f".format(walletDiscount)}"
            binding.tvCouponDiscount.text = "- £${"%.2f".format(couponDiscount)}"
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
                binding.tvPaymentStatus.text = "Pay now"
                // Add click listener for Pay Now button
                binding.tvPaymentStatus.setOnClickListener {
                    binding.tvPaymentStatus.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                    // Fetch and show bank details
                    bankDetailViewModel.bankDetails(token)
                }
            } else if (it.payment_status.lowercase() == "paid") {
                binding.tvPaymentStatus.text = "Download Invoice"
                // Store invoice PDF URL for use in click listener
                val invoicePdfUrl = it.invoice_pdf
                // Add click listener for Download Invoice button
                binding.tvPaymentStatus.setOnClickListener {
                    binding.tvPaymentStatus.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                    // Download invoice PDF
                    downloadInvoice(invoicePdfUrl)
                }
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
    
    // Observe bank details from ViewModel
    private fun observeBankDetails() {
        bankDetailViewModel.bankDetailResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let { bankDetail ->
                // Show bank details popup
                showBankDetailsPopup(bankDetail.bank_detail)
            }
        }
        
        bankDetailViewModel.apiError.observe(this) { error ->
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
        
        bankDetailViewModel.onFailure.observe(this) { throwable ->
            Toast.makeText(this, "Network Error: ${throwable?.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Show bank details in popup dialog
    private fun showBankDetailsPopup(bankDetail: BankDetail) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bank_details, null)
        
        val tvCompanyName = dialogView.findViewById<TextView>(R.id.tvCompanyName)
        val tvBankName = dialogView.findViewById<TextView>(R.id.tvBankName)
        val tvAccountNumber = dialogView.findViewById<TextView>(R.id.tvAccountNumber)
        val tvSortCode = dialogView.findViewById<TextView>(R.id.tvSortCode)
        
        tvCompanyName.text = bankDetail.company_name
        tvBankName.text = bankDetail.bank_name
        tvAccountNumber.text = bankDetail.account_number
        tvSortCode.text = bankDetail.sort_code
        
        bankDetail.note?.let {
            dialogView.findViewById<TextView>(R.id.tvNote).text = it
            dialogView.findViewById<View>(R.id.noteLayout).visibility = View.VISIBLE
        } ?: run {
            dialogView.findViewById<View>(R.id.noteLayout).visibility = View.GONE
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Handle close button click
        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        
        // Setup copy functionality
        setupCopyButton(dialogView, R.id.btnCopyCompanyName, tvCompanyName.text.toString(), "Bank Holder Name")
        setupCopyButton(dialogView, R.id.btnCopyBankName, tvBankName.text.toString(), "Bank Name")
        setupCopyButton(dialogView, R.id.btnCopyAccountNumber, tvAccountNumber.text.toString(), "Account Number")
        setupCopyButton(dialogView, R.id.btnCopySortCode, tvSortCode.text.toString(), "Sort Code")

        
        dialog.show()
    }
    
    // Helper function to setup copy button functionality
    private fun setupCopyButton(dialogView: View, buttonId: Int, textToCopy: String, label: String) {
        dialogView.findViewById<ImageView>(buttonId).setOnClickListener {
            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            copyToClipboard(textToCopy, label)
        }
    }
    
    // Copy text to clipboard
    private fun copyToClipboard(text: String, label: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    // Download invoice PDF
    private fun downloadInvoice(invoicePdfUrl: String?) {
        if (invoicePdfUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Invoice PDF not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Download PDF in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(invoicePdfUrl)
                val connection = url.openConnection()
                connection.connect()
                
                val inputStream = connection.getInputStream()
                
                // For Android 10+ (API 29+), use MediaStore or Downloads directory
                val downloadsDir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10+ uses scoped storage
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) 
                        ?: getFilesDir()
                } else {
                    // Android 9 and below
                    if (ContextCompat.checkSelfPermission(this@OrderDetailActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                        != PackageManager.PERMISSION_GRANTED) {
                        withContext(Dispatchers.Main) {
                            ActivityCompat.requestPermissions(
                                this@OrderDetailActivity,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                1001
                            )
                        }
                        return@launch
                    }
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
                
                val fileName = "Invoice_${order?.order_id ?: "Order"}.pdf"
                val file = File(downloadsDir, fileName)
                
                FileOutputStream(file).use { output ->
                    inputStream.copyTo(output)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderDetailActivity, "Invoice downloaded successfully", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderDetailActivity, "Error downloading invoice: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            order?.let { downloadInvoice(it.invoice_pdf) }
        } else {
            Toast.makeText(this, "Storage permission required to download invoice", Toast.LENGTH_SHORT).show()
        }
    }
}