package com.app.truewebapp.ui.component.main.cart

// Import statements for required Android and Kotlin libraries
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log // Added for debugging purposes
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.cart.Cart
import com.app.truewebapp.data.dto.cart.CartRequest
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.databinding.FragmentCartBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.component.main.shop.ProductAdapterListener
import com.app.truewebapp.ui.viewmodel.CartViewModel
import com.app.truewebapp.ui.viewmodel.DeliverySettingsViewModel
import com.app.truewebapp.ui.viewmodel.WalletBalanceViewModel
import com.app.truewebapp.ui.viewmodel.WishlistViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest // Used for collecting Flow emissions
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// CartFragment is responsible for managing cart-related UI inside a fragment
class CartFragment : Fragment(), ProductAdapterListener {

    // View binding for accessing UI components
    private lateinit var binding: FragmentCartBinding

    // Adapter for displaying cart items in RecyclerView
    private lateinit var cartAdapter: CartAdapter

    // ViewModel for handling wishlist-related actions
    private lateinit var wishlistViewModel: WishlistViewModel

    // Lazy initialization of DAO for accessing cart database
    private val cartDao by lazy { context?.let { CartDatabase.getInstance(it).cartDao() } }

    // Authentication token for API calls
    private var token = ""

    // User's wallet balance
    private var walletBalance = ""

    // Store variant ID for wishlist toggle updates
    private var currentWishlistVariantId: String? = null

    // ViewModels for delivery settings, wallet balance, and cart operations
    private lateinit var deliverySettingsViewModel: DeliverySettingsViewModel
    private lateinit var walletBalanceViewModel: WalletBalanceViewModel
    private lateinit var cartViewModel: CartViewModel

    // Minimum order value required for free delivery
    private var minOrderValue: String? = null
    
    // Minimum order value required to place an order
    private var minOrderPlace: String? = null

