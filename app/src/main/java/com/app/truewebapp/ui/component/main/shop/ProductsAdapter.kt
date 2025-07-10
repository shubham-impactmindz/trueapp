package com.app.truewebapp.ui.component.main.shop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.browse.Product
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.utils.GlideApp
import kotlinx.coroutines.*

class ProductsAdapter(
    private val listener: ProductAdapterListener,
    private var products: List<Product>,
    private val title: String?,
    private val cdnURL: String,
    private val context: Context
) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    private val cartDao = CartDatabase.getInstance(context).cartDao()

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_products, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.textBrand.text = product.brand_name
        holder.textTitle.text = Html.fromHtml(product.mproduct_title, Html.FROM_HTML_MODE_LEGACY).toString()

        val values = product.options.take(2).mapNotNull { product.option_value[it] }
            .map { it.split(" ").joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) } }
        holder.textFlavour.text = values.joinToString("/")

        val imageUrl = if (!product.image.isNullOrEmpty()) cdnURL + product.image else cdnURL + product.mproduct_image
        GlideApp.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_logo_red_blue)
            .error(R.drawable.ic_logo_red_blue)
            .into(holder.imgProduct)

        updateWishlistUI(holder, product)
        updatePriceUI(holder, product)
        updateDealTagUI(holder, product)
        holder.finalPrice.text = "£ %.2f".format(product.price)

        CoroutineScope(Dispatchers.Main).launch {

            val quantity = withContext(Dispatchers.IO) {
                cartDao.getQuantityByVariantId(product.mvariant_id) ?: 0
            }
            updateCartUI(holder, product, quantity)
        }

        holder.btnFavorite.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {

                val quantity = withContext(Dispatchers.IO) {
                    cartDao.getQuantityByVariantId(product.mvariant_id) ?: 0
                }
                if (quantity>0) {
                    updateFavouriteAsync(product, quantity, true)
                }
            }

            listener.onUpdateWishlist(product.mvariant_id.toString())
        }

        holder.btnFavoriteSelected.setOnClickListener {

            CoroutineScope(Dispatchers.Main).launch {

                val quantity = withContext(Dispatchers.IO) {
                    cartDao.getQuantityByVariantId(product.mvariant_id) ?: 0
                }
                if (quantity>0) {
                    updateFavouriteAsync(product, quantity, false)
                }
            }
            listener.onUpdateWishlist(product.mvariant_id.toString())
        }
    }

    private fun updateCartUI(holder: ViewHolder, product: Product, initialCount: Int) {
        var quantity = initialCount

        if (product.quantity == 0) {
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
        } else {
            updateOfferUI(holder, product)
            holder.finalPrice.setTextColor(context.getColor(android.R.color.black))
            holder.btnAdd.isEnabled = true
            holder.btnAddMore.isEnabled = true
            holder.btnMinus.isEnabled = true

            if (quantity > 0) {
                holder.llCartSign.visibility = View.VISIBLE
                holder.btnAdd.visibility = View.GONE
                holder.textNoOfItems.text = quantity.toString()
            } else {
                holder.llCartSign.visibility = View.GONE
                holder.btnAdd.visibility = View.VISIBLE
            }

            holder.btnAdd.setOnClickListener {
                quantity = 1
                updateCartAsync(product, quantity)
                holder.textNoOfItems.text = quantity.toString()
                holder.btnAdd.visibility = View.GONE
                holder.llCartSign.visibility = View.VISIBLE
            }

            holder.btnAddMore.setOnClickListener {
                quantity++
                updateCartAsync(product, quantity)
                holder.textNoOfItems.text = quantity.toString()
            }

            holder.btnMinus.setOnClickListener {
                quantity--
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

    private fun updateCartAsync(product: Product, quantity: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cartItem = CartItemEntity(
                    variantId = product.mvariant_id,
                    title = product.mproduct_title ?: "",
                    options = listToMap(product.options),
                    image = product.image,
                    fallbackImage = product.mproduct_image,
                    price = product.price,
                    comparePrice = product.compare_price ?: 0.0,
                    isWishlisted = product.user_info_wishlist,
                    cdnURL = cdnURL,
                    quantity = quantity
                )
                cartDao.insertOrUpdateItem(cartItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateFavouriteAsync(product: Product, quantity: Int, b: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cartItem = CartItemEntity(
                    variantId = product.mvariant_id,
                    title = product.mproduct_title ?: "",
                    options = listToMap(product.options),
                    image = product.image,
                    fallbackImage = product.mproduct_image,
                    price = product.price,
                    comparePrice = product.compare_price ?: 0.0,
                    isWishlisted = b,
                    cdnURL = cdnURL,
                    quantity = quantity
                )
                cartDao.insertOrUpdateItem(cartItem)

                withContext(Dispatchers.Main) {
                    listener.onUpdateCart(quantity, product.mvariant_id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun deleteCartItemAsync(product: Product) {
        CoroutineScope(Dispatchers.IO).launch {
            cartDao.deleteItemByVariantId(product.mvariant_id)
            withContext(Dispatchers.Main) {
                listener.onUpdateCart(0, product.mvariant_id)
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
        if (product.compare_price == 0.0) {
            holder.comparePrice.visibility = View.GONE
        } else {
            holder.comparePrice.visibility = View.VISIBLE
            holder.comparePrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            holder.comparePrice.text = "£ %.2f".format(product.compare_price)
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
            holder.lottieCheckmark.setAnimation(animationRes)
        }
    }

    private fun listToMap(options: List<String>?): Map<String, String> {
        return options?.mapIndexed { index, value -> "option_${index + 1}" to value }?.toMap() ?: emptyMap()
    }

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    fun getProductIndex(productId: String): Int {
        return products.indexOfFirst { it.mproduct_id.toString() == productId }
    }

    fun notifyProductChanged(productId: String) {
        val position = getProductIndex(productId)
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = products.size
}
