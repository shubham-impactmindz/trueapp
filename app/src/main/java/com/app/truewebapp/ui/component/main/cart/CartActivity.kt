package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {
    lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cartListRecycler.layoutManager = LinearLayoutManager(this)
        cartAdapter = CartAdapter()
        binding.cartListRecycler.adapter = cartAdapter

        binding.checkoutLayout.setOnClickListener {
            val intent = Intent(this, CheckOutActivity::class.java)
            startActivity(intent)
        }
        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}