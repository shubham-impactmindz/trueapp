package com.app.truewebapp.ui.component.main

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.app.truewebapp.R
import com.app.truewebapp.databinding.ActivityMainBinding
import com.app.truewebapp.ui.component.main.cart.CartUpdateListener
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.dashboard.TabSwitcher
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.util.ArrayDeque

class MainActivity : AppCompatActivity(), TabSwitcher, CartUpdateListener {

    private lateinit var adapter: ViewPagerAdapter
    private lateinit var binding: ActivityMainBinding
    private val tabHistoryStack = ArrayDeque<Int>()
    private var lastBackPressedTime: Long = 0L
    var increment = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system bars insets (status + nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false // Disable swipe

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
                    tab.orCreateBadge.apply {
                        number = 0
                        isVisible = false
                        backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.colorSecondary)
                    }
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

        // Track tab changes and push to history
        // Track tab changes and push to history
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val position = tab?.position ?: 0
                if (tabHistoryStack.isEmpty() || tabHistoryStack.last() != position) {
                    tabHistoryStack.add(position)
                    if (position != 0) {
                        increment = 0
                    }
                }
                binding.viewPager.currentItem = position
                // Add this line to perform haptic feedback on the selected tab's view
                tab?.view?.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
                )
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // You can also add haptic feedback here if a tab is re-selected.
                tab?.view?.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
                )
            }
        })
        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tabHistoryStack.size > 1) {
                    tabHistoryStack.removeLast()
                    val previousTab = tabHistoryStack.removeLast() ?: 0
                    binding.viewPager.currentItem = previousTab
                    binding.tabLayout.getTabAt(previousTab)?.select()
                } else {
                    if (increment == 0){
                        increment++
                        tabHistoryStack.clear()
                        binding.tabLayout.getTabAt(0)?.select()
                    }else{

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressedTime < 2000) {
                            // Exit the app
                            finish()
                        } else {
                            lastBackPressedTime = currentTime
                            showExitSnackbar()
                        }
                    }
                }
            }
        })
        loadInitialCartCount()
    }

    private fun loadInitialCartCount() {
        val dao = CartDatabase.getInstance(this).cartDao()

        lifecycleScope.launch {
            dao.getCartItemCount().collect { totalCount ->
                // totalCount will be null if the cart is empty, so handle that
                updateCartBadge(totalCount)
            }
        }
    }

    fun updateCartBadge(count: Int) {
        val cartTab = binding.tabLayout.getTabAt(2) // Cart tab is at position 2
        cartTab?.let { tab ->
            val badge = tab.orCreateBadge
            if (count > 0) {
                badge.number = count
                badge.isVisible = true
            } else {
                badge.isVisible = false
            }
        }
    }

    private fun showExitSnackbar() {
        val snackBar = Snackbar.make(binding.root, "Press back again to exit", Snackbar.LENGTH_SHORT)

        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        view.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimaryDark)) // customize color

        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }


    override fun switchToShopTab() {
        binding.viewPager.currentItem = 1
        binding.tabLayout.getTabAt(1)?.select()
    }

    override fun switchToCartTab() {
        binding.viewPager.currentItem = 2
        binding.tabLayout.getTabAt(2)?.select()
    }

    override fun onCartItemsUpdated(count: Int) {
        updateCartBadge(count)
    }
}
