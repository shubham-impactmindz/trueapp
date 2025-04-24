package com.app.truewebapp.ui.component.main.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import de.hdodenhof.circleimageview.CircleImageView

class RoundImageAdapter(private val images: List<Int>) :
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

        holder.imageView.setImageResource(images[position])
//        holder.tvCategory.text = brands[position]
    }

    override fun getItemCount(): Int = images.size
}
