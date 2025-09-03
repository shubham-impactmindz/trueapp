package com.app.truewebapp.ui.component.main.cart

// Importing necessary Android and project-specific classes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.coupons.Data

// RecyclerView Adapter for displaying Coupons with eligibility checks
class CouponAdapter(
    private val allCoupons: List<Data>,             // List of all coupons available
    private val subtotal: Double,                   // Current subtotal of cart (used for eligibility check)
    private val appliedCode: String?,               // Coupon code that has already been applied
    private val onApply: (Data) -> Unit             // Lambda function triggered when user applies a coupon
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0           // Constant to identify header view type
        private const val TYPE_COUPON = 1           // Constant to identify coupon item view type
    }

    // Filtering only eligible coupons based on can_be_applied flag and minimum cart value
    private val eligibleCoupons = allCoupons.filter {
        it.can_be_applied == true &&                               // Must be explicitly allowed to apply
                it.min_cart_value.toDoubleOrNull()                 // Convert min_cart_value string to Double safely
                    ?.let { min -> subtotal >= min } == true       // Eligible if subtotal is greater or equal
    }

    // Remaining coupons that are not eligible
    private val ineligibleCoupons = allCoupons.filterNot { eligibleCoupons.contains(it) }

    // Build a display list mixing headers ("Eligible"/"Not Eligible") and coupons
    private val displayList = buildList {
        if (eligibleCoupons.isNotEmpty()) {
            add("Eligible Coupons")        // Header text
            addAll(eligibleCoupons)        // Add eligible coupons list
        }
        if (ineligibleCoupons.isNotEmpty()) {
            add("Not Eligible")            // Header text
            addAll(ineligibleCoupons)      // Add ineligible coupons list
        }
    }

    // Return view type based on whether the item is a String (Header) or Coupon (Data)
    override fun getItemViewType(position: Int): Int {
        return if (displayList[position] is String) TYPE_HEADER else TYPE_COUPON
    }

    // Return total count of items (headers + coupons combined)
    override fun getItemCount(): Int = displayList.size

    // Inflate the appropriate view depending on viewType (Header or Coupon)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            // Inflate header layout for section title
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_coupon_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            // Inflate coupon layout for coupon item
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_coupon, parent, false)
            CouponViewHolder(view)
        }
    }

    // Bind data to ViewHolder depending on its type
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            // If header, simply set the section title text
            holder.headerTitle.text = displayList[position] as String
        } else if (holder is CouponViewHolder) {
            // Cast display item to Coupon data
            val coupon = displayList[position] as Data
            val context = holder.itemView.context

            // Check if this coupon is already applied
            val isApplied = appliedCode == coupon.code

            // Check coupon eligibility (must be applicable + subtotal must meet min cart value)
            val isEligible = coupon.can_be_applied == true &&
                    coupon.min_cart_value.toDoubleOrNull()?.let { subtotal >= it } == true

            // Create discount text depending on coupon type (fixed / percent)
            val discountText = when (coupon.discount_type.lowercase()) {
                "percent" -> "${coupon.discount_value}%"
                "fixed" -> "£${coupon.discount_value}"
                else -> coupon.discount_value
            }

            // Bind coupon title as "CODE - Discount Value"
            holder.title.text = "${coupon.code} - $discountText"

            // Show coupon description as "Valid above minimum value"
            holder.desc.text = "Valid above £${coupon.min_cart_value}"

            // Configure Apply button behavior based on coupon state
            holder.applyBtn.apply {
                when {
                    isApplied -> {                        // Already applied coupon
                        text = "APPLIED"
                        isEnabled = false
                    }
                    !isEligible -> {                      // Coupon exists but not eligible
                        text = "NOT ELIGIBLE"
                        isEnabled = false
                    }
                    else -> {                             // Eligible coupon → enable Apply button
                        text = "APPLY"
                        isEnabled = true
                        setOnClickListener { onApply(coupon) } // Trigger callback when clicked
                    }
                }
            }
        }
    }

    // ViewHolder for Coupon item layout
    inner class CouponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.couponTitle)          // Coupon title text
        val desc: TextView = itemView.findViewById(R.id.couponDescription)     // Coupon description text
        val applyBtn: TextView = itemView.findViewById(R.id.applyButton)       // Apply button
    }

    // ViewHolder for Header layout
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerTitle: TextView = itemView.findViewById(R.id.sectionHeader)  // Header title text
    }
}