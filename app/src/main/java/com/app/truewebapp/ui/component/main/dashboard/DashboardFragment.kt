package com.app.truewebapp.ui.component.main.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.FragmentDashboardBinding
import com.app.truewebapp.ui.component.main.account.WalletActivity

class DashboardFragment : Fragment() {

    lateinit var binding: FragmentDashboardBinding
    private lateinit var notificationsAdapter: NotificationsAdapter
    private lateinit var ordersAdapter: OrdersAdapter
    private var tabSwitcher: TabSwitcher? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TabSwitcher) {
            tabSwitcher = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        tabSwitcher = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNotifications()
    }

    private fun setupNotifications() {
        binding.recentNotificationsRecycler.layoutManager = LinearLayoutManager(context)
        val options = listOf(
            NotificationOption("Yov've left products in your basket"),
            NotificationOption("Meltz Dubai Chocolate - STOCK UP TODAY!"),
            NotificationOption("Yov've left products in your basket"),
            NotificationOption("Meltz Dubai Chocolate - STOCK UP TODAY!"),
            NotificationOption("Yov've left products in your basket"),
        )

        notificationsAdapter = NotificationsAdapter(options) { option ->
            val intent = Intent(context, NotificationDetailActivity::class.java)
            startActivity(intent)
        }
        binding.recentNotificationsRecycler.adapter = notificationsAdapter

        binding.recentOrdersRecycler.layoutManager = LinearLayoutManager(context)
        val optionsOrders = listOf(
            OrderOption("1466941","15:21 19/11/2024","PAID","FULFILLED","200","20","430.42"),
            OrderOption("1439719","18:16 13/10/2024","PAID","FULFILLED","100","10","208.80"),
        )

        ordersAdapter = OrdersAdapter(optionsOrders) { option ->
            val intent = Intent(context, OrderDetailActivity::class.java)
            startActivity(intent)
        }
        binding.recentOrdersRecycler.adapter = ordersAdapter

        binding.viewOrderLayout.setOnClickListener {
            val intent = Intent(context, OrdersListActivity::class.java)
            startActivity(intent)
        }

        binding.shopLayout.setOnClickListener {
            tabSwitcher?.switchToShopTab()
        }

        binding.favLayout.setOnClickListener {
            tabSwitcher?.switchToShopTab()
        }

        binding.creditLayout.setOnClickListener {
            val intent = Intent(context, WalletActivity::class.java)
            startActivity(intent)
        }

        binding.referralSection.setOnClickListener {
            val intent = Intent(context, ReferralActivity::class.java)
            startActivity(intent)
        }
    }
}