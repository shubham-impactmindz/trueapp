package com.app.truewebapp.ui.component.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.truewebapp.ui.component.main.account.AccountFragment
import com.app.truewebapp.ui.component.main.dashboard.DashboardFragment
import com.app.truewebapp.ui.component.main.rewards.RewardsFragment
import com.app.truewebapp.ui.component.main.shop.ShopFragment
import com.app.truewebapp.ui.component.main.wallet.WalletFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 5 // Number of Tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardFragment()
            1 -> ShopFragment()
            2 -> WalletFragment()
            3 -> RewardsFragment()
            4 -> AccountFragment()
            else -> DashboardFragment()
        }
    }
}