package com.app.truewebapp.ui.component.main.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.ui.component.main.dashboard.NotificationOption

class ServicesAdapter(private val options:  List<NotificationOption>) :
    RecyclerView.Adapter<ServicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pointsRecyclerView: RecyclerView = view.findViewById(R.id.pointsRecycler)
        val textSolution: TextView = view.findViewById(R.id.textSolution)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_services, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.textSolution.text = options[position].name
        val optionsPoints = listOf(
            NotificationOption("Compare and get real time business analytics"),
            NotificationOption("Fast & Easy Billing"),
            NotificationOption("Discount & Promotions Management"),
            NotificationOption("Price management from Head office"),
            NotificationOption("Rack Management"),
            NotificationOption("Retail Customer Relationship Management"),
        )

        val pointsAdapter = PointsAdapter(optionsPoints) // Create an adapter for the points
        holder.pointsRecyclerView.adapter = pointsAdapter

        // 3. Set the layout manager for the inner RecyclerView
        holder.pointsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context) // Or GridLayoutManager, etc.

    }

    override fun getItemCount(): Int = options.size
}
