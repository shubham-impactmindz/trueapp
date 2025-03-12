package com.app.truewebapp.ui.component.main.account

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.ActivityCompanyAddressBinding

class CompanyAddressActivity : AppCompatActivity() {

    lateinit var binding: ActivityCompanyAddressBinding
    lateinit var adapter: AddressListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.addressRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AddressListAdapter()
        binding.addressRecyclerView.adapter = adapter
        binding.newBranchLayout.setOnClickListener {
            val intent = Intent(this, AddAddressActivity::class.java)
            startActivity(intent)
        }
        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}