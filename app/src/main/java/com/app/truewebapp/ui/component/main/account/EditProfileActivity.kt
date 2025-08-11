package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.data.dto.profile.ProfileRequest
import com.app.truewebapp.databinding.ActivityEditProfileBinding
import com.app.truewebapp.ui.viewmodel.EditUserProfileViewModel
import com.google.gson.Gson

class EditProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityEditProfileBinding
    private var token = ""
    lateinit var loginResponse: LoginResponse
    private lateinit var editUserProfileViewModel: EditUserProfileViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        editUserProfileViewModel = ViewModelProvider(this)[EditUserProfileViewModel::class.java]
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"
        val user_detailsJson = preferences.getString("userResponse", "")
        loginResponse = Gson().fromJson(user_detailsJson, LoginResponse::class.java)
        observeEditUserProfile()
        val nameParts = loginResponse.user_detail.name.split(" ")
        val firstName = nameParts.firstOrNull() ?: ""
        val lastName = nameParts.drop(1).joinToString(" ")

        binding.firstNameInput.setText(firstName)
        binding.lastNameInput.setText(lastName)
        binding.mobileNumberInput.setText(loginResponse.user_detail.mobile)
        binding.emailAddressInput.setText(loginResponse.user_detail.email)
        binding.companyNameInput.setText(loginResponse.user_detail.company_name)
        binding.address1Input.setText(loginResponse.user_detail.address1)
        binding.address2Input.setText(loginResponse.user_detail.address2)
        binding.cityInput.setText(loginResponse.user_detail.city)
        binding.postCodeInput.setText(loginResponse.user_detail.postcode)
        binding.countryInput.setText(loginResponse.user_detail.country)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
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
        binding.btnUpdate.setOnClickListener {
            binding.btnUpdate.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val request = ProfileRequest(
                name = binding.firstNameInput.text.toString().trim() +" " +binding.lastNameInput.text.toString().trim(),
                mobile = binding.mobileNumberInput.text.toString().trim(),
                email = binding.emailAddressInput.text.toString().trim(),
                company_name = binding.companyNameInput.text.toString().trim(),
                address1 = binding.address1Input.text.toString().trim(),
                address2 = binding.address2Input.text.toString().trim(),
                city = binding.cityInput.text.toString().trim(),
                postcode = binding.postCodeInput.text.toString().trim(),
                country = binding.countryInput.text.toString().trim(),
            )
            if (validateInputs()) {
                editUserProfileViewModel.editUserProfile(token, request)
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val mobile = binding.mobileNumberInput.text.toString().trim()
        val email = binding.emailAddressInput.text.toString().trim()
        val companyName = binding.companyNameInput.text.toString().trim()
        val address1 = binding.address1Input.text.toString().trim()
        val address2 = binding.address2Input.text.toString().trim()
        val city = binding.cityInput.text.toString().trim()
        val postcode = binding.postCodeInput.text.toString().trim()
        val country = binding.countryInput.text.toString().trim()

        if (firstName.isEmpty()) {
            binding.firstNameInput.error = "First name is required"
            isValid = false
        }

        if (lastName.isEmpty()) {
            binding.lastNameInput.error = "Last name is required"
            isValid = false
        }

        if (mobile.isEmpty()) {
            binding.mobileNumberInput.error = "Mobile number is required"
            isValid = false
        } else if (mobile.length < 10) {
            binding.mobileNumberInput.error = "Mobile number too short"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.emailAddressInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailAddressInput.error = "Invalid email format"
            isValid = false
        }

        if (companyName.isEmpty()) {
            binding.companyNameInput.error = "Company name is required"
            isValid = false
        }

        if (address1.isEmpty()) {
            binding.address1Input.error = "Address Line 1 is required"
            isValid = false
        }

        if (city.isEmpty()) {
            binding.cityInput.error = "City is required"
            isValid = false
        }

        if (postcode.isEmpty()) {
            binding.postCodeInput.error = "Postcode is required"
            isValid = false
        }

        if (country.isEmpty()) {
            binding.countryInput.error = "Country is required"
            isValid = false
        }

        return isValid
    }

    private fun observeEditUserProfile() {
        editUserProfileViewModel.userProfileResponse.observe(this) { response ->
            response?.let {
                if (it.status) {

                    if (it.user_detail == null){

                    }else {

                        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
                        val gson = Gson()
                        val jsonString = gson.toJson(it) // Convert object to JSON string
                        preferences?.edit()?.putString("userResponse", jsonString)?.apply()
                        finish()
                    }
                }
            }
        }

        editUserProfileViewModel.isLoading.observe(this) {
            // Optional: handle shimmer or loader here
        }

        editUserProfileViewModel.apiError.observe(this) {
            Toast.makeText(this, "API Error: $it", Toast.LENGTH_SHORT).show()
        }

        editUserProfileViewModel.onFailure.observe(this) {
            Toast.makeText(this, "Network Error: ${it?.message}", Toast.LENGTH_SHORT).show()
        }
    }
}