package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.databinding.ActivityWalletBinding

class WalletActivity : AppCompatActivity() {
    lateinit var binding: ActivityWalletBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}