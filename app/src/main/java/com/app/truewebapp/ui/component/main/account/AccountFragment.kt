package com.app.truewebapp.ui.component.main.account

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.truewebapp.R
import com.app.truewebapp.databinding.FragmentAccountBinding
import com.app.truewebapp.ui.component.login.LoginActivity

class AccountFragment : Fragment() {
    lateinit var binding: FragmentAccountBinding



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAccountPage()
    }

    private fun setupAccountPage() {
        binding.repLayout.setOnClickListener {
            val intent = Intent(context, RepDetailActivity::class.java)
            startActivity(intent)
        }
        binding.myOrdersLayout.setOnClickListener {
            val intent = Intent(context, MyOrdersActivity::class.java)
            startActivity(intent)
        }
        binding.myRewardsLayout.setOnClickListener {
            val intent = Intent(context, RewardsActivity::class.java)
            startActivity(intent)
        }
        binding.companyAddressLayout.setOnClickListener {
            val intent = Intent(context, CompanyAddressActivity::class.java)
            startActivity(intent)
        }
        binding.paymentOptionLayout.setOnClickListener {
            val intent = Intent(context, PaymentOptionsActivity::class.java)
            startActivity(intent)
        }
        binding.servicesLayout.setOnClickListener {
            val intent = Intent(context, ServicesActivity::class.java)
            startActivity(intent)
        }
        binding.profileLayout.setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java)
            startActivity(intent)
        }
        binding.walletLayout.setOnClickListener {
            val intent = Intent(context, WalletActivity::class.java)
            startActivity(intent)
        }
        binding.notificationLayout.setOnClickListener {
            val intent = Intent(context, NotificationsActivity::class.java)
            startActivity(intent)
        }
        binding.passwordLayout.setOnClickListener {
            val intent = Intent(context, ChangePasswordActivity::class.java)
            startActivity(intent)
        }
        binding.logoutLayout.setOnClickListener {
            showCustomLogoutDialog()
        }
        binding.deleteLayout.setOnClickListener {
            showCustomDeleteDialog()
        }
    }

    private fun showCustomDeleteDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnLogout = dialog.findViewById<Button>(R.id.btnLogout)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        tvTitle.setText("Delete Account")
        tvMessage.setText("Are you sure you want to delete account?")
        btnLogout.setText("Delete")

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnLogout.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        dialog.show()
    }

    private fun showCustomLogoutDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnLogout = dialog.findViewById<Button>(R.id.btnLogout)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnLogout.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        dialog.show()
    }

    private fun performLogout() {
        val sharedPreferences = context?.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.clear()?.apply()

        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

}