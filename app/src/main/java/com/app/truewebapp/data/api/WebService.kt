package com.app.truewebapp.data.api


import com.app.accutecherp.data.dto.city.cityResponse
import com.app.accutecherp.data.dto.clientstatus.clientStatusResponse
import com.app.accutecherp.data.dto.comment.CommentResponse
import com.app.accutecherp.data.dto.orders.*
import com.app.accutecherp.data.dto.product.productDataResponse
import com.app.accutecherp.data.dto.product.productGrpResponse
import com.app.accutecherp.data.dto.product.productPakingResponse
import com.app.accutecherp.data.dto.product.productUnitResponse
import com.app.accutecherp.data.dto.profile.ProfileResponse
import com.app.accutecherp.data.dto.sitevisit.VisitHistoryResponse
import com.app.accutecherp.data.dto.state.stateResponse
import com.app.truewebapp.data.dto.brands.BrandsResponse
import com.app.truewebapp.data.dto.browse.BannerResponse
import com.app.truewebapp.data.dto.browse.CategoriesResponse
import com.app.truewebapp.data.dto.cart.CartRequest
import com.app.truewebapp.data.dto.cart.CartResponse
import com.app.truewebapp.data.dto.change_password.ChangePasswordRequest
import com.app.truewebapp.data.dto.change_password.ChangePasswordResponse
import com.app.truewebapp.data.dto.company_address.CompanyAddressDeleteRequest
import com.app.truewebapp.data.dto.company_address.CompanyAddressRequest
import com.app.truewebapp.data.dto.company_address.CompanyAddressResponse
import com.app.truewebapp.data.dto.dashboard_banners.BigBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.DealsBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.FruitsBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.NewProductsBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.RoundBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.SmallBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.TopSellerBannersResponse
import com.app.truewebapp.data.dto.login.LoginRequest
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.data.dto.register.RegisterRequest
import com.app.truewebapp.data.dto.register.RegisterResponse
import com.app.truewebapp.data.dto.register.VerifyRepResponse
import com.app.truewebapp.data.dto.reset_password.ResetPasswordRequest
import com.app.truewebapp.data.dto.reset_password.ResetPasswordResponse
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.data.dto.wishlist.WishlistResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*


/**
 *  All web services are declared here
 */
@JvmSuppressWildcards
interface WebService {

    @POST("/api/login")
    fun fetchLogin(
        @Body loginRequest: LoginRequest
    ): Call<LoginResponse>

    @POST("/api/register")
    fun fetchRegister(
        @Body registerRequest: RegisterRequest
    ): Call<RegisterResponse>

    @GET("/api/categories")
    fun fetchCategories(
        @Query("search") search: String? = null,
        @Header("Authorization") token: String,
        @Query("mbrand_id") filters: String? = null,
    ): Call<CategoriesResponse>


    @POST("/api/wishlist/add")
    fun fetchWishlist(
        @Header("Authorization") token: String,
        @Body wishlistRequest: WishlistRequest
    ): Call<WishlistResponse>

    @GET("/api/browse-banner")
    fun fetchBanner(
        @Header("Authorization") token: String,
        ): Call<BannerResponse>

    @GET("/api/brands")
    fun fetchBrands(
        @Header("Authorization") token: String,
        @Query("user_id") userId: String? = null,
        ): Call<BrandsResponse>

    @GET("/api/round-banner")
    fun fetchRoundBanners(
        @Header("Authorization") token: String,
        ): Call<RoundBannersResponse>

    @GET("/api/big-banner")
    fun fetchBigBanners(
        @Header("Authorization") token: String,
        ): Call<BigBannersResponse>

    @GET("/api/small-banner")
    fun fetchSmallBanners(
        @Header("Authorization") token: String,
        ): Call<SmallBannersResponse>

    @GET("/api/deals-banner")
    fun fetchDealsBanners(
        @Header("Authorization") token: String,
        ): Call<DealsBannersResponse>

