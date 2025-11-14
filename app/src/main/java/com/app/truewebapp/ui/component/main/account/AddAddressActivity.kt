package com.app.truewebapp.ui.component.main.account

// Import required Android and Kotlin libraries
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
import com.app.truewebapp.data.dto.company_address.CompanyAddressRequest
import com.app.truewebapp.databinding.ActivityAddAddressBinding
import com.app.truewebapp.ui.viewmodel.CompanyAddressUpsertViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar

/**
 * AddAddressActivity handles adding or updating the company address.
 * It provides UI form validation, interacts with ViewModel to make API calls,
 * and returns result back to the calling Activity.
 */
class AddAddressActivity : AppCompatActivity() {

    // ViewBinding for accessing views from activity_add_address.xml
    lateinit var binding: ActivityAddAddressBinding

    // ViewModel for handling company address insert/update API requests
    private lateinit var companyAddressUpsertViewModel: CompanyAddressUpsertViewModel

    // Authorization token for API calls
    private var token = ""

    // Flag to prevent multiple API calls when Save is clicked multiple times
    private var enabled = true

    /**
     * onCreate is called when the Activity is created.
     * It initializes UI, retrieves token, sets click listeners, and observes ViewModel responses.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityAddAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get saved preferences (token) from SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()

        // Initialize ViewModel
        companyAddressUpsertViewModel = ViewModelProvider(this)[CompanyAddressUpsertViewModel::class.java]

        // Setup LiveData observers
        initializeObservers()

        // Set country to GB and make it non-editable
        binding.countryInput.setText("GB")
        binding.countryInput.setTextColor(getColor(R.color.black))
        binding.countryInput.isEnabled = false
        binding.countryInput.isFocusable = false
        binding.countryInput.isClickable = false

        // Handle system bars (status + navigation bar) padding using insets
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
                HapticFeedbackConstants.VIRTUAL_KEY, // Provide vibration feedback
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            finish() // Close activity
        }

        // Handle save button click
        binding.saveLayout.setOnClickListener {
            binding.saveLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY, // Provide vibration feedback
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            // Validate input fields before making API call
            if (validateInputs()) {
                if (enabled) {
                    enabled = false // Prevent multiple API submissions

                    // Call API through ViewModel with form data
                    companyAddressUpsertViewModel.companyAddress(
                        token,
                        CompanyAddressRequest(
                            binding.companyNameInput.text.toString().trim(),
                            binding.address1Input.text.toString().trim(),
                            binding.address2Input.text.toString().trim(),
                            binding.cityInput.text.toString().trim(),
                            binding.countryInput.text.toString().trim(),
                            binding.postCodeInput.text.toString().trim()
                        )
                    )
                }
            }
        }
    }

    /**
     * Initializes LiveData observers for handling API responses and failures.
     */
    private fun initializeObservers() {
        // Observe API response for adding/updating address
        companyAddressUpsertViewModel.companyAddressResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    // On success, show success message
                    showTopSnackBar("Address Added Successfully")

                    // Return result to calling Activity
                    val resultIntent = Intent().apply {
                        putExtra("address_added", true)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)

                    // Clear input fields and finish activity
                    clearValue()
                    finish()
                } else {
                    // Enable button again if API failed
                    enabled = true
                }
            }
        }

        // Observe loading state (can be used to show progress/shimmer)
        companyAddressUpsertViewModel.isLoading.observe(this) {
            if (it == true) {
                // Loading state (UI handling can be added here if needed)
            }
        }

        // Observe API error response
        companyAddressUpsertViewModel.apiError.observe(this) {
            enabled = true
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe network or unknown failures
        companyAddressUpsertViewModel.onFailure.observe(this) {
            enabled = true
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, this))
        }
    }

    /**
     * Displays a Snackbar at the bottom of the screen with custom styling.
     * @param message The message to be displayed in the Snackbar
     */
    private fun showTopSnackBar(message: String) {
        // Create Snackbar with message
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        // Customize Snackbar layout parameters
        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Set background color
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))

        // Customize text view inside Snackbar
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show() // Show Snackbar
    }

    /**
     * Validates all input fields of the form.
     * @return true if all inputs are valid, false otherwise
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        // Read trimmed values from input fields
        val companyName = binding.companyNameInput.text.toString().trim()
        val address1 = binding.address1Input.text.toString().trim()
        val city = binding.cityInput.text.toString().trim()
        val country = binding.countryInput.text.toString().trim()
        val postCode = binding.postCodeInput.text.toString().trim()

        // Validate required fields
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

    /**
     * Clears all input fields in the form.
     */
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