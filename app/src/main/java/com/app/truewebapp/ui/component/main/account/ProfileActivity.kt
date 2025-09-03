package com.app.truewebapp.ui.component.main.account

// Import required Android & Kotlin libraries
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.databinding.ActivityProfileBinding
import com.app.truewebapp.ui.viewmodel.UserProfileViewModel
import com.google.gson.Gson

// Activity to display user profile details
class ProfileActivity : AppCompatActivity() {

    // View binding instance to access layout views
    lateinit var binding: ActivityProfileBinding

    // Holds user login details (parsed from SharedPreferences)
    lateinit var loginResponse: LoginResponse

    // ViewModel to handle user profile API calls
    private lateinit var userProfileViewModel: UserProfileViewModel

    // Token for authorization in API calls
    private var token = ""

    // Lifecycle method: called when activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using View Binding
        binding = ActivityProfileBinding.inflate(layoutInflater)

        // Set the inflated view as the activity's content
        setContentView(binding.root)

        // Get stored SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)

        // Initialize UserProfileViewModel using ViewModelProvider
        userProfileViewModel = ViewModelProvider(this)[UserProfileViewModel::class.java]

        // Retrieve Bearer token from SharedPreferences
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"

        // Start observing LiveData from ViewModel
        observeUserProfile()

        // Fetch profile data from server using ViewModel
        userProfileViewModel.userProfile(token)

        // Retrieve stored user details JSON from SharedPreferences
        val user_detailsJson = preferences.getString("userResponse", "")

        // Convert JSON string into LoginResponse object using Gson
        loginResponse = Gson().fromJson(user_detailsJson, LoginResponse::class.java)

        // Handle system bar insets (status + navigation bar) to adjust layout padding
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,     // Add padding for status bar
                bottom = systemBars.bottom // Add padding for navigation bar
            )
            insets
        }

        // Handle back button click
        binding.backLayout.setOnClickListener {
            // Provide haptic feedback on click
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ignore global haptic setting
            )
            finish() // Close current activity
        }

        // Handle edit button click
        binding.editButton.setOnClickListener {
            // Provide haptic feedback on click
            binding.editButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            // Start EditProfileActivity
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    // Observe LiveData values from UserProfileViewModel
    private fun observeUserProfile() {
        // Observe user profile API response
        userProfileViewModel.userProfileResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    if (it.user_detail == null) {
                        // If no user details, do nothing
                    } else {
                        // Save updated user details to SharedPreferences
                        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
                        val gson = Gson()
                        val jsonString = gson.toJson(it) // Convert object to JSON string
                        preferences?.edit()?.putString("userResponse", jsonString)?.apply()

                        // Extract initials from full name
                        val name = it.user_detail.name.trim()
                        val initials = name
                            .split(" ") // Split by spaces
                            .filter { it.isNotBlank() } // Ignore empty parts
                            .map { it.first().uppercaseChar() } // Take first letter, uppercase
                            .joinToString(" ") // Join initials

                        // Update UI with user profile details
                        binding.tvInitial.text = initials
                        binding.tvName.text = it.user_detail.name
                        binding.tvCompanyName.text = it.user_detail.company_name
                        // Concatenate full address and display
                        "${it.user_detail.address1}, ${it.user_detail.address2}, ${it.user_detail.city}, ${it.user_detail.country}, ${it.user_detail.postcode}".also { binding.tvCompanyAddress.text = it }
                        binding.tvPhoneNumber.text = it.user_detail.mobile
                        binding.tvEmail.text = it.user_detail.email

                        // Display Rep Code if available, else "N/A"
                        if (it.rep_details?.rep_code?.isNotEmpty() == true) {
                            binding.tvRepCode.text = it.rep_details.rep_code
                        } else {
                            binding.tvRepCode.text = "N/A"
                        }
                    }
                }
            }
        }

        // Observe loading state (can be used for shimmer or progress bar)
        userProfileViewModel.isLoading.observe(this) {
            // Optional: show or hide loader here
        }

        // Observe API error messages
        userProfileViewModel.apiError.observe(this) {
            Toast.makeText(this, "API Error: $it", Toast.LENGTH_SHORT).show()
        }

        // Observe network failure events
        userProfileViewModel.onFailure.observe(this) {
            Toast.makeText(this, "Network Error: ${it?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Lifecycle method: called when activity resumes (comes to foreground)
    override fun onResume() {
        super.onResume()

        // Retrieve user details from SharedPreferences again
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        val user_detailsJson = preferences.getString("userResponse", "")

        // Parse stored user details JSON into LoginResponse
        loginResponse = Gson().fromJson(user_detailsJson, LoginResponse::class.java)

        // Extract initials from full name
        val name = loginResponse.user_detail.name.trim()
        val initials = name
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.first().uppercaseChar() }
            .joinToString(" ")

        // Update UI fields with latest profile info
        binding.tvInitial.text = initials
        binding.tvName.text = loginResponse.user_detail.name
        binding.tvCompanyName.text = loginResponse.user_detail.company_name
        "${loginResponse.user_detail.address1}, ${loginResponse.user_detail.address2}, ${loginResponse.user_detail.city}, ${loginResponse.user_detail.country}, ${loginResponse.user_detail.postcode}".also { binding.tvCompanyAddress.text = it }
        binding.tvPhoneNumber.text = loginResponse.user_detail.mobile
        binding.tvEmail.text = loginResponse.user_detail.email

        // Show rep code if exists, else display "N/A"
        if (loginResponse.rep_details?.rep_code?.isNotEmpty() == true) {
            binding.tvRepCode.text = loginResponse.rep_details!!.rep_code
        } else {
            binding.tvRepCode.text = "N/A"
        }
    }
}