package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.ActivityServicesBinding
import com.app.truewebapp.ui.component.main.dashboard.NotificationOption

class ServicesActivity : AppCompatActivity() {
    lateinit var binding:ActivityServicesBinding
    lateinit var servicesAdapter: ServicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServicesBinding.inflate(layoutInflater)
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
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            finish()
        }

        val options = listOf(
            NotificationOption("Basic POS", 1),
            NotificationOption("Advanced POS", 2),
            NotificationOption("Premium POS", 3),
        )
        binding.servicesRecyclerView.layoutManager = LinearLayoutManager(this)

        servicesAdapter = ServicesAdapter(options, this, this)
        binding.servicesRecyclerView.adapter = servicesAdapter
    }
}