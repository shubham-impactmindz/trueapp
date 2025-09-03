package com.app.truewebapp.ui.component.login // Package declaration, defines this file's namespace

// Import required Android and app-specific classes
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson

/**
 * Fragment responsible for handling user login functionality.
 * Includes UI initialization, input validation, login API call,
 * response handling, and navigation.
 */
class LoginFragment : Fragment() {

    // View binding for accessing UI components of FragmentLogin layout
    lateinit var binding: FragmentLoginBinding

    // ViewModel responsible for handling login API logic
    private lateinit var loginViewModel: LoginViewModel

    /**
     * Called to create and inflate the fragment's view hierarchy.
     * @param inflater Used to inflate the layout XML
     * @param container Optional parent view
     * @param savedInstanceState Previous state if re-created
     * @return Root view of the binding
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called immediately after onCreateView().
     * Sets up ViewModel, observers, and click listeners.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel for login API
        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Setup clickable Terms & Privacy text
        setupTermsAndPrivacy()

        // Setup observers for API responses
        initializeObservers()

        // Handle login button click
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                // Build login request with email & password
                val request = LoginRequest(
                    email = binding.emailInput.text.toString().trim(),
                    password = binding.passwordInput.text.toString().trim(),
                )
                // Call login API
                loginViewModel.login(request)
            }
        }

        // Handle reset password navigation
        binding.btnResetPassword.setOnClickListener {
            val intent = Intent(context, ResetPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Validates login form inputs (email and password).
     * @return true if inputs are valid, false otherwise
     */
    private fun validateInputs(): Boolean {
        var isValid = true
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        // Email validation
        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Invalid email format"
            isValid = false
        }

        // Password validation
        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.passwordInput.error = "Password must be at least 8 characters"
            isValid = false
        }
        return isValid
    }

    /**
     * Clears email and password fields after successful login.
     */
    private fun clearValue(){
        binding.emailInput.text?.clear()
        binding.passwordInput.text?.clear()
    }

    /**
     * Initializes LiveData observers to handle API responses from ViewModel.
     */
    private fun initializeObservers() {
        // Observe login response
        loginViewModel.loginResponse.observe(
            viewLifecycleOwner, Observer {
                it?.let {
                    if (it.status) { // If login is successful
                        if (it.user_detail.admin_approval == "Pending" || it.user_detail.admin_approval == "Declined" ){
                            // Show dialog if admin approval is not granted
                            showCustomRegisterDialog()
                        }
                        else{
                            // Clear input fields
                            clearValue()

                            // Show success message
                            showTopSnackBar("Login Successfully")

                            // Save user data & token in SharedPreferences
                            val preferences = context?.getSharedPreferences(
                                SHARED_PREF_NAME,
                                AppCompatActivity.MODE_PRIVATE
                            )
                            preferences?.edit()?.putString("token", it.token)?.apply()
                            preferences?.edit()?.putString("userId", it.user_detail.id.toString())?.apply()

                            // Convert user object to JSON string for storage
                            val gson = Gson()
                            val jsonString = gson.toJson(it)
                            preferences?.edit()?.putString("userResponse", jsonString)?.apply()

                            // Navigate to MainActivity
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    } else {
                        // Show API error message
                        showTopSnackBar(it.message)
                    }
                }
            }
        )

        // Observe loading state (show/hide progress bar)
        loginViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        // Observe API error messages
        loginViewModel.apiError.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(it)
        })

        // Observe network or unexpected failures
        loginViewModel.onFailure.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        })
    }

    /**
     * Displays a custom Snackbar at the bottom of the screen with styled text.
     * @param message Message to show
     */
    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)

        // Customize snackbar layout parameters
        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Set background color
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))

        // Customize text color and alignment
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }

    /**
     * Sets up Terms and Privacy clickable text using SpannableString.
     * Opens WebViewActivity when clicked.
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

        // Apply spans for clickable sections
        val termsStart = fullText.indexOf("Terms and Conditions")
        val termsEnd = termsStart + "Terms and Conditions".length
        spannableString.setSpan(termsClickable, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1E88E5")), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val privacyStart = fullText.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length
        spannableString.setSpan(privacyClickable, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#1E88E5")), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set text with spans and enable click
        binding.termsTextView.text = spannableString
        binding.termsTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Displays a custom dialog when admin approval is pending or declined.
     */
    private fun showCustomRegisterDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_register)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        // Initialize dialog views
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)

        // Set title and click listener
        tvTitle.text = "Admin Approval Pending"
        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Called when view is destroyed.
     * Used to clean up resources if required.
     */
    override fun onDestroyView() {
        super.onDestroyView()
    }
}
