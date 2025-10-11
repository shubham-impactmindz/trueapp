package com.app.truewebapp.ui.component.main.dashboard

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.referral.ReferralRequest
import com.app.truewebapp.databinding.ActivityReferralBinding
import com.app.truewebapp.ui.viewmodel.ReferralViewModel
import com.google.android.material.snackbar.Snackbar

class ReferralActivity : AppCompatActivity() {
    lateinit var binding: ActivityReferralBinding
    // ViewModel for handling referral API calls
    private lateinit var referralViewModel: ReferralViewModel
    // Tokens parameters
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModels for referral
        referralViewModel = ViewModelProvider(this)[ReferralViewModel::class.java]
        // Get auth token from SharedPreferences
        val preferences = getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        observeReferral()
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

        binding.sendReferralLayout.setOnClickListener {
            binding.sendReferralLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            if (validateInputs()){
                val request = ReferralRequest(

                    name = binding.referralNameInput.text.toString().trim(),
                    city = binding.referralCityInput.text.toString().trim(),
                    email = binding.referralNumberInput.text.toString().trim(),
                )
                referralViewModel.sendReferral(token,request)
            }
        }
    }

    /**
     * Displays a custom styled Snackbar at the bottom of the screen.
     * @param message Message to show
     */
    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Customize background color
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))

        // Customize text properties
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }

    /**
     * Observes ReferralViewModel LiveData to handle referral responses.
     */
    private fun observeReferral() {
        // Observe referral API response
        referralViewModel.referralModel.observe(this, Observer {
            it?.let {
                if (it.status) {
                    // On success: clear form, hide loader, show snackbar
                    clearInputs()
                    showTopSnackBar("Referral Sent Successfully")
                }
            }
        })

        // Observe loading state
        referralViewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBarLayout.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        })

        // Observe API errors
        referralViewModel.apiError.observe(this, Observer {
            showTopSnackBar(it ?: "Unexpected API error")
        })

        // Observe failure cases (like network errors)
        referralViewModel.onFailure.observe(this, Observer {
            showTopSnackBar("Network error. Please try again.")
        })
    }

    private fun clearInputs() {
        binding.referralNameInput.text?.clear()
        binding.referralCityInput.text?.clear()
        binding.referralNumberInput.text?.clear()
    }


    /**
     * Validates user inputs from the registration form.
     * @return true if all inputs are valid, false otherwise
     */
    private fun validateInputs(): Boolean {
        var isValid = true


        val name = binding.referralNameInput.text.toString().trim()
        val city = binding.referralCityInput.text.toString().trim()
        val email = binding.referralNumberInput.text.toString().trim()
        // Validation rules for each input field
        if (name.isEmpty()) {
            binding.referralNameInput.error = "Name is required"
            isValid = false
        }

        if (city.isEmpty()) {
            binding.referralCityInput.error = "City is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.referralNumberInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.referralNumberInput.error = "Invalid email format"
            isValid = false
        }

        return isValid
    }
}