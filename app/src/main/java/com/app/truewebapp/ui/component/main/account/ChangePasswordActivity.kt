package com.app.truewebapp.ui.component.main.account

// Import required Android and app-specific classes
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
import com.app.truewebapp.data.dto.change_password.ChangePasswordRequest
import com.app.truewebapp.databinding.ActivityChangePasswordBinding
import com.app.truewebapp.ui.viewmodel.ChangePasswordViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar

/**
 * Activity that handles password change functionality.
 * It provides UI for entering current password, new password, and confirming new password.
 */
class ChangePasswordActivity : AppCompatActivity() {

    // View binding for accessing UI elements
    lateinit var binding: ActivityChangePasswordBinding

    // ViewModel responsible for handling API logic related to changing password
    private lateinit var changePasswordViewModel: ChangePasswordViewModel

    // Authentication token retrieved from SharedPreferences
    private var token = ""

    /**
     * Called when the activity is created.
     * Responsible for initializing UI, setting up observers, and handling button clicks.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)

        // Set the activity's root view
        setContentView(binding.root)

        // Retrieve token from shared preferences and prefix with "Bearer"
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()

        // Initialize ViewModel for change password API
        changePasswordViewModel = ViewModelProvider(this)[ChangePasswordViewModel::class.java]

        // Observe LiveData from ViewModel
        observeChangePassword()

        // Handle system bars insets (status bar and navigation bar padding)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,     // Apply padding for status bar
                bottom = systemBars.bottom // Apply padding for nav bar
            )
            insets
        }

        // Handle back button click
        binding.backLayout.setOnClickListener {
            // Provide haptic feedback when button is pressed
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ignore global settings for haptics
            )
            finish() // Close the activity
        }

        // Handle change password button click
        binding.llChangePassword.setOnClickListener {
            // Provide haptic feedback when button is pressed
            binding.llChangePassword.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            // Validate inputs before making API request
            if (validateInputs()) {
                // Call ViewModel to perform password change request
                changePasswordViewModel.changePassword(
                    token,
                    ChangePasswordRequest(
                        binding.passwordInput.text.toString().trim(),        // Current password
                        binding.newPasswordInput.text.toString().trim(),     // New password
                        binding.confirmPasswordInput.text.toString().trim()  // Confirm new password
                    )
                )
            }
        }
    }

    /**
     * Validates user input fields for changing password.
     *
     * @return true if all inputs are valid, false otherwise.
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        // Extract input values from EditTexts
        val password = binding.passwordInput.text.toString().trim()
        val newPassword = binding.newPasswordInput.text.toString().trim()
        val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

        // Validate current password
        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.passwordInput.error = "Password must be at least 8 characters"
            isValid = false
        }

        // Validate new password
        if (newPassword.isEmpty()) {
            binding.newPasswordInput.error = "New Password is required"
            isValid = false
        } else if (newPassword.length < 8) {
            binding.newPasswordInput.error = "New Password must be at least 8 characters"
            isValid = false
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInput.error = "Confirm Password is required"
            isValid = false
        } else if (confirmPassword != newPassword) {
            binding.confirmPasswordInput.error = "New Password & Confirm Password Don't Match"
            isValid = false
        }

        return isValid
    }

    /**
     * Clears all input fields after successful password change.
     */
    private fun clearValue() {
        binding.passwordInput.text?.clear()
        binding.newPasswordInput.text?.clear()
        binding.confirmPasswordInput.text?.clear()
    }

    /**
     * Observes LiveData from the ChangePasswordViewModel and handles UI updates.
     */
    private fun observeChangePassword() {
        // Observe password change response
        changePasswordViewModel.changePasswordResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    // Show success message
                    showTopSnackBar("Password Changed Successfully")
                    // Clear input fields
                    clearValue()
                    // Close the activity
                    finish()
                }
            }
        }

        // Observe loading state
        changePasswordViewModel.isLoading.observe(this) {
            if (it == true) {
                // Can show a loading shimmer here if needed
            }
        }

        // Observe API errors (e.g., validation errors)
        changePasswordViewModel.apiError.observe(this) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe general failures (network issues, etc.)
        changePasswordViewModel.onFailure.observe(this) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, this))
        }
    }

    /**
     * Shows a Snackbar message aligned at the bottom of the screen.
     *
     * @param message The message to display.
     */
    private fun showTopSnackBar(message: String) {
        // Create a Snackbar with the given message
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        // Customize Snackbar position (bottom of the screen)
        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Set background color of Snackbar
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))

        // Customize text color and alignment
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        // Show Snackbar
        snackBar.show()
    }
}