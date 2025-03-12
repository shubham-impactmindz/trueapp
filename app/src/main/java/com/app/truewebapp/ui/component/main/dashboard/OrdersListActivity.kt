package com.app.truewebapp.ui.component.main.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.ActivityOrdersListBinding

class OrdersListActivity : AppCompatActivity() {

    lateinit var binding: ActivityOrdersListBinding
    lateinit var adapter: OrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recentOrdersRecycler.layoutManager = LinearLayoutManager(this)
        val optionsOrders = listOf(
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
        )

        adapter = OrdersAdapter(optionsOrders) { option ->
            val intent = Intent(this, OrderDetailActivity::class.java)
            startActivity(intent)
        }
        binding.recentOrdersRecycler.adapter = adapter


        binding.backLayout.setOnClickListener {
            finish()
        }
    }
}