    @GET("/api/fruit-banner")
    fun fetchFruitsBanners(
        @Header("Authorization") token: String,
        ): Call<FruitsBannersResponse>

    @GET("/api/new-product-banner")
    fun fetchNewProductsBanners(
        @Header("Authorization") token: String,
        ): Call<NewProductsBannersResponse>

    @GET("/api/top-seller-banner")
    fun fetchTopSellerBanners(
        @Header("Authorization") token: String,
        ): Call<TopSellerBannersResponse>

    @POST("/api/change-password")
    fun fetchChangePassword(
        @Header("Authorization") token: String,
        @Body changePasswordRequest: ChangePasswordRequest
        ): Call<ChangePasswordResponse>

    @GET("/api/company-address")
    fun fetchCompanyAddress(
        @Header("Authorization") token: String,
        ): Call<CompanyAddressResponse>

    @POST("/api/company-address/upsert")
    fun fetchUpsertCompanyAddress(
        @Header("Authorization") token: String,
        @Body companyAddressRequest: CompanyAddressRequest
        ): Call<ChangePasswordResponse>

    @HTTP(method = "DELETE", path = "/api/company-address/delete", hasBody = true)
    fun fetchDeleteCompanyAddress(
        @Header("Authorization") token: String,
        @Body companyAddressDeleteRequest: CompanyAddressDeleteRequest
    ): Call<ChangePasswordResponse>

    @GET("/api/reps/check/")
    fun fetchVerifyUser(
        @Query("userName") userName: String
    ): Call<VerifyRepResponse>

    @DELETE("/api/user-account/delete")
    fun fetchDeleteAccount(
        @Header("Authorization") token: String,
    ): Call<ChangePasswordResponse>

    @POST("/api/forgot-password")
    fun fetchResetPassword(
        @Body resetPasswordRequest: ResetPasswordRequest
    ): Call<ResetPasswordResponse>

    @POST("/api/cart-item/update")
    fun fetchCart(
        @Header("Authorization") token: String,
        @Body cartRequest: CartRequest
    ): Call<ChangePasswordResponse>

    @GET("/api/cart-item")
    fun fetchCartItems(
        @Header("Authorization") token: String
    ): Call<CartResponse>

    @FormUrlEncoded
    @POST("api/get_states")
    fun fetchState(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String
    ): Call<stateResponse>

    @FormUrlEncoded
    @POST("api/get_cities")
    fun fetchCities(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("state_id") stateId: String
    ): Call<cityResponse>

    @FormUrlEncoded
    @POST("api/get_product_group")
    fun fetchProductGroup(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String
    ): Call<productGrpResponse>

    @FormUrlEncoded
    @POST("api/get_products")
    fun fetchProductData(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("group_id") groupId: String
    ): Call<productDataResponse>

    @Multipart
    @POST("api/save_party")
    fun postData(
        @Part("auth_key") authKey: RequestBody,
        @Part("login_key") userName: RequestBody,
        @Part userfile: MultipartBody.Part?,
        @Part("account_type_id") account_type_id: RequestBody,
        @Part("account") account: RequestBody,
        @Part("contact_name") contact_name: RequestBody,
        @Part("contact_info") contact_info: RequestBody,
        @Part("address") address: RequestBody,
        @Part("plot_no") plot: RequestBody,
        @Part("sector_street") street: RequestBody,
        @Part("locality") locality: RequestBody,
        @Part("landmark") landmark: RequestBody,
        @Part("city_id") city_id: RequestBody,
        @Part("zip") zip: RequestBody,
        @Part("geo_location") geo_location: RequestBody,
        @Part("comment") comment: RequestBody,
        @Part("product_ids") product_ids: RequestBody,
        @Part("client_status_id") client_status_id: RequestBody,
    ): Call<CommentResponse>

