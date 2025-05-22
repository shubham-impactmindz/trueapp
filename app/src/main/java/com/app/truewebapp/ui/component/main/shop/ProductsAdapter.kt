package com.app.truewebapp.ui.component.main.shop

import android.annotation.SuppressLint
import android.graphics.Paint
import android.text.Html
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
import com.app.truewebapp.utils.GlideApp

class ProductsAdapter(
    private val listener: ProductAdapterListener,
    private var products: List<Product>,
    private val title: String?,
    private val cdnURL: String
) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

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

        val titleText = Html.fromHtml(product.mproduct_title, Html.FROM_HTML_MODE_LEGACY).toString()
        holder.textTitle.text = titleText
        val values = product.options
            .take(2)
            .mapNotNull { product.option_value[it] }
            .map { value ->
                value.split(" ")
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { it.uppercaseChar() }
                    }
            }

        holder.textFlavour.text = values.joinToString("/")

        GlideApp.with(holder.itemView.context)
            .load(cdnURL + product.mproduct_image)
            .placeholder(R.drawable.ic_logo_red_blue)
            .error(R.drawable.ic_logo_red_blue)
            .into(holder.imgProduct)

        var count = 0
        val productName = "Product $position"

        // --- Set Prices, Wishlist, Offers ---
        updateWishlistUI(holder, product)
        updatePriceUI(holder, product)
        updateDealTagUI(holder, product)

        holder.finalPrice.text = "£ ${product.price}"

        if (product.quantity == 0) {
            // Out of stock UI
//            holder.textOutOfStock.visibility = View.VISIBLE
            holder.tvOffer.text = "Out of stock"
            holder.textBrand.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.textTitle.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.textFlavour.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.tvOffer.visibility = View.VISIBLE
            holder.llOffer.visibility = View.VISIBLE
            holder.finalPrice.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.btnAdd.isEnabled = false
            holder.btnAddMore.isEnabled = false
            holder.btnMinus.isEnabled = false
            holder.llCartSign.visibility = View.GONE
            holder.btnAdd.visibility = View.GONE
        } else {
            // In stock UI
//            holder.textOutOfStock.visibility = View.GONE
            updateOfferUI(holder, product)
            holder.finalPrice.setTextColor(holder.itemView.context.getColor(android.R.color.black))
            holder.btnAdd.isEnabled = true
            holder.btnAddMore.isEnabled = true
            holder.btnMinus.isEnabled = true

            // Cart logic
            holder.btnAdd.setOnClickListener {
                count = 1
                holder.textNoOfItems.text = count.toString()
                holder.btnAdd.visibility = View.GONE
                holder.llCartSign.visibility = View.VISIBLE
            }

            holder.btnAddMore.setOnClickListener {
                count++
                holder.textNoOfItems.text = count.toString()
                listener.onUpdateCart(count, productName)
            }

            holder.btnMinus.setOnClickListener {
                if (count > 1) {
                    count--
                    holder.textNoOfItems.text = count.toString()
                    listener.onUpdateCart(count, productName)
                } else {
                    count = 0
                    holder.llCartSign.visibility = View.GONE
                    holder.btnAdd.visibility = View.VISIBLE
                    listener.onUpdateCart(count, productName)
                }
            }
        }

        holder.btnFavorite.setOnClickListener {
            listener.onUpdateWishlist(product.mvariant_id.toString())
        }

        holder.btnFavoriteSelected.setOnClickListener {
            listener.onUpdateWishlist(product.mvariant_id.toString())
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
            holder.comparePrice.text = "£ ${product.compare_price}"
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
            if (product.product_deal_tag.lowercase() == "flash deal") {
                holder.lottieCheckmark.setAnimation(R.raw.flash_deals)
            } else {
                holder.lottieCheckmark.setAnimation(R.raw.sale)
            }
        }
    }

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    fun getProductIndex(productId: String): Int {
        return products.indexOfFirst { it.mproduct_id.toString() == productId }
    }

    fun notifyProductChanged(productId: String) {
        val position = products.indexOfFirst { it.mproduct_id.toString() == productId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = products.size
}

class ProductDiffCallback(
    private val oldList: List<Product>,
    private val newList: List<Product>
) : androidx.recyclerview.widget.DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].mproduct_id == newList[newItemPosition].mproduct_id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}