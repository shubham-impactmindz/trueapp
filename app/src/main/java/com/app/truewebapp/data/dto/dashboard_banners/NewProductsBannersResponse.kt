package com.app.truewebapp.data.dto.dashboard_banners

data class NewProductsBannersResponse (
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val slider_header: String,
    val newProductBanners: List<NewProductBanners>,
)

data class NewProductBanners(
    val new_product_id: Int,
    val mvariant_id: Int,
    val product: Product,
)

data class Product(
    val mproduct_id: Int,
    val mproduct_title: String,
    val mproduct_image: String,
    val mproduct_slug: String,
    val mproduct_desc: String,
    val status: String,
    val saleschannel: List<String>,
    val brand_id: Int,
    val brand_name: String,
    val type_id: Int,
    val product_type: String,
    val tag_ids: List<Int>,
    val tag_names: List<String>,
    val mvariant_id: Int,
    val sku: String,
    val image: String?,
    val price: Double,
    val quantity: Int,
    val compare_price: Double?,
    val cost_price: Double?,
    val taxable: Int,
    val barcode: String,
    val options: List<String>,
    val option_value: Map<String, String>,
    val mlocation_id: Int,
    val product_deal_tag: String?,
    val product_offer: String?,
    var user_info_wishlist: Boolean
)
