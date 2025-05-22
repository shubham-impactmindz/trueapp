package com.app.truewebapp.ui.component.login

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginRequest
import com.app.truewebapp.databinding.FragmentLoginBinding
import com.app.truewebapp.ui.component.main.MainActivity
import com.app.truewebapp.ui.viewmodel.LoginViewModel
import com.app.truewebapp.utils.ApiFailureTypes

class LoginFragment : Fragment() {

    lateinit var binding: FragmentLoginBinding
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        setupTermsAndPrivacy()
        initializeObservers()

        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                val request = LoginRequest(
                    email = binding.emailInput.text.toString().trim(),
                    password = binding.passwordInput.text.toString().trim(),
                )
                loginViewModel.login(request)
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()


        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Invalid email format"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.passwordInput.error = "Password must be at least 8 characters"
            isValid = false
        }
        return isValid
    }

    private fun clearValue(){
        binding.emailInput.text?.clear()
        binding.passwordInput.text?.clear()
    }

    private fun initializeObservers() {
        loginViewModel.loginResponse.observe(
            viewLifecycleOwner, Observer {
                it?.let {
                    if (it.status) {
                        if (it.user.admin_approval == "Pending" || it.user.admin_approval == "Declined" ){
                            showCustomRegisterDialog()
                        }
                        else{
                            clearValue()
                            showMessage("Login Successfully")
                            val preferences = context?.getSharedPreferences(SHARED_PREF_NAME,
                                AppCompatActivity.MODE_PRIVATE
                            )
                            preferences?.edit()?.putString("token", it.token)?.apply()
                            preferences?.edit()?.putString("userId", it.user.id.toString())?.apply()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        // Navigate or store preferences here
                    } else {
                        showMessage(it.message)
                    }
                }
            }
        )

        loginViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        loginViewModel.apiError.observe(viewLifecycleOwner, Observer {
            showMessage(it ?: "Unexpected API error")
        })

        loginViewModel.onFailure.observe(viewLifecycleOwner, Observer {
            showMessage(ApiFailureTypes().getFailureMessage(it, context))
        })
    }


    private fun showMessage(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    
    private fun setupTermsAndPrivacy() {
        val fullText = "By selecting Login, you agree to our Terms and Conditions and Privacy Policy"
        val spannableString = SpannableString(fullText)

        val termsClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(requireContext(), WebViewActivity::class.java)
                intent.putExtra("title", "Terms and Conditions")
                startActivity(intent)
            }
        }

        val privacyClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(requireContext(), WebViewActivity::class.java)
                intent.putExtra("title", "Privacy Policy")
                startActivity(intent)
            }
        }

        val termsStart = fullText.indexOf("Terms and Conditions")
        val termsEnd = termsStart + "Terms and Conditions".length
        spannableString.setSpan(termsClickable, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1E88E5")), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val privacyStart = fullText.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length
        spannableString.setSpan(privacyClickable, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1E88E5")), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.termsTextView.text = spannableString
        binding.termsTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun showCustomRegisterDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_register)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnOk = dialog.findViewById<Button>(R.id.btnOk)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)

        tvTitle.text = "Admin Approval Pending"
        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}

