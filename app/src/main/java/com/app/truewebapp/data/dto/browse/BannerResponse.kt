package com.app.truewebapp.data.dto.browse

data class BannerResponse(
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val browseBanners: List<BrowseBanners>
)

data class BrowseBanners(
    val browsebanner_id: Int,
    val browsebanner_name: String,
    val browsebanner_image: String,
    val browsebanner_position: Int,
    val main_mcat_id: Int,
    val mcat_id: Int,
    val msubcat_id: Int,
    val mproduct_id: Int,
    val created_at: String,
    val updated_at: String,
)
