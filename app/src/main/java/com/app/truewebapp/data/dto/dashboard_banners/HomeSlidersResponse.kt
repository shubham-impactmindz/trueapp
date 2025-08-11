package com.app.truewebapp.data.dto.dashboard_banners

data class HomeSlidersResponse(
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val roundSliders: List<RoundSlider>,
    val bigSliders: List<BigSlider>,
    val smallSliders: List<SmallSlider>,
    val dealsHeader: String,
    val dealsSliders: List<DealSlider>,
    val fruitHeader: String,
    val fruitSliders: List<FruitSlider>
)

data class RoundSlider(
    val home_round_banner_id: Int,
    val home_round_banner_name: String,
    val home_round_banner_image: String,
    val home_round_banner_position: Int,
    val main_mcat_id: Int,
    val mcat_id: Int,
    val msubcat_id: Int,
    val mproduct_id: Int
)

data class BigSlider(
    val home_large_banner_id: Int,
    val home_large_banner_name: String,
    val home_large_banner_image: String,
    val home_large_banner_position: Int,
    val main_mcat_id: Int,
    val mcat_id: Int,
    val msubcat_id: Int,
    val mproduct_id: Int
)

data class SmallSlider(
    val home_small_banner_id: Int,
    val home_small_banner_name: String,
    val home_small_banner_image: String,
    val home_small_banner_position: Int,
    val main_mcat_id: Int,
    val mcat_id: Int,
    val msubcat_id: Int,
    val mproduct_id: Int
)

data class DealSlider(
    val home_explore_deal_banner_id: Int,
    val home_explore_deal_banner_name: String,
    val home_explore_deal_banner_image: String,
    val home_explore_deal_banner_position: Int,
    val main_mcat_id: Int,
    val mcat_id: Int,
    val msubcat_id: Int,
    val mproduct_id: Int
)

data class FruitSlider(
    val home_fruit_banner_id: Int,
    val home_fruit_banner_name: String,
    val home_fruit_banner_image: String,
    val home_fruit_banner_position: Int,
    val main_mcat_id: Int,
    val mcat_id: Int,
    val msubcat_id: Int,
    val mproduct_id: Int
)