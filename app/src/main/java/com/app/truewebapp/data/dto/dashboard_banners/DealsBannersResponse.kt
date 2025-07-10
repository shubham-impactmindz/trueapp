package com.app.truewebapp.data.dto.dashboard_banners

data class DealsBannersResponse (
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val slider_header: String,
    val dealsSliders: List<DealsSliders>,
)

data class DealsSliders(
    val home_explore_deal_banner_id: Int,
    val home_explore_deal_banner_name: String,
    val home_explore_deal_banner_image: String,
    val home_explore_deal_banner_position: Int,
    val main_mcat_id: Int,
    val mcat_id: Int,
    val msubcat_id: Int,
    val mproduct_id: Int,
    val created_at: String,
    val updated_at: String,
)
