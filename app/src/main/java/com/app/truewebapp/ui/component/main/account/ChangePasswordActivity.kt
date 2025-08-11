package com.app.truewebapp.ui.component.main.account

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

class ChangePasswordActivity : AppCompatActivity() {
    lateinit var binding: ActivityChangePasswordBinding
    private lateinit var changePasswordViewModel: ChangePasswordViewModel
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        changePasswordViewModel = ViewModelProvider(this)[ChangePasswordViewModel::class.java]
        observeChangePassword()
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
        binding.llChangePassword.setOnClickListener {
            binding.llChangePassword.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            if (validateInputs()){
                changePasswordViewModel.changePassword(token, ChangePasswordRequest(
                    binding.passwordInput.text.toString().trim(),
                    binding.newPasswordInput.text.toString().trim(),
                    binding.confirmPasswordInput.text.toString().trim()
                ))
            }
        }
    }


    private fun validateInputs(): Boolean {
        var isValid = true
        val password = binding.passwordInput.text.toString().trim()
        val newPassword = binding.newPasswordInput.text.toString().trim()
        val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.passwordInput.error = "Password must be at least 8 characters"
            isValid = false
        }

        if (newPassword.isEmpty()) {
            binding.newPasswordInput.error = "New Password is required"
            isValid = false
        } else if (newPassword.length < 8) {
            binding.newPasswordInput.error = "New Password must be at least 8 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInput.error = "Confirm Password is required"
            isValid = false
        } else if (confirmPassword != newPassword) {
            binding.confirmPasswordInput.error = "New Password & Confirm Password Don't Match"
            isValid = false
        }
        return isValid
    }

    private fun clearValue(){
        binding.passwordInput.text?.clear()
        binding.newPasswordInput.text?.clear()
        binding.confirmPasswordInput.text?.clear()
    }

    private fun observeChangePassword() {
        changePasswordViewModel.changePasswordResponse.observe(this) { response ->
            response?.let {
//                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    showTopSnackBar("Password Changed Successfully")
                    clearValue()
                    finish()
                }
            }
        }

        changePasswordViewModel.isLoading.observe(this) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {

            }
        }

        changePasswordViewModel.apiError.observe(this) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        changePasswordViewModel.onFailure.observe(this) {
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
}