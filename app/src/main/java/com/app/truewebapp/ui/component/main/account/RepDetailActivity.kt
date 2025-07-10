package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.databinding.ActivityRepDetailBinding
import com.google.gson.Gson

class RepDetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityRepDetailBinding
    lateinit var loginResponse: LoginResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRepDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        val userJson = preferences.getString("userResponse", "")
        loginResponse = Gson().fromJson(userJson, LoginResponse::class.java)
        if (loginResponse.rep_details == null){
            binding.repLayout.visibility = View.GONE
            binding.phoneNumberLayout.visibility = View.GONE
            binding.emailLayout.visibility = View.GONE
            binding.viewRep.visibility = View.GONE
            binding.viewEmail.visibility = View.GONE
            binding.noDataTextView.visibility = View.VISIBLE
        }else {

            val name = loginResponse.rep_details?.user?.name?.trim()
            val initials = name
                ?.split(" ")
                ?.filter { it.isNotBlank() }
                ?.map { it.first().uppercaseChar() }
                ?.joinToString(" ")
            binding.tvInitial.text = initials
            binding.tvName.text = loginResponse.rep_details?.user?.name
            binding.tvMobile.text = loginResponse.rep_details?.user?.mobile
            binding.tvEmail.text = loginResponse.rep_details?.user?.email
        }
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
            finish()
        }
    }
}