package com.app.truewebapp.ui.component.main

// Import required Android and Jetpack libraries
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
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.component.main.dashboard.TabSwitcher
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.ArrayDeque

// Main activity of the application that manages navigation tabs, cart updates, and exit behavior
class MainActivity : AppCompatActivity(), TabSwitcher, CartUpdateListener {

    // Adapter to manage fragments within the ViewPager2
    private lateinit var adapter: ViewPagerAdapter

    // ViewBinding instance for accessing views in activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // Stack to keep track of tab navigation history (for back press handling)
    private val tabHistoryStack = ArrayDeque<Int>()

    // Tracks last time the back button was pressed (used for double-press exit)
    private var lastBackPressedTime: Long = 0L

    // Increment counter for tracking back press behavior
    var increment = 0

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply system bar (status + nav) insets to the root view
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Adjust padding for system bars
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        // Initialize ViewPager adapter for managing fragments
        adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        // Disable user swipe navigation (tabs are only switched programmatically)
        binding.viewPager.isUserInputEnabled = false

        // Attach TabLayout with ViewPager2 using TabLayoutMediator
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                // Home tab
                0 -> {
                    tab.text = "Home"
                    tab.setIcon(R.drawable.ic_home_unselected)
                }
                // Browse tab
                1 -> {
                    tab.text = "Browse"
                    tab.setIcon(R.drawable.ic_menu)
                }
                // Cart tab with badge
                2 -> {
                    tab.text = "Cart"
                    tab.setIcon(R.drawable.ic_cart_unselected)
                    tab.orCreateBadge.apply {
                        number = 0 // initial count
                        isVisible = false // badge hidden by default
                        backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.colorSecondary)
                    }
                }
                // Wallet tab
                3 -> {
                    tab.text = "Wallet"
                    tab.setIcon(R.drawable.ic_wallet_unselected)
                }
                // Account tab
                4 -> {
                    tab.text = "Account"
                    tab.setIcon(R.drawable.ic_account_unselected)
                }
            }
        }.attach() // Finalize binding

        // Add listener to track tab changes
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {

            // Called when a tab is selected
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val position = tab?.position ?: 0
                // Push tab position into history stack if it's not duplicate
                if (tabHistoryStack.isEmpty() || tabHistoryStack.last() != position) {
                    tabHistoryStack.add(position)
                    if (position != 0) {
                        increment = 0 // Reset increment when not on Home
                    }
                }
                // Update ViewPager to match selected tab
                binding.viewPager.currentItem = position
                // Perform haptic feedback on tab selection
                tab?.view?.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }

            // Called when a tab is unselected (not used here)
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}

            // Called when a tab is reselected
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Optional haptic feedback for reselection
                tab?.view?.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        })

        // Handle back press behavior with custom logic
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tabHistoryStack.size > 1) {
                    // Pop last tab and navigate to previous tab
                    tabHistoryStack.removeLast()
                    val previousTab = tabHistoryStack.removeLast() ?: 0
                    binding.viewPager.currentItem = previousTab
                    binding.tabLayout.getTabAt(previousTab)?.select()
                } else {
                    if (increment == 0) {
                        // First back press: reset to Home tab
                        increment++
                        tabHistoryStack.clear()
                        binding.tabLayout.getTabAt(0)?.select()
                    } else {
                        // Second back press: check for double-tap exit
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressedTime < 2000) {
                            // Exit the app
                            finish()
                        } else {
                            // Show Snackbar to confirm exit
                            lastBackPressedTime = currentTime
                            showExitSnackbar()
                        }
                    }
                }
            }
        })

        // Load initial cart badge count from database
        loadInitialCartCount()
    }

    // Loads the cart count from Room database and updates badge
    private fun loadInitialCartCount() {
        val dao = CartDatabase.getInstance(this).cartDao()

        // Collect cart count in a coroutine
        lifecycleScope.launch {
            dao.getAllItems().collect { cartItems ->
                // Calculate total count including free items
                val totalCount = calculateTotalCount(cartItems)
                // Update cart badge with current count
                updateCartBadge(totalCount)
            }
        }
    }
    
    // Calculate total count including free items
    private fun calculateTotalCount(cartItems: List<CartItemEntity>): Int {
        // Calculate paid quantity
        val paidQuantity = cartItems.sumOf { it.quantity }
        
        // Calculate free items for each cart item with deals
        var freeQuantity = 0
        cartItems.forEach { item ->
            if (item.dealType == "buy_x_get_y") {
                val buyQty = item.dealBuyQuantity ?: 0
                val getQty = item.dealGetQuantity ?: 0
                if (buyQty > 0 && getQty > 0 && item.quantity >= buyQty) {
                    freeQuantity += (item.quantity / buyQty) * getQty
                }
            }
        }
        
        return paidQuantity + freeQuantity
    }

    // Updates the cart tab badge with item count
    fun updateCartBadge(count: Int) {
        val cartTab = binding.tabLayout.getTabAt(2) // Cart tab is always at index 2
        cartTab?.let { tab ->
            val badge = tab.orCreateBadge
            if (count > 0) {
                badge.number = count // Show count
                badge.isVisible = true
            } else {
                badge.isVisible = false // Hide badge when count = 0
            }
        }
    }

    // Shows a Snackbar prompting the user to press back again to exit
    private fun showExitSnackbar() {
        // Create Snackbar message
        val snackBar = Snackbar.make(binding.root, "Press back again to exit", Snackbar.LENGTH_SHORT)

        // Customize Snackbar layout
        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Customize background color
        view.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimaryDark))

        // Customize Snackbar text appearance
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        // Display Snackbar
        snackBar.show()
    }

    // Implementation of TabSwitcher: switch to Browse tab
    override fun switchToShopTab() {
        binding.viewPager.currentItem = 1
        binding.tabLayout.getTabAt(1)?.select()
    }

    // Implementation of TabSwitcher: switch to Cart tab
    override fun switchToCartTab() {
        binding.viewPager.currentItem = 2
        binding.tabLayout.getTabAt(2)?.select()
    }

    // Implementation of CartUpdateListener: update cart badge count
    override fun onCartItemsUpdated(count: Int) {
        updateCartBadge(count)
    }
}