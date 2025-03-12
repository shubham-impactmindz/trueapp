package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.databinding.ActivityCheckOutBinding

class CheckOutActivity : AppCompatActivity() {

    lateinit var binding: ActivityCheckOutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fetch data from API
        val deliveryMethods = fetchDeliveryMethodsFromAPI()
        val dispatchAddresses = fetchDispatchAddressesFromAPI()

        // Create RadioButtons dynamically
        createRadioButtons(binding.deliveryMethodRadioGroup, deliveryMethods)
        createRadioButtons(binding.dispatchToRadioGroup, dispatchAddresses)
        binding.textPayment.setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java)
            startActivity(intent)
        }

        binding.backLayout.setOnClickListener {
            finish()
        }
    }

    private fun fetchDeliveryMethodsFromAPI(): List<String> {
        // Replace with your actual API call
        return listOf(
            "Next Working Day Delivery - Estimated: Tomorrow (£5.00)",
            "Standard Delivery - Estimated: 2-4 Working Days (£3.00)"
        )
    }

    private fun fetchDispatchAddressesFromAPI(): List<String> {
        // Replace with your actual API call
        return listOf(
            "Immyz Ltd, 78 Stockport Road, Ashton-under-Lyne, Greater Manchester, OL7 OLH",
            "Another Address, Some Street, Some City, Some Postcode"
        )
    }

    private fun createRadioButtons(radioGroup: RadioGroup, options: List<String>) {
        for (option in options) {
            val radioButton = RadioButton(this).apply {
                text = option
                textSize = 14f  // Set text size
                typeface = Typeface.DEFAULT // Set font family
                setPadding(8, 8, 8, 8) // Set padding
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(10, 10, 0, 10) // Set margins
                }
            }
            radioGroup.addView(radioButton)
        }
    }
}