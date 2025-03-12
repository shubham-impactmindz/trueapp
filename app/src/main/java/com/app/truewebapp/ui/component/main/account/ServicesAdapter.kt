package com.app.truewebapp.ui.component.main.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R

class ServicesAdapter() :
    RecyclerView.Adapter<ServicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pointsRecyclerView: RecyclerView = view.findViewById(R.id.pointsRecycler)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_services, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val pointsAdapter = PointsAdapter() // Create an adapter for the points
        holder.pointsRecyclerView.adapter = pointsAdapter

        // 3. Set the layout manager for the inner RecyclerView
        holder.pointsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context) // Or GridLayoutManager, etc.

    }

    override fun getItemCount(): Int = 5
}
