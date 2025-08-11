package com.app.truewebapp.ui.component.main.cart

import android.content.Context
import android.graphics.Paint
import android.text.Html
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDao
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.component.main.shop.ProductAdapterListener
import com.app.truewebapp.utils.GlideApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartAdapter(
    private val context: Context,
    private val listener: ProductAdapterListener,
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    // Removed direct cartDao usage here, as data comes via updateData from Fragment
    // private val cartDao = CartDatabase.getInstance(context).cartDao()
    private val cartItemList = mutableListOf<CartItemEntity>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textNoOfItems: TextView = view.findViewById(R.id.textNoOfItems)
        val imageProduct: ImageView = view.findViewById(R.id.imageProduct)
        val btnMinus: ImageView = view.findViewById(R.id.btnMinus)
        val btnAddMore: ImageView = view.findViewById(R.id.btnAddMore)
        val finalPrice: TextView = view.findViewById(R.id.finalPrice)
        val comparePrice: TextView = view.findViewById(R.id.comparePrice)
        val btnFavorite: ImageView = view.findViewById(R.id.btnFavorite)
        val btnFavoriteSelected: ImageView = view.findViewById(R.id.btnFavoriteSelected)
        val linearDelete: LinearLayout = view.findViewById(R.id.linearDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_product, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = cartItemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = cartItemList[position]
        // Capitalize and join the first two option values
        val optionText = item.options.values
            .take(2).joinToString(" ") { value ->
                value.split(" ")
                    .joinToString("/") { word -> word.replaceFirstChar(Char::uppercaseChar) }
            } // Join the two option strings with a space

// Compose full title with options in brackets, only if options exist
        val fullTitle = if (optionText.isNotEmpty()) {
            "${item.title} ($optionText)"
        } else {
            item.title ?: ""
        }

// Safely set the text using Html.fromHtml
        holder.textTitle.text = Html.fromHtml(fullTitle, Html.FROM_HTML_MODE_LEGACY).toString()

        holder.textNoOfItems.text = item.quantity.toString()

        // Calculate total price for the item (price per unit * quantity)
        holder.finalPrice.text = "£%.2f".format(item.price)

        // Compare price logic
        if (item.comparePrice != 0.0) {
            holder.comparePrice.visibility = View.VISIBLE
            holder.comparePrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            holder.comparePrice.setTextColor(context.getColor(R.color.black))
            holder.finalPrice.setTextColor(context.getColor(R.color.colorSecondary))
            holder.comparePrice.text = "£%.2f".format(item.comparePrice) // Display original compare price
        } else {
            holder.comparePrice.visibility = View.GONE
            holder.finalPrice.setTextColor(context.getColor(R.color.black))
        }

        // Image loading logic
        val imageUrl = if (!item.image.isNullOrEmpty()) {
            item.cdnURL + item.image
        } else if (!item.fallbackImage.isNullOrEmpty()) {
            item.cdnURL + item.fallbackImage
        } else {
            // Log if both image and fallbackImage are null/empty, or use a default app-wide placeholder
            Log.w("CartAdapter", "Image and fallbackImage are null or empty for variantId: ${item.variantId}")
            null // Or a constant for a truly default placeholder if cdnURL is not needed
        }

        GlideApp.with(context)
            .load(imageUrl) // Glide handles null URLs gracefully, but explicit null check is good
            .placeholder(R.drawable.ic_logo_red_blue)
            .error(R.drawable.ic_logo_red_blue)
            .into(holder.imageProduct)

        // Wishlist UI update
        if (item.isWishlisted) {
            holder.btnFavoriteSelected.visibility = View.VISIBLE
            holder.btnFavorite.visibility = View.GONE
        } else {
            holder.btnFavoriteSelected.visibility = View.GONE
            holder.btnFavorite.visibility = View.VISIBLE
        }

        // Click listeners for wishlist
        holder.btnFavorite.setOnClickListener {
            holder.btnFavorite.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            listener.onUpdateWishlist(item.variantId.toString())
        }
        holder.btnFavoriteSelected.setOnClickListener {
            holder.btnFavoriteSelected.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            listener.onUpdateWishlist(item.variantId.toString())
        }

        // Click listeners for quantity change
        holder.btnAddMore.setOnClickListener {
            holder.btnAddMore.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            // Here, the adapter needs to interact with the database directly.
            // So, cartDao needs to be initialized.
            val cartDao = CartDatabase.getInstance(context).cartDao()
            val newQty = item.quantity + 1
            updateCartQuantity(cartDao, item, newQty) // Pass cartDao
        }

        holder.btnMinus.setOnClickListener {
            holder.btnMinus.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            // Here, the adapter needs to interact with the database directly.
            val cartDao = CartDatabase.getInstance(context).cartDao()
            val newQty = item.quantity - 1
            if (newQty > 0) {
                updateCartQuantity(cartDao, item, newQty) // Pass cartDao
            } else {
                deleteCartItem(cartDao, item.variantId) // Pass cartDao
            }
        }

        // Click listener for delete button
        holder.linearDelete.setOnClickListener {
            holder.linearDelete.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            // Here, the adapter needs to interact with the database directly.
            val cartDao = CartDatabase.getInstance(context).cartDao()
            deleteCartItem(cartDao, item.variantId) // Pass cartDao
        }
    }

    // Modified updateCartQuantity to accept CartDao
    private fun updateCartQuantity(cartDao: CartDao, item: CartItemEntity, quantity: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedItem = item.copy(quantity = quantity)
            cartDao.insertOrUpdateItem(updatedItem)
            withContext(Dispatchers.Main) {
                // This local list update is for immediate responsiveness,
                // but the primary refresh comes from the Flow in the Fragment.
                val index = cartItemList.indexOfFirst { it.variantId == item.variantId }
                if (index != -1) {
                    cartItemList[index] = updatedItem
                    notifyItemChanged(index)
                    listener.onUpdateCart(quantity, item.variantId)
                }
            }
        }
    }

    // Modified deleteCartItem to accept CartDao
    private fun deleteCartItem(cartDao: CartDao, variantId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            cartDao.deleteItemByVariantId(variantId)
            withContext(Dispatchers.Main) {
                // This local list update is for immediate responsiveness,
                // but the primary refresh comes from the Flow in the Fragment.
                val index = cartItemList.indexOfFirst { it.variantId == variantId }
                if (index != -1) {
                    cartItemList.removeAt(index)
                    notifyItemRemoved(index)
                    listener.onUpdateCart(0, variantId)
                }
            }
        }
    }

    /**
     * Updates the adapter's data set and notifies changes.
     * This method should be called whenever the cart data changes from the database.
     */
    fun updateData(newCartItems: List<CartItemEntity>) {
        // Using DiffUtil might be more efficient for large lists,
        // but for a cart, notifyDataSetChanged is often sufficient.
        cartItemList.clear()
        cartItemList.addAll(newCartItems)
        notifyDataSetChanged()
        Log.d("CartAdapter", "Data updated. Item count: ${cartItemList.size}")
    }
}