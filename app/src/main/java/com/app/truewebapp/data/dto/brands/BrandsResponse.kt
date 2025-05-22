package com.app.truewebapp.data.dto.brands

data class BrandsResponse (
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val mbrands: List<MBrands>,
    val wishlistbrand: List<WishlistBrand>,
)

data class MBrands(
    val mbrand_id: Int,
    val mbrand_name: String,
    val mbrand_image: String,
    val created_at: String,
    val updated_at: String,
    var isSelected: Boolean,
)

data class WishlistBrand(
    val mbrand_id: Int,
    val mbrand_name: String,
    val mbrand_image: String,
)