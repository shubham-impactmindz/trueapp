package com.app.truewebapp.ui.component.main.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.company_address.CompanyAddresses

class AddressListAdapter(
    private val companyAddresses: List<CompanyAddresses>,
    private val listener: AddressClickListener
) : RecyclerView.Adapter<AddressListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val linearAddress: LinearLayout = view.findViewById(R.id.linearAddress)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = companyAddresses[position]
        "${item.user_company_name}, ${item.company_address1}, ${item.company_address2}, ${item.company_city}, ${item.company_country}, ${item.company_postcode}".also { holder.tvAddress.text = it }

        holder.linearAddress.setOnClickListener {
            listener.onEditAddressClicked(item.user_company_address_id.toString(),item.user_company_name.toString(),item.company_address1,item.company_address2,item.company_city,
                item.company_country,item.company_postcode)
        }
    }

    override fun getItemCount(): Int = companyAddresses.size
}

