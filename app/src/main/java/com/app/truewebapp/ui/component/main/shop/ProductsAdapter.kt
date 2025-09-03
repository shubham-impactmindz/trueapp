package com.app.truewebapp.ui.component.main.shop

// Android and Kotlin imports
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.text.Html
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.browse.Product
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.utils.GlideApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// RecyclerView Adapter class responsible for binding product data to item views
class ProductsAdapter(
    private val listener: ProductAdapterListener, // Listener to handle callbacks (wishlist, cart updates)
    private var products: List<Product>, // List of product objects to display
    private val title: String?, // Optional title for section/category
    private val cdnURL: String, // Base CDN URL for product images
    private val context: Context // Context required for DB, resources, etc.
) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    // DAO instance to access cart database
    private val cartDao = CartDatabase.getInstance(context).cartDao()

    // ViewHolder holds references to all UI components for a single product item
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llCartSign: LinearLayout = view.findViewById(R.id.llCartSign)
        val btnAdd: LinearLayout = view.findViewById(R.id.btnAdd)
        val btnMinus: ImageView = view.findViewById(R.id.btnMinus)
        val btnAddMore: ImageView = view.findViewById(R.id.btnAddMore)
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val textNoOfItems: TextView = view.findViewById(R.id.textNoOfItems)
        val finalPrice: TextView = view.findViewById(R.id.finalPrice)
        val comparePrice: TextView = view.findViewById(R.id.comparePrice)
        val textBrand: TextView = view.findViewById(R.id.textBrand)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textFlavour: TextView = view.findViewById(R.id.textFlavour)
        val lottieCheckmark: LottieAnimationView = view.findViewById(R.id.lottieCheckmark)
        val btnFavorite: ImageView = view.findViewById(R.id.btnFavorite)
        val btnFavoriteSelected: ImageView = view.findViewById(R.id.btnFavoriteSelected)
        val llOffer: LinearLayout = view.findViewById(R.id.llOffer)
        val tvOffer: TextView = view.findViewById(R.id.tvOffer)
    }

    // Called when RecyclerView needs a new ViewHolder object
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate product item layout from XML
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_products, parent, false)
        return ViewHolder(view) // Return a new ViewHolder containing item views
    }

    // Called to bind product data to the ViewHolder
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position] // Get product at current position

        // Set brand name
        holder.textBrand.text = product.brand_name
        // Parse HTML product title into plain text
        holder.textTitle.text = Html.fromHtml(product.mproduct_title, Html.FROM_HTML_MODE_LEGACY).toString()

        // Extract and format option values (e.g., size/flavour)
        val values = product.options.take(2).mapNotNull { product.option_value[it] }
            .map { it.split(" ").joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) } }
        holder.textFlavour.text = values.joinToString("/")

        // Build image URL (fallback if main image missing)
        val imageUrl = if (!product.image.isNullOrEmpty()) cdnURL + product.image else cdnURL + product.mproduct_image
        // Load image using Glide with placeholder and error fallback
        GlideApp.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_logo_red_blue)
            .error(R.drawable.ic_logo_red_blue)
            .into(holder.imgProduct)

        // Update different UI sections for wishlist, price, deal tags
        updateWishlistUI(holder, product)
        updatePriceUI(holder, product)
        updateDealTagUI(holder, product)

        // Display formatted product price
        holder.finalPrice.text = "£%.2f".format(product.price)

        // Launch coroutine to check current cart quantity for this product
        CoroutineScope(Dispatchers.Main).launch {
            val quantity = withContext(Dispatchers.IO) {
                cartDao.getQuantityByVariantId(product.mvariant_id) ?: 0
            }
            updateCartUI(holder, product, quantity) // Update cart-related UI
        }

        // Handle click on "Add to Wishlist" button
        holder.btnFavorite.setOnClickListener {
            holder.btnFavorite.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            CoroutineScope(Dispatchers.Main).launch {
                val quantity = withContext(Dispatchers.IO) {
                    cartDao.getQuantityByVariantId(product.mvariant_id) ?: 0
                }
                if (quantity > 0) {
                    updateFavouriteAsync(product, quantity, true) // Mark as favorite
                }
            }
            listener.onUpdateWishlist(product.mvariant_id.toString()) // Notify listener
        }

        // Handle click on "Remove from Wishlist" button
        holder.btnFavoriteSelected.setOnClickListener {
            holder.btnFavoriteSelected.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            CoroutineScope(Dispatchers.Main).launch {
                val quantity = withContext(Dispatchers.IO) {
                    cartDao.getQuantityByVariantId(product.mvariant_id) ?: 0
                }
                if (quantity > 0) {
                    updateFavouriteAsync(product, quantity, false) // Remove from favorite
                }
            }
            listener.onUpdateWishlist(product.mvariant_id.toString())
        }
    }

    // Update UI for cart interactions (add, remove, update)
    private fun updateCartUI(holder: ViewHolder, product: Product, initialCount: Int) {
        var quantity = initialCount

        if (product.quantity == 0) { // Handle "out of stock" state
            holder.tvOffer.text = "Out of stock"
            holder.textBrand.setTextColor(context.getColor(android.R.color.darker_gray))
            holder.textTitle.setTextColor(context.getColor(android.R.color.darker_gray))
            holder.textFlavour.setTextColor(context.getColor(android.R.color.darker_gray))
            holder.finalPrice.setTextColor(context.getColor(android.R.color.darker_gray))
            holder.tvOffer.visibility = View.VISIBLE
            holder.llOffer.visibility = View.VISIBLE
            holder.btnAdd.isEnabled = false
            holder.btnAddMore.isEnabled = false
            holder.btnMinus.isEnabled = false
            holder.llCartSign.visibility = View.GONE
            holder.btnAdd.visibility = View.GONE
        } else { // Product is available
            updateOfferUI(holder, product)
            holder.finalPrice.setTextColor(context.getColor(android.R.color.black))
            holder.btnAdd.isEnabled = true
            holder.btnAddMore.isEnabled = true
            holder.btnMinus.isEnabled = true

            // Show current quantity if > 0, otherwise show Add button
            if (quantity > 0) {
                holder.llCartSign.visibility = View.VISIBLE
                holder.btnAdd.visibility = View.GONE
                holder.textNoOfItems.text = quantity.toString()
            } else {
                holder.llCartSign.visibility = View.GONE
                holder.btnAdd.visibility = View.VISIBLE
            }

            // Handle Add button click (initial add to cart)
            holder.btnAdd.setOnClickListener {
                holder.btnAdd.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
                quantity = 1
                updateCartAsync(product, quantity)
                holder.textNoOfItems.text = quantity.toString()
                holder.btnAdd.visibility = View.GONE
                holder.llCartSign.visibility = View.VISIBLE
            }

            // Handle Add More button click (increase quantity)
            holder.btnAddMore.setOnClickListener {
                quantity++
                holder.btnAddMore.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                updateCartAsync(product, quantity)
                holder.textNoOfItems.text = quantity.toString()
            }

            // Handle Minus button click (decrease quantity or remove)
            holder.btnMinus.setOnClickListener {
                quantity--
                holder.btnMinus.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
                if (quantity > 0) {
                    updateCartAsync(product, quantity)
                    holder.textNoOfItems.text = quantity.toString()
                } else {
                    deleteCartItemAsync(product)
                    holder.llCartSign.visibility = View.GONE
                    holder.btnAdd.visibility = View.VISIBLE
                }
            }
        }
    }

    // Insert or update cart item in DB
    private fun updateCartAsync(product: Product, quantity: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cartItem = CartItemEntity(
                    variantId = product.mvariant_id,
                    title = product.mproduct_title ?: "",
                    options = product.option_value,
                    image = product.image,
                    fallbackImage = product.mproduct_image,
                    price = product.price,
                    comparePrice = product.compare_price ?: 0.0,
                    isWishlisted = product.user_info_wishlist,
                    cdnURL = cdnURL,
                    quantity = quantity,
                    taxable = product.taxable
                )
                cartDao.insertOrUpdateItem(cartItem) // Save to DB
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Update wishlist status in DB
    private fun updateFavouriteAsync(product: Product, quantity: Int, b: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cartItem = CartItemEntity(
                    variantId = product.mvariant_id,
                    title = product.mproduct_title ?: "",
                    options = product.option_value,
                    image = product.image,
                    fallbackImage = product.mproduct_image,
                    price = product.price,
                    comparePrice = product.compare_price ?: 0.0,
                    isWishlisted = b,
                    cdnURL = cdnURL,
                    quantity = quantity,
                    taxable = product.taxable
                )
                cartDao.insertOrUpdateItem(cartItem) // Save updated wishlist state

                withContext(Dispatchers.Main) {
                    listener.onUpdateCart(quantity, product.mvariant_id) // Notify UI
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Delete product from cart DB
    private fun deleteCartItemAsync(product: Product) {
        CoroutineScope(Dispatchers.IO).launch {
            cartDao.deleteItemByVariantId(product.mvariant_id)
            withContext(Dispatchers.Main) {
                listener.onUpdateCart(0, product.mvariant_id)
            }
        }
    }

    // Update UI for wishlist buttons
    private fun updateWishlistUI(holder: ViewHolder, product: Product) {
        if (product.user_info_wishlist) {
            holder.btnFavoriteSelected.visibility = View.VISIBLE
            holder.btnFavorite.visibility = View.GONE
        } else {
            holder.btnFavoriteSelected.visibility = View.GONE
            holder.btnFavorite.visibility = View.VISIBLE
        }
    }

    // Update UI for displaying price and compare price
    private fun updatePriceUI(holder: ViewHolder, product: Product) {
        if (product.compare_price == 0.0) {
            holder.comparePrice.visibility = View.GONE
            holder.finalPrice.setTextColor(context.getColor(R.color.black))
        } else {
            holder.comparePrice.visibility = View.VISIBLE
            holder.comparePrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG // Strike-through old price
            holder.comparePrice.setTextColor(context.getColor(R.color.black))
            holder.finalPrice.setTextColor(context.getColor(R.color.colorSecondary))
            holder.comparePrice.text = "£%.2f".format(product.compare_price)
        }
    }

    // Update UI for product offers
    private fun updateOfferUI(holder: ViewHolder, product: Product) {
        if (product.product_offer.isNullOrEmpty()) {
            holder.llOffer.visibility = View.INVISIBLE
            holder.tvOffer.visibility = View.INVISIBLE
        } else {
            holder.llOffer.visibility = View.VISIBLE
            holder.tvOffer.visibility = View.VISIBLE
            holder.tvOffer.text = product.product_offer
        }
    }

    // Update UI for product deal tags (e.g., Flash Deal, Sale)
    private fun updateDealTagUI(holder: ViewHolder, product: Product) {
        if (product.product_deal_tag.isNullOrEmpty()) {
            holder.lottieCheckmark.visibility = View.INVISIBLE
        } else {
            holder.lottieCheckmark.visibility = View.VISIBLE
            val animationRes = if (product.product_deal_tag.lowercase() == "flash deal") {
                R.raw.flash_deals
            } else {
                R.raw.sale
            }
            holder.lottieCheckmark.setAnimation(animationRes) // Set correct animation
        }
    }

    // Replace current dataset with a new one and refresh RecyclerView
    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    // Find index of product in list by its ID
    fun getProductIndex(productId: String): Int {
        return products.indexOfFirst { it.mproduct_id.toString() == productId }
    }

    // Notify adapter that product at given ID has changed
    fun notifyProductChanged(productId: String) {
        val position = getProductIndex(productId)
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    // Return number of products in list
    override fun getItemCount(): Int = products.size
}