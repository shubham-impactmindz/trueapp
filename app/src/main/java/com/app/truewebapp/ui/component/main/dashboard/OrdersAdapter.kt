package com.app.truewebapp.ui.component.main.dashboard

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.order.Orders
import com.app.truewebapp.ui.component.main.cart.CartActivity
import java.text.SimpleDateFormat
import java.util.Locale

class OrdersAdapter(
    private val orders: MutableList<Orders>,
    private val onItemClick: (Orders) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    fun addOrders(newOrders: List<Orders>) {
        val start = orders.size
        orders.addAll(newOrders)
        notifyItemRangeInserted(start, newOrders.size)
    }

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
        val option = orders[position]
        holder.textOrderNo.text = option.order_id.toString()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val parsedDate = inputFormat.parse(option.order_date)
        val formattedDate = parsedDate?.let { outputFormat.format(it) } ?: option.order_date

        holder.textOrderDate.text = formattedDate
        holder.textPayment.text = option.payment_status
        holder.textFullFill.text = option.fulfillment_status
        holder.textUnit.text = option.units.toString()
        holder.textSku.text = option.skus.toString()
        holder.textTotalPaid.text = "£ ${option.summary.payment_total}"
        holder.itemView.setOnClickListener { onItemClick(option) }

        holder.textReorderItems.setOnClickListener {
            val intent = Intent(holder.itemView.context, CartActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = orders.size
}
