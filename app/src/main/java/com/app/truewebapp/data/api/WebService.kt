package com.app.truewebapp.data.api


import com.app.truewebapp.data.dto.bank.BankDetailResponse
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
import com.app.truewebapp.data.dto.coupons.CouponsResponse
import com.app.truewebapp.data.dto.dashboard_banners.BigBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.DealsBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.FruitsBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.HomeSlidersResponse
import com.app.truewebapp.data.dto.dashboard_banners.NewProductsBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.ProductSlidersResponse
import com.app.truewebapp.data.dto.dashboard_banners.RoundBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.SmallBannersResponse
import com.app.truewebapp.data.dto.dashboard_banners.TopSellerBannersResponse
import com.app.truewebapp.data.dto.delivery.DeliveryMethodsResponse
import com.app.truewebapp.data.dto.delivery.DeliverySettingsResponse
import com.app.truewebapp.data.dto.login.LoginRequest
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.data.dto.order.OrderPlaceResponse
import com.app.truewebapp.data.dto.order.OrderRequest
import com.app.truewebapp.data.dto.order.OrdersResponse
import com.app.truewebapp.data.dto.profile.ProfileRequest
import com.app.truewebapp.data.dto.profile.UserProfileResponse
import com.app.truewebapp.data.dto.referral.ReferralRequest
import com.app.truewebapp.data.dto.referral.ReferralResponse
import com.app.truewebapp.data.dto.register.RegisterRequest
import com.app.truewebapp.data.dto.register.RegisterResponse
import com.app.truewebapp.data.dto.register.VerifyRepResponse
import com.app.truewebapp.data.dto.reset_password.ResetPasswordRequest
import com.app.truewebapp.data.dto.reset_password.ResetPasswordResponse
import com.app.truewebapp.data.dto.wallet.WalletBalanceResponse
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.data.dto.wishlist.WishlistResponse
import com.app.truewebapp.data.dto.service.RegisterInterestRequest
import com.app.truewebapp.data.dto.service.RegisterInterestResponse
import com.app.truewebapp.data.dto.stripe.StripeConfigResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query


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

    @GET("/api/products-banner")
    fun fetchProductsBanner(
        @Header("Authorization") token: String,
        ): Call<ProductSlidersResponse>

    @GET("/api/home-banner")
    fun fetchHomeBanner(
        @Header("Authorization") token: String,
        ): Call<HomeSlidersResponse>

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

    @GET("/api/delivery-methods")
    fun fetchDeliveryMethods(
        @Header("Authorization") token: String
    ): Call<DeliveryMethodsResponse>

    @GET("/api/coupons")
    fun fetchCoupons(
        @Header("Authorization") token: String
    ): Call<CouponsResponse>

    @GET("/api/settings/delivery-setting")
    fun fetchDeliverySetting(
        @Header("Authorization") token: String
    ): Call<DeliverySettingsResponse>

    @GET("/api/bank-detail")
    fun fetchBankDetail(
        @Header("Authorization") token: String
    ): Call<BankDetailResponse>

    @GET("/api/stripe-config")
    fun fetchStripeConfig(
        @Header("Authorization") token: String
    ): Call<StripeConfigResponse>

    @POST("/api/orders")
    fun fetchOrderPlace(
        @Header("Authorization") token: String,
        @Body orderRequest: OrderRequest
    ): Call<OrderPlaceResponse>

    @GET("/api/orders")
    fun fetchOrders(
        @Query("page") page: String? = null,
        @Query("per_page") per_page: String? = null,
        @Header("Authorization") token: String,
    ): Call<OrdersResponse>

    @GET("/api/user-profile")
    fun fetchUserProfile(
        @Header("Authorization") token: String
    ): Call<UserProfileResponse>

    @PUT("/api/user-profile/update")
    fun fetchEditUserProfile(
        @Header("Authorization") token: String,
        @Body profileRequest: ProfileRequest
    ): Call<UserProfileResponse>

    @GET("/api/wallet/balance")
    fun fetchWalletBalance(
        @Header("Authorization") token: String
    ): Call<WalletBalanceResponse>

    @POST("/api/send-referral")
    fun sendReferral(
        @Header("Authorization") token: String,
        @Body referralRequest: ReferralRequest
    ): Call<ReferralResponse>

    @POST("/api/service-solutions")
    fun registerInterest(
        @Header("Authorization") token: String,
        @Body registerInterestRequest: RegisterInterestRequest
    ): Call<RegisterInterestResponse>
}