package com.app.truewebapp.ui.component.main.cart.cartdatabase

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData

class CartRepository(private val cartDao: CartDao) {

    // LiveData for total count badge
    val totalCountLiveData: LiveData<Int> =
        cartDao.getCartItemCount().asLiveData()

    fun quantityLiveData(variantId: Int): LiveData<Int?> =
        cartDao.getQuantityFlowByVariantId(variantId).asLiveData()

    suspend fun insertOrUpdate(item: CartItemEntity) = cartDao.insertOrUpdateItem(item)
    suspend fun deleteByVariant(variantId: Int) = cartDao.deleteItemByVariantId(variantId)
    suspend fun isWishlisted(variantId: Int) = cartDao.isWishlisted(variantId)
}
