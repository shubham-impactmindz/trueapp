package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log // Added for debugging
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.databinding.FragmentCartBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.component.main.shop.ProductAdapterListener
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
    private var currentWishlistVariantId: String? = null // Store variant ID for wishlist toggles

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
    }

    private fun initializeViewModels() {
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer ${preferences?.getString("token", "") ?: ""}"
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
        cartAdapter = CartAdapter(requireContext(), this@CartFragment)
        binding.cartListRecycler.layoutManager = LinearLayoutManager(context)
        binding.cartListRecycler.adapter = cartAdapter
        Log.d("CartFragment", "RecyclerView and Adapter set up.")
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
                    binding.textTotal.text = "£ 0.00" // Reset total price for empty cart
                }
            }
        }
    }

    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        val totalAmount = cartItems.sumOf { it.price * it.quantity }
        binding.textTotal.text = "£ %.2f".format(totalAmount)
        Log.d("CartFragment", "Total amount updated: £ %.2f".format(totalAmount))
    }

    private fun setupView() {
        binding.checkoutLayout.setOnClickListener {
            val intent = Intent(context, CheckOutActivity::class.java)
            // Optionally pass total amount or cart items data if CheckOutActivity needs it
            startActivity(intent)
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