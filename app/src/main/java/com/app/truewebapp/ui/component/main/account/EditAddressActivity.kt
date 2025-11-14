package com.app.truewebapp.ui.component.main.account
// Defines the package where this Activity class resides (under account module)

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
// Import required Android, Jetpack, and app-specific classes

// Activity for editing or deleting an existing company address
class EditAddressActivity : AppCompatActivity() {

    // ViewBinding for accessing views in activity_edit_address.xml
    lateinit var binding: ActivityEditAddressBinding

    // ViewModel to handle updating (upsert) company address
    private lateinit var companyAddressUpsertViewModel: CompanyAddressUpsertViewModel

    // ViewModel to handle deleting company address
    private lateinit var companyAddressDeleteViewModel: CompanyAddressDeleteViewModel

    // Authentication token used for API requests
    private var token = ""

    // Lifecycle method called when Activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate binding and set root view
        binding = ActivityEditAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve token stored in SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()

        // Initialize ViewModels using ViewModelProvider
        companyAddressUpsertViewModel = ViewModelProvider(this)[CompanyAddressUpsertViewModel::class.java]
        companyAddressDeleteViewModel = ViewModelProvider(this)[CompanyAddressDeleteViewModel::class.java]

        // Initialize observers for ViewModels
        updateAddressObservers()
        deleteAddressObservers()

        // Populate UI fields with values received from intent
        binding.companyNameInput.setText(intent.getStringExtra("companyName") ?: "")
        binding.address1Input.setText(intent.getStringExtra("companyAddress1") ?: "")
        if (intent.getStringExtra("companyAddress2")?.isNotEmpty() == true) {
            binding.address2Input.setText(intent.getStringExtra("companyAddress2") ?: "")
        } else {
            binding.address2Input.setText("")
        }
        binding.cityInput.setText(intent.getStringExtra("companyCity") ?: "")
        // Set country to GB and make it non-editable
        binding.countryInput.setText("GB")
        binding.countryInput.setTextColor(getColor(R.color.black))
        binding.countryInput.isEnabled = false
        binding.countryInput.isFocusable = false
        binding.countryInput.isClickable = false
        binding.postCodeInput.setText(intent.getStringExtra("companyPostCode") ?: "")

        // Handle window insets (status/navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        // Setup keyboard detection and focus listeners
        setupKeyboardDetection()
        setupFocusListeners()

