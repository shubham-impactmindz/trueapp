package com.app.truewebapp.ui.component.main.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.order.Items
import com.bumptech.glide.Glide

class ItemListAdapter(private val cdnURL: String?) : RecyclerView.Adapter<ItemListAdapter.ViewHolder>() {

    private var items: List<Items> = emptyList()

    fun setItems(data: List<Items>) {
        items = data
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productTitle: TextView = view.findViewById(R.id.tvProduct)
        val quantity: TextView = view.findViewById(R.id.tvQuantity)
        val price: TextView = view.findViewById(R.id.tvPrice)
        val iconProduct: ImageView = view.findViewById(R.id.iconProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        Glide.with(holder.itemView.context)
            .load(cdnURL + item.product.mproduct_image)
            .thumbnail(0.1f)
            .dontAnimate()
            .into(holder.iconProduct)
        val values = item.variant.options
            .take(2)
            .mapNotNull { item.variant.option_value[it] }
            .map { it.split(" ").joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) } }

        val variantText = if (values.isNotEmpty()) " (${values.joinToString("/")})" else ""

        holder.productTitle.text = item.product.mproduct_title + variantText
        holder.quantity.text = item.quantity.toString()
        holder.price.text = "£ ${item.unit_price}"

    }

    override fun getItemCount(): Int = items.size
}
