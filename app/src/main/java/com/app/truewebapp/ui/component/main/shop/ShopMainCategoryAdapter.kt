package com.app.truewebapp.ui.component.main.shop

// Import necessary Android and project-specific libraries
import android.text.Html
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
import com.app.truewebapp.data.dto.browse.MainCategories

// RecyclerView Adapter for displaying main categories and expanding/collapsing their subcategories
class ShopMainCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener, // Listener for handling product-related events
    mainCategories: List<MainCategories>, // List of main categories
    private val cdnURL: String // CDN base URL for loading images
) : RecyclerView.Adapter<ShopMainCategoryAdapter.MainCategoryViewHolder>() {

    // Holds the set of expanded main category indices (allows multiple to be expanded)
    private val expandedMainCategoryIndices = mutableSetOf<Int>()

    // Mutable list of main categories (copied from constructor input)
    private var mainCategories: MutableList<MainCategories> = mainCategories.toMutableList()

    // Map to store sub-adapters for each main category index
    private val shopCategoryAdapters = mutableMapOf<Int, ShopCategoryAdapter>()

    // Reference to the parent RecyclerView that this adapter is attached to
    private var parentRecyclerView: RecyclerView? = null

    // ViewHolder class for each main category item
    inner class MainCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // RecyclerView inside each main category for displaying subcategories
        private val categoryRecyclerView: RecyclerView = view.findViewById(R.id.categoryRecycler)

        // Layout container for the main category
        private val linearCategory: LinearLayout = view.findViewById(R.id.linearCategory)

        // Arrow icons for expand/collapse state
        private val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        private val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)

        // TextView for displaying main category name
        private val tvProduct: TextView = view.findViewById(R.id.tvProduct)

        // Binds data to the ViewHolder
        fun bind(
            position: Int, // Position of the item
            category: MainCategories, // Category data object
            isExpanded: Boolean, // Whether this category is currently expanded
            toggleExpand: (Int) -> Unit // Function to toggle expand/collapse
        ) {
            // Convert HTML-encoded category name to plain text and set it to TextView
            val title = Html.fromHtml(category.main_mcat_name, Html.FROM_HTML_MODE_LEGACY).toString()
            tvProduct.text = title.uppercase()

            // Apply different background based on category title
            linearCategory.setBackgroundResource(
                if (title.equals("Deals & Offers", ignoreCase = true))
                    R.drawable.border_solid_secondary
                else
                    R.drawable.border_solid_primary
            )

            // Only create/update adapter if category is expanded (performance optimization)
            if (isExpanded) {
                // Get or create the ShopCategoryAdapter for subcategories
                val adapter = shopCategoryAdapters.getOrPut(position) {
                    ShopCategoryAdapter(productAdapterListener, category.categories, cdnURL)
                }

                // If the RecyclerView adapter is not set, attach it and initialize layout manager
                if (categoryRecyclerView.adapter != adapter) {
                    categoryRecyclerView.adapter = adapter
                    categoryRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                    categoryRecyclerView.setHasFixedSize(true)
                }

                // Update subcategories while preserving expansion state
                adapter.updateCategoriesPreserveExpansion(category.categories)
            } else {
                // Remove adapter when collapsed to free memory and improve performance
                categoryRecyclerView.adapter = null
                shopCategoryAdapters.remove(position)
            }

            // Update expand/collapse UI based on state
            updateExpansionUI(isExpanded)

            // Set click listener for expanding/collapsing category
            linearCategory.setOnClickListener {
                // Provide haptic feedback when clicked
                linearCategory.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ignore global settings for feedback
                )
                toggleExpand(position) // Toggle expansion
                // Removed automatic scroll to prevent view jumping to top
            }
        }

        // Updates UI elements for expansion state
        private fun updateExpansionUI(isExpanded: Boolean) {
            categoryRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            iconArrowDown.visibility = if (isExpanded) View.GONE else View.VISIBLE
            iconArrowUp.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        // Scrolls the selected item into view within NestedScrollView
        private fun scrollToPosition() {
            itemView.post {
                var parentView: View? = itemView
                // Traverse upwards until finding the NestedScrollView
                while (parentView?.parent is View && parentView.parent !is NestedScrollView) {
                    parentView = parentView.parent as? View
                }

                val nestedScrollView = parentView?.parent as? NestedScrollView
                nestedScrollView?.post {
                    nestedScrollView.smoothScrollTo(0, itemView.top)
                }
            }
        }

        // Getter for subcategory adapter of this main category
        fun getCategoryAdapter(): ShopCategoryAdapter? {
            val position = bindingAdapterPosition
            return if (position != RecyclerView.NO_POSITION) shopCategoryAdapters[position] else null
        }

        // Getter for subcategory RecyclerView of this main category
        fun getCategoryRecycler(): RecyclerView = categoryRecyclerView
    }

    // Inflates the layout and creates a ViewHolder for main category
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainCategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return MainCategoryViewHolder(view)
    }
    
    // Called when adapter is attached to RecyclerView
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parentRecyclerView = recyclerView
    }
    
    // Called when adapter is detached from RecyclerView
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        parentRecyclerView = null
    }

    // Binds data to ViewHolder at the given position
    override fun onBindViewHolder(holder: MainCategoryViewHolder, position: Int) {
        val category = mainCategories[position]
        val isExpanded = expandedMainCategoryIndices.contains(position)
        holder.bind(position, category, isExpanded) { clickedPosition ->
            handleCategoryClick(clickedPosition)
        }
    }

    // Handles expand/collapse logic for main categories
    private fun handleCategoryClick(clickedPosition: Int) {
        // Get the RecyclerView reference to preserve scroll position
        val recyclerView = getRecyclerView()
        val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager
        
        // Save current scroll position and the clicked item's view reference
        val scrollPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val clickedView = layoutManager?.findViewByPosition(clickedPosition)
        val clickedViewTop = clickedView?.top ?: 0
        
        // Save the scroll offset of the first visible item
        val firstVisibleView = layoutManager?.findViewByPosition(scrollPosition)
        val scrollOffset = firstVisibleView?.top ?: 0
        
        // Temporarily disable item animations to prevent scroll jumps
        val originalAnimator = recyclerView?.itemAnimator
        recyclerView?.itemAnimator = null
        
        if (expandedMainCategoryIndices.contains(clickedPosition)) {
            // Collapse if already expanded
            expandedMainCategoryIndices.remove(clickedPosition)
            notifyItemChanged(clickedPosition)
        } else {
            // Expand the clicked category (don't collapse others)
            expandedMainCategoryIndices.add(clickedPosition)
            notifyItemChanged(clickedPosition)
        }
        
        // Restore scroll position after layout change to prevent jumping
        recyclerView?.post {
            // Calculate the new position based on the clicked item's position
            // This accounts for the height change when expanding/collapsing
            val newScrollPosition = layoutManager?.findFirstVisibleItemPosition() ?: scrollPosition
            
            // Try to maintain the same visual position
            if (newScrollPosition == scrollPosition) {
                // If we're still at the same position, restore the exact offset
                layoutManager?.scrollToPositionWithOffset(scrollPosition, scrollOffset)
            } else {
                // If position changed, try to maintain relative position to clicked item
                val newClickedView = layoutManager?.findViewByPosition(clickedPosition)
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
    
    // Helper method to get the RecyclerView that this adapter is attached to
    private fun getRecyclerView(): RecyclerView? {
        return parentRecyclerView
    }

    // Returns total number of main categories
    override fun getItemCount(): Int = mainCategories.size

    // Expands a specific main category by index
    fun expandCategory(position: Int) {
        if (!expandedMainCategoryIndices.contains(position)) {
            expandedMainCategoryIndices.add(position)
            notifyItemChanged(position)
        }
    }

    // Collapses a specific category by index
    fun collapseCategory(position: Int? = null) {
        if (position != null) {
            // Collapse specific category
            if (expandedMainCategoryIndices.remove(position)) {
                notifyItemChanged(position)
            }
        } else {
            // Collapse all categories (for backward compatibility)
            val indicesToCollapse = expandedMainCategoryIndices.toList()
            expandedMainCategoryIndices.clear()
            indicesToCollapse.forEach { notifyItemChanged(it) }
        }
    }

    // Returns index of currently expanded category (-1 if none) - for backward compatibility
    fun getExpandedCategoryIndex(): Int = expandedMainCategoryIndices.firstOrNull() ?: -1

    // Updates categories list while preserving expansion state
    fun updateCategoriesPreserveExpansion(newList: List<MainCategories>) {
        // Get currently expanded category IDs
        val currentExpandedCategoryIds = expandedMainCategoryIndices.mapNotNull { index ->
            mainCategories.getOrNull(index)?.main_mcat_id
        }.toSet()

        // Check if the list actually changed
        val listChanged = mainCategories.size != newList.size || 
                         mainCategories.zip(newList).any { (old, new) -> old.main_mcat_id != new.main_mcat_id }

        if (!listChanged) {
            // Only update adapters if list structure is the same
            mainCategories.forEachIndexed { index, mainCategory ->
                shopCategoryAdapters[index]?.updateCategoriesPreserveExpansion(mainCategory.categories)
            }
            return
        }

        // Replace old list with new categories
        mainCategories.clear()
        mainCategories.addAll(newList)

        // Find new indices of the previously expanded categories
        val newExpandedIndices = mutableSetOf<Int>()
        mainCategories.forEachIndexed { index, category ->
            if (currentExpandedCategoryIds.contains(category.main_mcat_id)) {
                newExpandedIndices.add(index)
            }
        }

        // Update expanded indices
        val oldIndices = expandedMainCategoryIndices.toSet()
        expandedMainCategoryIndices.clear()
        expandedMainCategoryIndices.addAll(newExpandedIndices)

        // Notify changes for all affected items (only changed items, not all)
        (oldIndices + newExpandedIndices).forEach { notifyItemChanged(it) }

        // Update adapters for each category (only for expanded ones to improve performance)
        newExpandedIndices.forEach { index ->
            val mainCategory = mainCategories.getOrNull(index) ?: return@forEach
            shopCategoryAdapters.getOrPut(index) {
                ShopCategoryAdapter(productAdapterListener, mainCategory.categories, cdnURL)
            }.updateCategoriesPreserveExpansion(mainCategory.categories)
        }
        
        // Remove adapters for collapsed categories to free memory
        shopCategoryAdapters.keys.removeAll { it !in newExpandedIndices && it !in oldIndices }
    }
}