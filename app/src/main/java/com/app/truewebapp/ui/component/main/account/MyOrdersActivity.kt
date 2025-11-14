package com.app.truewebapp.ui.component.main.account

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.order.Orders
import com.app.truewebapp.databinding.ActivityMyOrdersBinding
import com.app.truewebapp.ui.component.main.dashboard.OrderDetailActivity
import com.app.truewebapp.ui.component.main.dashboard.OrdersAdapter
import com.app.truewebapp.ui.viewmodel.OrdersViewModel

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyOrdersBinding
    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var ordersViewModel: OrdersViewModel
    private var token = ""
    private var cdnURL = ""

    private var currentPage = 1
    private val perPage = 10
    private var isLoadingMore = false
    private var hasMorePages = true
    private val allOrders = mutableListOf<Orders>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        ordersViewModel = ViewModelProvider(this)[OrdersViewModel::class.java]
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"

        setupRecyclerView()
        observeOrders()
        loadNextPage()

        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            finish()
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.recentOrdersRecycler.layoutManager = layoutManager

        ordersAdapter = OrdersAdapter(allOrders) { order ->
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra("order_data", order)
            intent.putExtra("cdnURL", cdnURL)
            startActivity(intent)
        }

        binding.recentOrdersRecycler.adapter = ordersAdapter

        binding.recentOrdersRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoadingMore && hasMorePages) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        loadNextPage()
                    }
                }
            }
        })
    }

    private fun loadNextPage() {
        isLoadingMore = true
        ordersViewModel.orders(currentPage.toString(), perPage.toString(), token)
    }

    private fun observeOrders() {
        ordersViewModel.ordersResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    if (it.orders.isNotEmpty()) {
                        binding.tvNoData.visibility = View.GONE
                        val newOrders = it.orders
                        cdnURL = it.cdnURL
                        allOrders.addAll(newOrders)

                        if (currentPage == 1) {
                            ordersAdapter.notifyDataSetChanged()
                        } else {
                            ordersAdapter.addOrders(newOrders)
                        }

                        currentPage++
                        isLoadingMore = false
                    } else {
                        // No more orders to load
                        hasMorePages = false
                        isLoadingMore = false
                        // Only show "No Data Found" if it's the first page and no orders exist
                        if (currentPage == 1 && allOrders.isEmpty()) {
                            binding.tvNoData.visibility = View.VISIBLE
                        } else {
                            binding.tvNoData.visibility = View.GONE
                        }
                    }
                } else {
                    hasMorePages = false
                    isLoadingMore = false
                    // Only show "No Data Found" if it's the first page and no orders exist
                    if (currentPage == 1 && allOrders.isEmpty()) {
                        binding.tvNoData.visibility = View.VISIBLE
                    } else {
                        binding.tvNoData.visibility = View.GONE
                    }
                }
            }
        }

        ordersViewModel.isLoading.observe(this) {
            // Optional: handle shimmer or loader here
        }

        ordersViewModel.apiError.observe(this) {
            Toast.makeText(this, "API Error: $it", Toast.LENGTH_SHORT).show()
        }

        ordersViewModel.onFailure.observe(this) {
            Toast.makeText(this, "Network Error: ${it?.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
