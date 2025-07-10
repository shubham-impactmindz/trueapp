package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.truewebapp.databinding.ActivityPaymentBinding

class PaymentActivity : AppCompatActivity() {

    lateinit var binding: ActivityPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Handle system bars insets (status + nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }


        val banks = listOf("Select your Bank", "Bank A", "Bank B", "Bank C")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, banks)
        binding.bankSpinner.adapter = adapter

        binding.payByBank.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.bankPaymentLayout.visibility = View.VISIBLE
                binding.cardPaymentLayout.visibility = View.GONE
            }
        }

        binding.backLayout.setOnClickListener {
            finish()
        }

        binding.payByCard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.bankPaymentLayout.visibility = View.GONE
                binding.cardPaymentLayout.visibility = View.VISIBLE
            }
        }

        binding.authorizePaymentButton.setOnClickListener {
            val intent = Intent(this, OrderSuccessActivity::class.java)
            startActivity(intent)
        }
        binding.completePaymentButton.setOnClickListener {
            val intent = Intent(this, OrderSuccessActivity::class.java)
            startActivity(intent)
        }

        binding.bankSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                binding.authorizePaymentButton.visibility = if (position > 0) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}
