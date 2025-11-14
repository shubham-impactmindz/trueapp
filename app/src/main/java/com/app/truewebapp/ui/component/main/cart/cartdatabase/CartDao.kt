package com.app.truewebapp.ui.component.main.cart.cartdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow // Import Flow

@Dao
interface CartDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE variantId = :variantId")
    suspend fun deleteItemByVariantId(variantId: Int)

    // --- CHANGE HERE: Return Flow for live updates ---
    @Query("SELECT * FROM cart_items")
    fun getAllItems(): Flow<List<CartItemEntity>>

    // Add this query to fetch a single item for wishlist updates
    @Query("SELECT * FROM cart_items WHERE variantId = :variantId LIMIT 1")
    suspend fun getItemByVariantId(variantId: Int): CartItemEntity?

    @Query("SELECT quantity FROM cart_items WHERE variantId = :variantId LIMIT 1")
    suspend fun getQuantityByVariantId(variantId: Int): Int?

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // --- NEW: Query to get the total units (sum of quantities) ---
    // This will emit a new total count every time the cart items change.
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items")
    fun getCartItemCount(): Flow<Int> // COALESCE ensures we get 0 instead of null when table is empty

    @Query("SELECT * FROM cart_items")
    suspend fun getAllItemsOnce(): List<CartItemEntity>

    @Query("SELECT quantity FROM cart_items WHERE variantId = :variantId")
    fun getQuantityFlowByVariantId(variantId: Int): Flow<Int?>

    @Query("SELECT isWishlisted FROM cart_items WHERE variantId = :variantId")
    suspend fun isWishlisted(variantId: Int): Boolean?
}