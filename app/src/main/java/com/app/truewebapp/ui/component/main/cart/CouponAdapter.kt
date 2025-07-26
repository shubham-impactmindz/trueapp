package com.app.truewebapp.ui.component.main.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.coupons.Data

class CouponAdapter(
    private val allCoupons: List<Data>,
    private val subtotal: Double,
    private val appliedCode: String?,
    private val onApply: (Data) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_COUPON = 1
    }

    // 🛠️ Updated eligibility logic with safe check for can_be_applied
    // This now correctly filters based on both can_be_applied and min_cart_value
    private val eligibleCoupons = allCoupons.filter {
        it.can_be_applied == true &&
                it.min_cart_value.toDoubleOrNull()?.let { min -> subtotal >= min } == true
    }

    private val ineligibleCoupons = allCoupons.filterNot { eligibleCoupons.contains(it) }

    private val displayList = buildList {
        if (eligibleCoupons.isNotEmpty()) {
            add("Eligible Coupons")
            addAll(eligibleCoupons)
        }
        if (ineligibleCoupons.isNotEmpty()) {
            add("Not Eligible")
            addAll(ineligibleCoupons)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (displayList[position] is String) TYPE_HEADER else TYPE_COUPON
    }

    override fun getItemCount(): Int = displayList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_coupon_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_coupon, parent, false)
            CouponViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.headerTitle.text = displayList[position] as String
        } else if (holder is CouponViewHolder) {
            val coupon = displayList[position] as Data
            val context = holder.itemView.context
            val isApplied = appliedCode == coupon.code

            // ✅ Fixed eligibility logic here too to include can_be_applied
            val isEligible = coupon.can_be_applied == true &&
                    coupon.min_cart_value.toDoubleOrNull()?.let { subtotal >= it } == true

            val discountText = when (coupon.discount_type.lowercase()) {
                "percent" -> "${coupon.discount_value}%"
                "fixed" -> "£${coupon.discount_value}"
                else -> coupon.discount_value
            }

            holder.title.text = "${coupon.code} - $discountText"
            holder.desc.text = "Valid above £${coupon.min_cart_value}"

            // Button state logic
            holder.applyBtn.apply {
                when {
                    isApplied -> {
                        text = "APPLIED"
                        isEnabled = false
                    }
                    !isEligible -> { // This now correctly accounts for `can_be_applied`
                        text = "NOT ELIGIBLE"
                        isEnabled = false
                    }
                    else -> {
                        text = "APPLY"
                        isEnabled = true
                        setOnClickListener { onApply(coupon) }
                    }
                }
            }
        }
    }

    inner class CouponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.couponTitle)
        val desc: TextView = itemView.findViewById(R.id.couponDescription)
        val applyBtn: TextView = itemView.findViewById(R.id.applyButton)
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerTitle: TextView = itemView.findViewById(R.id.sectionHeader)
    }
}