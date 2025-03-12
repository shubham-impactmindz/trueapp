package com.app.truewebapp.ui.component.main.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.databinding.ActivityReferralBinding

class ReferralActivity : AppCompatActivity() {
    lateinit var binding: ActivityReferralBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferralBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}