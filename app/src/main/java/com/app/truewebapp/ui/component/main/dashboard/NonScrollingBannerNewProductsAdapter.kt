package com.app.truewebapp.ui.component.main.dashboard

// Suppress lint warnings for hardcoded text (used for setting TextView text directly)
import android.annotation.SuppressLint
// Android core imports
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
// Lifecycle imports for observing LiveData
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
// Animation library for displaying animated checkmarks (e.g., flash deals, sales)
import com.airbnb.lottie.LottieAnimationView
// Project resource references
import com.app.truewebapp.R
// Data model imports
import com.app.truewebapp.data.dto.dashboard_banners.Product
import com.app.truewebapp.data.dto.dashboard_banners.ProductBanner
// Local database entities and ViewModel for cart management
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartViewModel
// Listener for handling wishlist/cart actions
import com.app.truewebapp.ui.component.main.shop.NewProductTopSellerAdapterListener
// Glide wrapper for image loading
import com.app.truewebapp.utils.GlideApp

/**
 * Adapter for displaying non-scrolling banners of **New Products** in a RecyclerView.
 *
 * @param listener Callback interface for wishlist/cart actions.
 * @param products List of [ProductBanner] items containing product info.
 * @param cdnURL Base URL for loading product images from CDN.
 * @param context Android context for inflating views and resources.
 * @param cartViewModel ViewModel for managing cart data with LiveData.
 * @param lifecycleOwner Lifecycle owner used for observing LiveData safely.
 */
