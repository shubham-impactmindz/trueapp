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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.company_address.CompanyAddressDeleteRequest
import com.app.truewebapp.data.dto.company_address.CompanyAddressRequest
import com.app.truewebapp.databinding.ActivityEditAddressBinding
import com.app.truewebapp.ui.viewmodel.CompanyAddressDeleteViewModel
import com.app.truewebapp.ui.viewmodel.CompanyAddressUpsertViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar

class EditAddressActivity : AppCompatActivity() {
    lateinit var binding: ActivityEditAddressBinding
    private lateinit var companyAddressUpsertViewModel: CompanyAddressUpsertViewModel
    private lateinit var companyAddressDeleteViewModel: CompanyAddressDeleteViewModel
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferences =getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        companyAddressUpsertViewModel = ViewModelProvider(this)[CompanyAddressUpsertViewModel::class.java]
        companyAddressDeleteViewModel = ViewModelProvider(this)[CompanyAddressDeleteViewModel::class.java]
        updateAddressObservers()
        deleteAddressObservers()
        binding.companyNameInput.setText(intent.getStringExtra("companyName") ?: "")
        binding.address1Input.setText(intent.getStringExtra("companyAddress1") ?: "")
        if (intent.getStringExtra("companyAddress2")?.isNotEmpty() == true) {
            binding.address2Input.setText(intent.getStringExtra("companyAddress2") ?: "")
        }else{
            binding.address2Input.setText("")
        }
        binding.cityInput.setText(intent.getStringExtra("companyCity") ?: "")
        binding.countryInput.setText(intent.getStringExtra("companyCountry") ?: "")
        binding.postCodeInput.setText(intent.getStringExtra("companyPostCode") ?: "")
        // Handle system bars insets (status + nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }
        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            finish()
        }
        binding.saveLayout.setOnClickListener {
            binding.saveLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            if (validateInputs()){
                companyAddressUpsertViewModel.companyAddress(token, CompanyAddressRequest(binding.companyNameInput.text.toString().trim(),
                    binding.address1Input.text.toString().trim(),binding.address2Input.text.toString().trim(),binding.cityInput.text.toString().trim(),
                    binding.countryInput.text.toString().trim(),binding.postCodeInput.text.toString().trim(),intent.getStringExtra("company_address_id"))
                )
            }
        }
        binding.deleteLayout.setOnClickListener {
            binding.deleteLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            companyAddressDeleteViewModel.deleteCompanyAddress(token, CompanyAddressDeleteRequest(intent.getStringExtra("company_address_id")))
        }
    }

    private fun updateAddressObservers() {
        companyAddressUpsertViewModel.companyAddressResponse.observe(this) { response ->
            response?.let {
//                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    showTopSnackBar("Address Updated Successfully")
                    val resultIntent = Intent().apply {
                        putExtra("address_updated", true)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    clearValue()
                    finish()
                }
            }
        }

        companyAddressUpsertViewModel.isLoading.observe(this) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {

            }
        }

        companyAddressUpsertViewModel.apiError.observe(this) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        companyAddressUpsertViewModel.onFailure.observe(this) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, this))
        }
    }

    private fun deleteAddressObservers() {
        companyAddressDeleteViewModel.companyAddressResponse.observe(this) { response ->
            response?.let {
//                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    showTopSnackBar("Address Deleted Successfully")
                    val resultIntent = Intent().apply {
                        putExtra("address_deleted", true)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    clearValue()
                    finish()
                }
            }
        }

        companyAddressDeleteViewModel.isLoading.observe(this) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {

            }
        }

        companyAddressDeleteViewModel.apiError.observe(this) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        companyAddressDeleteViewModel.onFailure.observe(this) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, this))
        }
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

    private fun validateInputs(): Boolean {
        var isValid = true
        val companyName = binding.companyNameInput.text.toString().trim()
        val address1 = binding.address1Input.text.toString().trim()
        val city = binding.cityInput.text.toString().trim()
        val country = binding.countryInput.text.toString().trim()
        val postCode = binding.postCodeInput.text.toString().trim()

        if (companyName.isEmpty()) {
            binding.companyNameInput.error = "Company Name is required"
            isValid = false
        } else if (address1.isEmpty()) {
            binding.address1Input.error = "Address1 is required"
            isValid = false
        } else if (city.isEmpty()) {
            binding.cityInput.error = "City is required"
            isValid = false
        } else if (country.isEmpty()) {
            binding.countryInput.error = "Country is required"
            isValid = false
        } else if (postCode.isEmpty()) {
            binding.postCodeInput.error = "Postcode is required"
            isValid = false
        }
        return isValid
    }

    private fun clearValue(){
        binding.companyNameInput.text?.clear()
        binding.address1Input.text?.clear()
        binding.address2Input.text?.clear()
        binding.cityInput.text?.clear()
        binding.countryInput.text?.clear()
        binding.postCodeInput.text?.clear()
    }
}