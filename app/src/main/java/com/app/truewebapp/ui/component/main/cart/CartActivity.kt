package com.app.truewebapp.ui.component.main.cart
// Defines the package where this class belongs (helps in organizing project structure)

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.cart.Cart
import com.app.truewebapp.data.dto.cart.CartRequest
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.databinding.ActivityCartBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.component.main.shop.ProductAdapterListener
import com.app.truewebapp.ui.viewmodel.CartViewModel
import com.app.truewebapp.ui.viewmodel.DeliverySettingsViewModel
import com.app.truewebapp.ui.viewmodel.WishlistViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Import statements: Android framework, Jetpack libraries, app classes, and coroutines

class CartActivity : AppCompatActivity(), ProductAdapterListener {
    // ViewBinding for safe access to layout XML views
    lateinit var binding: ActivityCartBinding

    // Adapter for displaying cart items in RecyclerView
    private lateinit var cartAdapter: CartAdapter

    // ViewModels for handling business logic and API calls
    private lateinit var wishlistViewModel: WishlistViewModel
    private lateinit var deliverySettingsViewModel: DeliverySettingsViewModel
    private lateinit var cartViewModel: CartViewModel

    // Lazy initialization for DAO (Data Access Object) from Room database
    private val cartDao by lazy { let { CartDatabase.getInstance(it).cartDao() } }

    // Token for API authentication
    private var token = ""

    // Keeps track of wishlist item ID during API request
    private var currentWishlistVariantId: String? = null

    // Minimum order value for free delivery
    private var minOrderValue: String? = null


    // Lifecycle method: called when activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityCartBinding.inflate(layoutInflater)

        // Set the content view to the root layout
        setContentView(binding.root)

