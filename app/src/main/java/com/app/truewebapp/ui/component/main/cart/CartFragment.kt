package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log // Added for debugging
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
import kotlinx.coroutines.flow.collectLatest // Correct import for collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartFragment : Fragment(), ProductAdapterListener {

    private lateinit var binding: FragmentCartBinding
    private lateinit var cartAdapter: CartAdapter
    private lateinit var wishlistViewModel: WishlistViewModel

    // Lazy initialization for cartDao, ensures context is available
    private val cartDao by lazy { context?.let { CartDatabase.getInstance(it).cartDao() } }

    private var token = ""
    private var walletBalance = ""
    private var currentWishlistVariantId: String? = null // Store variant ID for wishlist toggles
    private lateinit var deliverySettingsViewModel: DeliverySettingsViewModel
    private lateinit var walletBalanceViewModel: WalletBalanceViewModel
    private lateinit var cartViewModel: CartViewModel
    private var minOrderValue: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModels()
        setupRecyclerView() // Initialize RecyclerView and Adapter here once
        observeCartItems()  // Start observing database changes here
        setupView()
        observeWishlistViewModel() // Observe wishlist actions
        observeDeliverySettingsViewModel() // Observe wishlist actions
        observeCart()
        observeWalletBalance()
    }

    private fun initializeViewModels() {
        walletBalanceViewModel = ViewModelProvider(this)[WalletBalanceViewModel::class.java]
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        deliverySettingsViewModel = ViewModelProvider(this)[DeliverySettingsViewModel::class.java]
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"
        deliverySettingsViewModel.deliverySetting(token)
    }

    // No need for loadCartFromDatabase() in onResume() if you're observing Flow.
    // The Flow automatically re-emits when data changes.
    override fun onResume() {
        super.onResume()
        observeCartItems()
        walletBalanceViewModel.walletBalance(token)
        // If there's a reason to re-evaluate the cart outside of direct DB changes (e.g.,
        // if user comes back from a product detail page where cart was updated locally but not through DB observer)
        // you might need a mechanism here, but for now, Flow is sufficient for DB changes.
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(requireContext(), this@CartFragment)
        binding.cartListRecycler.layoutManager = LinearLayoutManager(context)
        binding.cartListRecycler.adapter = cartAdapter
        Log.d("CartFragment", "RecyclerView and Adapter set up.")
    }

    private fun observeWalletBalance() {
        walletBalanceViewModel.walletBalanceResponse.observe(viewLifecycleOwner) { response ->
            response?.let { it ->

                if (it.success) {
                    walletBalance = it.balance
                    "£$walletBalance".also { binding.tvWalletBalance.text = it }
                }
            }
        }

        walletBalanceViewModel.isLoading.observe(viewLifecycleOwner) {

        }

        walletBalanceViewModel.apiError.observe(viewLifecycleOwner) {

        }

        walletBalanceViewModel.onFailure.observe(viewLifecycleOwner) {

        }
    }

    private fun observeCart() {
        cartViewModel.changePasswordResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.status) {

                    val intent = Intent(context, CheckOutActivity::class.java)
                    intent.putExtra("minOrderValue",minOrderValue)
                    intent.putExtra("walletBalance",walletBalance)
                    // Optionally pass total amount or cart items data if CheckOutActivity needs it
                    startActivity(intent)
                }

            }
        }

        // Observe other LiveData from ViewModel for better UX
        cartViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide a progress indicator if needed
        }
        cartViewModel.apiError.observe(viewLifecycleOwner) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        cartViewModel.onFailure.observe(viewLifecycleOwner) { failure ->
            showTopSnackBar("Network error: ${failure?.message}")
        }
    }

    // This is where data comes from and updates the adapter
    private fun observeCartItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            cartDao?.getAllItems()?.collectLatest { cartItems ->
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
                    binding.textItems.text = "0 Units"
                    binding.textSKU.text = "0 SKUs"
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
            binding.textDelivery.setTextColor(ContextCompat.getColor(requireContext(), R.color.textGrey)) // Optional style
        } else {
            binding.textDelivery.text = "FREE DELIVERY"
            binding.textDelivery.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorGreen)) // Optional style
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
                    cartDao?.getAllItemsOnce()
                }

                // Optional: Skip if cart is empty
                if (cartItems?.isEmpty() == true) {
                    Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Step 2: Map items to Cart DTOs
                val cartList = cartItems?.map {
                    Cart(
                        mvariant_id = it.variantId.toString(),
                        quantity = it.quantity.toString()
                    )
                }

                // Step 3: Create request and send
                val cartRequest = CartRequest(cart = cartList!!)
                cartViewModel.cart(token, cartRequest)
            }
        }
    }

    // Renamed from updateWishlistInAdapter to reflect its role in observing ViewModel
    private fun observeWishlistViewModel() {
        wishlistViewModel.wishlistResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.status) {
                    showTopSnackBar(it.message ?: "Wishlist updated successfully!")
                    // Update the database based on the wishlist toggle, which will
                    // in turn trigger the Flow in observeCartItems and refresh the UI.
                    currentWishlistVariantId?.let { variantIdString ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val variantIdInt = variantIdString.toIntOrNull()
                            if (variantIdInt != null) {
                                val item = withContext(Dispatchers.IO) {
                                    cartDao?.getItemByVariantId(variantIdInt)
                                }
                                item?.let { existingItem ->
                                    val updatedItem = existingItem.copy(isWishlisted = !existingItem.isWishlisted)
                                    cartDao?.insertOrUpdateItem(updatedItem) // Update in DB
                                    Log.d("CartFragment", "Wishlist status updated in DB for variant: $variantIdInt")
                                }
                            }
                        }
                    }
                } else {
                    showTopSnackBar(it.message ?: "Failed to update wishlist.")
                }
                currentWishlistVariantId = null // Clear the stored ID after processing
            }
        }

        // Observe other LiveData from ViewModel for better UX
        wishlistViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide a progress indicator if needed
        }
        wishlistViewModel.apiError.observe(viewLifecycleOwner) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        wishlistViewModel.onFailure.observe(viewLifecycleOwner) { failure ->
            showTopSnackBar("Network error: ${failure?.message}")
        }
    }

    private fun observeDeliverySettingsViewModel() {
        deliverySettingsViewModel.deliverySettingsResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.status) {
                    minOrderValue = it.min_order_free_delivery
                }
            }
        }

        // Observe other LiveData from ViewModel for better UX
        deliverySettingsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide a progress indicator if needed
        }
        deliverySettingsViewModel.apiError.observe(viewLifecycleOwner) { errorMessage ->
            showTopSnackBar(errorMessage ?: "An API error occurred.")
        }
        deliverySettingsViewModel.onFailure.observe(viewLifecycleOwner) { failure ->
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
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
        )

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