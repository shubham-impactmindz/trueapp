package com.app.truewebapp.ui.component.main.cart.cartdatabase

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class CartViewModel(private val repository: CartRepository) : ViewModel() {

    val allCartItems: LiveData<List<CartItemEntity>> = repository.allItemsLiveData
    val totalCount: LiveData<Int> = repository.totalCountLiveData

    fun getQuantityFlow(variantId: Int): LiveData<Int?> {
        return repository.quantityLiveData(variantId)
    }
    fun quantityLiveData(variantId: Int): LiveData<Int?> = repository.quantityLiveData(variantId)

    fun addOrUpdateCart(item: CartItemEntity) = viewModelScope.launch {
        repository.insertOrUpdate(item)
    }

    fun deleteCartByVariant(variantId: Int) = viewModelScope.launch {
        repository.deleteByVariant(variantId)
    }
}