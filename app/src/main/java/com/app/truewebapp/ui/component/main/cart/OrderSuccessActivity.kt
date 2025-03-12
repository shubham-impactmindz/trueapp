package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.R
import com.app.truewebapp.databinding.ActivityOrderSuccessBinding
import com.app.truewebapp.ui.component.main.MainActivity

class OrderSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start Lottie animation
        binding.lottieCheckmark.playAnimation()

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        // Apply animations
        binding.textOrderPlaced.startAnimation(fadeIn)
        binding.textOrderMessage.startAnimation(fadeIn)
        binding.btnContinue.startAnimation(slideUp)

        // Handle button click
        binding.btnContinue.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
