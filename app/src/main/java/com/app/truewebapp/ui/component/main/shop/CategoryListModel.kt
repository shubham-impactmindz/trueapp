package com.app.truewebapp.ui.component.main.shop

import com.google.gson.annotations.SerializedName

data class CategoryListModel(
    @SerializedName("success") val success: Long? = null,
    @SerializedName("brands") val brands: List<Brand>? = null,
    @SerializedName("categories") val categories: List<Category>? = emptyList()
)

data class Brand(
    @SerializedName("braID") val braID: Long? = null,
    @SerializedName("braTitle") val braTitle: String? = null
)

data class Category(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("categoryImage") val categoryImage: Boolean? = null,
    @SerializedName("new") val new: Boolean? = null,
    @SerializedName("hot") val hot: Boolean? = null,
    @SerializedName("comingSoon") val comingSoon: Boolean? = null,
    @SerializedName("clearance") val clearance: Boolean? = null,
    @SerializedName("crazyPrice") val crazyPrice: Boolean? = null,
    @SerializedName("whileStocksLast") val whileStocksLast: Boolean? = null,
    @SerializedName("isLimitedTimeOnly") val isLimitedTimeOnly: Boolean? = null,
    @SerializedName("dealOfTheWeek") val dealOfTheWeek: Boolean? = null,
    @SerializedName("trending") val trending: Boolean? = null,
    @SerializedName("noOuterBox") val noOuterBox: Boolean? = null,
    @SerializedName("subCats") val subCats: List<SubCat>? = emptyList() // Ensure it's always a list
)

data class SubCat(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("new") val new: Boolean? = null,
    @SerializedName("hot") val hot: Boolean? = null,
    @SerializedName("comingSoon") val comingSoon: Boolean? = null,
    @SerializedName("clearance") val clearance: Boolean? = null,
    @SerializedName("crazyPrice") val crazyPrice: Boolean? = null,
    @SerializedName("whileStocksLast") val whileStocksLast: Boolean? = null,
    @SerializedName("isLimitedTimeOnly") val isLimitedTimeOnly: Boolean? = null,
    @SerializedName("dealOfTheWeek") val dealOfTheWeek: Boolean? = null,
    @SerializedName("trending") val trending: Boolean? = null,
    @SerializedName("noOuterBox") val noOuterBox: Boolean? = null,
    @SerializedName("products") val products: List<Product>? = emptyList() // Ensure it's always a list
)

data class Product(
    @SerializedName("type") val type: Type? = null,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("img") val img: String? = null,
    @SerializedName("stock") val stock: Long? = null,
    @SerializedName("outerQty") val outerQty: Long? = null,
    @SerializedName("price") val price: String? = null,
    @SerializedName("credit") val credit: String? = null,
    @SerializedName("brandID") val brandID: Long? = null,
    @SerializedName("catID") val catID: Long? = null,
    @SerializedName("basketState") val basketState: Long? = null,
    @SerializedName("favouriteState") val favouriteState: Long? = null,
    @SerializedName("notifyState") val notifyState: Long? = null,
    @SerializedName("max") val max: Long? = null,
    @SerializedName("state") val state: Boolean? = null
)

enum class Type {
    Feat,
    Reg
}
