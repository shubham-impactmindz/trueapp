package com.app.truewebapp.ui.component.main.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.service.RegisterInterestRequest
import com.app.truewebapp.ui.component.main.dashboard.NotificationOption
import com.app.truewebapp.ui.viewmodel.RegisterInterestViewModel
import com.google.android.material.textfield.TextInputEditText

class ServicesAdapter(
    private val options: List<NotificationOption>,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner
) : RecyclerView.Adapter<ServicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pointsRecyclerView: RecyclerView = view.findViewById(R.id.pointsRecycler)
        val textSolution: TextView = view.findViewById(R.id.textSolution)
        val tvRegisterInterest: TextView = view.findViewById(R.id.tvRegisterInterest)
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
        holder.tvRegisterInterest.setOnClickListener {
            showRegisterInterestDialog(holder.itemView.context, options[position])
        }

    }

    override fun getItemCount(): Int = options.size
    
    // Show register interest dialog
    private fun showRegisterInterestDialog(context: android.content.Context, serviceOption: NotificationOption) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_register_interest, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.emailInput)
        val mobileInput = dialogView.findViewById<TextInputEditText>(R.id.mobileInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<View>(R.id.btnSubmit)
        val tvSubmitText = dialogView.findViewById<TextView>(R.id.tvSubmitText)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Initialize ViewModel
        val viewModel = ViewModelProvider(viewModelStoreOwner)[RegisterInterestViewModel::class.java]
        
        // Get token from SharedPreferences
        val preferences = context.getSharedPreferences(SHARED_PREF_NAME, android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${preferences.getString("token", "").orEmpty()}"
        
        // Observe ViewModel responses
        viewModel.registerInterestResponse.observe(lifecycleOwner) { response ->
            response?.let {
                if (it.status) {
                    Toast.makeText(context, it.message ?: "Interest registered successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, it.message ?: "Failed to register interest", Toast.LENGTH_SHORT).show()
                    // Hide loading
                    progressBar.visibility = View.GONE
                    tvSubmitText.visibility = View.VISIBLE
                    btnSubmit.isEnabled = true
                }
            }
        }
        
        viewModel.isLoading.observe(lifecycleOwner) { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                tvSubmitText.visibility = View.GONE
                btnSubmit.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                tvSubmitText.visibility = View.VISIBLE
                btnSubmit.isEnabled = true
            }
        }
        
        viewModel.apiError.observe(lifecycleOwner) { error ->
            Toast.makeText(context, error ?: "An error occurred", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            tvSubmitText.visibility = View.VISIBLE
            btnSubmit.isEnabled = true
        }
        
        viewModel.onFailure.observe(lifecycleOwner) { throwable ->
            Toast.makeText(context, "Network error: ${throwable?.message}", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            tvSubmitText.visibility = View.VISIBLE
            btnSubmit.isEnabled = true
        }
        
        // Cancel button click
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Submit button click
        btnSubmit.setOnClickListener {
            val name = nameInput.text?.toString()?.trim() ?: ""
            val email = emailInput.text?.toString()?.trim() ?: ""
            val phone = mobileInput.text?.toString()?.trim() ?: ""
            val note = descriptionInput.text?.toString()?.trim() ?: ""
            
            // Validate inputs
            if (name.isEmpty()) {
                nameInput.error = "Name is required"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                mobileInput.error = "Mobile number is required"
                return@setOnClickListener
            }
            
            // Create request
            val request = RegisterInterestRequest(
                service_solution_id = serviceOption.id,
                name = name,
                phone = phone,
                email = email,
                note = note
            )
            
            // Call API
            viewModel.registerInterest(token, request)
        }
        
        dialog.show()
    }
}
