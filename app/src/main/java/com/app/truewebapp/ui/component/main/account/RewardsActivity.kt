package com.app.truewebapp.ui.component.main.account

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.truewebapp.databinding.ActivityRewardsBinding
import com.app.truewebapp.ui.component.main.dashboard.ReferralActivity

class RewardsActivity : AppCompatActivity() {
    lateinit var binding: ActivityRewardsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        binding.cardViewReferral.setOnClickListener {
            val intent = Intent(this, ReferralActivity::class.java)
            startActivity(intent)
        }
        binding.buttonSend.setOnClickListener {
            val intent = Intent(this, ReferralActivity::class.java)
            startActivity(intent)
        }
    }
}