class NonScrollingBannerNewProductsAdapter(
    private val listener: NewProductTopSellerAdapterListener, // Handles UI updates (wishlist, cart)
    private var products: List<ProductBanner>,                // List of product banners
    private val cdnURL: String,                               // CDN URL for product images
    private val context: Context,                             // Context reference
    private val cartViewModel: CartViewModel,                 // Cart ViewModel for DB operations
    private val lifecycleOwner: LifecycleOwner                // LifecycleOwner for LiveData observation
) : RecyclerView.Adapter<NonScrollingBannerNewProductsAdapter.ViewHolder>() {

    /**
     * ViewHolder class holding references to all UI components inside item_dashboard_products.xml.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llCartSign: LinearLayout = view.findViewById(R.id.llCartSign)
        val btnAdd: ImageView = view.findViewById(R.id.btnAdd)
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

    /**
     * Inflates the item layout and creates a new [ViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_products, parent, false)
        return ViewHolder(v)
    }

    /**
     * Binds product data to the UI for a specific position.
     */
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val banner = products[position]   // Current banner item
        val product = banner.product      // Extract product from banner

        // Set brand, title, and flavour text
        holder.textBrand.text = product.brand_name
        holder.textTitle.text = Html.fromHtml(product.mproduct_title, Html.FROM_HTML_MODE_LEGACY)
        holder.textFlavour.text = product.options
            .take(2) // Take first 2 options
            .mapNotNull { product.option_value[it] }
            .joinToString("/") {
                // Capitalize each word in the option
                it.split(" ").joinToString(" ") { s -> s.replaceFirstChar(Char::uppercaseChar) }
            }

        // Load product image with Glide
        val imageUrl = cdnURL + (product.image ?: product.mproduct_image)
        GlideApp.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_logo_red_blue) // Show default logo while loading
            .error(R.drawable.ic_logo_red_blue)       // Fallback image on error
            .into(holder.imgProduct)

        // Update UI states
        updateWishlistUI(holder, product)
        updatePriceUI(holder, product)
        updateDealTagUI(holder, product)
        updateOfferUI(holder, product)

        // Set price text
        holder.finalPrice.text = "£%.2f".format(product.price)

        // Default visibility setup
        holder.llCartSign.visibility = View.GONE
        holder.btnAdd.visibility = View.VISIBLE

        // Observe LiveData for cart quantity updates
        cartViewModel.quantityLiveData(product.mvariant_id)
            .observe(lifecycleOwner) { qty ->
                updateCartUI(holder, product, qty ?: 0)
            }

        // Wishlist button clicks
        holder.btnFavorite.setOnClickListener {
            holder.btnFavorite.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            listener.onUpdateWishlist(product.mvariant_id.toString(), "New Product")
        }
        holder.btnFavoriteSelected.setOnClickListener {
            holder.btnFavoriteSelected.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            listener.onUpdateWishlist(product.mvariant_id.toString(), "New Product")
        }

        // Add to cart button click
        holder.btnAdd.setOnClickListener {
            holder.btnAdd.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            holder.textNoOfItems.text = "1"

            // Update cart in DB and notify listener
            listener.onUpdateCart(1, product.mvariant_id)
            cartViewModel.addOrUpdateCart(toEntity(product, 1))
        }

        // Increase quantity button
        holder.btnAddMore.setOnClickListener {
            val newQty = (holder.textNoOfItems.text.toString().toIntOrNull() ?: 0) + 1
            cartViewModel.addOrUpdateCart(toEntity(product, newQty))
            holder.btnAddMore.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }

        // Decrease quantity button
        holder.btnMinus.setOnClickListener {
            val currentQty = (holder.textNoOfItems.text.toString().toIntOrNull() ?: 0)
            if (currentQty > 1) {
                cartViewModel.addOrUpdateCart(toEntity(product, currentQty - 1))
            } else {
                cartViewModel.deleteCartByVariant(product.mvariant_id)
                listener.onUpdateCart(0, product.mvariant_id)
            }
            holder.btnMinus.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    /**
     * Converts a [Product] into a [CartItemEntity] for database storage.
     */
    private fun toEntity(product: Product, qty: Int): CartItemEntity {
        return CartItemEntity(
            variantId = product.mvariant_id,
            title = product.mproduct_title ?: "",
            options = product.option_value,
            image = product.image,
            fallbackImage = product.mproduct_image,
            price = product.price,
            comparePrice = product.compare_price ?: 0.0,
            isWishlisted = product.user_info_wishlist,
            cdnURL = cdnURL,
            quantity = qty,
            taxable = product.taxable
        )
    }

    /**
     * Updates cart UI based on stock availability and quantity.
     */
    private fun updateCartUI(holder: ViewHolder, product: Product, quantity: Int) {
        if (product.quantity < 0) {
            // Out of stock
            holder.tvOffer.text = "Out of stock"
            holder.tvOffer.visibility = View.VISIBLE
            holder.llOffer.visibility = View.VISIBLE
            holder.btnAdd.visibility = View.GONE
            holder.llCartSign.visibility = View.GONE
            
            // Fade text for out of stock items
            holder.textBrand.alpha = 0.5f
            holder.textTitle.alpha = 0.5f
            holder.textFlavour.alpha = 0.5f
            holder.finalPrice.alpha = 0.5f
            holder.comparePrice.alpha = 0.5f
        } else {
            // Reset alpha for in-stock items
            holder.textBrand.alpha = 1.0f
            holder.textTitle.alpha = 1.0f
            holder.textFlavour.alpha = 1.0f
            holder.finalPrice.alpha = 1.0f
            holder.comparePrice.alpha = 1.0f
            
            if (quantity > 0) {
                // Item already in cart
                holder.btnAdd.visibility = View.GONE
                holder.llCartSign.visibility = View.VISIBLE
                holder.textNoOfItems.text = quantity.toString()
            } else if (quantity == 0) {
                // Not in cart
                holder.llCartSign.visibility = View.GONE
                holder.btnAdd.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Updates wishlist icon state depending on product's wishlist status.
     */
    private fun updateWishlistUI(holder: ViewHolder, product: Product) {
        if (product.user_info_wishlist) {
            holder.btnFavoriteSelected.visibility = View.VISIBLE
            holder.btnFavorite.visibility = View.GONE
        } else {
            holder.btnFavoriteSelected.visibility = View.GONE
            holder.btnFavorite.visibility = View.VISIBLE
        }
    }

    /**
     * Updates product price UI (handles compare price with strikethrough).
     */
    private fun updatePriceUI(holder: ViewHolder, product: Product) {
        if (product.compare_price == null || product.compare_price == 0.0) {
            holder.comparePrice.visibility = View.GONE
            holder.finalPrice.setTextColor(context.getColor(R.color.black))
        } else {
            holder.comparePrice.visibility = View.VISIBLE
            holder.comparePrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            holder.comparePrice.setTextColor(context.getColor(R.color.black))
            holder.finalPrice.setTextColor(context.getColor(R.color.colorSecondary))
            holder.comparePrice.text = "£%.2f".format(product.compare_price)
        }
    }

    /**
     * Updates UI to show deal tags (e.g., Flash Deal, Sale).
     */
    private fun updateDealTagUI(holder: ViewHolder, product: Product) {
        if (product.product_deal_tag.isNullOrEmpty()) {
            holder.lottieCheckmark.visibility = View.INVISIBLE
        } else {
            holder.lottieCheckmark.visibility = View.VISIBLE
            val animRes = if (product.product_deal_tag.lowercase() == "flash deal")
                R.raw.flash_deals
            else
                R.raw.sale
            holder.lottieCheckmark.setAnimation(animRes)
        }
    }

    /**
     * Updates UI to display product offers.
     */
    private fun updateOfferUI(holder: ViewHolder, product: Product) {
        val dealText = calculateDealText(product)
        
        if (dealText.isNullOrEmpty()) {
            holder.llOffer.visibility = View.INVISIBLE
            holder.tvOffer.visibility = View.INVISIBLE
        } else {
            holder.llOffer.visibility = View.VISIBLE
            holder.tvOffer.visibility = View.VISIBLE
            holder.tvOffer.text = dealText
        }
    }
    
    /**
     * Calculate deal text based on deal type and parameters
     */
    private fun calculateDealText(product: Product): String? {
        return when (product.deal_type) {
            "buy_x_get_y" -> {
                val buyQty = product.deal_buy_quantity
                val getQty = product.deal_get_quantity
                if (buyQty != null && getQty != null && buyQty > 0 && getQty > 0) {
                    "Buy $buyQty Get $getQty Free"
                } else {
                    product.product_offer // Fallback to original offer text
                }
            }
            "volume_discount" -> {
                val dealQty = product.deal_quantity
                val dealPrice = product.deal_price
                if (dealQty != null && dealPrice != null && dealQty > 0 && dealPrice > 0) {
                    "Any $dealQty for £%.2f".format(dealPrice)
                } else {
                    product.product_offer // Fallback to original offer text
                }
            }
            else -> {
                // If no deal_type or unknown type, use original product_offer
                product.product_offer
            }
        }
    }

    /**
     * Returns the number of product items in the adapter.
     */
    override fun getItemCount(): Int = products.size
}