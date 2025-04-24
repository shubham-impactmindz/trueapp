package com.app.truewebapp.ui.component.main.shop

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.app.truewebapp.R
import com.app.truewebapp.utils.GlideApp

class ProductsAdapter(
    private val listener: ProductAdapterListener,
    private val products: List<Product>?,
    private val title: String?
) :
    RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llCartSign: LinearLayout = view.findViewById(R.id.llCartSign)
        val btnAdd: LinearLayout = view.findViewById(R.id.btnAdd)
        val btnMinus: ImageView = view.findViewById(R.id.btnMinus)
        val btnAddMore: ImageView = view.findViewById(R.id.btnAddMore)
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val textNoOfItems: TextView = view.findViewById(R.id.textNoOfItems)
        val finalPrice: TextView = view.findViewById(R.id.finalPrice)
        val comparePrice: TextView = view.findViewById(R.id.comparePrice)
        val textBrand: TextView = view.findViewById(R.id.textBrand)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val lottieCheckmark: LottieAnimationView = view.findViewById(R.id.lottieCheckmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_products, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = products?.get(position)
        Log.d("setupShopUI", "Products Loaded: $option")
        holder.textBrand.text = title
        holder.textTitle.text = option?.title
        GlideApp.with(holder.itemView.context)
            .load(option?.img)
            .placeholder(R.drawable.ic_lays)
            .error(R.drawable.ic_lays)
            .into(holder.imgProduct)
        var count = 0
        val productName = "Product $position" // Get product name dynamically if available

        holder.comparePrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
        holder.btnAdd.setOnClickListener {
            count = 1
            holder.textNoOfItems.text = count.toString()
            holder.btnAdd.visibility = View.GONE
            holder.llCartSign.visibility = View.VISIBLE
            listener.onAddToCartClicked(count, productName)
        }

        holder.btnAddMore.setOnClickListener {
            count++
            holder.textNoOfItems.text = count.toString()
            listener.onAddToCartClicked(count, productName)
        }

        holder.btnMinus.setOnClickListener {
            if (count > 1) {
                count--
                holder.textNoOfItems.text = count.toString()
                listener.onAddToCartClicked(count, productName)
            } else {
                count = 0
                holder.llCartSign.visibility = View.GONE
                holder.btnAdd.visibility = View.VISIBLE
                listener.onAddToCartClicked(count, productName)
            }
        }
        if (position == 1 || position == 3) {
            holder.lottieCheckmark.setAnimation(R.raw.sale) // R.raw.sale -> sale.json
        } else {
            holder.lottieCheckmark.setAnimation(R.raw.flash_deals) // R.raw.black_friday -> black_friday.json
        }
    }

    override fun getItemCount(): Int = products?.size ?: 0
}