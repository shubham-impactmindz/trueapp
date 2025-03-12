package com.app.truewebapp.ui.component.main.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.ActivityOrderDetailBinding

class OrderDetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityOrderDetailBinding
    lateinit var adapter: ItemListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.orderItemsRecycler.layoutManager = LinearLayoutManager(this)

        adapter = ItemListAdapter()
        binding.orderItemsRecycler.adapter = adapter

        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}