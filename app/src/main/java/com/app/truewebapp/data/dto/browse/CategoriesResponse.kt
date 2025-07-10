package com.app.truewebapp.data.dto.browse

data class CategoriesResponse(
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val main_categories: List<MainCategories>
)

data class MainCategories(
    val main_mcat_id: Int,
    val main_mcat_name: String,
    val created_at: String,
    val updated_at: String,
    val categories: List<Category>
)
data class Category(
    val mcat_id: Int,
    val mcat_name: String,
    val created_at: String,
    val updated_at: String,
    val subcategories: List<Subcategory>
)

data class Subcategory(
    val msubcat_id: Int,
    val mcat_id: Int,
    val msubcat_name: String,
    val msubcat_slug: String,
    val msubcat_tag: String?,
    val msubcat_image: String,
    val msubcat_publish: List<String>,
    val offer_name: String?,
    val start_time: String?,
    val end_time: String?,
    val msubcat_type: String,
    val logical_operator: String,
    val product_ids: List<Int>,
    val created_at: String,
    val updated_at: String,
    val products: List<Product>
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
    var quantity: Int,
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
