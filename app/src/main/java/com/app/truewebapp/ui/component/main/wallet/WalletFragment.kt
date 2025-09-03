package com.app.truewebapp.ui.component.main.wallet

// Import necessary Android and Jetpack libraries
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.FragmentWalletBinding
import com.app.truewebapp.ui.viewmodel.WalletBalanceViewModel

// Fragment class for displaying wallet information
class WalletFragment : Fragment() {

    // ViewBinding instance to access layout views
    private lateinit var binding: FragmentWalletBinding

    // ViewModel to handle wallet balance related API calls and data
    private lateinit var walletBalanceViewModel: WalletBalanceViewModel

    // Token for authentication, retrieved from SharedPreferences
    private var token = ""

    // Called to inflate the Fragment's UI layout
    override fun onCreateView(
        inflater: LayoutInflater, // Used to inflate the XML layout
        container: ViewGroup?,    // Parent view in which the fragment's UI is attached
        savedInstanceState: Bundle? // Saved state if fragment is re-created
    ): View {
        // Inflate the layout using ViewBinding
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        // Return the root view of the layout
        return binding.root
    }

    // Called when the Fragment becomes visible and interactive again
    override fun onResume() {
        super.onResume()
        // Get SharedPreferences to retrieve saved token
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        // Build the Bearer token string
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        // Trigger API call to fetch wallet balance using ViewModel
        walletBalanceViewModel.walletBalance(token)
    }

    // Called after onCreateView, once the view hierarchy is created
    @SuppressLint("ClickableViewAccessibility") // Suppressing accessibility warning (not critical here)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel for wallet balance
        walletBalanceViewModel = ViewModelProvider(this)[WalletBalanceViewModel::class.java]

        // Observe LiveData from ViewModel for wallet updates
        observeWalletBalance()
    }

    // Observes LiveData from WalletBalanceViewModel to update UI
    private fun observeWalletBalance() {
        // Observe wallet balance response from API
        walletBalanceViewModel.walletBalanceResponse.observe(viewLifecycleOwner) { response ->
            response?.let { it ->
                // Check if API returned success
                if (it.success) {
                    // Update UI text with wallet balance (formatted with £ symbol)
                    "£${it.balance}".also { binding.tvAmount.text = it }
                }
            }
        }

        // Observe loading state (can be used to show/hide progress bar)
        walletBalanceViewModel.isLoading.observe(viewLifecycleOwner) {
            // Handle loader UI here if required
        }

        // Observe API error messages
        walletBalanceViewModel.apiError.observe(viewLifecycleOwner) {
            // Handle API error (e.g., show toast or dialog)
        }

        // Observe network or unexpected failures
        walletBalanceViewModel.onFailure.observe(viewLifecycleOwner) {
            // Handle failure (e.g., show error message)
        }
    }
}