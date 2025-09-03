package com.app.truewebapp.ui.component.main.shop

// Required Android imports
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.brands.BrandsResponse
import com.app.truewebapp.data.dto.brands.MBrands
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

// RecyclerView Adapter to display brand images in a grid/list
class BrandsAdapter(
    private val images: List<MBrands> = emptyList(), // List of brand objects to display (default empty)
    private val cdnURL: String = "", // Base URL to load images from CDN
    private val type: String, // Type of brand selection (e.g., "all", "single-select")
    private val brandsResponse: BrandsResponse? = null // Optional full response object for context
) : RecyclerView.Adapter<BrandsAdapter.ViewHolder>() {

    // ViewHolder class holds references to each item view’s components
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Brand image to display logo
        val brandImage: ImageView = view.findViewById(R.id.brandImage)

        // Tick mark image (shown when brand is selected)
        val selectedTick: ImageView = view.findViewById(R.id.selectedTick)

        // Parent CardView layout of each brand item
        val productBrandLayout: CardView = view.findViewById(R.id.productBrandLayout)
    }

    // Called when RecyclerView needs a new ViewHolder to represent an item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the XML layout for a brand item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_brand, parent, false)
        // Return a new ViewHolder with inflated view
        return ViewHolder(view)
    }

    // Called to bind data (brand info) to a ViewHolder at the given position
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the brand object for the current position
        val brand = images[position]

        // Load brand image into ImageView using Glide with rounded corners
        Glide.with(holder.itemView.context)
            .load(cdnURL + brand.mbrand_image) // Combine CDN URL with brand image path
            .transform(RoundedCorners(25)) // Apply rounded corner transformation
            .into(holder.brandImage) // Set result into brandImage

        // Set border style based on selection state of the brand
        holder.productBrandLayout.setBackgroundResource(
            if (brand.isSelected) R.drawable.border_imageview_selected
            else R.drawable.border_imageview
        )

        // Show/hide tick mark based on selection state
        holder.selectedTick.visibility = if (brand.isSelected) View.VISIBLE else View.GONE

        // If type is not "all", enable click-to-select functionality
        if (type.lowercase() != "all") {
            // Set click listener on the CardView
            holder.productBrandLayout.setOnClickListener {
                // Perform haptic feedback (vibration) when clicked
                holder.productBrandLayout.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ignores user global haptic settings
                )
                // Toggle the brand's selection state
                brand.isSelected = !brand.isSelected
                // Notify adapter to refresh this item (so UI updates instantly)
                notifyItemChanged(position)
            }
        }
    }

    // Returns total number of items in the dataset
    override fun getItemCount(): Int = images.size
}