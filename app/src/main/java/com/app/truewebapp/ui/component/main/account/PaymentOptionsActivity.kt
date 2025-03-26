package com.app.truewebapp.ui.component.main.account

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.ActivityPaymentOptionsBinding

class PaymentOptionsActivity : AppCompatActivity() {

    lateinit var binding: ActivityPaymentOptionsBinding
    private lateinit var paymentsAdapter: PaymentsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.savedPaymentsRecyclerView.layoutManager = LinearLayoutManager(this)
        paymentsAdapter = PaymentsAdapter()
        binding.savedPaymentsRecyclerView.adapter = paymentsAdapter
        binding.backLayout.setOnClickListener {
            finish()
        }
        binding.cardLayout.setOnClickListener {
            val intent = Intent(this, AddCardActivity::class.java)
            startActivity(intent)
        }
    }
}