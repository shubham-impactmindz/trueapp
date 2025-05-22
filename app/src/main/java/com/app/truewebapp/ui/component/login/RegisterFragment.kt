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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.register.RegisterRequest
import com.app.truewebapp.databinding.FragmentRegisterBinding
import com.app.truewebapp.ui.viewmodel.RegisterViewModel
import com.app.truewebapp.utils.ApiFailureTypes

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var registerViewModel: RegisterViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        setupTermsAndPrivacy()
        initializeObservers()

        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                val request = RegisterRequest(
                    first_name = binding.firstNameInput.text.toString().trim(),
                    last_name = binding.lastNameInput.text.toString().trim(),
                    mobile = binding.mobileNumberInput.text.toString().trim(),
                    email = binding.emailAddressInput.text.toString().trim(),
                    password = binding.passwordInput.text.toString().trim(),
                    rep_code = binding.repCodeInput.text.toString().trim(),
                    company_name = binding.companyNameInput.text.toString().trim(),
                    address1 = binding.address1Input.text.toString().trim(),
                    address2 = binding.address2Input.text.toString().trim(),
                    city = binding.cityInput.text.toString().trim(),
                    postcode = binding.postCodeInput.text.toString().trim(),
                    country = binding.countryInput.text.toString().trim(),
                )
                registerViewModel.register(request)
            }
        }
    }
    private fun validateInputs(): Boolean {
        var isValid = true

        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val mobile = binding.mobileNumberInput.text.toString().trim()
        val email = binding.emailAddressInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val repCode = binding.repCodeInput.text.toString().trim()
        val companyName = binding.companyNameInput.text.toString().trim()
        val address1 = binding.address1Input.text.toString().trim()
        val address2 = binding.address2Input.text.toString().trim()
        val city = binding.cityInput.text.toString().trim()
        val postcode = binding.postCodeInput.text.toString().trim()
        val country = binding.countryInput.text.toString().trim()

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

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.passwordInput.error = "Password must be at least 8 characters"
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

        return isValid
    }

    private fun clearValue(){
        binding.firstNameInput.text?.clear()
        binding.lastNameInput.text?.clear()
        binding.mobileNumberInput.text?.clear()
        binding.emailAddressInput.text?.clear()
        binding.passwordInput.text?.clear()
        binding.repCodeInput.text?.clear()
        binding.companyNameInput.text?.clear()
        binding.address1Input.text?.clear()
        binding.address2Input.text?.clear()
        binding.cityInput.text?.clear()
        binding.postCodeInput.text?.clear()
        binding.countryInput.text?.clear()
    }

    private fun initializeObservers() {
        registerViewModel.registerResponse.observe(
            viewLifecycleOwner, Observer {
                it?.let {
                    if (it.status) {
                        clearValue()
                        showMessage("Registered Successfully")
                        showCustomRegisterDialog()
                        // Navigate or store preferences here
                    } else {
                        showMessage(it.message)
                    }
                }
            }
        )

        registerViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        registerViewModel.apiError.observe(viewLifecycleOwner, Observer {
            showMessage(it ?: "Unexpected API error")
        })

        registerViewModel.onFailure.observe(viewLifecycleOwner, Observer {
            showMessage(ApiFailureTypes().getFailureMessage(it, context))
        })
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

    private fun fetchHtmlAndOpen(type: String) {

        requireActivity().runOnUiThread {
            binding.progressBarLayout.visibility = View.VISIBLE
        }

    }

    private fun showToast(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCustomRegisterDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_register)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnOk = dialog.findViewById<Button>(R.id.btnOk)

        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun showMessage(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}

