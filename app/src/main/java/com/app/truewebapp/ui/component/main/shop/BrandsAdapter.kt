package com.app.truewebapp.ui.component.main.shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class BrandsAdapter(private val images: List<Int>) : RecyclerView.Adapter<BrandsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val brandImage: ImageView = view.findViewById(R.id.brandImage)
        val selectedTick: ImageView = view.findViewById(R.id.selectedTick)
        val productBrandLayout: CardView = view.findViewById(R.id.productBrandLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_brand, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(images[position])
            .transform(RoundedCorners(25)) // rounded corner
            .into(holder.brandImage)
        holder.productBrandLayout.setOnClickListener {

            holder.productBrandLayout.setBackgroundResource(R.drawable.border_imageview_selected)
            holder.selectedTick.visibility = View.VISIBLE

        }
    }

    override fun getItemCount(): Int = images.size
}