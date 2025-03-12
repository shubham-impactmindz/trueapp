package com.app.truewebapp.ui.component.main.shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R

class BrandsAdapter() : RecyclerView.Adapter<BrandsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val brandImage: ImageView = view.findViewById(R.id.brandImage)
        val productBrandLayout: LinearLayout = view.findViewById(R.id.productBrandLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_brand, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.productBrandLayout.setOnClickListener {

            holder.productBrandLayout.setBackgroundResource(R.drawable.border_secondary_rect)

        }
    }

    override fun getItemCount(): Int = 15
}