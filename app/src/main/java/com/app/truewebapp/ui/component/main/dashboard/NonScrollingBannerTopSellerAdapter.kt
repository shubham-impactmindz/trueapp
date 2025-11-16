// Importing required Android and Kotlin libraries for UI, context, lifecycle, etc.
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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.dashboard_banners.Product
import com.app.truewebapp.data.dto.dashboard_banners.ProductBanner
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartViewModel
import com.app.truewebapp.ui.component.main.shop.NewProductTopSellerAdapterListener
import com.app.truewebapp.utils.GlideApp

// Adapter class for RecyclerView to show Top Seller products in a non-scrolling banner
class NonScrollingBannerTopSellerAdapter(
    private val listener: NewProductTopSellerAdapterListener, // Listener for handling user actions like wishlist/cart
    private var products: List<ProductBanner>, // List of product banners to display
    private val cdnURL: String, // Base URL for loading product images from CDN
    private val context: Context, // Application/Activity context
    private val cartViewModel: CartViewModel, // ViewModel for managing cart operations
    private val lifecycleOwner: LifecycleOwner // LifecycleOwner to observe LiveData
) : RecyclerView.Adapter<NonScrollingBannerTopSellerAdapter.ViewHolder>() {

    // ViewHolder class to hold references to the views for each item in the RecyclerView
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llCartSign: LinearLayout = view.findViewById(R.id.llCartSign) // Layout for cart controls
        val btnAdd: ImageView = view.findViewById(R.id.btnAdd) // "Add to cart" button
        val btnMinus: ImageView = view.findViewById(R.id.btnMinus) // Decrease quantity button
        val btnAddMore: ImageView = view.findViewById(R.id.btnAddMore) // Increase quantity button
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct) // Product image
        val textNoOfItems: TextView = view.findViewById(R.id.textNoOfItems) // Text showing number of items in cart
        val finalPrice: TextView = view.findViewById(R.id.finalPrice) // Final selling price
        val comparePrice: TextView = view.findViewById(R.id.comparePrice) // Original/compare price (for discounts)
        val textBrand: TextView = view.findViewById(R.id.textBrand) // Brand name
        val textTitle: TextView = view.findViewById(R.id.textTitle) // Product title
        val textFlavour: TextView = view.findViewById(R.id.textFlavour) // Product flavor/options
        val lottieCheckmark: LottieAnimationView = view.findViewById(R.id.lottieCheckmark) // Lottie animation for deal tags
        val btnFavorite: ImageView = view.findViewById(R.id.btnFavorite) // Add to favorites button
        val btnFavoriteSelected: ImageView = view.findViewById(R.id.btnFavoriteSelected) // Remove from favorites button
        val llOffer: LinearLayout = view.findViewById(R.id.llOffer) // Layout for offer text
        val tvOffer: TextView = view.findViewById(R.id.tvOffer) // Offer text view
    }

    // Inflates the item layout and creates a ViewHolder instance
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_products, parent, false) // Inflate product layout
        return ViewHolder(v) // Return ViewHolder with inflated view
    }

    // Binds data to the ViewHolder at the given position
    @SuppressLint("SetTextI18n") // Suppressing warning for string concatenation in setText
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val banner = products[position] // Get product banner at position
        val product = banner.product // Get product from banner

        // Set brand name
        holder.textBrand.text = product.brand_name

        // Set product title using HTML formatting
        holder.textTitle.text = Html.fromHtml(product.mproduct_title, Html.FROM_HTML_MODE_LEGACY)

        // Format and set flavor/options text (capitalize words, join with "/")
        holder.textFlavour.text = product.options
            .take(2) // Take first 2 options
            .mapNotNull { product.option_value[it] } // Get option values
            .joinToString("/") {
                it.split(" ").joinToString(" ") { s -> s.replaceFirstChar(Char::uppercaseChar) }
            }

        // Construct image URL (use fallback if image is null)
        val imageUrl = cdnURL + (product.image ?: product.mproduct_image)

        // Load image with Glide
        GlideApp.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_logo_red_blue) // Show placeholder while loading
            .error(R.drawable.ic_logo_red_blue) // Show fallback if error occurs
            .into(holder.imgProduct)

        // Update different parts of the UI
        updateWishlistUI(holder, product) // Update wishlist icon
        updatePriceUI(holder, product) // Update price and compare price
        updateDealTagUI(holder, product) // Update deal tag animation
        updateOfferUI(holder, product) // Update offers
        holder.finalPrice.text = "£%.2f".format(product.price) // Set formatted price

        // Default visibility for cart controls
        holder.llCartSign.visibility = View.GONE
        holder.btnAdd.visibility = View.VISIBLE

        // Observe quantity from cart and update UI accordingly
        cartViewModel.quantityLiveData(product.mvariant_id)
            .observe(lifecycleOwner) { qty ->
                updateCartUI(holder, product, qty ?: 0) // Update cart UI when quantity changes
            }

        // Handle favorite button click (add to wishlist)
        holder.btnFavorite.setOnClickListener {
            holder.btnFavorite.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY, // Provide haptic feedback
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            listener.onUpdateWishlist(product.mvariant_id.toString(), "Top Seller") // Notify listener
        }

        // Handle favorite selected button click (remove from wishlist)
        holder.btnFavoriteSelected.setOnClickListener {
            holder.btnFavoriteSelected.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            listener.onUpdateWishlist(product.mvariant_id.toString(), "Top Seller") // Notify listener
        }

        // Handle add button click (add product to cart)
        holder.btnAdd.setOnClickListener {
            holder.btnAdd.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            holder.textNoOfItems.text = "1" // Set quantity text to 1
            listener.onUpdateCart(1, product.mvariant_id) // Notify listener
            cartViewModel.addOrUpdateCart(toEntity(product, 1)) // Add to database
        }

        // Handle add more button click (increase product quantity)
        holder.btnAddMore.setOnClickListener {
            val newQty = (holder.textNoOfItems.text.toString().toIntOrNull() ?: 0) + 1 // Increase qty
            cartViewModel.addOrUpdateCart(toEntity(product, newQty)) // Update DB
            listener.onUpdateCart(newQty, product.mvariant_id) // Notify listener to check for deals
            holder.btnAddMore.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }

        // Handle minus button click (decrease product quantity or remove)
        holder.btnMinus.setOnClickListener {
            val currentQty = (holder.textNoOfItems.text.toString().toIntOrNull() ?: 0) // Get current qty
            if (currentQty > 1) {
                val newQty = currentQty - 1
                cartViewModel.addOrUpdateCart(toEntity(product, newQty)) // Reduce qty
                listener.onUpdateCart(newQty, product.mvariant_id) // Notify listener
            } else {
                cartViewModel.deleteCartByVariant(product.mvariant_id) // Remove from DB
                listener.onUpdateCart(0, product.mvariant_id) // Notify listener
            }
            holder.btnMinus.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    // Converts a Product object into a CartItemEntity for DB storage
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
            taxable = product.taxable,
            dealType = product.deal_type,
            dealBuyQuantity = product.deal_buy_quantity,
            dealGetQuantity = product.deal_get_quantity,
            dealQuantity = product.deal_quantity,
            dealPrice = product.deal_price
        )
    }

    // Updates UI for cart visibility and stock status
    private fun updateCartUI(holder: ViewHolder, product: Product, quantity: Int) {
        if (product.quantity < 0) {
            // Show out of stock message
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
            
            // If quantity > 0, show cart controls
            if (quantity > 0) {
                holder.btnAdd.visibility = View.GONE
                holder.llCartSign.visibility = View.VISIBLE
                holder.textNoOfItems.text = quantity.toString()
            } else if (quantity == 0) {
                // If not in cart, show add button
                holder.llCartSign.visibility = View.GONE
                holder.btnAdd.visibility = View.VISIBLE
            }
        }
    }

    // Updates wishlist icons based on product state
    private fun updateWishlistUI(holder: ViewHolder, product: Product) {
        if (product.user_info_wishlist) {
            holder.btnFavoriteSelected.visibility = View.VISIBLE
            holder.btnFavorite.visibility = View.GONE
        } else {
            holder.btnFavoriteSelected.visibility = View.GONE
            holder.btnFavorite.visibility = View.VISIBLE
        }
    }

    // Updates price UI and compare price (strike-through)
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

    // Updates UI for showing deal tags (flash deal/sale)
    private fun updateDealTagUI(holder: ViewHolder, product: Product) {
        if (product.product_deal_tag.isNullOrEmpty()) {
            holder.lottieCheckmark.visibility = View.INVISIBLE
        } else {
            holder.lottieCheckmark.visibility = View.VISIBLE
            val animRes = if (product.product_deal_tag.lowercase() == "flash deal")
                R.raw.flash_deals else R.raw.sale
            holder.lottieCheckmark.setAnimation(animRes)
        }
    }

    // Updates offer-related UI
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
    
    // Calculate deal text based on deal type and parameters
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

    // Returns total number of items in the adapter
    override fun getItemCount(): Int = products.size
}