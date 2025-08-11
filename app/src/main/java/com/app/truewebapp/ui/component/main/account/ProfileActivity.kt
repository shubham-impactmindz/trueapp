package com.app.truewebapp.ui.component.main.account

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.databinding.ActivityProfileBinding
import com.app.truewebapp.ui.viewmodel.UserProfileViewModel
import com.google.gson.Gson

class ProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileBinding
    lateinit var loginResponse: LoginResponse
    private lateinit var userProfileViewModel: UserProfileViewModel
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        userProfileViewModel = ViewModelProvider(this)[UserProfileViewModel::class.java]
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"
        observeUserProfile()
        userProfileViewModel.userProfile(token)
        val user_detailsJson = preferences.getString("userResponse", "")
        loginResponse = Gson().fromJson(user_detailsJson, LoginResponse::class.java)
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
        binding.editButton.setOnClickListener {
            binding.editButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeUserProfile() {
        userProfileViewModel.userProfileResponse.observe(this) { response ->
            response?.let {
                if (it.status) {

                    if (it.user_detail == null){

                    }else {

                        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
                        val gson = Gson()
                        val jsonString = gson.toJson(it) // Convert object to JSON string
                        preferences?.edit()?.putString("userResponse", jsonString)?.apply()
                        val name = it.user_detail.name.trim()
                        val initials = name
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .map { it.first().uppercaseChar() }
                            .joinToString(" ")
                        binding.tvInitial.text = initials
                        binding.tvName.text = it.user_detail.name
                        binding.tvCompanyName.text = it.user_detail.company_name
                        "${it.user_detail.address1}, ${it.user_detail.address2}, ${it.user_detail.city}, ${it.user_detail.country}, ${it.user_detail.postcode}".also { binding.tvCompanyAddress.text = it }
                        binding.tvPhoneNumber.text = it.user_detail.mobile
                        binding.tvEmail.text = it.user_detail.email
                        if (it.rep_details?.rep_code?.isNotEmpty() == true) {
                            binding.tvRepCode.text = it.rep_details.rep_code
                        }else{
                            binding.tvRepCode.text = "N/A"
                        }
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

    override fun onResume() {
        super.onResume()

        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        val user_detailsJson = preferences.getString("userResponse", "")
        loginResponse = Gson().fromJson(user_detailsJson, LoginResponse::class.java)
        val name = loginResponse.user_detail.name.trim()
        val initials = name
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.first().uppercaseChar() }
            .joinToString(" ")
        binding.tvInitial.text = initials
        binding.tvName.text = loginResponse.user_detail.name
        binding.tvCompanyName.text = loginResponse.user_detail.company_name
        "${loginResponse.user_detail.address1}, ${loginResponse.user_detail.address2}, ${loginResponse.user_detail.city}, ${loginResponse.user_detail.country}, ${loginResponse.user_detail.postcode}".also { binding.tvCompanyAddress.text = it }
        binding.tvPhoneNumber.text = loginResponse.user_detail.mobile
        binding.tvEmail.text = loginResponse.user_detail.email
        if (loginResponse.rep_details?.rep_code?.isNotEmpty() == true) {
            binding.tvRepCode.text = loginResponse.rep_details!!.rep_code
        }else{
            binding.tvRepCode.text = "N/A"
        }
    }
}