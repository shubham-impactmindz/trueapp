package com.app.truewebapp.ui.component.main.dashboard

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.ui.component.main.cart.CartActivity

class OrdersAdapter(private val options: List<OrderOption>, private val listener: (OrderOption) -> Unit) :
    RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textOrderNo: TextView = view.findViewById(R.id.tvOrderNo)
        val textOrderDate: TextView = view.findViewById(R.id.tvOrderDate)
        val textPayment: TextView = view.findViewById(R.id.tvPaymentStatus)
        val textFullFill: TextView = view.findViewById(R.id.tvFulfillmentStatus)
        val textUnit: TextView = view.findViewById(R.id.tvUnits)
        val textSku: TextView = view.findViewById(R.id.tvSkus)
        val textTotalPaid: TextView = view.findViewById(R.id.tvTotalPaid)
        val textReorderItems: TextView = view.findViewById(R.id.textReorderItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.textOrderNo.text = option.orderNo
        holder.textOrderDate.text = option.orderDate
        holder.textPayment.text = option.paymentStatus
        holder.textFullFill.text = option.fullFillStatus
        holder.textUnit.text = option.units
        holder.textSku.text = option.skus
        holder.textTotalPaid.text = option.totalPaid
        holder.itemView.setOnClickListener { listener(option) }

        holder.textReorderItems.setOnClickListener {
            val intent = Intent(holder.itemView.context, CartActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = options.size
}
