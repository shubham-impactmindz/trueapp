package com.app.truewebapp.ui.component.login // Package declaration for login-related components

// Required imports for Android components, views, and utilities
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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.register.RegisterRequest
import com.app.truewebapp.databinding.FragmentRegisterBinding
import com.app.truewebapp.ui.viewmodel.RegisterViewModel
import com.app.truewebapp.ui.viewmodel.VerifyRepViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment responsible for handling user registration.
 * Validates inputs, integrates with RegisterViewModel and VerifyRepViewModel,
 * observes API responses, and displays success/error messages.
 */
class RegisterFragment : Fragment() {

    // View binding for fragment_register.xml layout
    private lateinit var binding: FragmentRegisterBinding

    // ViewModel for handling registration API calls
    private lateinit var registerViewModel: RegisterViewModel

    // ViewModel for verifying representative codes before registration
    private lateinit var verifyRepViewModel: VerifyRepViewModel

    /**
     * Called to create and inflate the fragment's view hierarchy.
     * @param inflater LayoutInflater to inflate XML
     * @param container Optional parent view
     * @param savedInstanceState Previous state if available
     * @return Root view of the binding
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout using view binding
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called after the view is created.
     * Initializes ViewModels, sets up terms text, and registers observers.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModels for registration and rep code verification
        registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        verifyRepViewModel = ViewModelProvider(this)[VerifyRepViewModel::class.java]

        // Setup terms and privacy policy text with clickable spans
        setupTermsAndPrivacy()

        // Register LiveData observers for ViewModels
        registerObservers()
        verifyObservers()

        // Handle register button click
        binding.btnRegister.setOnClickListener {
            if (validateInputs()) { // Validate user inputs first
                if(binding.repCodeInput.text.toString().trim().isEmpty()){
                    // If no rep code entered, proceed with registration directly
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
                        referral_code = binding.referralCodeInput.text.toString().trim(),
                    )
                    registerViewModel.register(request)
                } else {
                    // If rep code entered, verify it first
                    verifyRepViewModel.verify(binding.repCodeInput.text.toString().trim())
                }
            }
        }
    }

    /**
     * Validates user inputs from the registration form.
     * @return true if all inputs are valid, false otherwise
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        // Extract values from input fields
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

        // Validation rules for each input field
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

    /**
     * Clears all form inputs after successful registration.
     */
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

    /**
     * Observes RegisterViewModel LiveData to handle registration responses.
     */
    private fun registerObservers() {
        // Observe registration API response
        registerViewModel.registerResponse.observe(
            viewLifecycleOwner, Observer {
                it?.let {
                    if (it.status) {
                        // On success: clear form, show snackbar and dialog
                        clearValue()
                        showTopSnackBar("Registered Successfully")
                        showCustomRegisterDialog()
                    } else {
                        // On error: show API message
                        showTopSnackBar(it.message)
                    }
                }
            }
        )

        // Observe loading state
        registerViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        // Observe API errors
        registerViewModel.apiError.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(it ?: "Unexpected API error")
        })

        // Observe failure cases (like network errors)
        registerViewModel.onFailure.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        })
    }

    /**
     * Observes VerifyRepViewModel LiveData to handle rep code verification.
     */
    private fun verifyObservers() {
        // Observe rep code verification response
        verifyRepViewModel.verifyRepResponse.observe(
            viewLifecycleOwner, Observer {
                it?.let {
                    if (it.status) {
                        // If verification passes, continue registration
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
                            referral_code = binding.referralCodeInput.text.toString().trim(),
                        )
                        registerViewModel.register(request)
                    } else {
                        // If verification fails, show message
                        showTopSnackBar("Please Check Rep Code Again")
                    }
                }
            }
        )

        // Observe loading state
        verifyRepViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        // Observe API errors
        verifyRepViewModel.apiError.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(it ?: "Unexpected API error")
        })

        // Observe failure cases (network issues, etc.)
        verifyRepViewModel.onFailure.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        })
    }

    /**
     * Configures Terms and Privacy clickable text using SpannableString.
     * Launches WebViewActivity when clicked.
     */
    private fun setupTermsAndPrivacy() {
        val fullText = "By selecting Login, you agree to our Terms and Conditions and Privacy Policy"
        val spannableString = SpannableString(fullText)

        // Clickable span for Terms
        val termsClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(requireContext(), WebViewActivity::class.java)
                intent.putExtra("title", "Terms and Conditions")
                startActivity(intent)
            }
        }

        // Clickable span for Privacy
        val privacyClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(requireContext(), WebViewActivity::class.java)
                intent.putExtra("title", "Privacy Policy")
                startActivity(intent)
            }
        }

        // Apply clickable spans with blue color
        val termsStart = fullText.indexOf("Terms and Conditions")
        val termsEnd = termsStart + "Terms and Conditions".length
        spannableString.setSpan(termsClickable, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1E88E5")), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val privacyStart = fullText.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length
        spannableString.setSpan(privacyClickable, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1E88E5")), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set text and enable link movement
        binding.termsTextView.text = spannableString
        binding.termsTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Example function placeholder for fetching HTML and opening it.
     * Currently just shows progress bar.
     */
    private fun fetchHtmlAndOpen(type: String) {
        requireActivity().runOnUiThread {
            binding.progressBarLayout.visibility = View.VISIBLE
        }
    }

    /**
     * Utility method to show a Toast message on the UI thread.
     * @param message Message to display
     */
    private fun showToast(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Displays a custom dialog after successful registration.
     */
    private fun showCustomRegisterDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_register)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnOk = dialog.findViewById<Button>(R.id.btnOk)

        // Close dialog when OK is pressed
        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))

        // Customize text properties
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }
}
