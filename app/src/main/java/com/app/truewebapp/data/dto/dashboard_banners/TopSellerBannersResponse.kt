package com.app.truewebapp.data.dto.dashboard_banners

data class TopSellerBannersResponse (
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val slider_header: String,
    val topSellerBanners: List<TopSellerBanners>,
)

data class TopSellerBanners(
    val top_seller_id: Int,
    val mvariant_id: Int,
    val product: Product,
)
