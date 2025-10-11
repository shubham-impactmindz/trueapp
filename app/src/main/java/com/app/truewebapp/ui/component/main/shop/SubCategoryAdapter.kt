package com.app.truewebapp.ui.component.main.shop

// Import necessary Android and third-party libraries
import android.os.CountDownTimer
import android.text.Html
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.browse.Subcategory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

/**
 * Adapter class for handling subcategories and their products inside a RecyclerView.
 * Each subcategory can expand/collapse to show products.
 */
class SubCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener, // Listener for product actions
    private val cdnURL: String // Base CDN URL for loading images
) : RecyclerView.Adapter<SubCategoryAdapter.SubCategoryViewHolder>() {

    // Mutable list of subcategories to display
    private val subCats = mutableListOf<Subcategory>()

    // Map to keep track of expanded/collapsed state for subcategories
    private val expandedMap = mutableMapOf<String, Boolean>()

    /**
     * ViewHolder class representing each subcategory item in the RecyclerView.
     * Holds references to views and handles data binding.
     */
    class SubCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // RecyclerView for products inside this subcategory
        val productsRecycler: RecyclerView = view.findViewById(R.id.productsRecycler)

        // Layout container for subcategory header
        val linearSubCategory: RelativeLayout = view.findViewById(R.id.linearSubCategory)

        // Arrow icons for expand/collapse
        val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)

        // ImageView for subcategory brand image
        val iconBrand: ImageView = view.findViewById(R.id.iconBrand)

        // TextView for subcategory name
        val tvProduct: TextView = view.findViewById(R.id.tvProduct)

        // Countdown timer views
        val tvCountdownTimer: TextView = view.findViewById(R.id.tvCountdownTimer)
        val tvCountdownTitle: TextView = view.findViewById(R.id.tvCountdownTitle)

        // Tag and hot label views
        val tvHot: TextView = view.findViewById(R.id.tvHot)
        val tvTag: TextView = view.findViewById(R.id.tvTag)

        // Container for countdown UI
        val countdownContainer: LinearLayout = view.findViewById(R.id.countdownContainer)

        // CountDownTimer reference (cancelled when not needed)
        var countdownTimer: CountDownTimer? = null

        /**
         * Helper method to get the ProductsAdapter associated with this subcategory.
         */
        fun getProductAdapter(): ProductsAdapter? {
            return productsRecycler.adapter as? ProductsAdapter
        }

        /**
         * Helper method to return the RecyclerView for products.
         */
        fun getProductRecycler(): RecyclerView = productsRecycler

        /**
         * Bind subcategory data to UI components inside this ViewHolder.
         *
         * @param subCategory the subcategory data to bind
         * @param isExpanded whether this subcategory is expanded
         * @param cdnURL base URL for images
         * @param productAdapterListener listener for product-related actions
         */
        fun bind(
            subCategory: Subcategory,
            isExpanded: Boolean,
            cdnURL: String,
            productAdapterListener: ProductAdapterListener
        ) {
            // Remove this line from here - it's too early
            // productsRecycler.post {
            //     productsRecycler.scrollToPosition(0)
            // }

            // Set subcategory name using HTML parsing for styled text
            tvProduct.text = Html.fromHtml(subCategory.msubcat_name ?: "", Html.FROM_HTML_MODE_LEGACY).toString()

            // Handle tags for hot/label indicators
            if (subCategory.msubcat_tag.isNullOrEmpty()) {
                tvHot.visibility = View.GONE
            } else {
                tvHot.visibility = View.VISIBLE
                if (subCategory.msubcat_tag.contains(",")) {
                    // Split into two tags if comma-separated
                    tvTag.visibility = View.VISIBLE
                    tvHot.text = subCategory.msubcat_tag.split(",")[0].trim().uppercase()
                    tvTag.text = subCategory.msubcat_tag.split(",")[1].trim().uppercase()
                } else {
                    tvTag.visibility = View.GONE
                    tvHot.text = subCategory.msubcat_tag.uppercase()
                }
            }

            // Load subcategory image using Glide with rounded corners
            Glide.with(itemView.context)
                .load(cdnURL + subCategory.msubcat_image)
                .transform(RoundedCorners(25))
                .into(iconBrand)

            // Initialize products RecyclerView with ProductsAdapter and GridLayoutManager
            productsRecycler.adapter = ProductsAdapter(productAdapterListener, subCategory.products, subCategory.msubcat_name, cdnURL, itemView.context, productsRecycler)
            productsRecycler.layoutManager = GridLayoutManager(itemView.context, 2)

            // Handle expanded/collapsed UI
            if (isExpanded) {
                // Show products and expanded arrow
                productsRecycler.visibility = View.VISIBLE
                iconArrowDown.visibility = View.GONE
                iconArrowUp.visibility = View.VISIBLE
                countdownTimer?.cancel()

                // Handle countdown timer for offers
                if (subCategory.start_time.isNullOrEmpty()) {
                    countdownContainer.visibility = View.GONE
                } else {
                    countdownContainer.visibility = View.VISIBLE
                    tvCountdownTitle.text = subCategory.offer_name?.uppercase()

                    try {
                        // Parse end time and calculate remaining duration
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val endDate = sdf.parse(subCategory.end_time ?: "")
                        val currentLocalTime = java.util.Calendar.getInstance().time
                        val diffMillis = endDate.time - currentLocalTime.time

                        if (diffMillis > 0) {
                            // Start countdown if valid
                            countdownTimer = object : CountDownTimer(diffMillis, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    // Convert time difference into days, hours, and minutes
                                    val seconds = millisUntilFinished / 1000
                                    val days = seconds / (24 * 3600)
                                    val hours = (seconds % (24 * 3600)) / 3600
                                    val minutes = (seconds % 3600) / 60

                                    // Display formatted countdown
                                    tvCountdownTimer.text = String.format("%02d : %02d : %02d", days, hours, minutes)
                                }

                                override fun onFinish() {
                                    // Expire countdown when finished
                                    tvCountdownTimer.text = "Expired"
                                    countdownContainer.visibility = View.GONE
                                }
                            }.start()
                        } else {
                            // Expired immediately
                            tvCountdownTimer.text = "Expired"
                            countdownContainer.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        // Hide countdown on error
                        countdownContainer.visibility = View.GONE
                        e.printStackTrace()
                    }
                }
            } else {
                // Collapse state: hide products and countdown
                countdownContainer.visibility = View.GONE
                productsRecycler.visibility = View.GONE
                iconArrowDown.visibility = View.VISIBLE
                iconArrowUp.visibility = View.GONE
                countdownTimer?.cancel()
            }
        }
    }

    /**
     * Inflate layout and create ViewHolder for subcategory item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubCategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sub_category, parent, false)
        return SubCategoryViewHolder(view)
    }

    /**
     * Bind data to ViewHolder at given position.
     * Handles expand/collapse toggle logic.
     */
    override fun onBindViewHolder(holder: SubCategoryViewHolder, position: Int) {
        val subCategory = subCats[position]
        val isExpanded = expandedMap[subCategory.msubcat_id.toString()] ?: false

        // Bind subcategory data
        holder.bind(subCategory, isExpanded, cdnURL, productAdapterListener)

        // Handle click for expand/collapse
        holder.linearSubCategory.setOnClickListener {
            holder.linearSubCategory.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            val subcatId = subCategory.msubcat_id.toString()
            val wasExpanded = expandedMap[subcatId] ?: false

            // ❌ remove this
            // expandedMap.clear()

            // ✅ just toggle this one
            expandedMap[subcatId] = !wasExpanded

            notifyDataSetChanged()
        }
    }

    /**
     * Return number of subcategories in the list.
     */
    override fun getItemCount(): Int = subCats.size

    /**
     * Programmatically expand a subcategory at given index.
     */
    fun expandSubCategory(index: Int) {
        val subcatId = subCats.getOrNull(index)?.msubcat_id?.toString() ?: return
        expandedMap.clear()
        expandedMap[subcatId] = true
        notifyDataSetChanged()
    }

    /**
     * Get index of a subcategory by its ID.
     */
    fun getSubCategoryIndex(subcatid: String): Int {
        return subCats.indexOfFirst { it.msubcat_id.toString() == subcatid }
    }

    /**
     * Get a subcategory at a given position if valid.
     */
    fun getSubCategoryAt(position: Int): Subcategory? {
        return if (position in subCats.indices) subCats[position] else null
    }

    /**
     * Scroll to a specific product inside a subcategory.
     * Expands the subcategory and scrolls both outer and inner RecyclerViews.
     */
    fun scrollToProduct(outerRecycler: RecyclerView, productId: String) {
        // Find subcategory containing product
        val subIndex = subCats.indexOfFirst { subcat ->
            subcat.products.any { it.mproduct_id.toString() == productId }
        }

        if (subIndex == -1) return

        // Expand subcategory containing product
        expandSubCategory(subIndex)

        // Scroll outer RecyclerView to subcategory
        outerRecycler.post {
            outerRecycler.scrollToPosition(subIndex)
            outerRecycler.postDelayed({
                // Scroll inner RecyclerView to specific product
                val viewHolder = outerRecycler.findViewHolderForAdapterPosition(subIndex) as? SubCategoryViewHolder
                val productIndex = viewHolder?.getProductAdapter()?.getProductIndex(productId)
                if (productIndex != null && productIndex >= 0) {
                    viewHolder.getProductRecycler().post {
                        viewHolder.getProductRecycler().scrollToPosition(productIndex)
                    }
                }
            }, 300)
        }
    }

    /**
     * Update the list of subcategories while preserving the currently expanded state.
     * Optionally triggers a callback when data changes.
     */
    fun updateSubCategoriesPreserveExpansion(newList: List<Subcategory>, onChanged: (() -> Unit)? = null) {
        // Save currently expanded subcategory ID
        val currentExpandedSubCategoryId = expandedMap.filter { it.value }.keys.firstOrNull()

        // Replace subcategory list with new data
        subCats.clear()
        subCats.addAll(newList)

        // Restore expanded state if possible
        expandedMap.clear()
        if (currentExpandedSubCategoryId != null) {
            val indexOfExpanded = subCats.indexOfFirst { it.msubcat_id.toString() == currentExpandedSubCategoryId }
            if (indexOfExpanded != -1) {
                expandedMap[currentExpandedSubCategoryId] = true
            }
        }

        // Notify adapter of dataset changes
        notifyDataSetChanged()

        // Trigger callback if provided
        onChanged?.invoke()
    }
}