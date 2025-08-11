package com.app.truewebapp.ui.component.main.cart

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

class CartActivity : AppCompatActivity(), ProductAdapterListener {
    lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter
    private lateinit var wishlistViewModel: WishlistViewModel
    private lateinit var deliverySettingsViewModel: DeliverySettingsViewModel
    private lateinit var cartViewModel: CartViewModel

    // Lazy initialization for cartDao, ensures context is available
    private val cartDao by lazy { let { CartDatabase.getInstance(it).cartDao() } }

    private var token = ""
    private var currentWishlistVariantId: String? = null // Store variant ID for wishlist toggles
    private var minOrderValue: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
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

        binding.backLayout.setOnClickListener {
            binding.backLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            finish()
        }
        initializeViewModels()
        setupRecyclerView() // Initialize RecyclerView and Adapter here once
        observeCartItems()  // Start observing database changes here
        setupView()
        observeWishlistViewModel() // Observe wishlist actions
        observeDeliverySettingsViewModel() // Observe wishlist actions
        observeCart() // Observe wishlist actions

    }



    private fun initializeViewModels() {
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        deliverySettingsViewModel = ViewModelProvider(this)[DeliverySettingsViewModel::class.java]
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"
        deliverySettingsViewModel.deliverySetting(token)
    }

    // No need for loadCartFromDatabase() in onResume() if you're observing Flow.
    // The Flow automatically re-emits when data changes.
    override fun onResume() {
        super.onResume()
        observeCartItems()
        // If there's a reason to re-evaluate the cart outside of direct DB changes (e.g.,
        // if user comes back from a product detail page where cart was updated locally but not through DB observer)
        // you might need a mechanism here, but for now, Flow is sufficient for DB changes.
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(this, this)
        binding.cartListRecycler.layoutManager = LinearLayoutManager(this)
        binding.cartListRecycler.adapter = cartAdapter
        Log.d("CartFragment", "RecyclerView and Adapter set up.")
    }

    // This is where data comes from and updates the adapter
    private fun observeCartItems() {
        this.lifecycleScope.launch {
            cartDao.getAllItems().collectLatest { cartItems ->
                Log.d("CartFragment", "Received ${cartItems.size} items from database Flow.")
                // Update the adapter's data
                cartAdapter.updateData(cartItems)

                if (cartItems.isNotEmpty()) {
                    binding.cartListRecycler.visibility = View.VISIBLE
                    binding.linearCartDetail.visibility = View.VISIBLE
                    binding.textEmptyCart.visibility = View.GONE
                    binding.imageCart.visibility = View.GONE
                    updateTotalAmount(cartItems) // Update total price
                } else {
                    binding.cartListRecycler.visibility = View.GONE
                    binding.linearCartDetail.visibility = View.GONE
                    binding.textEmptyCart.visibility = View.VISIBLE
                    binding.imageCart.visibility = View.VISIBLE
                    binding.textTotal.text = "£0.00" // Reset total price for empty cart
                }
            }
        }
    }

    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        val totalQuantity = cartItems.sumOf { it.quantity }
        binding.textItems.text = "$totalQuantity Units"
        binding.textSKU.text = "${cartItems.size} SKUs"

        val totalAmount = cartItems.sumOf { it.price * it.quantity }
        var deliveryFee = 0.0
        var minOrder = minOrderValue?.toDoubleOrNull() ?: 0.0

        // Check if free delivery condition is met
        if (totalAmount < minOrder) {
            deliveryFee = 0.0

            val amountLeft = minOrder - totalAmount
            binding.textDelivery.text = "Spend £%.2f more for FREE delivery".format(amountLeft)
            binding.textDelivery.setTextColor(ContextCompat.getColor(this, R.color.textGrey)) // Optional style
        } else {
            binding.textDelivery.text = "FREE DELIVERY"
            binding.textDelivery.setTextColor(ContextCompat.getColor(this, R.color.colorGreen)) // Optional style
        }

        val finalTotal = totalAmount + deliveryFee
        binding.textTotal.text = "£%.2f".format(finalTotal)

        Log.d("CartFragment", "Total: £$totalAmount, Delivery: £$deliveryFee, Final: £$finalTotal")
    }

    private fun setupView() {
        binding.checkoutLayout.setOnClickListener {
            binding.checkoutLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            lifecycleScope.launch {
                // Step 1: Fetch cart items once from DB
                val cartItems = withContext(Dispatchers.IO) {
                    cartDao.getAllItemsOnce()
                }

                // Optional: Skip if cart is empty
                if (cartItems.isEmpty()) {
                    Toast.makeText(applicationContext, "Cart is empty", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Step 2: Map items to Cart DTOs
                val cartList = cartItems.map {
                    Cart(
                        mvariant_id = it.variantId.toString(),
                        quantity = it.quantity.toString()
                    )
                }

                // Step 3: Create request and send
                val cartRequest = CartRequest(cart = cartList)
                cartViewModel.cart(token, cartRequest)
            }
        }
    }



    // Renamed from updateWishlistInAdapter to reflect its role in observing ViewModel
    private fun observeWishlistViewModel() {
        wishlistViewModel.wishlistResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    showTopSnackBar(it.message ?: "Wishlist updated successfully!")
                    // Update the database based on the wishlist toggle, which will
                    // in turn trigger the Flow in observeCartItems and refresh the UI.
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
                currentWishlistVariantId = null // Clear the stored ID after processing
            }
        }

        // Observe other LiveData from ViewModel for better UX
        wishlistViewModel.isLoading.observe(this) { isLoading ->
            // Show/hide a progress indicator if needed
        }
        wishlistViewModel.apiError.observe(this) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        wishlistViewModel.onFailure.observe(this) { failure ->
            showTopSnackBar("Network error: ${failure?.message}")
        }
    }

    private fun observeCart() {
        cartViewModel.changePasswordResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    val intent = Intent(this, CheckOutActivity::class.java)
                    intent.putExtra("minOrderValue",minOrderValue)
                    // Optionally pass total amount or cart items data if CheckOutActivity needs it
                    startActivity(intent)
                }

            }
        }

        // Observe other LiveData from ViewModel for better UX
        cartViewModel.isLoading.observe(this) { isLoading ->
            // Show/hide a progress indicator if needed
        }
        cartViewModel.apiError.observe(this) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        cartViewModel.onFailure.observe(this) { failure ->
            showTopSnackBar("Network error: ${failure?.message}")
        }
    }

    private fun observeDeliverySettingsViewModel() {
        deliverySettingsViewModel.deliverySettingsResponse.observe(this) { response ->
            response?.let {
                if (it.status) {
                    minOrderValue = it.min_order_free_delivery
                }
            }
        }

        // Observe other LiveData from ViewModel for better UX
        deliverySettingsViewModel.isLoading.observe(this) { isLoading ->
            // Show/hide a progress indicator if needed
        }
        deliverySettingsViewModel.apiError.observe(this) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        deliverySettingsViewModel.onFailure.observe(this) { failure ->
            showTopSnackBar("Network error: ${failure?.message}")
        }
    }

    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        val view = snackBar.view
        val params = view.layoutParams as? FrameLayout.LayoutParams
        params?.let {
            it.gravity = Gravity.BOTTOM
            it.bottomMargin = 50
            view.layoutParams = it
        }

        view.setBackgroundColor(
            ContextCompat.getColor(this, R.color.colorPrimaryDark)
        )

        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
            setTextColor(Color.WHITE)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        snackBar.show()
    }

    // ProductAdapterListener implementation
    override fun onUpdateWishlist(mvariant_id: String) {
        val preferences = getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        val userId = preferences?.getString("userId", "").orEmpty()
        this.currentWishlistVariantId = mvariant_id // Store the variant ID before calling API
        wishlistViewModel.wishlist(token, WishlistRequest(userId, mvariant_id))
    }

    override fun onUpdateCart(totalItems: Int, productId: Int) {
        // This callback means the adapter has successfully updated the database.
        // Since we're observing the database with Flow, the UI will automatically refresh.
        // You can use this callback for other side effects, e.g., updating a global cart counter
        // in your main activity's bottom navigation bar, if applicable.
        Log.d("CartFragment", "Cart item updated via adapter: ProductId $productId, Quantity $totalItems")
    }
}