package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.databinding.ActivityEditAddressBinding

class EditAddressActivity : AppCompatActivity() {
    lateinit var binding: ActivityEditAddressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}