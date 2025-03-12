package com.app.truewebapp.ui.component.main.shop

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R

class SubCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener,
    private val subCats: List<SubCat>?
) :
    RecyclerView.Adapter<SubCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productsRecycler: RecyclerView = view.findViewById(R.id.productsRecycler)
        val linearSubCategory: LinearLayout = view.findViewById(R.id.linearSubCategory)
        val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)
        val tvProduct: TextView = view.findViewById(R.id.tvProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sub_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = subCats?.get(position)
        Log.d("setupShopUI", "SubCategories Loaded: $option")
        holder.tvProduct.text = option?.title ?: ""
        val productsAdapter = ProductsAdapter(productAdapterListener,option?.products,option?.title)
        holder.productsRecycler.adapter = productsAdapter
        holder.productsRecycler.layoutManager = GridLayoutManager(holder.itemView.context, 2)

        holder.linearSubCategory.setOnClickListener {
            val isVisible = holder.productsRecycler.isVisible
            holder.productsRecycler.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.iconArrowDown.visibility = if (isVisible) View.VISIBLE else View.GONE
            holder.iconArrowUp.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }

    override fun getItemCount(): Int = subCats?.size ?: 0
}