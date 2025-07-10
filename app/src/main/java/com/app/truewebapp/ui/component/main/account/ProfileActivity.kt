package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.databinding.ActivityProfileBinding
import com.google.gson.Gson

class ProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileBinding
    lateinit var loginResponse: LoginResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
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
            binding.tvRepCode.text = loginResponse.rep_details?.rep_code
        }else{
            binding.tvRepCode.text = "N/A"
        }
        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}