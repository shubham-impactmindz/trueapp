package com.app.truewebapp.ui.component.main.account

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

class CompanyAddressActivity : AppCompatActivity(), AddressClickListener {

    lateinit var binding: ActivityCompanyAddressBinding
    lateinit var adapter: AddressListAdapter
    private lateinit var companyAddressViewModel: CompanyAddressViewModel
    private var token = ""
    private lateinit var editAddressLauncher: ActivityResultLauncher<Intent>
    private lateinit var addAddressLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        editAddressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val isUpdated = result.data?.getBooleanExtra("address_updated", false) ?: false
                if (isUpdated) {
                    companyAddressViewModel.companyAddress(token)
                }
                val isDeleted = result.data?.getBooleanExtra("address_deleted", false) ?: false
                if (isDeleted) {
                    companyAddressViewModel.companyAddress(token)
                }
            }
        }
        addAddressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val isAdded = result.data?.getBooleanExtra("address_added", false) ?: false
                if (isAdded) {
                    companyAddressViewModel.companyAddress(token)
                }
            }
        }
        val preferences =getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        companyAddressViewModel = ViewModelProvider(this)[CompanyAddressViewModel::class.java]
        initializeObservers()
        companyAddressViewModel.companyAddress(token)
        // Handle system bars insets (status + nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        binding.newBranchLayout.setOnClickListener {
            binding.newBranchLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(this, AddAddressActivity::class.java)
            addAddressLauncher.launch(intent)
        }
        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            finish()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            companyAddressViewModel.companyAddress(token)
        }
    }

    private fun initializeObservers() {
        companyAddressViewModel.companyAddressResponse.observe(
            this, Observer {
                it?.let {
                    binding.swipeRefreshLayout.isRefreshing = false
                    if (it.status) {
                        binding.shimmerLayoutAddress.visibility = View.GONE
                        if (it.company_addresses.isNotEmpty()){
                            binding.tvNoData.visibility = View.GONE
                            binding.addressRecyclerView.visibility = View.VISIBLE
                            binding.addressRecyclerView.layoutManager = LinearLayoutManager(this)
                            adapter = AddressListAdapter(it.company_addresses, this)
                            binding.addressRecyclerView.adapter = adapter

                        }else {
                            binding.tvNoData.visibility = View.VISIBLE
                            binding.addressRecyclerView.visibility = View.GONE
                        }
                        // Navigate or store preferences here
                    } else {
                        binding.shimmerLayoutAddress.visibility = View.GONE
                        showTopSnackBar(it.message)
                    }
                }
            }
        )

        companyAddressViewModel.isLoading.observe(this, Observer {
            binding.shimmerLayoutAddress.visibility = View.GONE
        })

        companyAddressViewModel.apiError.observe(this, Observer {
            binding.shimmerLayoutAddress.visibility = View.GONE
            showTopSnackBar(it) // Now this will show exact API message
        })

        companyAddressViewModel.onFailure.observe(this, Observer {
            binding.shimmerLayoutAddress.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, applicationContext))
        })
    }

    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark)) // customize color

        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }

    override fun onEditAddressClicked(
        addressId: String,
        companyName: String,
        companyAddress1: String,
        companyAddress2: String?,
        companyCity: String,
        companyCountry: String,
        companyPostcode: String
    ) {
        val intent = Intent(this, EditAddressActivity::class.java)
        intent.putExtra("company_address_id", addressId)
        intent.putExtra("companyName", companyName)
        intent.putExtra("companyAddress1",companyAddress1 )
        intent.putExtra("companyAddress2",companyAddress2 )
        intent.putExtra("companyCity",companyCity )
        intent.putExtra("companyCountry",companyCountry )
        intent.putExtra("companyPostCode",companyPostcode )
        editAddressLauncher.launch(intent)
    }
}