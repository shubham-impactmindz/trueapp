package com.app.truewebapp.ui.component.main.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.truewebapp.databinding.FragmentCartBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CartBottomSheetFragment(private val cartData: CartData) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentCartBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        if (cartData.totalItems > 0) {
            binding.textItems.text = "Total Items: ${cartData.totalItems}"
            binding.textTotal.text = "Total Price: $${cartData.totalPrice}"
            binding.textProductName.text = "com.app.truewebapp.ui.component.main.shop.Product: ${cartData.productName}"
        } else {
            dismiss() // Hide bottom sheet if cart is empty
        }
    }
}