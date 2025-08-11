package com.app.truewebapp.ui.component.main.shop

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

class ShopMainCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener,
    mainCategories: List<MainCategories>,
    private val cdnURL: String
) : RecyclerView.Adapter<ShopMainCategoryAdapter.MainCategoryViewHolder>() {

    private var expandedMainCategoryIndex: Int = -1
    private var mainCategories: MutableList<MainCategories> = mainCategories.toMutableList()
    private val shopCategoryAdapters = mutableMapOf<Int, ShopCategoryAdapter>()

    inner class MainCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val categoryRecyclerView: RecyclerView = view.findViewById(R.id.categoryRecycler)
        private val linearCategory: LinearLayout = view.findViewById(R.id.linearCategory)
        private val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        private val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)
        private val tvProduct: TextView = view.findViewById(R.id.tvProduct)

        fun bind(
            position: Int,
            category: MainCategories,
            isExpanded: Boolean,
            toggleExpand: (Int) -> Unit
        ) {
            val title = Html.fromHtml(category.main_mcat_name, Html.FROM_HTML_MODE_LEGACY).toString()
            tvProduct.text = title

            linearCategory.setBackgroundResource(
                if (title.equals("Deals & Offers", ignoreCase = true))
                    R.drawable.border_solid_secondary
                else
                    R.drawable.border_solid_primary
            )

            val adapter = shopCategoryAdapters.getOrPut(position) {
                ShopCategoryAdapter(productAdapterListener, category.categories, cdnURL)
            }
            if (categoryRecyclerView.adapter != adapter) {
                categoryRecyclerView.adapter = adapter
                categoryRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                categoryRecyclerView.setHasFixedSize(true)
            }
            adapter.updateCategoriesPreserveExpansion(category.categories)

            updateExpansionUI(isExpanded)

            linearCategory.setOnClickListener {
                linearCategory.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
                )
                val wasExpanded = expandedMainCategoryIndex == position
                toggleExpand(position)
                if (!wasExpanded) scrollToPosition()
            }
        }

        private fun updateExpansionUI(isExpanded: Boolean) {
            categoryRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            iconArrowDown.visibility = if (isExpanded) View.GONE else View.VISIBLE
            iconArrowUp.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        private fun scrollToPosition() {
            itemView.post {
                var parentView: View? = itemView
                while (parentView?.parent is View && parentView.parent !is NestedScrollView) {
                    parentView = parentView.parent as? View
                }

                val nestedScrollView = parentView?.parent as? NestedScrollView
                nestedScrollView?.post {
                    nestedScrollView.smoothScrollTo(0, itemView.top)
                }
            }
        }

        fun getCategoryAdapter(): ShopCategoryAdapter? {
            val position = bindingAdapterPosition
            return if (position != RecyclerView.NO_POSITION) shopCategoryAdapters[position] else null
        }

        fun getCategoryRecycler(): RecyclerView = categoryRecyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainCategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return MainCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainCategoryViewHolder, position: Int) {
        val category = mainCategories[position]
        val isExpanded = expandedMainCategoryIndex == position
        holder.bind(position, category, isExpanded) { clickedPosition ->
            handleCategoryClick(clickedPosition)
        }
    }

    private fun handleCategoryClick(clickedPosition: Int) {
        val previousExpanded = expandedMainCategoryIndex
        if (expandedMainCategoryIndex == clickedPosition) {
            expandedMainCategoryIndex = -1
            notifyItemChanged(clickedPosition)
        } else {
            expandedMainCategoryIndex = clickedPosition
            if (previousExpanded != -1) notifyItemChanged(previousExpanded)
            notifyItemChanged(clickedPosition)
        }
    }

    override fun getItemCount(): Int = mainCategories.size

    fun expandCategory(position: Int) {
        if (expandedMainCategoryIndex != position) {
            val previousIndex = expandedMainCategoryIndex
            expandedMainCategoryIndex = position
            if (previousIndex != -1) notifyItemChanged(previousIndex)
            notifyItemChanged(position)
        }
    }

    fun collapseCategory() {
        if (expandedMainCategoryIndex != -1) {
            val prevIndex = expandedMainCategoryIndex
            expandedMainCategoryIndex = -1
            notifyItemChanged(prevIndex)
        }
    }

    fun getExpandedCategoryIndex(): Int = expandedMainCategoryIndex

    fun updateCategoriesPreserveExpansion(newList: List<MainCategories>) {
        val currentExpandedCategoryId =
            if (expandedMainCategoryIndex != -1)
                mainCategories.getOrNull(expandedMainCategoryIndex)?.main_mcat_id
            else null

        mainCategories.clear()
        mainCategories.addAll(newList)

        val newExpandedIndex = mainCategories.indexOfFirst { it.main_mcat_id == currentExpandedCategoryId }
        if (newExpandedIndex != expandedMainCategoryIndex) {
            val oldExpandedIndex = expandedMainCategoryIndex
            expandedMainCategoryIndex = newExpandedIndex
            if (oldExpandedIndex != -1) notifyItemChanged(oldExpandedIndex)
            if (expandedMainCategoryIndex != -1) notifyItemChanged(expandedMainCategoryIndex)
        }

        mainCategories.forEachIndexed { index, mainCategory ->
            shopCategoryAdapters.getOrPut(index) {
                ShopCategoryAdapter(productAdapterListener, mainCategory.categories, cdnURL)
            }.updateCategoriesPreserveExpansion(mainCategory.categories)
        }

        notifyDataSetChanged()
    }
}