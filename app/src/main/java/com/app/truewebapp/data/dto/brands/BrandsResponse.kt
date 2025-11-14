package com.app.truewebapp.data.dto.brands

import com.google.gson.annotations.SerializedName

data class BrandsResponse (
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    @SerializedName("brands")
    val mbrands: List<MBrands>?,
    @SerializedName("favouritebrands")
    val wishlistbrand: List<WishlistBrand>?,
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
