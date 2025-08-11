package com.app.truewebapp.data.dto.dashboard_banners


data class ProductSlidersResponse(
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val newProductHeader: String,
    val newProductBanners: List<ProductBanner>,
    val topSellerHeader: String,
    val topSellerBanners: List<ProductBanner>
)

data class ProductBanner(
    val new_product_id: Int? = null,
    val top_seller_id: Int? = null,
    val mvariant_id: Int,
    val product: Product
)