        // Handle back button click
        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Provide haptic feedback
            )
            finish() // Close current activity
        }

        // Handle save button click
        binding.saveLayout.setOnClickListener {
            binding.saveLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            // Validate input fields before sending update request
            if (validateInputs()) {
                companyAddressUpsertViewModel.companyAddress(
                    token,
                    CompanyAddressRequest(
                        binding.companyNameInput.text.toString().trim(),
                        binding.address1Input.text.toString().trim(),
                        binding.address2Input.text.toString().trim(),
                        binding.cityInput.text.toString().trim(),
                        binding.countryInput.text.toString().trim(),
                        binding.postCodeInput.text.toString().trim(),
                        intent.getStringExtra("company_address_id")
                    )
                )
            }
        }

        // Handle delete button click
        binding.deleteLayout.setOnClickListener {
            binding.deleteLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            // Call delete API with selected address ID
            companyAddressDeleteViewModel.deleteCompanyAddress(
                token,
                CompanyAddressDeleteRequest(intent.getStringExtra("company_address_id"))
            )
        }
    }

    // Set up observers for update (upsert) address ViewModel
    private fun updateAddressObservers() {
        // Observe update response
        companyAddressUpsertViewModel.companyAddressResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    // Show success message
                    showTopSnackBar("Address Updated Successfully")

                    // Send result back to parent activity
                    val resultIntent = Intent().apply {
                        putExtra("address_updated", true)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)

                    // Clear input fields and close
                    clearValue()
                    finish()
                }
            }
        }

        // Observe loading state (could show shimmer or loader if needed)
        companyAddressUpsertViewModel.isLoading.observe(this) {
            if (it == true) {
                // Loading state - currently no UI shown
            }
        }

        // Observe API error messages
        companyAddressUpsertViewModel.apiError.observe(this) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe general failures (e.g. network issues)
        companyAddressUpsertViewModel.onFailure.observe(this) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, this))
        }
    }

    // Set up observers for delete address ViewModel
    private fun deleteAddressObservers() {
        // Observe delete response
        companyAddressDeleteViewModel.companyAddressResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    // Show success message
                    showTopSnackBar("Address Deleted Successfully")

                    // Send result back to parent activity
                    val resultIntent = Intent().apply {
                        putExtra("address_deleted", true)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)

                    // Clear inputs and finish
                    clearValue()
                    finish()
                }
            }
        }

        // Observe loading state
        companyAddressDeleteViewModel.isLoading.observe(this) {
            if (it == true) {
                // Loading state placeholder
            }
        }

        // Observe API error
        companyAddressDeleteViewModel.apiError.observe(this) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe failures
        companyAddressDeleteViewModel.onFailure.observe(this) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, this))
        }
    }

    // Show custom Snackbar with message at bottom of screen
    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        // Customize Snackbar position and margins
        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Set Snackbar background color
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))

        // Customize Snackbar text
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        // Show Snackbar
        snackBar.show()
    }

    // Validate user input fields before saving
    private fun validateInputs(): Boolean {
        var isValid = true

        // Extract trimmed input values
        val companyName = binding.companyNameInput.text.toString().trim()
        val address1 = binding.address1Input.text.toString().trim()
        val city = binding.cityInput.text.toString().trim()
        val postCode = binding.postCodeInput.text.toString().trim()

        // Validate mandatory fields
        if (companyName.isEmpty()) {
            binding.companyNameInput.error = "Company Name is required"
            isValid = false
        } else if (address1.isEmpty()) {
            binding.address1Input.error = "Address1 is required"
            isValid = false
        } else if (city.isEmpty()) {
            binding.cityInput.error = "City is required"
            isValid = false
        } else if (postCode.isEmpty()) {
            binding.postCodeInput.error = "Postcode is required"
            isValid = false
        }
        // Country is fixed to "GB" and always valid, no need to validate
        return isValid
    }

    // Clear all input fields
    private fun clearValue() {
        binding.companyNameInput.text?.clear()
        binding.address1Input.text?.clear()
        binding.address2Input.text?.clear()
        binding.cityInput.text?.clear()
        binding.countryInput.setText("GB") // Reset to GB instead of clearing
        binding.postCodeInput.text?.clear()
    }

    /**
     * Sets up focus change listeners to auto-scroll to focused fields
     */
    private fun setupFocusListeners() {
        val scrollView = binding.mainLayout
        
        // List of all input fields (excluding country which is disabled)
        val inputFields = listOf(
            binding.companyNameInput,
            binding.address1Input,
            binding.address2Input,
            binding.cityInput,
            binding.postCodeInput
        )
        
        // Add focus change listeners to all input fields
        inputFields.forEach { inputField ->
            inputField.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    // Post with delay to ensure keyboard is shown and layout is adjusted
                    view.postDelayed({
                        // Scroll to make the focused field visible with some extra space
                        val location = IntArray(2)
                        view.getLocationInWindow(location)
                        val fieldY = location[1]
                        
                        // Calculate scroll position to center the field in the visible area
                        val scrollY = fieldY - (scrollView.height / 3)
                        scrollView.smoothScrollTo(0, maxOf(0, scrollY))
                    }, 300) // Delay to allow keyboard animation to complete
                }
            }
        }
    }

    /**
     * Sets up keyboard detection to dynamically adjust bottom padding
     */
    private fun setupKeyboardDetection() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLayout) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            // Calculate additional padding needed when keyboard is visible
            val keyboardHeight = imeInsets.bottom
            val density = resources.displayMetrics.density
            val additionalPadding = if (keyboardHeight > 0) {
                // Keyboard is visible - add extra padding based on keyboard height
                // Use keyboard height + extra buffer to ensure all fields are accessible
                val extraBuffer = (100 * density).toInt()
                keyboardHeight + extraBuffer
            } else {
                // Keyboard is hidden - use minimal padding
                (50 * density).toInt()
            }
            
            // Apply the dynamic padding to the linear layout (first child of ScrollView)
            val scrollView = view as? android.widget.ScrollView
            val linearLayout = scrollView?.getChildAt(0) as? android.widget.LinearLayout
            linearLayout?.setPadding(
                linearLayout.paddingLeft,
                linearLayout.paddingTop,
                linearLayout.paddingRight,
                additionalPadding
            )
            
            insets
        }
    }
}