package com.app.truewebapp.ui.component.main.cart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.databinding.FragmentCartBinding

class CartFragment : Fragment() {

    lateinit var binding: FragmentCartBinding
    private lateinit var cartAdapter: CartAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCart()
    }

    private fun setupCart() {
        binding.cartListRecycler.layoutManager = LinearLayoutManager(context)
        cartAdapter = CartAdapter()
        binding.cartListRecycler.adapter = cartAdapter

        binding.checkoutLayout.setOnClickListener {
            val intent = Intent(context, CheckOutActivity::class.java)
            startActivity(intent)
        }
    }
}