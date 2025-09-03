// Define the package for this class
package com.app.truewebapp.ui.component.main.dashboard

// Import necessary Android and third-party libraries
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.order.Items
import com.bumptech.glide.Glide

/**
 * ItemListAdapter is a RecyclerView adapter that displays a list of order items.
 *
 * @param cdnURL Optional base URL for loading product images
 */
class ItemListAdapter(private val cdnURL: String?) : RecyclerView.Adapter<ItemListAdapter.ViewHolder>() {

    // Holds the list of items to display. Initialized as empty.
    private var items: List<Items> = emptyList()

    /**
     * Updates the adapter with a new list of items and refreshes the RecyclerView.
     *
     * @param data New list of Items
     */
    fun setItems(data: List<Items>) {
        items = data                    // Replace current list with new data
        notifyDataSetChanged()          // Notify RecyclerView to refresh the UI
    }

    /**
     * ViewHolder class holds references to the views for each item in the list.
     *
     * @param view The inflated layout for a single list item
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Initialize UI components using findViewById
        val productTitle: TextView = view.findViewById(R.id.tvProduct)        // Product title text
        val quantity: TextView = view.findViewById(R.id.badgeQuantity)        // Quantity badge
        val price: TextView = view.findViewById(R.id.tvPrice)                 // Unit price
        val tvTotalAmount: TextView = view.findViewById(R.id.tvTotalAmount)   // Total amount
        val iconProduct: ImageView = view.findViewById(R.id.iconProduct)      // Product image
    }

    /**
     * Creates a new ViewHolder instance by inflating the item layout.
     *
     * @param parent Parent ViewGroup
     * @param viewType View type (unused here since all items share same layout)
     * @return A new ViewHolder with inflated layout
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the XML layout for an item row
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_item, parent, false)
        return ViewHolder(view) // Return the created ViewHolder
    }

    /**
     * Binds data from an item to the corresponding ViewHolder.
     *
     * @param holder ViewHolder for the item
     * @param position Position of the item in the list
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the current item from the list
        val item = items[position]

        // Load product image using Glide
        Glide.with(holder.itemView.context)
            .load(cdnURL + item.product.mproduct_image) // Concatenate CDN URL and product image path
            .thumbnail(0.1f)                            // Load a low-resolution thumbnail first
            .dontAnimate()                              // Disable animations for performance
            .into(holder.iconProduct)                   // Display the image in ImageView

        // Extract up to 2 variant options for the product
        val values = item.variant.options
            .take(2) // Limit to 2 options only
            .mapNotNull { item.variant.option_value[it] } // Get option values from map if available
            .map { optionValue ->
                // Capitalize the first character of each word
                optionValue.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar(Char::uppercaseChar)
                }
            }

        // Build a variant text string if values are present (e.g., " (Red/XL)")
        val variantText = if (values.isNotEmpty()) " (${values.joinToString("/")})" else ""

        // Set product title + variant text
        holder.productTitle.text = item.product.mproduct_title + variantText

        // Display product quantity
        holder.quantity.text = item.quantity.toString()

        // Format and display unit price (e.g., £12.50)
        holder.price.text = "£${"%.2f".format((item.unit_price))}"

        // Format and display total amount (quantity * unit price)
        holder.tvTotalAmount.text = "Total: £${"%.2f".format((item.quantity * item.unit_price))}"
    }

    /**
     * Returns the total number of items in the list.
     *
     * @return Size of the items list
     */
    override fun getItemCount(): Int = items.size
}