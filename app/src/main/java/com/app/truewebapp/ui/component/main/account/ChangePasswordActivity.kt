package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {
    lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}