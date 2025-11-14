package com.app.truewebapp.ui.component.main.account
// Package declaration for organizing classes in the app

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.databinding.ActivityRepDetailBinding
import com.app.truewebapp.ui.viewmodel.UserProfileViewModel
import com.google.gson.Gson
// Import statements for required Android, Jetpack, and project-specific classes

class RepDetailActivity : AppCompatActivity() {
    // Binding object to access layout views using ViewBinding
    lateinit var binding: ActivityRepDetailBinding

    // Holds user login details parsed from shared preferences
    lateinit var loginResponse: LoginResponse

    // ViewModel instance for handling user profile API logic
    private lateinit var userProfileViewModel: UserProfileViewModel

    // Token string for authenticated API requests
    private var token = ""

    // Lifecycle method called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityRepDetailBinding.inflate(layoutInflater)

        // Set the content view to the root of the binding
        setContentView(binding.root)

        // Retrieve shared preferences for stored user/session data
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)

        // Initialize the ViewModel for this activity
        userProfileViewModel = ViewModelProvider(this)[UserProfileViewModel::class.java]

        // Get stored user JSON string from preferences
        val userJson = preferences.getString("userResponse", "")

        // Build bearer token for API calls
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"

        // Attach observers to listen to API responses
        observeUserProfile()

        // Call the API to fetch user profile details
        userProfileViewModel.userProfile(token)

        // Parse stored user JSON into LoginResponse object using Gson
        loginResponse = Gson().fromJson(userJson, LoginResponse::class.java)

        // Handle system UI insets for status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Get system bar sizes
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Add padding to root view for proper layout
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        // Handle click on back button
        binding.backLayout.setOnClickListener {
            // Trigger haptic feedback on click
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Flag to bypass global settings
            )
            // Close the activity and return to previous screen
            finish()
        }
    }

    // Observes changes from ViewModel LiveData and updates UI accordingly
    private fun observeUserProfile() {
        // Observe user profile API response
        userProfileViewModel.userProfileResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    // Check if representative details are null
                    if (it.rep_details == null) {
                        // Hide all rep-related views if no data is available
                        binding.repLayout.visibility = View.GONE
                        binding.phoneNumberLayout.visibility = View.GONE
                        binding.emailLayout.visibility = View.GONE
                        binding.repCodeLayout.visibility = View.GONE
                        binding.noDataLayout.visibility = View.VISIBLE
                    } else {
                        // Show rep-related views
                        binding.repLayout.visibility = View.VISIBLE
                        binding.phoneNumberLayout.visibility = View.VISIBLE
                        binding.emailLayout.visibility = View.VISIBLE
                        binding.repCodeLayout.visibility = View.VISIBLE
                        binding.noDataLayout.visibility = View.GONE
                        
                        // Extract representative name
                        val name = it.rep_details.name.trim()

                        // Generate initials from name (first letters of each word)
                        val initials = name
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .map { it.first().uppercaseChar() }
                            .joinToString(" ")

                        // Update UI with rep details
                        binding.tvInitial.text = initials
                        binding.tvName.text = it.rep_details.name
                        binding.tvMobile.text = it.rep_details.mobile
                        binding.tvEmail.text = it.rep_details.email
                        binding.tvRepCode.text = it.rep_details.rep_code
                    }
                }
            }
        }

        // Observe loading state (optional loader handling)
        userProfileViewModel.isLoading.observe(this) {
            // Optional: show or hide loading indicator here
        }

        // Observe API error responses
        userProfileViewModel.apiError.observe(this) {
            Toast.makeText(this, "API Error: $it", Toast.LENGTH_SHORT).show()
        }

        // Observe network failures
        userProfileViewModel.onFailure.observe(this) {
            Toast.makeText(this, "Network Error: ${it?.message}", Toast.LENGTH_SHORT).show()
        }
    }
}