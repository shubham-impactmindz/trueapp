package com.app.truewebapp.ui.component.main.account
// Package declaration for organizing classes into logical modules

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.ActivityWalletBinding
import com.app.truewebapp.ui.viewmodel.WalletBalanceViewModel
// Import statements for Android, Jetpack, and project-specific classes

class WalletActivity : AppCompatActivity() {
    // ViewBinding object for accessing layout views safely
    lateinit var binding: ActivityWalletBinding

    // ViewModel instance for handling wallet balance API logic
    private lateinit var walletBalanceViewModel: WalletBalanceViewModel

    // Token used for authenticated API calls
    private var token = ""

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the XML layout using ViewBinding
        binding = ActivityWalletBinding.inflate(layoutInflater)

        // Set the content view to the root view of the binding
        setContentView(binding.root)

        // Handle system UI insets (status bar and navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Extract system bar dimensions
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top and bottom padding dynamically to avoid overlap
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets // Return updated insets
        }

        // Set click listener for the back button layout
        binding.backLayout.setOnClickListener {
            // Provide haptic feedback when button is clicked
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ensures feedback even if disabled globally
            )
            // Close the activity and go back
            finish()
        }

        // Initialize WalletBalanceViewModel using ViewModelProvider
        walletBalanceViewModel = ViewModelProvider(this)[WalletBalanceViewModel::class.java]

        // Attach observers to monitor wallet balance updates
        observeWalletBalance()

        // Retrieve stored preferences for token
        val preferences = getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)

        // Build bearer token for API call (empty string if not available)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()

        // Fetch wallet balance from server using token
        walletBalanceViewModel.walletBalance(token)
    }

    // Function to observe LiveData from WalletBalanceViewModel
    private fun observeWalletBalance() {
        // Observe wallet balance API response
        walletBalanceViewModel.walletBalanceResponse.observe(this) { response ->
            response?.let { it ->
                // If API response indicates success
                if (it.success) {
                    // Format balance with £ symbol and update UI
                    "£${it.balance}".also { binding.tvAmount.text = it }
                }
            }
        }

        // Observe loading state (optional UI handling like showing loader)
        walletBalanceViewModel.isLoading.observe(this) {
            // Loader handling can be implemented here
        }

        // Observe API error responses (e.g., invalid token, server error)
        walletBalanceViewModel.apiError.observe(this) {
            // Error handling can be added here
        }

        // Observe failure cases (e.g., no internet, timeout)
        walletBalanceViewModel.onFailure.observe(this) {
            // Failure handling can be added here
        }
    }
}