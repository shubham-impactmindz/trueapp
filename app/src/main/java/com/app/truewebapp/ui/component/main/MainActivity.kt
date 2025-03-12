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
                    tab.text = "Dashboard"
                    tab.setIcon(R.drawable.ic_dashboard)
                }
                1 -> {
                    tab.text = "Shop"
                    tab.setIcon(R.drawable.ic_shop)
                }
                2 -> {
                    tab.text = "Rewards"
                    tab.setIcon(R.drawable.ic_rewards)
                }
                3 -> {
                    tab.text = "Account"
                    tab.setIcon(R.drawable.ic_account)
                }
            }
        }.attach()
    }

    override fun switchToShopTab() {
        binding.viewPager.currentItem = 1
    }
}