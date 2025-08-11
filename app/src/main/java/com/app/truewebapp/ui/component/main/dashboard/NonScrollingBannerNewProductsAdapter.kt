package com.app.truewebapp.ui.component.main.dashboard

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

class NonScrollingBannerNewProductsAdapter(
    private val listener: NewProductTopSellerAdapterListener,
    private var products: List<ProductBanner>,
    private val cdnURL: String,
    private val context: Context,
    private val cartViewModel: CartViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<NonScrollingBannerNewProductsAdapter.ViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_products, parent, false)
        return ViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val banner = products[position]
        val product = banner.product

        holder.textBrand.text = product.brand_name
        holder.textTitle.text = Html.fromHtml(product.mproduct_title, Html.FROM_HTML_MODE_LEGACY)
        holder.textFlavour.text = product.options
            .take(2)
            .mapNotNull { product.option_value[it] }.joinToString("/") {
                it.split(" ").joinToString(" ") { s -> s.replaceFirstChar(Char::uppercaseChar) }
            }

        val imageUrl = cdnURL + (product.image ?: product.mproduct_image)
        GlideApp.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_logo_red_blue)
            .error(R.drawable.ic_logo_red_blue)
            .into(holder.imgProduct)

        updateWishlistUI(holder, product)
        updatePriceUI(holder, product)
        updateDealTagUI(holder, product)
        updateOfferUI(holder, product)
        holder.finalPrice.text = "£%.2f".format(product.price)

        // Set default visibility
        holder.llCartSign.visibility = View.GONE
        holder.btnAdd.visibility = View.VISIBLE

        // Observe quantity LiveData
        cartViewModel.quantityLiveData(product.mvariant_id)
            .observe(lifecycleOwner) { qty ->
                updateCartUI(holder, product, qty ?: 0)
            }

        holder.btnFavorite.setOnClickListener {
            holder.btnFavorite.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            listener.onUpdateWishlist(product.mvariant_id.toString(), "New Product")
        }
        holder.btnFavoriteSelected.setOnClickListener {
            holder.btnFavoriteSelected.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            listener.onUpdateWishlist(product.mvariant_id.toString(), "New Product")
        }

        holder.btnAdd.setOnClickListener {
            holder.btnAdd.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            holder.textNoOfItems.text = "1"

            listener.onUpdateCart(1, product.mvariant_id)
            cartViewModel.addOrUpdateCart(toEntity(product, 1))

            // Force rebind of this item
//            notifyItemChanged(holder.bindingAdapterPosition)
        }

        holder.btnAddMore.setOnClickListener {
            val newQty = (holder.textNoOfItems.text.toString().toIntOrNull() ?: 0) + 1
            cartViewModel.addOrUpdateCart(toEntity(product, newQty))
            holder.btnAddMore.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
        }
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
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
        }
    }

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

    private fun updateCartUI(holder: ViewHolder, product: Product, quantity: Int) {
        if (product.quantity == 0) {
            holder.tvOffer.text = "Out of stock"
            holder.tvOffer.visibility = View.VISIBLE
            holder.llOffer.visibility = View.VISIBLE
            holder.btnAdd.visibility = View.GONE
            holder.llCartSign.visibility = View.GONE
        } else {
            if (quantity > 0) {
                holder.btnAdd.visibility = View.GONE
                holder.llCartSign.visibility = View.VISIBLE
                holder.textNoOfItems.text = quantity.toString()
            } else if (quantity == 0){
                holder.llCartSign.visibility = View.GONE
                holder.btnAdd.visibility = View.VISIBLE
            }
        }
    }

    private fun updateWishlistUI(holder: ViewHolder, product: Product) {
        if (product.user_info_wishlist) {
            holder.btnFavoriteSelected.visibility = View.VISIBLE
            holder.btnFavorite.visibility = View.GONE
        } else {
            holder.btnFavoriteSelected.visibility = View.GONE
            holder.btnFavorite.visibility = View.VISIBLE
        }
    }

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

    private fun updateDealTagUI(holder: ViewHolder, product: Product) {
        if (product.product_deal_tag.isNullOrEmpty()) {
            holder.lottieCheckmark.visibility = View.INVISIBLE
        } else {
            holder.lottieCheckmark.visibility = View.VISIBLE
            val animRes = if (product.product_deal_tag.lowercase() == "flash deal") R.raw.flash_deals else R.raw.sale
            holder.lottieCheckmark.setAnimation(animRes)
        }
    }

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

    override fun getItemCount(): Int = products.size
}
