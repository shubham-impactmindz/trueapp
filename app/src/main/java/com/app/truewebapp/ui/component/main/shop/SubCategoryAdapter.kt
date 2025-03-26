package com.app.truewebapp.ui.component.main.shop

import android.os.CountDownTimer
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import java.util.concurrent.TimeUnit

class SubCategoryAdapter(
    private val productAdapterListener: ProductAdapterListener,
    private val subCats: List<SubCat>?
) : RecyclerView.Adapter<SubCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productsRecycler: RecyclerView = view.findViewById(R.id.productsRecycler)
        val linearSubCategory: RelativeLayout = view.findViewById(R.id.linearSubCategory)
        val iconArrowDown: ImageView = view.findViewById(R.id.iconArrowDown)
        val iconArrowUp: ImageView = view.findViewById(R.id.iconArrowUp)
        val tvProduct: TextView = view.findViewById(R.id.tvProduct)
        val tvCountdownTimer: TextView = view.findViewById(R.id.tvCountdownTimer)
        val countdownContainer: LinearLayout = view.findViewById(R.id.countdownContainer)

        var countdownTimer: CountDownTimer? = null
    }

    private val totalTimeMillis = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
    private var startTimeMillis = System.currentTimeMillis() // Store the initial time

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sub_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = subCats?.get(position)
        Log.d("setupShopUI", "SubCategories Loaded: $option")
        var title = option?.title ?: ""

        // Decode HTML entities (specifically &amp;)
        title =
            Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString()
        holder.tvProduct.text = title
        val productsAdapter = ProductsAdapter(productAdapterListener, option?.products, option?.title)
        holder.productsRecycler.adapter = productsAdapter
        holder.productsRecycler.layoutManager = GridLayoutManager(holder.itemView.context, 2)

        holder.linearSubCategory.setOnClickListener {
            val isVisible = holder.productsRecycler.isVisible
            holder.productsRecycler.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.iconArrowDown.visibility = if (isVisible) View.VISIBLE else View.GONE
            holder.iconArrowUp.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.countdownContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
        }

        startCountdown(holder)
    }

    private fun startCountdown(holder: ViewHolder) {
        holder.countdownTimer?.cancel()

        val elapsedTime = System.currentTimeMillis() - startTimeMillis
        val remainingTime = totalTimeMillis - elapsedTime

        holder.countdownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateCountdownText(holder.tvCountdownTimer, millisUntilFinished)
            }

            override fun onFinish() {
                startTimeMillis = System.currentTimeMillis() // Reset start time
                startCountdown(holder) // Restart countdown
            }
        }.start()
    }

    private fun updateCountdownText(tvCountdownTimer: TextView, millisUntilFinished: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60

        tvCountdownTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun getItemCount(): Int = subCats?.size ?: 0
}
