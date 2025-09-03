package com.app.truewebapp.ui.component.login

// Import required Android and Jetpack libraries
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.reset_password.ResetPasswordRequest
import com.app.truewebapp.databinding.ActivityResetPasswordBinding
import com.app.truewebapp.ui.viewmodel.ResetPasswordViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar

// Activity to handle Reset Password screen logic
class ResetPasswordActivity : AppCompatActivity() {

    // ViewBinding reference for accessing layout views
    lateinit var binding: ActivityResetPasswordBinding

    // ViewModel instance for handling Reset Password business logic and API calls
    private lateinit var resetPasswordViewModel: ResetPasswordViewModel

    // Lifecycle method: Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)

        // Set inflated layout as content view
        setContentView(binding.root)

        // Initialize ResetPasswordViewModel using ViewModelProvider
        resetPasswordViewModel = ViewModelProvider(this)[ResetPasswordViewModel::class.java]

        // Setup LiveData observers
        initializeObservers()

        // Handle system bar insets (status & navigation bars) for proper layout adjustment
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Get dimensions of system bars
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Add padding to root view to avoid overlap with system bars
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        // Handle back button click -> Close activity
        binding.backLayout.setOnClickListener {
            finish()
        }

        // Handle reset button click
        binding.btnReset.setOnClickListener {
            // Validate inputs before making API call
            if (validateInputs()) {
                // Create request object for reset password API
                val request = ResetPasswordRequest(
                    email = binding.emailInput.text.toString().trim(),
                )
                // Call ViewModel function to trigger API request
                resetPasswordViewModel.resetPassword(request)
            }
        }
    }

    // Function to initialize and observe LiveData from ViewModel
    private fun initializeObservers() {
        // Observe reset password API response LiveData
        resetPasswordViewModel.resetPasswordResponse.observe(
            this, Observer {
                it?.let {
                    if (it.status) {
                        // Success case: clear inputs and show success popup
                        clearValue()
                        showPopupMessage("A link has been sent to your email to reset your password.")
                    } else {
                        // Failure case from API response
                        showPopupMessage(it.message)
                    }
                }
            }
        )

        // Observe loading state LiveData to show/hide progress bar
        resetPasswordViewModel.isLoading.observe(this, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        // Observe API error LiveData
        resetPasswordViewModel.apiError.observe(this, Observer {
            // Show popup with exact API error message
            showPopupMessage(it)
        })

        // Observe network failure LiveData
        resetPasswordViewModel.onFailure.observe(this, Observer {
            // Show network failure message using ApiFailureTypes helper
            showPopupMessage(ApiFailureTypes().getFailureMessage(it, this))
        })
    }

    // Function to show an AlertDialog popup message
    private fun showPopupMessage(message: String, title: String = "") {
        AlertDialog.Builder(this)
            .setTitle(title) // Set dialog title
            .setMessage(message) // Set dialog message
            .setPositiveButton("OK", null) // Add OK button
            .setCancelable(true) // Allow cancel
            .show() // Show dialog
    }

    // Function to show a custom styled Snackbar message
    private fun showTopSnackBar(message: String) {
        // Create Snackbar with message
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        // Customize Snackbar view layout params
        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM // Position Snackbar at bottom
        params.bottomMargin = 50 // Add bottom margin
        view.layoutParams = params

        // Set custom background color
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))

        // Customize text inside Snackbar
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE) // Change text color to white
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER // Center align text

        // Show Snackbar
        snackBar.show()
    }

    // Function to clear input fields after successful API call
    private fun clearValue(){
        binding.emailInput.text?.clear()
    }

    // Function to validate email input field before API request
    private fun validateInputs(): Boolean {
        var isValid = true
        // Get email input from EditText
        val email = binding.emailInput.text.toString().trim()

        // Check if email is empty
        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            isValid = false
        }
        // Check if email format is valid
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Invalid email format"
            isValid = false
        }
        return isValid
    }
}