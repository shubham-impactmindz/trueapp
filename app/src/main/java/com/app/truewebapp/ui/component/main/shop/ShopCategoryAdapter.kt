package com.app.truewebapp.ui.component.main.shop

// Importing required Android and app-specific classes
import android.text.Html
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.browse.Category

/**
 * Adapter class for displaying a list of shop categories in a RecyclerView.
 * Handles category expansion/collapse and maintains state.
 */
class ShopCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener, // Listener to handle product-related actions
    categories: List<Category>, // Initial list of categories
    private val cdnURL: String // CDN URL used for media loading
) : RecyclerView.Adapter<ShopCategoryAdapter.CategoryViewHolder>() {

    // Keeps track of expanded/collapsed state for each category using category ID
    private val expandedMap = mutableMapOf<String, Boolean>()

    // Holds the current list of categories, mutable for updates
    private var categoryList: MutableList<Category> = categories.toMutableList()

    // Holds sub-category adapters mapped by their category index
    private val subCategoryAdapters = mutableMapOf<Int, SubCategoryAdapter>()

    /**
     * ViewHolder class for holding category UI elements.
     */
    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // RecyclerView for displaying subcategories
        private val subCategoryRecyclerView: RecyclerView = view.findViewById(R.id.subCategoryRecycler)
        // Parent layout for each category item
        val linearCategory: LinearLayout = view.findViewById(R.id.linearCategory)
        // Arrow icons for expand/collapse state
        private val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        private val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)
        // TextView for category title
        private val tvProduct: TextView = view.findViewById(R.id.tvProduct)

        /**
         * Binds a category item to the ViewHolder and handles expansion state.
         */
        fun bind(
            position: Int, // Position of category in the list
            category: Category, // Current category object
            isExpanded: Boolean, // Whether the category is expanded
            toggleExpand: (String) -> Unit // Function to toggle expansion
        ) {
            // Extract and set category name with HTML formatting
            val title = Html.fromHtml(category.mcat_name, Html.FROM_HTML_MODE_LEGACY).toString()
            tvProduct.text = title.uppercase()

            // Apply background style based on category name
            linearCategory.setBackgroundResource(
                if (title.equals("Deals & Offers", ignoreCase = true))
                    R.drawable.border_solid_secondary
                else
                    R.drawable.border_solid_light_red
            )

            // Only create/update adapter if category is expanded (performance optimization)
            if (isExpanded) {
                // Get or create SubCategoryAdapter for this category position
                val adapter = subCategoryAdapters.getOrPut(position) {
                    SubCategoryAdapter(productAdapterListener, cdnURL).apply {
                        updateSubCategoriesPreserveExpansion(category.subcategories)
                    }
                }

                // Set RecyclerView adapter and layout manager if not already set
                if (subCategoryRecyclerView.adapter != adapter) {
                    subCategoryRecyclerView.adapter = adapter
                    subCategoryRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                    subCategoryRecyclerView.setHasFixedSize(true)
                } else {
                    // Update subcategories if adapter already exists
                    adapter.updateSubCategoriesPreserveExpansion(category.subcategories)
                }
            } else {
                // Remove adapter when collapsed to free memory and improve performance
                subCategoryRecyclerView.adapter = null
                subCategoryAdapters.remove(position)
            }

            // Update UI based on expanded/collapsed state
            updateExpansionUI(isExpanded)

            // Handle category click for toggling expand/collapse
            linearCategory.setOnClickListener {
                linearCategory.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag for ignoring system haptic settings
                )
                toggleExpand(category.mcat_id.toString())
                // Removed automatic scroll to prevent view jumping to top
            }
        }

        /**
         * Updates UI for expand/collapse state.
         */
        private fun updateExpansionUI(isExpanded: Boolean) {
            Log.e("ShopCategoryAdapter", "updateExpansionUI: isExpanded=$isExpanded")
            subCategoryRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            iconArrowUp.visibility = if (isExpanded) View.VISIBLE else View.GONE
            iconArrowDown.visibility = if (isExpanded) View.GONE else View.VISIBLE
            if (isExpanded && subCategoryRecyclerView.layoutManager == null) {
                subCategoryRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            }
        }

        /**
         * Scrolls the parent NestedScrollView to the current item position when expanded.
         */
        private fun scrollToPosition() {
            itemView.post {
                var parentView: View? = itemView
                // Traverse up until NestedScrollView is found
                while (parentView?.parent is View && parentView.parent !is NestedScrollView) {
                    parentView = parentView.parent as? View
                }

                // Smooth scroll the NestedScrollView to current category
                val nestedScrollView = parentView?.parent as? NestedScrollView
                nestedScrollView?.post {
                    nestedScrollView.smoothScrollTo(0, itemView.top)
                }
            }
        }

        // Provides access to the SubCategoryAdapter at this ViewHolder's position
        fun getSubCategoryAdapter(): SubCategoryAdapter? = subCategoryAdapters[adapterPosition]

        // Provides access to the subcategory RecyclerView
        fun getSubCategoryRecycler(): RecyclerView = subCategoryRecyclerView
    }

    /**
     * Called when adapter is attached to RecyclerView
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parentRecyclerView = recyclerView
    }
    
    /**
     * Called when adapter is detached from RecyclerView
     */
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        parentRecyclerView = null
    }
    
    /**
     * Inflates the layout for category items and creates ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_category, parent, false)
        return CategoryViewHolder(view)
    }

    /**
     * Binds category data to the ViewHolder.
     */
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        val isExpanded = expandedMap[category.mcat_id.toString()] ?: false
        holder.bind(position, category, isExpanded) { catId ->
            handleCategoryClick(catId)
        }
    }

    /**
     * Handles expand/collapse logic for a category when clicked.
     */
    private fun handleCategoryClick(catId: String) {
        val wasExpanded = expandedMap[catId] ?: false
        // Toggle the clicked category without clearing others (allow multiple expanded)
        expandedMap[catId] = !wasExpanded
        
        // Find the position of the clicked category and notify only that item
        val position = categoryList.indexOfFirst { it.mcat_id.toString() == catId }
        if (position != -1) {
            // Preserve scroll position before notifying change
            val recyclerView = getRecyclerView()
            val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager
            
            // Save current scroll position and the clicked item's view reference
            val scrollPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
            val clickedView = layoutManager?.findViewByPosition(position)
            val clickedViewTop = clickedView?.top ?: 0
            
            // Save the scroll offset of the first visible item
            val firstVisibleView = layoutManager?.findViewByPosition(scrollPosition)
            val scrollOffset = firstVisibleView?.top ?: 0
            
            // Temporarily disable item animations to prevent scroll jumps
            val originalAnimator = recyclerView?.itemAnimator
            recyclerView?.itemAnimator = null
            
            notifyItemChanged(position)
            
            // Restore scroll position after layout change to prevent jumping
            recyclerView?.post {
                // Calculate the new position based on the clicked item's position
                val newScrollPosition = layoutManager?.findFirstVisibleItemPosition() ?: scrollPosition
                
                // Try to maintain the same visual position
                if (newScrollPosition == scrollPosition) {
                    // If we're still at the same position, restore the exact offset
                    layoutManager?.scrollToPositionWithOffset(scrollPosition, scrollOffset)
                } else {
                    // If position changed, try to maintain relative position to clicked item
                    val newClickedView = layoutManager?.findViewByPosition(position)
                    if (newClickedView != null) {
                        // Calculate how much the clicked item moved
                        val newClickedViewTop = newClickedView.top
                        val delta = newClickedViewTop - clickedViewTop
                        
                        // Adjust scroll to compensate for the movement
                        val adjustedOffset = scrollOffset - delta
                        layoutManager?.scrollToPositionWithOffset(scrollPosition, adjustedOffset)
                    } else {
                        // Fallback: just restore the original position
                        layoutManager?.scrollToPositionWithOffset(scrollPosition, scrollOffset)
                    }
                }
                
                // Restore item animator after a short delay
                recyclerView?.postDelayed({
                    recyclerView?.itemAnimator = originalAnimator
                }, 100)
            }
        }
    }
    
    // Reference to parent RecyclerView for scroll preservation
    private var parentRecyclerView: RecyclerView? = null
    
    // Helper method to get the RecyclerView that this adapter is attached to
    private fun getRecyclerView(): RecyclerView? {
        return parentRecyclerView
    }

    /**
     * Returns total number of categories in the list.
     */
    override fun getItemCount(): Int = categoryList.size

    /**
     * Expands the given category by ID.
     */
    fun expandCategory(categoryId: String) {
        // Expand without clearing others (allow multiple expanded)
        if (expandedMap[categoryId] != true) {
            expandedMap[categoryId] = true
            val position = categoryList.indexOfFirst { it.mcat_id.toString() == categoryId }
            if (position != -1) {
                notifyItemChanged(position)
            }
        }
    }

    /**
     * Retrieves category ID at a given index.
     */
    fun getCategoryIdAt(index: Int): String? {
        return if (index in 0 until categoryList.size) categoryList[index].mcat_id.toString() else null
    }

    /**
     * Collapses the given category by ID.
     */
    fun collapseCategory(catId: String) {
        if (expandedMap[catId] == true) {
            expandedMap[catId] = false
            val position = categoryList.indexOfFirst { it.mcat_id.toString() == catId }
            if (position != -1) {
                notifyItemChanged(position)
            }
        }
    }

    /**
     * Returns the index of a category by ID.
     */
    fun getCategoryIndex(catId: String): Int {
        return categoryList.indexOfFirst { it.mcat_id.toString() == catId }
    }

    /**
     * Updates the list of categories while preserving the currently expanded one.
     */
    fun updateCategoriesPreserveExpansion(newList: List<Category>) {
        // Save currently expanded category IDs
        val currentExpandedCategoryIds = expandedMap.filter { it.value }.keys.toSet()

        // Check if the list actually changed
        val listChanged = categoryList.size != newList.size || 
                         categoryList.zip(newList).any { (old, new) -> old.mcat_id != new.mcat_id }

        if (!listChanged) {
            // Only update adapters for expanded categories if list structure is the same
            currentExpandedCategoryIds.forEach { catId ->
                val position = categoryList.indexOfFirst { it.mcat_id.toString() == catId }
                if (position != -1) {
                    subCategoryAdapters[position]?.updateSubCategoriesPreserveExpansion(categoryList[position].subcategories)
                }
            }
            return
        }

        // Replace old list with new categories
        categoryList.clear()
        categoryList.addAll(newList)

        // Restore previously expanded categories
        val newExpandedIds = mutableSetOf<String>()
        categoryList.forEach { category ->
            val catId = category.mcat_id.toString()
            if (currentExpandedCategoryIds.contains(catId)) {
                expandedMap[catId] = true
                newExpandedIds.add(catId)
            } else {
                expandedMap[catId] = false
            }
        }

        // Update subcategory adapters only for expanded categories (performance optimization)
        newExpandedIds.forEach { catId ->
            val position = categoryList.indexOfFirst { it.mcat_id.toString() == catId }
            if (position != -1) {
                subCategoryAdapters.getOrPut(position) {
                    SubCategoryAdapter(productAdapterListener, cdnURL)
                }.updateSubCategoriesPreserveExpansion(categoryList[position].subcategories)
            }
        }
        
        // Remove adapters for collapsed categories to free memory
        subCategoryAdapters.keys.removeAll { index ->
            val catId = categoryList.getOrNull(index)?.mcat_id?.toString()
            catId !in newExpandedIds
        }

        // Notify only changed items, not all items
        categoryList.forEachIndexed { index, category ->
            val catId = category.mcat_id.toString()
            if (expandedMap[catId] != null) {
                notifyItemChanged(index)
            }
        }
    }
}