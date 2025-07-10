package com.app.truewebapp.ui.component.main.account

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.ActivityMyOrdersBinding
import com.app.truewebapp.ui.component.main.dashboard.OrderDetailActivity
import com.app.truewebapp.ui.component.main.dashboard.OrderOption
import com.app.truewebapp.ui.component.main.dashboard.OrdersAdapter

class MyOrdersActivity : AppCompatActivity() {

    lateinit var binding: ActivityMyOrdersBinding
    private lateinit var ordersAdapter: OrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOrdersBinding.inflate(layoutInflater)
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
        binding.recentOrdersRecycler.layoutManager = LinearLayoutManager(this)
        val optionsOrders = listOf(
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
        )

        ordersAdapter = OrdersAdapter(optionsOrders) { option ->
            val intent = Intent(this, OrderDetailActivity::class.java)
            startActivity(intent)
        }
        binding.recentOrdersRecycler.adapter = ordersAdapter
        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}