    @FormUrlEncoded
    @POST("api/get_client_status")
    fun fetchClientStatus(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String
    ): Call<clientStatusResponse>
    /////////////////////////////////////////ORDERS//////////////////////////////////////////////
    @FormUrlEncoded
    @POST("api/get_customer")
    fun fetchCustomerList(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String
    ): Call<customerResponse>

    @FormUrlEncoded
    @POST("api/get_payment_mode")
    fun fetchPaymentMode(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String
    ): Call<paymentModeResponse>

    @FormUrlEncoded
    @POST("api/get_sale_product")
    fun fetchSalesProducts(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String
    ): Call<salesProductResponse>

    @FormUrlEncoded
    @POST("api/get_product_packing")
    fun fetchProductPaking(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("product_id") prodId: String,
        @Field("account_type_id") accntTypeId: String,
    ): Call<productPakingResponse>

    @FormUrlEncoded
    @POST("api/get_product_units")
    fun fetchProductUnit(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("product_id") prodId: String,
        @Field("product_packing_id") prdtPakingId: String,
    ): Call<productUnitResponse>

    @FormUrlEncoded
    @POST("api/get_product_detail")
    fun fetchProductDetails(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("product_id") prodId: String,
        @Field("state_id") stateId: String,
    ): Call<productDetailResponse>

    @FormUrlEncoded
    @POST("api/save_sale_order")
    fun placeOrders(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
//        @Field("orderNo") orderNo: String,
        @Field("so_prefix") so_prefix: String,
        @Field("orderDate") orderDate: String,
        @Field("customerId") customerId: String,
        @Field("customer_state_id") customer_state_id: String,
        @Field("effectDateFrom") effectDateFrom: String,
        @Field("effectDateTill") effectDateTill: String,
        @Field("paymentModeId") paymentModeId: String,
        @Field("accountTypeId") accountTypeId: String,
        @Field("advcanceAmount") advcanceAmount: String,
        @Field("grandTotal") grandTotal: String,
        @Field("cr_days") cr_days: String,
        @Field("product_data") product_data: String,
    ): Call<placeOrderResponse>

    @FormUrlEncoded
    @POST("api/get_so_data")
    fun fetchOrderHistory(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("from_so_no") from_so_no: String,
        @Field("to_so_no") to_so_no: String,
        @Field("from_so_date") from_so_date: String,
        @Field("to_so_date") to_so_date: String,
        @Field("client_ids") client_ids: String,
    ): Call<orderHistoryResponse>

    @Multipart
    @POST("api/update_profile")
    fun profileUpdate(
        @Part("auth_key") authKey: RequestBody,
        @Part("login_key") userName: RequestBody,
        @Part userfile: MultipartBody.Part?,
        @Part("employee_name") employee_name: RequestBody,
        @Part("employee_mobile") employee_mobile: RequestBody,
        @Part("employee_email") employee_email: RequestBody,
    ): Call<ProfileResponse>

    @FormUrlEncoded
    @POST("api/update_profile")
    fun profileUpdatewithoutImage(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("employee_name") employee_name: String,
        @Field("employee_mobile") employee_mobile: String,
        @Field("employee_email") employee_email: String,
    ): Call<ProfileResponse>

    @FormUrlEncoded
    @POST("api/change_password")
    fun changePassword(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("old_password") old_password: String,
        @Field("new_password") new_password: String,
        @Field("confirm_password") confirm_password: String,
    ): Call<ProfileResponse>

    @FormUrlEncoded
    @POST("api/save_visit_data")
    fun saveSiteVisit(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("VisitTime") VisitTime: String,
        @Field("GeoLocation") GeoLocation: String,
        @Field("LocationText") LocationText: String,
        @Field("Date") Date: String,
        @Field("Purpose") Purpose: String,
    ): Call<ProfileResponse>

    @FormUrlEncoded
    @POST("api/get_visit_history")
    fun fetchVisitHistory(
        @Field("auth_key") authKey: String,
        @Field("login_key") userName: String,
        @Field("Date") date: String,
    ): Call<VisitHistoryResponse>
}