package com.app.truewebapp.ui.component.main.cart.cartdatabase

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.app.truewebapp.utils.Converters

@Entity(tableName = "cart_items")
@TypeConverters(Converters::class)
data class CartItemEntity(
    @PrimaryKey val variantId: Int,
    val title: String?, // Made nullable for safety based on adapter's usage
    val options: Map<String, String>,
    val image: String?,
    val fallbackImage: String?,
    val price: Double,
    val comparePrice: Double,
    var isWishlisted: Boolean, // Made 'var' so we can change it after fetching
    val cdnURL: String,
    val quantity: Int,
    val taxable: Int
)