package com.app.truewebapp.ui.component.main.account

// Import necessary Android and Kotlin libraries
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.data.dto.profile.ProfileRequest
import com.app.truewebapp.databinding.ActivityEditProfileBinding
import com.app.truewebapp.ui.viewmodel.EditUserProfileViewModel
import com.google.gson.Gson

// Activity class for editing user profile
class EditProfileActivity : AppCompatActivity() {

    // View binding for accessing layout elements
    lateinit var binding: ActivityEditProfileBinding

    // Authentication token for API calls
    private var token = ""

    // Stores user login response (details of the logged-in user)
    lateinit var loginResponse: LoginResponse

    // ViewModel to handle edit profile related operations
    private lateinit var editUserProfileViewModel: EditUserProfileViewModel

    // Lifecycle method: Called when activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using View Binding
        binding = ActivityEditProfileBinding.inflate(layoutInflater)

        // Set inflated layout as the activity view
        setContentView(binding.root)

        // Access stored preferences (shared storage)
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)

        // Initialize ViewModel using Android's ViewModelProvider
        editUserProfileViewModel = ViewModelProvider(this)[EditUserProfileViewModel::class.java]

        // Retrieve token from preferences and format it as Bearer token
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"

        // Retrieve user details stored as JSON string in preferences
        val user_detailsJson = preferences.getString("userResponse", "")

        // Convert JSON string into LoginResponse object using Gson
        loginResponse = Gson().fromJson(user_detailsJson, LoginResponse::class.java)

        // Start observing LiveData from ViewModel
        observeEditUserProfile()

        // Split user’s full name into first and last name
        val nameParts = loginResponse.user_detail.name.split(" ")
        val firstName = nameParts.firstOrNull() ?: "" // first word = first name
        val lastName = nameParts.drop(1).joinToString(" ") // remaining words = last name

        // Pre-fill the EditText fields with user data
        binding.firstNameInput.setText(firstName)
        binding.lastNameInput.setText(lastName)
        binding.mobileNumberInput.setText(loginResponse.user_detail.mobile)
        binding.emailAddressInput.setText(loginResponse.user_detail.email)
        binding.companyNameInput.setText(loginResponse.user_detail.company_name)
        binding.address1Input.setText(loginResponse.user_detail.address1)
        binding.address2Input.setText(loginResponse.user_detail.address2)
        binding.cityInput.setText(loginResponse.user_detail.city)
        binding.postCodeInput.setText(loginResponse.user_detail.postcode)
        binding.countryInput.setText(loginResponse.user_detail.country)

        // Adjust layout padding based on system bars (status bar + navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top, // padding for status bar
                bottom = systemBars.bottom // padding for navigation bar
            )
            insets
        }

        // Setup keyboard detection and focus listeners
        setupKeyboardDetection()
        setupFocusListeners()

        // Back button click listener
        binding.backLayout.setOnClickListener {
            // Provide haptic feedback (vibration effect)
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag to ignore system haptic setting
            )
            finish() // Close activity and return to previous screen
        }

        // Update button click listener
        binding.btnUpdate.setOnClickListener {
            // Provide haptic feedback
            binding.btnUpdate.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            // Create request object with updated profile details
            val request = ProfileRequest(
                name = binding.firstNameInput.text.toString().trim() +" " +binding.lastNameInput.text.toString().trim(),
                mobile = binding.mobileNumberInput.text.toString().trim(),
                email = binding.emailAddressInput.text.toString().trim(),
                company_name = binding.companyNameInput.text.toString().trim(),
                address1 = binding.address1Input.text.toString().trim(),
                address2 = binding.address2Input.text.toString().trim(),
                city = binding.cityInput.text.toString().trim(),
                postcode = binding.postCodeInput.text.toString().trim(),
                country = binding.countryInput.text.toString().trim(),
            )

            // Validate inputs before submitting request
            if (validateInputs()) {
                // Call ViewModel to update profile if validation passes
                editUserProfileViewModel.editUserProfile(token, request)
            }
        }
    }

    // Function to validate user inputs before API call
    private fun validateInputs(): Boolean {
        var isValid = true

        // Retrieve values from input fields
        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val mobile = binding.mobileNumberInput.text.toString().trim()
        val email = binding.emailAddressInput.text.toString().trim()
        val companyName = binding.companyNameInput.text.toString().trim()
        val address1 = binding.address1Input.text.toString().trim()
        val address2 = binding.address2Input.text.toString().trim()
        val city = binding.cityInput.text.toString().trim()
        val postcode = binding.postCodeInput.text.toString().trim()
        val country = binding.countryInput.text.toString().trim()

        // Field validations
        if (firstName.isEmpty()) {
            binding.firstNameInput.error = "First name is required"
            isValid = false
        }

        if (lastName.isEmpty()) {
            binding.lastNameInput.error = "Last name is required"
            isValid = false
        }

        if (mobile.isEmpty()) {
            binding.mobileNumberInput.error = "Mobile number is required"
            isValid = false
        } else if (mobile.length < 10) {
            binding.mobileNumberInput.error = "Mobile number too short"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.emailAddressInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailAddressInput.error = "Invalid email format"
            isValid = false
        }

        if (companyName.isEmpty()) {
            binding.companyNameInput.error = "Company name is required"
            isValid = false
        }

        if (address1.isEmpty()) {
            binding.address1Input.error = "Address Line 1 is required"
            isValid = false
        }

        if (city.isEmpty()) {
            binding.cityInput.error = "City is required"
            isValid = false
        }

        if (postcode.isEmpty()) {
            binding.postCodeInput.error = "Postcode is required"
            isValid = false
        }

        if (country.isEmpty()) {
            binding.countryInput.error = "Country is required"
            isValid = false
        }

        return isValid // Return validation status
    }

    // Function to observe LiveData responses from ViewModel
    private fun observeEditUserProfile() {
        // Observe profile update response
        editUserProfileViewModel.userProfileResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    if (it.user_detail == null) {
                        // If no user details returned, do nothing
                    } else {
                        // Save updated user details back into SharedPreferences
                        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
                        val gson = Gson()
                        val jsonString = gson.toJson(it) // Convert updated object to JSON string
                        preferences?.edit()?.putString("userResponse", jsonString)?.apply()
                        finish() // Close activity after successful update
                    }
                }
            }
        }

        // Observe loading state (can be used to show/hide loader)
        editUserProfileViewModel.isLoading.observe(this) {
            // Optional: handle shimmer or loader here
        }

        // Observe API error messages
        editUserProfileViewModel.apiError.observe(this) {
            Toast.makeText(this, "API Error: $it", Toast.LENGTH_SHORT).show()
        }

        // Observe network failure cases
        editUserProfileViewModel.onFailure.observe(this) {
            Toast.makeText(this, "Network Error: ${it?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sets up focus change listeners to auto-scroll to focused fields
     */
    private fun setupFocusListeners() {
        val scrollView = binding.mainLayout
        
        // List of all input fields
        val inputFields = listOf(
            binding.firstNameInput,
            binding.lastNameInput,
            binding.mobileNumberInput,
            binding.emailAddressInput,
            binding.companyNameInput,
            binding.address1Input,
            binding.address2Input,
            binding.cityInput,
            binding.postCodeInput,
            binding.countryInput
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
            val imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            
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