package com.app.truewebapp.ui.component.main.account

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
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.FragmentAccountBinding
import com.app.truewebapp.ui.component.login.LoginActivity
import com.app.truewebapp.ui.component.login.WebViewActivity
import com.app.truewebapp.ui.viewmodel.DeleteAccountViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar

class AccountFragment : Fragment() {
    lateinit var binding: FragmentAccountBinding
    private lateinit var deleteAccountViewModel: DeleteAccountViewModel
    private var token = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        deleteAccountViewModel = ViewModelProvider(this)[DeleteAccountViewModel::class.java]
        initializeObservers()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAccountPage()
    }


    private fun initializeObservers() {
        deleteAccountViewModel.changePasswordResponse.observe(
            viewLifecycleOwner, Observer {
                it?.let {
                    if (it.status) {
                        performLogout()
                        // Navigate or store preferences here
                    } else {
                        showTopSnackBar(it.message)
                    }
                }
            }
        )

        deleteAccountViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        deleteAccountViewModel.apiError.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(it) // Now this will show exact API message
        })

        deleteAccountViewModel.onFailure.observe(viewLifecycleOwner, Observer {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        })
    }

    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)

        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)) // customize color

        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }

    private fun setupAccountPage() {
        binding.repLayout.setOnClickListener {
            binding.repLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, RepDetailActivity::class.java)
            startActivity(intent)
        }
        binding.myOrdersLayout.setOnClickListener {
            binding.myOrdersLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, MyOrdersActivity::class.java)
            startActivity(intent)
        }
        binding.myRewardsLayout.setOnClickListener {
            binding.myRewardsLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, RewardsActivity::class.java)
            startActivity(intent)
        }
        binding.companyAddressLayout.setOnClickListener {
            binding.companyAddressLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, CompanyAddressActivity::class.java)
            startActivity(intent)
        }
        binding.paymentOptionLayout.setOnClickListener {
            binding.paymentOptionLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, PaymentOptionsActivity::class.java)
            startActivity(intent)
        }
        binding.servicesLayout.setOnClickListener {
            binding.servicesLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, ServicesActivity::class.java)
            startActivity(intent)
        }
        binding.profileLayout.setOnClickListener {
            binding.profileLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, ProfileActivity::class.java)
            startActivity(intent)
        }
        binding.walletLayout.setOnClickListener {
            binding.walletLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, WalletActivity::class.java)
            startActivity(intent)
        }
        binding.notificationLayout.setOnClickListener {
            binding.notificationLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, NotificationsActivity::class.java)
            startActivity(intent)
        }
        binding.passwordLayout.setOnClickListener {
            binding.passwordLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, ChangePasswordActivity::class.java)
            startActivity(intent)
        }
        binding.termsLayout.setOnClickListener {
            binding.termsLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("title", "Terms and Conditions")
            startActivity(intent)
        }
        binding.privacyLayout.setOnClickListener {
            binding.privacyLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("title", "Privacy Policy")
            startActivity(intent)
        }
        binding.logoutLayout.setOnClickListener {
            binding.logoutLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            showCustomLogoutDialog()
        }
        binding.deleteLayout.setOnClickListener {
            binding.deleteLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
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
            btnCancel.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            dialog.dismiss()
        }

        btnLogout.setOnClickListener {
            btnLogout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            dialog.dismiss()

            val preferences =context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
            token = "Bearer " + preferences?.getString("token", "").orEmpty()
            deleteAccountViewModel.deleteAccount(token)
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
            btnCancel.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            dialog.dismiss()
        }

        btnLogout.setOnClickListener {
            btnLogout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            dialog.dismiss()
            performLogout()
        }

        dialog.show()
    }

    private fun showToast(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogout() {
        val sharedPreferences = context?.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.clear()?.apply()

        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

}