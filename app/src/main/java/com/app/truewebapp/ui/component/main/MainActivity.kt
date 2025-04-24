package com.app.truewebapp.ui.component.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.R
import com.app.truewebapp.databinding.ActivityMainBinding
import com.app.truewebapp.ui.component.main.dashboard.TabSwitcher
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity(), TabSwitcher {

    private lateinit var adapter: ViewPagerAdapter
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Home"
                    tab.setIcon(R.drawable.ic_home_unselected)
                }
                1 -> {
                    tab.text = "Browse"
                    tab.setIcon(R.drawable.ic_menu)
                }
                2 -> {
                    tab.text = "Cart"
                    tab.setIcon(R.drawable.ic_cart_unselected)
                }
                3 -> {
                    tab.text = "Wallet"
                    tab.setIcon(R.drawable.ic_wallet_unselected)
                }
                4 -> {
                    tab.text = "Account"
                    tab.setIcon(R.drawable.ic_account_unselected)
                }
            }
        }.attach()
        binding.viewPager.isUserInputEnabled = false
    }

    override fun switchToShopTab() {
        binding.viewPager.currentItem = 1
    }
}