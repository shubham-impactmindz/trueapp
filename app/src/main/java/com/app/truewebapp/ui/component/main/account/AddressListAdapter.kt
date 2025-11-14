package com.app.truewebapp.ui.component.main.account

// Import statements for required Android and app-specific classes
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.data.dto.company_address.CompanyAddresses

/**
 * RecyclerView Adapter for displaying a list of company addresses.
 *
 * @param companyAddresses List of company addresses to display.
 * @param listener Listener for handling edit address click events.
 */
class AddressListAdapter(
    private val companyAddresses: List<CompanyAddresses>,  // List of company addresses passed to adapter
    private val listener: AddressClickListener             // Listener interface for click actions
) : RecyclerView.Adapter<AddressListAdapter.ViewHolder>() {

    /**
     * ViewHolder class that holds reference to UI elements for each item in the list.
     *
     * @param view The itemView representing a single address item.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Linear layout that wraps the whole address item
        val linearAddress: LinearLayout = view.findViewById(R.id.linearAddress)
        // TextView that shows the formatted address
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
    }

    /**
     * Inflates the layout for each item in the RecyclerView.
     *
     * @param parent The parent ViewGroup where the item will be attached.
     * @param viewType The type of view (not used here as we have a single view type).
     * @return A ViewHolder containing the inflated layout.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout XML for a single address item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds data (company address) to the ViewHolder for display.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the current address item from the list
        val item = companyAddresses[position]

        // Concatenate all address fields into a single string and set it to TextView
        "${item.user_company_name}, ${item.company_address1}, ${item.company_address2}, ${item.company_city}, GB, ${item.company_postcode}"
            .also { holder.tvAddress.text = it }

        // Handle click on the linear layout wrapping the address
        holder.linearAddress.setOnClickListener {
            // Provide haptic feedback when item is clicked
            holder.linearAddress.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,               // Type of haptic feedback
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag to ignore global settings
            )

            // Notify listener with full address details (for edit action)
            listener.onEditAddressClicked(
                item.user_company_address_id.toString(),
                item.user_company_name.toString(),
                item.company_address1,
                item.company_address2,
                item.company_city,
                item.company_country,
                item.company_postcode
            )
        }
    }

    /**
     * Returns the total number of items in the address list.
     *
     * @return The size of the address list.
     */
    override fun getItemCount(): Int = companyAddresses.size
}
