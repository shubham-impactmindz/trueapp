package com.app.truewebapp.ui.component.login
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.truewebapp.R
import com.app.truewebapp.databinding.ActivityLoginBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var adapter: AuthPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
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
        val viewpager = binding.viewPager
        val tabLayout = binding.tabLayout

        adapter = AuthPagerAdapter(this)
        viewpager.adapter = adapter

        // Sync TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewpager) { tab, position ->
            tab.text = if (position == 0) "Login" else "Register"
        }.attach()

        tabLayout.getTabAt(0)?.select()
        tabLayout.getTabAt(0)?.view?.setBackgroundResource(R.drawable.border_tab_primary)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.view?.setBackgroundResource(R.drawable.border_tab_primary)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.view?.setBackgroundResource(R.drawable.border_tab_light)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

    }
}
