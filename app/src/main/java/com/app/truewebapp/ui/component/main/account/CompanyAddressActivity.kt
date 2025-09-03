package com.app.truewebapp.ui.component.main.account
// Package declaration for organizing the class under "account" feature module

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.ActivityCompanyAddressBinding
import com.app.truewebapp.ui.viewmodel.CompanyAddressViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar
// Import statements for Android, Jetpack, and app-specific classes

// Activity responsible for displaying, adding, and editing company addresses
class CompanyAddressActivity : AppCompatActivity(), AddressClickListener {

    // View binding instance for accessing layout views without findViewById
    lateinit var binding: ActivityCompanyAddressBinding

    // RecyclerView adapter for displaying list of addresses
    lateinit var adapter: AddressListAdapter

    // ViewModel for managing company address data and API interactions
    private lateinit var companyAddressViewModel: CompanyAddressViewModel

    // Token string for authentication with API calls
    private var token = ""

    // Launcher for starting EditAddressActivity and receiving results
    private lateinit var editAddressLauncher: ActivityResultLauncher<Intent>

    // Launcher for starting AddAddressActivity and receiving results
    private lateinit var addAddressLauncher: ActivityResultLauncher<Intent>

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityCompanyAddressBinding.inflate(layoutInflater)
        // Set the root view of binding as the content view
        setContentView(binding.root)

        // Register launcher to handle results from EditAddressActivity
        editAddressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Check if address was updated and refresh list
                val isUpdated = result.data?.getBooleanExtra("address_updated", false) ?: false
                if (isUpdated) {
                    companyAddressViewModel.companyAddress(token)
                }
                // Check if address was deleted and refresh list
                val isDeleted = result.data?.getBooleanExtra("address_deleted", false) ?: false
                if (isDeleted) {
                    companyAddressViewModel.companyAddress(token)
                }
            }
        }

        // Register launcher to handle results from AddAddressActivity
        addAddressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Check if address was added and refresh list
                val isAdded = result.data?.getBooleanExtra("address_added", false) ?: false
                if (isAdded) {
                    companyAddressViewModel.companyAddress(token)
                }
            }
        }

        // Get saved authentication token from SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()

        // Initialize ViewModel for company addresses
        companyAddressViewModel = ViewModelProvider(this)[CompanyAddressViewModel::class.java]

        // Set up LiveData observers
        initializeObservers()

        // Fetch company address list initially
        companyAddressViewModel.companyAddress(token)

        // Handle system bar (status/navigation) padding for root view
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        // Handle "Add New Branch" button click
        binding.newBranchLayout.setOnClickListener {
            // Provide haptic feedback for touch event
            binding.newBranchLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag to ignore system settings
            )
            // Launch AddAddressActivity for result
            val intent = Intent(this, AddAddressActivity::class.java)
            addAddressLauncher.launch(intent)
        }

        // Handle back button click
        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            finish() // Close the current activity
        }

        // Swipe-to-refresh reloads company addresses
        binding.swipeRefreshLayout.setOnRefreshListener {
            companyAddressViewModel.companyAddress(token)
        }
    }

    // Function to set up observers for LiveData in ViewModel
    private fun initializeObservers() {
        // Observe address list API response
        companyAddressViewModel.companyAddressResponse.observe(
            this, Observer {
                it?.let {
                    // Stop refresh animation once response arrives
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (it.status) {
                        // Hide shimmer loading layout
                        binding.shimmerLayoutAddress.visibility = View.GONE

                        // If addresses exist, show them in RecyclerView
                        if (it.company_addresses.isNotEmpty()) {
                            binding.tvNoData.visibility = View.GONE
                            binding.addressRecyclerView.visibility = View.VISIBLE
                            binding.addressRecyclerView.layoutManager = LinearLayoutManager(this)
                            adapter = AddressListAdapter(it.company_addresses, this)
                            binding.addressRecyclerView.adapter = adapter
                        } else {
                            // Show "No Data" if list is empty
                            binding.tvNoData.visibility = View.VISIBLE
                            binding.addressRecyclerView.visibility = View.GONE
                        }
                    } else {
                        // API returned failure status, show error message
                        binding.shimmerLayoutAddress.visibility = View.GONE
                        showTopSnackBar(it.message)
                    }
                }
            }
        )

        // Observe loading state
        companyAddressViewModel.isLoading.observe(this, Observer {
            binding.shimmerLayoutAddress.visibility = View.GONE
        })

        // Observe API error messages
        companyAddressViewModel.apiError.observe(this, Observer {
            binding.shimmerLayoutAddress.visibility = View.GONE
            showTopSnackBar(it) // Show actual API error message
        })

        // Observe failure events (like network failure)
        companyAddressViewModel.onFailure.observe(this, Observer {
            binding.shimmerLayoutAddress.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, applicationContext))
        })
    }

    // Function to display custom Snackbar at bottom of screen
    private fun showTopSnackBar(message: String) {
        // Create a Snackbar with message and duration
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        // Adjust Snackbar position and margins
        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Customize Snackbar background color
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))

        // Customize Snackbar text color and alignment
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        // Finally show Snackbar
        snackBar.show()
    }

    // Callback from AddressClickListener interface
    // Called when user wants to edit an address from list
    override fun onEditAddressClicked(
        addressId: String,
        companyName: String,
        companyAddress1: String,
        companyAddress2: String?,
        companyCity: String,
        companyCountry: String,
        companyPostcode: String
    ) {
        // Create intent to open EditAddressActivity with existing address data
        val intent = Intent(this, EditAddressActivity::class.java)
        intent.putExtra("company_address_id", addressId)
        intent.putExtra("companyName", companyName)
        intent.putExtra("companyAddress1", companyAddress1)
        intent.putExtra("companyAddress2", companyAddress2)
        intent.putExtra("companyCity", companyCity)
        intent.putExtra("companyCountry", companyCountry)
        intent.putExtra("companyPostCode", companyPostcode)
        // Launch EditAddressActivity for result
        editAddressLauncher.launch(intent)
    }
}