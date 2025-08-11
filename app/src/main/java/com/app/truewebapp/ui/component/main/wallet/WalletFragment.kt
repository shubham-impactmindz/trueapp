package com.app.truewebapp.ui.component.main.wallet

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

class WalletFragment : Fragment() {
    private lateinit var binding: FragmentWalletBinding
    private lateinit var walletBalanceViewModel: WalletBalanceViewModel
    private var token = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        walletBalanceViewModel.walletBalance(token)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        walletBalanceViewModel = ViewModelProvider(this)[WalletBalanceViewModel::class.java]
        observeWalletBalance()
    }

    private fun observeWalletBalance() {
        walletBalanceViewModel.walletBalanceResponse.observe(viewLifecycleOwner) { response ->
            response?.let { it ->

                if (it.success) {
                    "£${it.balance}".also { binding.tvAmount.text = it }
                }
            }
        }

        walletBalanceViewModel.isLoading.observe(viewLifecycleOwner) {

        }

        walletBalanceViewModel.apiError.observe(viewLifecycleOwner) {

        }

        walletBalanceViewModel.onFailure.observe(viewLifecycleOwner) {

        }
    }
}