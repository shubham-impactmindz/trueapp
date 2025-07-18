package com.app.truewebapp.ui.component.main.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.coupons.Data

class CouponAdapter(
    private val coupons: List<Data>,
    private val subtotal: Double,
    private var appliedCode: String?,
    private val onApply: (Data) -> Unit
) : RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    inner class CouponViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.couponTitle)
        val desc = view.findViewById<TextView>(R.id.couponDescription)
        val applyBtn = view.findViewById<TextView>(R.id.applyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_coupon, parent, false)
        return CouponViewHolder(view)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        val coupon = coupons[position]
        val formattedDiscount = when (coupon.discount_type?.lowercase()) {
            "fixed" -> "£${coupon.discount_value}"
            "percent" -> "${coupon.discount_value}%"
            else -> coupon.discount_value // fallback
        }

        holder.title.text = "${coupon.code} - $formattedDiscount"

        holder.desc.text = "Valid for orders above - £${coupon.min_cart_value}"

        if (appliedCode == coupon.code) {
            holder.applyBtn.text = "APPLIED"
            holder.applyBtn.isEnabled = false
        } else {
            holder.applyBtn.text = "APPLY"
            holder.applyBtn.isEnabled = subtotal >= coupon.min_cart_value.toDoubleOrNull()!!

            holder.applyBtn.setOnClickListener {
                onApply(coupon)
            }
        }
    }

    override fun getItemCount() = coupons.size
}
