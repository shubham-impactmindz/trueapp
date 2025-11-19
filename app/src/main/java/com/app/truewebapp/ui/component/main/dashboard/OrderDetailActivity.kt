package com.app.truewebapp.ui.component.main.dashboard

// Importing required Android and Jetpack libraries
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.bank.BankDetail
import com.app.truewebapp.data.dto.order.GenerateInvoiceRequest
import com.app.truewebapp.data.dto.order.Orders
import com.app.truewebapp.databinding.ActivityOrderDetailBinding
import com.app.truewebapp.ui.component.login.WebViewActivity
import com.app.truewebapp.ui.viewmodel.BankDetailViewModel
import com.app.truewebapp.ui.viewmodel.GenerateInvoiceViewModel
import com.app.truewebapp.ui.viewmodel.OrderDetailViewModel
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

    // ViewModel for generating invoice
    private lateinit var generateInvoiceViewModel: GenerateInvoiceViewModel

    // ViewModel for order detail
    private lateinit var orderDetailViewModel: OrderDetailViewModel

    // Token for API calls
    private var token = ""

    // Order data
    private var order: Orders? = null

    // CDN URL for combining with invoice PDF path
    private var cdnURL: String? = null

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
        cdnURL = intent.getStringExtra("cdnURL")

        // Initialize ViewModel and token
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences.getString("token", "").orEmpty()}"
        bankDetailViewModel = ViewModelProvider(this)[BankDetailViewModel::class.java]
        generateInvoiceViewModel = ViewModelProvider(this)[GenerateInvoiceViewModel::class.java]
        orderDetailViewModel = ViewModelProvider(this)[OrderDetailViewModel::class.java]

        // Observe bank details
        observeBankDetails()

        // Observe invoice generation
        observeInvoiceGeneration()

        // Observe order detail
        observeOrderDetail()

        // Call order detail API
        order?.let {
            orderDetailViewModel.orderDetail(it.order_id, token)
        }

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
                // Add click listener for Download Invoice button
                binding.tvPaymentStatus.setOnClickListener {
                    binding.tvPaymentStatus.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                    // Generate invoice via API
                    generateInvoice()
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
            Toast.makeText(this, "Network Error: ${throwable?.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }
    
    // Observe invoice generation from ViewModel
    // Observe invoice generation from ViewModel
    private fun observeInvoiceGeneration() {
        generateInvoiceViewModel.generateInvoiceResponse.observe(this) { response ->
            response?.takeIf { it.status }?.let { invoiceResponse ->
                val invoicePdfPath = invoiceResponse.invoice_pdf
                val cdnBaseUrl = invoiceResponse.cdnURL ?: cdnURL

                if (!invoicePdfPath.isNullOrEmpty()) {
                    val baseUrl = cdnBaseUrl?.trimEnd('/') ?: ""
                    val pdfPath = invoicePdfPath.trimStart('/')
                    val fullInvoiceUrl = "$baseUrl/$pdfPath"

                    downloadInvoice(fullInvoiceUrl)
                } else {
                    Toast.makeText(this, "Invoice PDF URL not available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        generateInvoiceViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                Toast.makeText(this, "Generating invoice...", Toast.LENGTH_SHORT).show()
            }
        }

        generateInvoiceViewModel.apiError.observe(this) { error ->
            Toast.makeText(this, "Error generating invoice: $error", Toast.LENGTH_SHORT).show()
        }

        generateInvoiceViewModel.onFailure.observe(this) { throwable ->
            Toast.makeText(this, "Network Error: ${throwable?.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // Generate invoice via API
    private fun generateInvoice() {
        order?.let { orderData ->
            val generateInvoiceRequest = GenerateInvoiceRequest(order_id = orderData.order_id)
            generateInvoiceViewModel.generateInvoice(token, generateInvoiceRequest)
        } ?: run {
            Toast.makeText(this, "Order data not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Observe order detail from ViewModel
    private fun observeOrderDetail() {
        orderDetailViewModel.orderDetailResponseLiveData.observe(this) { response ->
            response?.takeIf { it.status }?.let { orderDetailResponse ->
                val orderDetail = orderDetailResponse.order

                // Handle tracking number display
                if (!orderDetail.tracking_number.isNullOrEmpty()) {
                    // Show tracking button and tracking number layout
                    binding.tracking.visibility = View.VISIBLE
                    binding.trackingNumberLayout.visibility = View.VISIBLE

                    // Set tracking number text
                    binding.tvTrackingNumber.text = orderDetail.tracking_number

                    // Set click listener for Track Order button
                    binding.tracking.setOnClickListener {
                        binding.tracking.performHapticFeedback(
                            HapticFeedbackConstants.VIRTUAL_KEY,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )
                        // Open tracking URL in WebView
                        orderDetail.track_your_order?.let { trackingUrl ->
                            val intent = Intent(this, WebViewActivity::class.java)
                            intent.putExtra("url", trackingUrl)
                            intent.putExtra("title", "Track Order")
                            startActivity(intent)
                        } ?: run {
                            Toast.makeText(
                                this,
                                "Tracking URL not available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // Set click listener for copy tracking number
                    binding.btnCopyTrackingNumber.setOnClickListener {
                        binding.btnCopyTrackingNumber.performHapticFeedback(
                            HapticFeedbackConstants.VIRTUAL_KEY,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )
                        copyToClipboard(orderDetail.tracking_number, "Tracking Number")
                    }
                } else {
                    // Hide tracking UI if tracking number is null
                    binding.tracking.visibility = View.GONE
                    binding.trackingNumberLayout.visibility = View.GONE
                }
            }
        }

        orderDetailViewModel.apiErrorLiveData.observe(this) { error ->
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }

        orderDetailViewModel.onFailureLiveData.observe(this) { throwable ->
            Toast.makeText(this, "Network Error: ${throwable?.message}", Toast.LENGTH_SHORT)
                .show()
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
        setupCopyButton(
            dialogView,
            R.id.btnCopyCompanyName,
            tvCompanyName.text.toString(),
            "Bank Holder Name"
        )
        setupCopyButton(
            dialogView,
            R.id.btnCopyBankName,
            tvBankName.text.toString(),
            "Bank Name"
        )
        setupCopyButton(
            dialogView,
            R.id.btnCopyAccountNumber,
            tvAccountNumber.text.toString(),
            "Account Number"
        )
        setupCopyButton(
            dialogView,
            R.id.btnCopySortCode,
            tvSortCode.text.toString(),
            "Sort Code"
        )

        dialog.show()
    }
    
    // Helper function to setup copy button functionality
    private fun setupCopyButton(
        dialogView: View,
        buttonId: Int,
        textToCopy: String,
        label: String
    ) {
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
                val downloadsDir =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        // Android 10+ uses scoped storage
                        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                            ?: getFilesDir()
                    } else {
                        // Android 9 and below
                        if (ContextCompat.checkSelfPermission(
                                this@OrderDetailActivity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
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

                // Verify file was written successfully
                if (file.exists() && file.length() > 0) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@OrderDetailActivity,
                            "Invoice downloaded successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Small delay to ensure file is fully written, then open the PDF file
                        binding.root.postDelayed({
                            openPdfFile(file)
                        }, 100)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@OrderDetailActivity,
                            "Failed to download invoice file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@OrderDetailActivity,
                        "Error downloading invoice: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // Open PDF file using Intent
    private fun openPdfFile(file: File) {
        try {
            // Check if file exists
            if (!file.exists()) {
                Toast.makeText(
                    this,
                    "PDF file not found at: ${file.absolutePath}",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val uri =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    // Use FileProvider for Android 7+ (API 24+)
                    FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )
                } else {
                    // For older Android versions
                    Uri.fromFile(file)
                }

            // Create intent to view PDF
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Grant permissions to all apps that can handle this intent
            val resolveInfos =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resolveInfos) {
                val packageName = resolveInfo.activityInfo.packageName
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Try to start activity with chooser (gives user option to select app)
            try {
                val chooser = Intent.createChooser(intent, "Open PDF with")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(chooser)
            } catch (e: Exception) {
                // If chooser fails, try direct intent
                try {
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        // Try with a more generic MIME type
                        val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "*/*")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        if (genericIntent.resolveActivity(packageManager) != null) {
                            startActivity(genericIntent)
                        } else {
                            Toast.makeText(
                                this,
                                "No app found to open PDF. Please install a PDF reader app.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e2: Exception) {
                    Toast.makeText(this, "Error opening PDF: ${e2.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("OrderDetailActivity", "Error opening PDF", e)
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
            // Re-trigger invoice generation after permission is granted
            generateInvoice()
        } else {
            Toast.makeText(
                this,
                "Storage permission required to download invoice",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}