    // Inflate the fragment's layout and initialize view binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called when the fragment’s view hierarchy has been created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModels()              // Initialize ViewModels
        setupRecyclerView()                 // Setup RecyclerView and adapter
        setupView()                         // Setup UI event listeners
        observeWishlistViewModel()          // Observe wishlist updates
        observeDeliverySettingsViewModel()  // Observe delivery settings updates
        observeCart()                       // Observe cart-related API calls
        observeWalletBalance()              // Observe wallet balance API
    }

    // Initialize required ViewModels and fetch token from SharedPreferences
    private fun initializeViewModels() {
        walletBalanceViewModel = ViewModelProvider(this)[WalletBalanceViewModel::class.java]
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        deliverySettingsViewModel = ViewModelProvider(this)[DeliverySettingsViewModel::class.java]
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer ${preferences?.getString("token", "") ?: ""}" // Retrieve token
        deliverySettingsViewModel.deliverySetting(token) // Fetch delivery settings
    }

    // Fragment lifecycle method called when resuming
    override fun onResume() {
        super.onResume()
        observeCartItems()                        // Observe local cart database
        walletBalanceViewModel.walletBalance(token) // Fetch wallet balance from API
    }

    // Setup RecyclerView with LinearLayoutManager and CartAdapter
    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(requireContext(), this@CartFragment)
        binding.cartListRecycler.layoutManager = LinearLayoutManager(context)
        binding.cartListRecycler.adapter = cartAdapter
        Log.d("CartFragment", "RecyclerView and Adapter set up.")
    }

    // Observe wallet balance updates from WalletBalanceViewModel
    private fun observeWalletBalance() {
        walletBalanceViewModel.walletBalanceResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.success) {
                    walletBalance = it.balance
                    "£$walletBalance".also { binding.tvWalletBalance.text = it }
                }
            }
        }

        // Observers for additional LiveData (loading, error, failure)
        walletBalanceViewModel.isLoading.observe(viewLifecycleOwner) {}
        walletBalanceViewModel.apiError.observe(viewLifecycleOwner) {}
        walletBalanceViewModel.onFailure.observe(viewLifecycleOwner) {}
    }

    // Observe cart API responses from CartViewModel
    private fun observeCart() {
        cartViewModel.changePasswordResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.status) {
                    // Start checkout activity if cart API succeeds
                    val intent = Intent(context, CheckOutActivity::class.java)
                    intent.putExtra("minOrderValue", minOrderValue)
                    intent.putExtra("walletBalance", walletBalance)
                    startActivity(intent)
                }
            }
        }

        // Observers for additional LiveData
        cartViewModel.isLoading.observe(viewLifecycleOwner) { }
        cartViewModel.apiError.observe(viewLifecycleOwner) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        cartViewModel.onFailure.observe(viewLifecycleOwner) { failure ->
            showTopSnackBar("Network error: ${failure?.message}")
        }
    }

    // Observe cart items from local database (Flow-based)
    private fun observeCartItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            cartDao?.getAllItems()?.collectLatest { cartItems ->
                Log.d("CartFragment", "Received ${cartItems.size} items from database Flow.")
                
                // Convert to display items and calculate free items
                val displayItems = convertToDisplayItems(cartItems)
                cartAdapter.updateData(displayItems) // Update adapter data

                if (displayItems.isNotEmpty()) {
                    binding.cartListRecycler.visibility = View.VISIBLE
                    binding.linearCartDetail.visibility = View.VISIBLE
                    binding.textEmptyCart.visibility = View.GONE
                    binding.imageCart.visibility = View.GONE
                    updateTotalAmount(cartItems) // Update price details (use original items for total calculation)
                } else {
                    binding.cartListRecycler.visibility = View.GONE
                    binding.linearCartDetail.visibility = View.GONE
                    binding.textEmptyCart.visibility = View.VISIBLE
                    binding.imageCart.visibility = View.VISIBLE
                    binding.textItems.text = "0 Units"
                    binding.textTotal.text = "£0.00" // Reset price
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

    // Calculate total cart value, apply delivery conditions, and update UI
    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        val totalQuantity = cartItems.sumOf { it.quantity }
        binding.textItems.text = "$totalQuantity Units"

        val totalAmount = cartItems.sumOf { it.price * it.quantity }
        var deliveryFee = 0.0
        val minOrder = minOrderValue?.toDoubleOrNull() ?: 0.0
        val minOrderPlaceValue = minOrderPlace?.toDoubleOrNull() ?: 0.0

        // Check if minimum order place value is not met first
        if (totalAmount < minOrderPlaceValue) {
            val amountNeeded = minOrderPlaceValue - totalAmount
            binding.textDelivery.text = "Add £%.2f for checkout".format(amountNeeded)
            binding.textDelivery.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary))
        } else if (totalAmount < minOrder) {
            deliveryFee = 0.0
            val amountLeft = minOrder - totalAmount
            binding.textDelivery.text = "Spend £%.2f more for FREE delivery".format(amountLeft)
            binding.textDelivery.setTextColor(ContextCompat.getColor(requireContext(), R.color.textGrey))
        } else {
            binding.textDelivery.text = "FREE DELIVERY"
            binding.textDelivery.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        }

        val finalTotal = totalAmount + deliveryFee
        binding.textTotal.text = "£%.2f".format(finalTotal)
        
        // Check minimum order place value and enable/disable checkout button
        if (totalAmount >= minOrderPlaceValue) {
            binding.checkoutLayout.isEnabled = true
            binding.checkoutLayout.alpha = 1.0f
            binding.checkoutLayout.setBackgroundResource(R.drawable.border_solid_secondary)
            // Reset text color to original for the TextView inside checkoutLayout
            (binding.checkoutLayout.getChildAt(0) as? TextView)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            binding.checkoutLayout.isEnabled = false
            binding.checkoutLayout.alpha = 1.0f // Keep full opacity but change colors
            binding.checkoutLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lightGrey))
            // Set text color to grey for the TextView inside checkoutLayout
            (binding.checkoutLayout.getChildAt(0) as? TextView)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.textGrey))
        }
        
        Log.d("CartFragment", "Total: £$totalAmount, Delivery: £$deliveryFee, Final: £$finalTotal, MinOrderPlace: £$minOrderPlaceValue")
    }

    // Setup checkout button click listener
    private fun setupView() {
        binding.checkoutLayout.setOnClickListener {
            binding.checkoutLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            
            // Check if checkout button is enabled
            if (!binding.checkoutLayout.isEnabled) {
                val minOrderPlaceValue = minOrderPlace?.toDoubleOrNull() ?: 0.0
                Toast.makeText(context, "Minimum order value is £%.2f to place an order".format(minOrderPlaceValue), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            lifecycleScope.launch {
                val cartItems = withContext(Dispatchers.IO) { cartDao?.getAllItemsOnce() }
                if (cartItems?.isEmpty() == true) {
                    Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val cartList = cartItems?.map {
                    Cart(mvariant_id = it.variantId.toString(), quantity = it.quantity.toString())
                }
                val cartRequest = CartRequest(cart = cartList!!)
                cartViewModel.cart(token, cartRequest) // API call
            }
        }
    }

    // Observe wishlist API responses and update local database accordingly
    private fun observeWishlistViewModel() {
        wishlistViewModel.wishlistResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.status) {
                    showTopSnackBar(it.message ?: "Wishlist updated successfully!")
                    currentWishlistVariantId?.let { variantIdString ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val variantIdInt = variantIdString.toIntOrNull()
                            if (variantIdInt != null) {
                                val item = withContext(Dispatchers.IO) {
                                    cartDao?.getItemByVariantId(variantIdInt)
                                }
                                item?.let { existingItem ->
                                    val updatedItem = existingItem.copy(isWishlisted = !existingItem.isWishlisted)
                                    cartDao?.insertOrUpdateItem(updatedItem)
                                    Log.d("CartFragment", "Wishlist status updated in DB for variant: $variantIdInt")
                                }
                            }
                        }
                    }
                } else {
                    showTopSnackBar(it.message ?: "Failed to update wishlist.")
                }
                currentWishlistVariantId = null
            }
        }

        // Observe other LiveData for better UX
        wishlistViewModel.isLoading.observe(viewLifecycleOwner) { }
        wishlistViewModel.apiError.observe(viewLifecycleOwner) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        wishlistViewModel.onFailure.observe(viewLifecycleOwner) { failure ->
            showTopSnackBar("Network error: ${failure?.message}")
        }
    }

    // Observe delivery settings and update min order value
    private fun observeDeliverySettingsViewModel() {
        deliverySettingsViewModel.deliverySettingsResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.status) {
                    minOrderValue = it.min_order_free_delivery
                    minOrderPlace = it.min_order_place
                    observeCartItems() // Start observing cart again
                }
            }
        }
        deliverySettingsViewModel.isLoading.observe(viewLifecycleOwner) { }
        deliverySettingsViewModel.apiError.observe(viewLifecycleOwner) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        deliverySettingsViewModel.onFailure.observe(viewLifecycleOwner) { failure ->
            showTopSnackBar("Network error: ${failure?.message}")
        }
    }

    // Show Snackbar message at bottom of screen
    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        val view = snackBar.view
        val params = view.layoutParams as? FrameLayout.LayoutParams
        params?.let {
            it.gravity = Gravity.BOTTOM
            it.bottomMargin = 50
            view.layoutParams = it
        }
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
            setTextColor(Color.WHITE)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        snackBar.show()
    }

    // ProductAdapterListener implementation
    override fun onUpdateWishlist(mvariant_id: String) {
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        val userId = preferences?.getString("userId", "").orEmpty()
        this.currentWishlistVariantId = mvariant_id
        wishlistViewModel.wishlist(token, WishlistRequest(userId, mvariant_id))
    }

    // Triggered when cart items are updated in the adapter
    override fun onUpdateCart(totalItems: Int, productId: Int) {
        Log.d("CartFragment", "Cart item updated via adapter: ProductId $productId, Quantity $totalItems")
    }
}