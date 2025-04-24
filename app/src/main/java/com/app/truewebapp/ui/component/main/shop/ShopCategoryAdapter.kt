package com.app.truewebapp.ui.component.main.shop

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R

class ShopCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener,
    private val options: CategoryListModel,
    private val images: List<Int>
) : RecyclerView.Adapter<ShopCategoryAdapter.ViewHolder>() {

    // ⬇️ Now we track multiple expanded items
    private val expandedPositions = mutableSetOf<Int>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val subCategoryRecyclerView: RecyclerView = view.findViewById(R.id.subCategoryRecycler)
        val linearCategory: LinearLayout = view.findViewById(R.id.linearCategory)
        val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)
        val tvProduct: TextView = view.findViewById(R.id.tvProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shop_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options.categories?.get(position)
        var title = option?.title ?: ""

        title = Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString()
        holder.tvProduct.text = title

        // Apply background color only to "Deals & Offers"
        if (title == "Deals & Offers") {
            holder.linearCategory.setBackgroundResource(R.drawable.border_solid_secondary)
        } else {
            holder.linearCategory.setBackgroundResource(R.drawable.border_solid_primary) // Set default background
        }

        val subCategoryAdapter = SubCategoryAdapter(productAdapterListener, option?.subCats, images)
        holder.subCategoryRecyclerView.adapter = subCategoryAdapter
        holder.subCategoryRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.subCategoryRecyclerView.setHasFixedSize(true)

        val isExpanded = expandedPositions.contains(position)
        holder.subCategoryRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.iconArrowDown.visibility = if (isExpanded) View.GONE else View.VISIBLE
        holder.iconArrowUp.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.linearCategory.setOnClickListener {
            if (expandedPositions.contains(position)) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)

            if (!isExpanded) {
                holder.itemView.post {
                    holder.itemView.requestLayout()
                    holder.itemView.invalidate()

                    holder.itemView.postDelayed({
                        var parentView: View? = holder.itemView
                        while (parentView?.parent != null && parentView.parent !is androidx.core.widget.NestedScrollView) {
                            parentView = parentView.parent as? View
                        }

                        val nestedScrollView = parentView?.parent as? androidx.core.widget.NestedScrollView
                        nestedScrollView?.smoothScrollTo(0, holder.itemView.top)
                    }, 150)
                }
            }
        }
    }

    override fun getItemCount(): Int = options.categories?.size ?: 0
}