package com.app.truewebapp.ui.component.main.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.ActivityServicesBinding

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
        binding.servicesRecyclerView.layoutManager = LinearLayoutManager(this)

        servicesAdapter = ServicesAdapter()
        binding.servicesRecyclerView.adapter = servicesAdapter
    }
}