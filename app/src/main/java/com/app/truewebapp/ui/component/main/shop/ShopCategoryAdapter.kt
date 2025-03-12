package com.app.truewebapp.ui.component.main.shop

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R

class ShopCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener,
    private val options: CategoryListModel
) : RecyclerView.Adapter<ShopCategoryAdapter.ViewHolder>() {

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

        // Decode HTML entities (specifically &amp;)
        title =
            Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString()
        holder.tvProduct.text = title

        val subCategoryAdapter = SubCategoryAdapter(productAdapterListener, option?.subCats)
        holder.subCategoryRecyclerView.adapter = subCategoryAdapter
        holder.subCategoryRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)

        holder.linearCategory.setOnClickListener {
            val isVisible = holder.subCategoryRecyclerView.isVisible
            holder.subCategoryRecyclerView.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.iconArrowDown.visibility = if (isVisible) View.VISIBLE else View.GONE
            holder.iconArrowUp.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }

    override fun getItemCount(): Int = options.categories?.size ?: 0
}

