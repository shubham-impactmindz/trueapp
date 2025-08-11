package com.app.truewebapp.ui.component.main.shop

import android.os.CountDownTimer
import android.text.Html
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.browse.Subcategory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class SubCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener,
    private val cdnURL: String
) : RecyclerView.Adapter<SubCategoryAdapter.SubCategoryViewHolder>() {

    private val subCats = mutableListOf<Subcategory>()
    private val expandedMap = mutableMapOf<String, Boolean>()

    class SubCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productsRecycler: RecyclerView = view.findViewById(R.id.productsRecycler)
        val linearSubCategory: RelativeLayout = view.findViewById(R.id.linearSubCategory)
        val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)
        val iconBrand: ImageView = view.findViewById(R.id.iconBrand)
        val tvProduct: TextView = view.findViewById(R.id.tvProduct)
        val tvCountdownTimer: TextView = view.findViewById(R.id.tvCountdownTimer)
        val tvCountdownTitle: TextView = view.findViewById(R.id.tvCountdownTitle)
        val tvHot: TextView = view.findViewById(R.id.tvHot)
        val tvTag: TextView = view.findViewById(R.id.tvTag)
        val countdownContainer: LinearLayout = view.findViewById(R.id.countdownContainer)

        var countdownTimer: CountDownTimer? = null

        fun getProductAdapter(): ProductsAdapter? {
            return productsRecycler.adapter as? ProductsAdapter
        }

        fun getProductRecycler(): RecyclerView = productsRecycler

        fun bind(
            subCategory: Subcategory,
            isExpanded: Boolean,
            cdnURL: String,
            productAdapterListener: ProductAdapterListener
        ) {
            tvProduct.text = Html.fromHtml(subCategory.msubcat_name ?: "", Html.FROM_HTML_MODE_LEGACY).toString()

            if (subCategory.msubcat_tag.isNullOrEmpty()) {
                tvHot.visibility = View.GONE
            } else {
                tvHot.visibility = View.VISIBLE
                if (subCategory.msubcat_tag.contains(",")){

                    tvTag.visibility = View.VISIBLE
                    tvHot.text = subCategory.msubcat_tag.split(",")[0].trim().uppercase()
                    tvTag.text = subCategory.msubcat_tag.split(",")[1].trim().uppercase()
                }else{
                    tvTag.visibility = View.GONE

                    tvHot.text = subCategory.msubcat_tag.uppercase()
                }
            }

            Glide.with(itemView.context)
                .load(cdnURL + subCategory.msubcat_image)
                .transform(RoundedCorners(25))
                .into(iconBrand)

            productsRecycler.adapter = ProductsAdapter(productAdapterListener, subCategory.products, subCategory.msubcat_name, cdnURL,itemView.context)
            productsRecycler.layoutManager = GridLayoutManager(itemView.context, 2)

            if (isExpanded) {
                productsRecycler.visibility = View.VISIBLE
                iconArrowDown.visibility = View.GONE
                iconArrowUp.visibility = View.VISIBLE
                countdownTimer?.cancel()

                if (subCategory.start_time.isNullOrEmpty()) {
                    countdownContainer.visibility = View.GONE
                } else {
                    countdownContainer.visibility = View.VISIBLE
                    tvCountdownTitle.text = subCategory.offer_name?.uppercase()

                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val endDate = sdf.parse(subCategory.end_time ?: "")
                        val currentLocalTime = java.util.Calendar.getInstance().time
                        val diffMillis = endDate.time - currentLocalTime.time

                        if (diffMillis > 0) {
                            countdownTimer = object : CountDownTimer(diffMillis, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    val seconds = millisUntilFinished / 1000
                                    val days = seconds / (24 * 3600)
                                    val hours = (seconds % (24 * 3600)) / 3600
                                    val minutes = (seconds % 3600) / 60

                                    tvCountdownTimer.text = String.format("%02d : %02d : %02d", days, hours, minutes)
                                }

                                override fun onFinish() {
                                    tvCountdownTimer.text = "Expired"
                                    countdownContainer.visibility = View.GONE
                                }
                            }.start()
                        } else {
                            tvCountdownTimer.text = "Expired"
                            countdownContainer.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        countdownContainer.visibility = View.GONE
                        e.printStackTrace()
                    }
                }
            } else {
                countdownContainer.visibility = View.GONE
                productsRecycler.visibility = View.GONE
                iconArrowDown.visibility = View.VISIBLE
                iconArrowUp.visibility = View.GONE
                countdownTimer?.cancel()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubCategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sub_category, parent, false)
        return SubCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubCategoryViewHolder, position: Int) {
        val subCategory = subCats[position]
        val isExpanded = expandedMap[subCategory.msubcat_id.toString()] ?: false

        holder.bind(subCategory, isExpanded, cdnURL, productAdapterListener)

        holder.linearSubCategory.setOnClickListener {
            holder.linearSubCategory.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val subcatId = subCategory.msubcat_id.toString()
            val wasExpanded = expandedMap[subcatId] ?: false
            expandedMap.clear()
            if (!wasExpanded) expandedMap[subcatId] = true
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = subCats.size

    fun expandSubCategory(index: Int) {
        val subcatId = subCats.getOrNull(index)?.msubcat_id?.toString() ?: return
        expandedMap.clear()
        expandedMap[subcatId] = true
        notifyDataSetChanged()
    }

    fun getSubCategoryIndex(subcatid: String): Int {
        return subCats.indexOfFirst { it.msubcat_id.toString() == subcatid }
    }

    fun getSubCategoryAt(position: Int): Subcategory? {
        return if (position in subCats.indices) subCats[position] else null
    }

    fun scrollToProduct(outerRecycler: RecyclerView, productId: String) {
        val subIndex = subCats.indexOfFirst { subcat ->
            subcat.products.any { it.mproduct_id.toString() == productId }
        }

        if (subIndex == -1) return

        expandSubCategory(subIndex)

        outerRecycler.post {
            outerRecycler.scrollToPosition(subIndex)
            outerRecycler.postDelayed({
                val viewHolder = outerRecycler.findViewHolderForAdapterPosition(subIndex) as? SubCategoryViewHolder
                val productIndex = viewHolder?.getProductAdapter()?.getProductIndex(productId)
                if (productIndex != null && productIndex >= 0) {
                    viewHolder.getProductRecycler().post {
                        viewHolder.getProductRecycler().scrollToPosition(productIndex)
                    }
                }
            }, 300)
        }
    }

    /**
     * Update subcategory list and notify if changed.
     * Optional: Pass a callback to react when data actually changed.
     */
    fun updateSubCategoriesPreserveExpansion(newList: List<Subcategory>, onChanged: (() -> Unit)? = null) {
        val currentExpandedSubCategoryId = expandedMap.filter { it.value }.keys.firstOrNull()

        subCats.clear()
        subCats.addAll(newList)

        // Preserve expanded state for subcategories
        expandedMap.clear()
        if (currentExpandedSubCategoryId != null) {
            val indexOfExpanded = subCats.indexOfFirst { it.msubcat_id.toString() == currentExpandedSubCategoryId }
            if (indexOfExpanded != -1) {
                expandedMap[currentExpandedSubCategoryId] = true
            }
        }

        // Update sub-adapters if necessary
        notifyDataSetChanged()

        onChanged?.invoke()
    }


}
