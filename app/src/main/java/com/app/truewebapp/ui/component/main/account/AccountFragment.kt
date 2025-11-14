package com.app.truewebapp.ui.component.main.account

// Import necessary Android and Kotlin libraries
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.FragmentAccountBinding
import com.app.truewebapp.ui.component.login.LoginActivity
import com.app.truewebapp.ui.component.login.WebViewActivity
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.dashboard.ReferralActivity
import com.app.truewebapp.ui.viewmodel.DeleteAccountViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AccountFragment is responsible for displaying and handling
 * user account-related actions like profile, orders, rewards,
 * services, wallet, notifications, logout, and delete account.
 */
class AccountFragment : Fragment() {

    // Binding object for accessing views from the layout file
    lateinit var binding: FragmentAccountBinding

    // ViewModel for delete account API call
    private lateinit var deleteAccountViewModel: DeleteAccountViewModel

    // Token for authenticated API requests
    private var token = ""

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    override fun onCreateView(
        inflater: LayoutInflater, // Used to inflate layout XML
        container: ViewGroup?,   // Parent view group
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using ViewBinding
        binding = FragmentAccountBinding.inflate(inflater, container, false)

        // Initialize the DeleteAccountViewModel
        deleteAccountViewModel = ViewModelProvider(this)[DeleteAccountViewModel::class.java]

        // Setup observers to listen for API responses
        initializeObservers()

        // Return the root view of this fragment
        return binding.root
    }

    /**
     * Called immediately after onCreateView() has returned.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup click listeners for account-related options
        setupAccountPage()
    }

    /**
     * Initializes observers for LiveData in DeleteAccountViewModel.
     */
    private fun initializeObservers() {
        // Observe change password/delete account response
        deleteAccountViewModel.changePasswordResponse.observe(
            viewLifecycleOwner, Observer {
                it?.let {
                    if (it.status) {
                        // If successful, perform logout
                        performLogout()
                    } else {
                        // Show error message from API
                        showTopSnackBar(it.message)
                    }
                }
            }
        )

        // Observe loading state for showing/hiding progress bar
        deleteAccountViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        // Observe API error messages
        deleteAccountViewModel.apiError.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(it) // Show exact API error
        })

        // Observe failure types (network, timeout, etc.)
        deleteAccountViewModel.onFailure.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        })
    }

    /**
     * Displays a Snackbar at the bottom of the screen with a custom style.
     */
    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)

        // Customize Snackbar position and margin
        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Set custom background color
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))

        // Customize text style
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }

    /**
     * Sets up click listeners for all account page options.
     * Each option navigates to its respective activity.
     */
    private fun setupAccountPage() {
        // Example: REP details navigation
        binding.repLayout.setOnClickListener {
            binding.repLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY, // Haptic feedback vibration
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, RepDetailActivity::class.java)
            startActivity(intent)
        }

        // My Orders navigation
        binding.myOrdersLayout.setOnClickListener {
            binding.myOrdersLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, MyOrdersActivity::class.java)
            startActivity(intent)
        }

        // My Rewards navigation
        binding.myRewardsLayout.setOnClickListener {
            binding.myRewardsLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, ReferralActivity::class.java)
            startActivity(intent)
        }

        // Company Address navigation
        binding.companyAddressLayout.setOnClickListener {
            binding.companyAddressLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, CompanyAddressActivity::class.java)
            startActivity(intent)
        }

        // Payment Options navigation
        binding.paymentOptionLayout.setOnClickListener {
            binding.paymentOptionLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, PaymentOptionsActivity::class.java)
            startActivity(intent)
        }

        // Services navigation
        binding.servicesLayout.setOnClickListener {
            binding.servicesLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, ServicesActivity::class.java)
            startActivity(intent)
        }

        // Profile navigation
        binding.profileLayout.setOnClickListener {
            binding.profileLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Wallet navigation
        binding.walletLayout.setOnClickListener {
            binding.walletLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, WalletActivity::class.java)
            startActivity(intent)
        }

        // Notifications navigation
        binding.notificationLayout.setOnClickListener {
            binding.notificationLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, NotificationsActivity::class.java)
            startActivity(intent)
        }

        // Change password navigation
        binding.passwordLayout.setOnClickListener {
            binding.passwordLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(context, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Terms and Conditions navigation (WebView)
        binding.termsLayout.setOnClickListener {
            binding.termsLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("title", "Terms and Conditions")
            startActivity(intent)
        }

        // Privacy Policy navigation (WebView)
        binding.privacyLayout.setOnClickListener {
            binding.privacyLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("title", "Privacy Policy")
            startActivity(intent)
        }

        // Logout dialog
        binding.logoutLayout.setOnClickListener {
            binding.logoutLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            showCustomLogoutDialog()
        }

        // Delete account dialog
        binding.deleteLayout.setOnClickListener {
            binding.deleteLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            showCustomDeleteDialog()
        }
    }

    /**
     * Shows a custom dialog asking for delete account confirmation.
     */
    private fun showCustomDeleteDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_logout) // Reusing same layout
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        // Find dialog views
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnLogout = dialog.findViewById<Button>(R.id.btnLogout)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)

        // Customize text
        tvTitle.text = "Delete Account"
        tvMessage.text = "Are you sure you want to delete account?"
        btnLogout.text = "Delete"

        // Cancel button click listener
        btnCancel.setOnClickListener {
            btnCancel.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            dialog.dismiss()
        }

        // Delete button click listener
        btnLogout.setOnClickListener {
            btnLogout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            dialog.dismiss()

            // Fetch stored token from SharedPreferences
            val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
            token = "Bearer " + preferences?.getString("token", "").orEmpty()

            // Call ViewModel to delete account
            deleteAccountViewModel.deleteAccount(token)
        }

        dialog.show()
    }

    /**
     * Shows a custom dialog asking for logout confirmation.
     */
    private fun showCustomLogoutDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnLogout = dialog.findViewById<Button>(R.id.btnLogout)

        // Cancel button
        btnCancel.setOnClickListener {
            btnCancel.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            dialog.dismiss()
        }

        // Logout button
        btnLogout.setOnClickListener {
            btnLogout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            dialog.dismiss()
            performLogout()
        }

        dialog.show()
    }

    /**
     * Utility function to show a short Toast message.
     */
    private fun showToast(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Performs logout by clearing shared preferences and cart database.
     * Redirects user to LoginActivity.
     */
    private fun performLogout() {
        // Clear shared preferences (user session)
        val sharedPreferences = context?.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.clear()?.apply()

        // Clear cart data from local Room database
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val cartDao = CartDatabase.getInstance(requireContext()).cartDao()
                cartDao.clearCart()
            }

            // Navigate to LoginActivity with cleared back stack
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Show success message
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }
}