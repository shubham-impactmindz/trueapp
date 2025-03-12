package com.app.truewebapp.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.databinding.ActivitySplashBinding
import com.app.truewebapp.ui.base.BaseActivity
import com.app.truewebapp.ui.component.login.LoginActivity

//import com.app.truewebapp.ui.component.dashboard.MainActivity
//import com.app.truewebapp.ui.component.login.LoginActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateToMainScreen()
    }

    private fun navigateToMainScreen() {
        Handler(Looper.getMainLooper()).postDelayed({
            val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
//            if (preferences.contains("login_key")) {
//                Log.e("login_key", "contain")
//                if (preferences.getString("login_key", "null").isNullOrEmpty()) {
////                    val intent = Intent(this, LoginActivity::class.java)
////                    startActivity(intent)
//                    finish()
//                } else {
////                    val intent = Intent(this, MainActivity::class.java)
////                    startActivity(intent)
//                    finish()
//                }
//            } else {
//                Log.e("login_keu", "not contain")
////                val intent = Intent(this, LoginActivity::class.java)
////                startActivity(intent)
//                finish()
//            }

        }, 3000)
    }

    override fun observeViewModel() {
    }

    override fun initViewBinding() {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}