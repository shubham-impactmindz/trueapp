package com.app.truewebapp.ui.component.login

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

class ResetPasswordActivity : AppCompatActivity() {
    lateinit var binding: ActivityResetPasswordBinding
    private lateinit var resetPasswordViewModel: ResetPasswordViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        resetPasswordViewModel = ViewModelProvider(this)[ResetPasswordViewModel::class.java]
        initializeObservers()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }
        binding.backLayout.setOnClickListener {
            finish()
        }
        binding.btnReset.setOnClickListener {
            if (validateInputs()) {
                val request = ResetPasswordRequest(
                    email = binding.emailInput.text.toString().trim(),
                )
                resetPasswordViewModel.resetPassword(request)
            }
        }
    }

    private fun initializeObservers() {
        resetPasswordViewModel.resetPasswordResponse.observe(
            this, Observer {
                it?.let {
                    if (it.status) {
                        clearValue()
                        showPopupMessage("A link has been sent to your email to reset your password.")
                    } else {
                        showPopupMessage(it.message)
                    }
                }
            }
        )

        resetPasswordViewModel.isLoading.observe(this, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        resetPasswordViewModel.apiError.observe(this, Observer {
            showPopupMessage(it) // Show exact API error in popup
        })

        resetPasswordViewModel.onFailure.observe(this, Observer {
            showPopupMessage(ApiFailureTypes().getFailureMessage(it, this))
        })
    }

    private fun showPopupMessage(message: String, title: String = "") {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show()
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

    private fun clearValue(){
        binding.emailInput.text?.clear()
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = binding.emailInput.text.toString().trim()


        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Invalid email format"
            isValid = false
        }
        return isValid
    }
}