        // Handle system UI insets (status + navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets // return updated insets
        }

        // Handle back button click with haptic feedback
        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            finish() // close activity
        }

        // Initialize ViewModels
        initializeViewModels()

        // Setup RecyclerView and Adapter for cart
        setupRecyclerView()

        // Start observing database changes via Flow
        observeCartItems()

        // Setup other views like checkout button
        setupView()

        // Attach observers to ViewModels
        observeWishlistViewModel()
        observeDeliverySettingsViewModel()
        observeCart()
    }


    // Initialize ViewModels and fetch delivery settings
    private fun initializeViewModels() {
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        deliverySettingsViewModel = ViewModelProvider(this)[DeliverySettingsViewModel::class.java]
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]

        // Get stored shared preferences for token
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"

        // Fetch delivery settings from server
        deliverySettingsViewModel.deliverySetting(token)
    }

    // Called every time the activity resumes (comes to foreground)
    override fun onResume() {
        super.onResume()
        // Re-attach observers to ensure cart is up to date
        observeCartItems()
    }

    // Setup RecyclerView and assign adapter
    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(this, this) // pass context and listener
        binding.cartListRecycler.layoutManager = LinearLayoutManager(this)
        binding.cartListRecycler.adapter = cartAdapter
        Log.d("CartFragment", "RecyclerView and Adapter set up.")
    }

    // Observe cart items stored in Room DB (using Flow)
    private fun observeCartItems() {
        this.lifecycleScope.launch {
            cartDao.getAllItems().collectLatest { cartItems ->
                Log.d("CartFragment", "Received ${cartItems.size} items from database Flow.")

                // Convert to display items and calculate free items
                val displayItems = convertToDisplayItems(cartItems)
                cartAdapter.updateData(displayItems) // Update adapter data

                if (displayItems.isNotEmpty()) {
                    // Show cart and hide empty state
                    binding.cartListRecycler.visibility = View.VISIBLE
                    binding.linearCartDetail.visibility = View.VISIBLE
                    binding.textEmptyCart.visibility = View.GONE
                    binding.imageCart.visibility = View.GONE
                    updateTotalAmount(cartItems) // Update totals (use original items for total calculation)
                } else {
                    // Show empty cart UI
                    binding.cartListRecycler.visibility = View.GONE
                    binding.linearCartDetail.visibility = View.GONE
                    binding.textEmptyCart.visibility = View.VISIBLE
                    binding.imageCart.visibility = View.VISIBLE
                    binding.textTotal.text = "£0.00" // Reset total
                }
            }
        }
    }
    
    // Convert CartItemEntity to CartDisplayItem and add free items
    private fun convertToDisplayItems(cartItems: List<CartItemEntity>): List<CartDisplayItem> {
        val displayItems = mutableListOf<CartDisplayItem>()
        
        cartItems.forEach { item ->
            // Add the original paid item
            val paidItem = CartDisplayItem(
                variantId = item.variantId,
                title = item.title,
                options = item.options,
                image = item.image,
                fallbackImage = item.fallbackImage,
                price = item.price,
                comparePrice = item.comparePrice,
                isWishlisted = item.isWishlisted,
                cdnURL = item.cdnURL,
                quantity = item.quantity,
                taxable = item.taxable,
                isFreeItem = false,
                dealType = item.dealType,
                dealBuyQuantity = item.dealBuyQuantity,
                dealGetQuantity = item.dealGetQuantity,
                dealQuantity = item.dealQuantity,
                dealPrice = item.dealPrice
            )
            displayItems.add(paidItem)
            
            // Calculate and add free items if applicable
            if (item.dealType == "buy_x_get_y") {
                val buyQty = item.dealBuyQuantity ?: 0
                val getQty = item.dealGetQuantity ?: 0
                
                if (buyQty > 0 && getQty > 0 && item.quantity >= buyQty) {
                    val freeItems = (item.quantity / buyQty) * getQty
                    if (freeItems > 0) {
                        val freeItem = CartDisplayItem(
                            variantId = item.variantId + 100000, // Unique ID for free item
                            title = item.title,
                            options = item.options,
                            image = item.image,
                            fallbackImage = item.fallbackImage,
                            price = 0.0,
                            comparePrice = 0.0,
                            isWishlisted = item.isWishlisted,
                            cdnURL = item.cdnURL,
                            quantity = freeItems,
                            taxable = item.taxable,
                            isFreeItem = true,
                            originalVariantId = item.variantId,
                            dealType = item.dealType,
                            dealBuyQuantity = item.dealBuyQuantity,
                            dealGetQuantity = item.dealGetQuantity,
                            dealQuantity = item.dealQuantity,
                            dealPrice = item.dealPrice
                        )
                        displayItems.add(freeItem)
                    }
                }
            }
        }
        
        return displayItems
    }

    // Calculate total amount, delivery, and update UI
    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        val totalQuantity = cartItems.sumOf { it.quantity }
        binding.textItems.text = "$totalQuantity Units"
        binding.textSKU.text = "${cartItems.size} SKUs"

        // Calculate total amount with volume discount pricing
        val totalAmount = cartItems.sumOf { item ->
            calculateItemPrice(item)
        }
        var deliveryFee = 0.0
        var minOrder = minOrderValue?.toDoubleOrNull() ?: 0.0

        // Check free delivery condition
        if (totalAmount < minOrder) {
            deliveryFee = 0.0
            val amountLeft = minOrder - totalAmount
            binding.textDelivery.text = "Spend £%.2f more for FREE delivery".format(amountLeft)
            binding.textDelivery.setTextColor(ContextCompat.getColor(this, R.color.textGrey))
        } else {
            binding.textDelivery.text = "Free Delivery"
            binding.textDelivery.setTextColor(ContextCompat.getColor(this, R.color.colorGreen))
        }

        val finalTotal = totalAmount + deliveryFee
        binding.textTotal.text = "£%.2f".format(finalTotal)

        Log.d("CartFragment", "Total: £$totalAmount, Delivery: £$deliveryFee, Final: £$finalTotal")
    }
    
    // Calculate item price considering volume discounts
    private fun calculateItemPrice(item: CartItemEntity): Double {
        return when (item.dealType) {
            "volume_discount" -> {
                val dealQty = item.dealQuantity ?: 0
                val dealPrice = item.dealPrice ?: 0.0
                
                if (dealQty > 0 && dealPrice > 0 && item.quantity >= dealQty) {
                    // Calculate complete deal sets
                    val completeSets = item.quantity / dealQty
                    // Calculate remaining items after complete sets
                    val remainingItems = item.quantity % dealQty
                    // Total = (complete sets * deal price) + (remaining items * item price)
                    (completeSets * dealPrice) + (remainingItems * item.price)
                } else {
                    // No deal applied, use regular pricing
                    item.price * item.quantity
                }
            }
            else -> {
                // For buy_x_get_y or no deal, use regular pricing
                item.price * item.quantity
            }
        }
    }

    // Setup checkout button click
    private fun setupView() {
        binding.checkoutLayout.setOnClickListener {
            binding.checkoutLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            lifecycleScope.launch {
                // Fetch cart items from DB
                val cartItems = withContext(Dispatchers.IO) {
                    cartDao.getAllItemsOnce()
                }

                if (cartItems.isEmpty()) {
                    Toast.makeText(applicationContext, "Cart is empty", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Map items into Cart DTO for API request
                val cartList = cartItems.map {
                    Cart(
                        mvariant_id = it.variantId.toString(),
                        quantity = it.quantity.toString()
                    )
                }

                // Create request object
                val cartRequest = CartRequest(cart = cartList)

                // Send request to server
                cartViewModel.cart(token, cartRequest)
            }
        }
    }

    // Observe wishlist ViewModel changes
    private fun observeWishlistViewModel() {
        wishlistViewModel.wishlistResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    showTopSnackBar(it.message ?: "Wishlist updated successfully!")

                    // Update Room DB to reflect wishlist changes
                    currentWishlistVariantId?.let { variantIdString ->
                        this.lifecycleScope.launch {
                            val variantIdInt = variantIdString.toIntOrNull()
                            if (variantIdInt != null) {
                                val item = withContext(Dispatchers.IO) {
                                    cartDao.getItemByVariantId(variantIdInt)
                                }
                                item?.let { existingItem ->
                                    val updatedItem = existingItem.copy(isWishlisted = !existingItem.isWishlisted)
                                    cartDao.insertOrUpdateItem(updatedItem)
                                }
                            }
                        }
                    }
                } else {
                    showTopSnackBar(it.message)
                }
                currentWishlistVariantId = null // Reset after operation
            }
        }

        wishlistViewModel.isLoading.observe(this) { /* show loader if needed */ }
        wishlistViewModel.apiError.observe(this) { showTopSnackBar(it ?: "An API error occurred.") }
        wishlistViewModel.onFailure.observe(this) { showTopSnackBar("Network error: ${it?.message}") }
    }

    // Observe cart checkout process
    private fun observeCart() {
        cartViewModel.changePasswordResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    val intent = Intent(this, CheckOutActivity::class.java)
                    intent.putExtra("minOrderValue", minOrderValue)
                    startActivity(intent)
                }
            }
        }

        cartViewModel.isLoading.observe(this) { /* loader if needed */ }
        cartViewModel.apiError.observe(this) { showTopSnackBar(it ?: "An API error occurred.") }
        cartViewModel.onFailure.observe(this) { showTopSnackBar("Network error: ${it?.message}") }
    }

    // Observe delivery settings API
    private fun observeDeliverySettingsViewModel() {
        deliverySettingsViewModel.deliverySettingsResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    minOrderValue = it.min_order_free_delivery
                }
            }
        }

        deliverySettingsViewModel.isLoading.observe(this) { /* loader if needed */ }
        deliverySettingsViewModel.apiError.observe(this) { showTopSnackBar(it ?: "An API error occurred.") }
        deliverySettingsViewModel.onFailure.observe(this) { showTopSnackBar("Network error: ${it?.message}") }
    }

    // Custom Snackbar UI for showing messages
    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        val view = snackBar.view
        val params = view.layoutParams as? FrameLayout.LayoutParams
        params?.let {
            it.gravity = Gravity.BOTTOM
            it.bottomMargin = 50
            view.layoutParams = it
        }

        // Customize background color and text
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
            setTextColor(Color.WHITE)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        snackBar.show()
    }

    // ProductAdapterListener: Called when wishlist is updated
    override fun onUpdateWishlist(mvariant_id: String) {
        val preferences = getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        val userId = preferences?.getString("userId", "").orEmpty()
        this.currentWishlistVariantId = mvariant_id
        wishlistViewModel.wishlist(token, WishlistRequest(userId, mvariant_id))
    }

    // ProductAdapterListener: Called when cart is updated
    override fun onUpdateCart(totalItems: Int, productId: Int) {
        Log.d("CartFragment", "Cart item updated via adapter: ProductId $productId, Quantity $totalItems")
        // Flow observer auto refreshes UI after DB update
    }
}