package com.app.truewebapp.ui.component.main.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.databinding.ActivityNotificationDetailBinding

class NotificationDetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityNotificationDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}