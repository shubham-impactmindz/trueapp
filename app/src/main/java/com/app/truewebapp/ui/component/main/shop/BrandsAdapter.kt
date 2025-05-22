package com.app.truewebapp.ui.component.main.shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.brands.BrandsResponse
import com.app.truewebapp.data.dto.brands.MBrands
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class BrandsAdapter(
    private val images: List<MBrands> = emptyList(),
    private val cdnURL: String = "",
    private val type: String,
    private val brandsResponse: BrandsResponse? = null
) : RecyclerView.Adapter<BrandsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val brandImage: ImageView = view.findViewById(R.id.brandImage)
        val selectedTick: ImageView = view.findViewById(R.id.selectedTick)
        val productBrandLayout: CardView = view.findViewById(R.id.productBrandLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_brand, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val brand = images[position]

        Glide.with(holder.itemView.context)
            .load(cdnURL + brand.mbrand_image)
            .transform(RoundedCorners(25))
            .into(holder.brandImage)

        holder.productBrandLayout.setBackgroundResource(
            if (brand.isSelected) R.drawable.border_imageview_selected
            else R.drawable.border_imageview
        )

        holder.selectedTick.visibility = if (brand.isSelected) View.VISIBLE else View.GONE

        if (type.lowercase() != "all") {
            holder.productBrandLayout.setOnClickListener {
                brand.isSelected = !brand.isSelected
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = images.size
}
