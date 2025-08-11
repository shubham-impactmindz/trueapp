package com.app.truewebapp.ui.component.main.account

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

class WalletActivity : AppCompatActivity() {
    lateinit var binding: ActivityWalletBinding
    private lateinit var walletBalanceViewModel: WalletBalanceViewModel
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        walletBalanceViewModel = ViewModelProvider(this)[WalletBalanceViewModel::class.java]
        observeWalletBalance()
        val preferences = getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        walletBalanceViewModel.walletBalance(token)
    }

    private fun observeWalletBalance() {
        walletBalanceViewModel.walletBalanceResponse.observe(this) { response ->
            response?.let { it ->

                if (it.success) {
                    "£${it.balance}".also { binding.tvAmount.text = it }
                }
            }
        }

        walletBalanceViewModel.isLoading.observe(this) {

        }

        walletBalanceViewModel.apiError.observe(this) {

        }

        walletBalanceViewModel.onFailure.observe(this) {

        }
    }
}