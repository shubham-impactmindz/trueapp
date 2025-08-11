package com.app.truewebapp.ui.component.main.shop

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

class ShopCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener,
    categories: List<Category>,
    private val cdnURL: String
) : RecyclerView.Adapter<ShopCategoryAdapter.CategoryViewHolder>() {

    private val expandedMap = mutableMapOf<String, Boolean>()
    private var categoryList: MutableList<Category> = categories.toMutableList()
    private val subCategoryAdapters = mutableMapOf<Int, SubCategoryAdapter>()

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val subCategoryRecyclerView: RecyclerView = view.findViewById(R.id.subCategoryRecycler)
        val linearCategory: LinearLayout = view.findViewById(R.id.linearCategory)
        private val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        private val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)
        private val tvProduct: TextView = view.findViewById(R.id.tvProduct)

        fun bind(
            position: Int,
            category: Category,
            isExpanded: Boolean,
            toggleExpand: (String) -> Unit
        ) {
            val title = Html.fromHtml(category.mcat_name, Html.FROM_HTML_MODE_LEGACY).toString()
            tvProduct.text = title

            linearCategory.setBackgroundResource(
                if (title.equals("Deals & Offers", ignoreCase = true))
                    R.drawable.border_solid_secondary
                else
                    R.drawable.border_solid_light_red
            )

            val adapter = subCategoryAdapters.getOrPut(position) {
                SubCategoryAdapter(productAdapterListener, cdnURL).apply {
                    updateSubCategoriesPreserveExpansion(category.subcategories)
                }
            }

            if (subCategoryRecyclerView.adapter != adapter) {
                subCategoryRecyclerView.adapter = adapter
                subCategoryRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                subCategoryRecyclerView.setHasFixedSize(true)
            } else {
                adapter.updateSubCategoriesPreserveExpansion(category.subcategories)
            }

            updateExpansionUI(isExpanded)

            linearCategory.setOnClickListener {
               linearCategory.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
                )
                toggleExpand(category.mcat_id.toString())
                if (!isExpanded) {
                    scrollToPosition()
                }
            }
        }

        private fun updateExpansionUI(isExpanded: Boolean) {
            Log.e("ShopCategoryAdapter", "updateExpansionUI: isExpanded=$isExpanded")
            subCategoryRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            iconArrowUp.visibility = if (isExpanded) View.VISIBLE else View.GONE
            iconArrowDown.visibility = if (isExpanded) View.GONE else View.VISIBLE
            if (isExpanded && subCategoryRecyclerView.layoutManager == null) {
                subCategoryRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            }
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

        fun getSubCategoryAdapter(): SubCategoryAdapter? = subCategoryAdapters[adapterPosition]
        fun getSubCategoryRecycler(): RecyclerView = subCategoryRecyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        val isExpanded = expandedMap[category.mcat_id.toString()] ?: false
        holder.bind(position, category, isExpanded) { catId ->
            handleCategoryClick(catId)
        }
    }

    private fun handleCategoryClick(catId: String) {
        val wasExpanded = expandedMap[catId] ?: false
        expandedMap.clear()
        expandedMap[catId] = !wasExpanded
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = categoryList.size

    // ✅ FIXED: This now works when called from fragment
    fun expandCategory(categoryId: String) {
        expandedMap.clear()
        expandedMap[categoryId] = true
        notifyDataSetChanged()
    }


    fun getCategoryIdAt(index: Int): String? {
        return if (index in 0 until categoryList.size) categoryList[index].mcat_id.toString() else null
    }


    fun collapseCategory(catId: String) {
        expandedMap[catId] = false
        notifyDataSetChanged()
    }

    fun getCategoryIndex(catId: String): Int {
        return categoryList.indexOfFirst { it.mcat_id.toString() == catId }
    }

    fun updateCategoriesPreserveExpansion(newList: List<Category>) {
        val currentExpandedCategoryId = expandedMap.filter { it.value }.keys.firstOrNull()

        categoryList.clear()
        categoryList.addAll(newList)

        expandedMap.clear()
        if (currentExpandedCategoryId != null) {
            expandedMap[currentExpandedCategoryId] = true
        }

        categoryList.forEachIndexed { index, category ->
            subCategoryAdapters.getOrPut(index) {
                SubCategoryAdapter(productAdapterListener, cdnURL)
            }.updateSubCategoriesPreserveExpansion(category.subcategories)
        }

        notifyDataSetChanged()
    }
}
