package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.databinding.ActivityRepDetailBinding
import com.app.truewebapp.ui.viewmodel.UserProfileViewModel
import com.google.gson.Gson

class RepDetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityRepDetailBinding
    lateinit var loginResponse: LoginResponse
    private lateinit var userProfileViewModel: UserProfileViewModel
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRepDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        userProfileViewModel = ViewModelProvider(this)[UserProfileViewModel::class.java]
        val userJson = preferences.getString("userResponse", "")
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"
        observeUserProfile()
        userProfileViewModel.userProfile(token)
        loginResponse = Gson().fromJson(userJson, LoginResponse::class.java)
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
    }

    private fun observeUserProfile() {
        userProfileViewModel.userProfileResponse.observe(this) { response ->
            response?.let {
                if (it.status) {

                    if (it.rep_details == null){
                        binding.repLayout.visibility = View.GONE
                        binding.phoneNumberLayout.visibility = View.GONE
                        binding.emailLayout.visibility = View.GONE
                        binding.viewRep.visibility = View.GONE
                        binding.viewEmail.visibility = View.GONE
                        binding.repCodeLayout.visibility = View.GONE
                        binding.noDataTextView.visibility = View.VISIBLE
                    }else {

                        val name = it.rep_details.name.trim()
                        val initials = name
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .map { it.first().uppercaseChar() }
                            .joinToString(" ")
                        binding.tvInitial.text = initials
                        binding.tvName.text = it.rep_details.name
                        binding.tvMobile.text = it.rep_details.mobile
                        binding.tvEmail.text = it.rep_details.email
                        binding.tvRepCode.text = it.rep_details.rep_code
                    }
                }
            }
        }

        userProfileViewModel.isLoading.observe(this) {
            // Optional: handle shimmer or loader here
        }

        userProfileViewModel.apiError.observe(this) {
            Toast.makeText(this, "API Error: $it", Toast.LENGTH_SHORT).show()
        }

        userProfileViewModel.onFailure.observe(this) {
            Toast.makeText(this, "Network Error: ${it?.message}", Toast.LENGTH_SHORT).show()
        }
    }
}