package com.app.truewebapp.ui.component.main.cart

/**
 * Data class representing an item in the cart display
 * Can be either a paid item or a free item
 */
data class CartDisplayItem(
    val variantId: Int,
    val title: String?,
    val options: Map<String, String>,
    val image: String?,
    val fallbackImage: String?,
    val price: Double,
    val comparePrice: Double,
    val isWishlisted: Boolean,
    val cdnURL: String,
    val quantity: Int,
    val taxable: Int,
    val isFreeItem: Boolean = false, // Indicates if this is a free item from deals
    val originalVariantId: Int? = null, // Reference to the original paid item for free items
    val dealType: String? = null,
    val dealBuyQuantity: Int? = null,
    val dealGetQuantity: Int? = null,
    val dealQuantity: Int? = null,
    val dealPrice: Double? = null
)

