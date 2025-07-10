package com.app.truewebapp.ui.component.main.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.dashboard_banners.RoundSliders
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class RoundImageAdapter(
    private val listener: RoundBannerListener,private val images: List<RoundSliders>,private val  cdnURL: String) :
    RecyclerView.Adapter<RoundImageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: CircleImageView = view.findViewById(R.id.ivRoundImage)
//        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_round_image, parent, false)

        // Adjust item width dynamically based on screen size
        val displayMetrics = parent.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val itemWidth = (screenWidth / 5.2).toInt() // Dividing by 4.5 for padding effect

        view.layoutParams = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(cdnURL + images[position].home_round_banner_image)
            .thumbnail(0.1f)
            .dontAnimate()
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            listener.onUpdateRoundBanner(images[position].main_mcat_id.toString(), images[position].mcat_id.toString(), images[position].msubcat_id.toString(), images[position].mproduct_id.toString())
        }
    }

    override fun getItemCount(): Int = images.size
}
