package com.app.truewebapp.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.ActivitySplashBinding
import com.app.truewebapp.ui.base.BaseActivity
import com.app.truewebapp.ui.component.login.LoginActivity
import com.app.truewebapp.ui.component.main.MainActivity
import com.bumptech.glide.Glide

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Load animations
//        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
//        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_logo)
//
//        // Start logo animation
//        binding.logoImage.startAnimation(zoomInAnimation)

        Glide.with(this)
            .asGif()
            .load(R.drawable.launchscreen) // Replace with your actual gif resource
            .into(binding.logoImage)
        // Delay text animation
        binding.logoImage.postDelayed({
//            binding.textView.visibility = TextView.VISIBLE

            navigateToMainScreen()
        }, 3000) // 1-second delay

        // Move to next screen after animation
//        binding.textView.postDelayed({
//
//            navigateToMainScreen()
//        }, 3500) // 2.5 seconds delay
    }

    private fun navigateToMainScreen() {
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        Handler(Looper.getMainLooper()).postDelayed({

            if (preferences.contains("userId")) {
                if (preferences.getString("userId", "null").isNullOrEmpty()) {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

        }, 10)
    }

    override fun observeViewModel() {

    }

    override fun initViewBinding() {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}