package com.app.truewebapp.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.ActivitySplashBinding
import com.app.truewebapp.ui.base.BaseActivity
import com.app.truewebapp.ui.component.login.LoginActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Load animations
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_logo)

        // Start logo animation
        binding.logoImage.startAnimation(zoomInAnimation)

        // Delay text animation
        binding.logoImage.postDelayed({
            binding.textView.visibility = TextView.VISIBLE
            binding.textView.startAnimation(slideUpAnimation)
        }, 1500) // 1-second delay

        // Move to next screen after animation
        binding.textView.postDelayed({

            navigateToMainScreen()
        }, 3500) // 2.5 seconds delay
    }

    private fun navigateToMainScreen() {
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
//        Handler(Looper.getMainLooper()).postDelayed({
//
////            if (preferences.contains("login_key")) {
////                Log.e("login_key", "contain")
////                if (preferences.getString("login_key", "null").isNullOrEmpty()) {
//////                    val intent = Intent(this, LoginActivity::class.java)
//////                    startActivity(intent)
////                    finish()
////                } else {
//////                    val intent = Intent(this, MainActivity::class.java)
//////                    startActivity(intent)
////                    finish()
////                }
////            } else {
////                Log.e("login_keu", "not contain")
//////                val intent = Intent(this, LoginActivity::class.java)
//////                startActivity(intent)
////                finish()
////            }
//
//        }, 3000)
    }

    override fun observeViewModel() {
    }

    override fun initViewBinding() {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}