package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

        binding.backLayout.setOnClickListener {
            finish()
        }

        val options = listOf(
            NotificationOption("Basic POS"),
            NotificationOption("Advanced POS"),
            NotificationOption("Premium POS"),
        )
        binding.servicesRecyclerView.layoutManager = LinearLayoutManager(this)

        servicesAdapter = ServicesAdapter(options)
        binding.servicesRecyclerView.adapter = servicesAdapter
    }
}