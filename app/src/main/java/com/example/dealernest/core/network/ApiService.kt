package com.example.dealernest.core.network

import com.example.dealernest.data.auth.dto.AdminProfileDto
import com.example.dealernest.data.auth.dto.LoginRequestDto
import com.example.dealernest.data.auth.dto.LoginResponseDto
import com.example.dealernest.data.collection.dto.CreateDsrCollectionRequestDto
import com.example.dealernest.data.collection.dto.DsrCollectionDto
import com.example.dealernest.data.order.dto.CreateOrderRequestDto
import com.example.dealernest.data.order.dto.OrderDto
import com.example.dealernest.data.product.dto.ProductDto
import com.example.dealernest.data.salesperformance.dto.SalesPerformanceResponseDto
import com.example.dealernest.data.vendor.dto.VendorDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("v1/admin/login")
    suspend fun login(@Body request: LoginRequestDto): ApiEnvelope<LoginResponseDto>

    @GET("v1/admin/profile")
    suspend fun getProfile(): ApiEnvelope<AdminProfileDto>

    // Raw Response<ResponseBody> - the logout call is best-effort (see AuthRepositoryImpl),
    // its body is never inspected, so there's nothing to gain from parsing it as an envelope.
    @POST("v1/admin/logout")
    suspend fun logout(): Response<ResponseBody>

    @GET("v1/vendors/search")
    suspend fun searchVendors(
        @Query("search") search: String?,
        @Query("limit") limit: Int,
    ): ApiEnvelope<List<VendorDto>>

    @GET("v1/vendors/{id}")
    suspend fun getVendor(@Path("id") id: Int): ApiEnvelope<VendorDto>

    @Multipart
    @POST("v1/vendors")
    suspend fun createVendor(
        @Part("shop_name") shopName: RequestBody,
        @Part("contact_person") contactPerson: RequestBody,
        @Part("email") email: RequestBody?,
        @Part("mobile") mobile: RequestBody,
        @Part("country_id") countryId: RequestBody,
        @Part("full_address") fullAddress: RequestBody,
        @Part("lat") lat: RequestBody?,
        @Part("long") long: RequestBody?,
        @Part("status") status: RequestBody,
        @Part image: MultipartBody.Part?,
    ): ApiEnvelope<VendorDto>

    // POST + "_method=PUT" override, not a true @PUT - multipart bodies can't be sent as a real
    // PUT from most HTTP clients (Laravel convention, see api-contract.md).
    @Multipart
    @POST("v1/vendors/{id}")
    suspend fun updateVendor(
        @Path("id") id: Int,
        @Part("_method") method: RequestBody,
        @Part("shop_name") shopName: RequestBody,
        @Part("contact_person") contactPerson: RequestBody,
        @Part("email") email: RequestBody?,
        @Part("mobile") mobile: RequestBody,
        @Part("country_id") countryId: RequestBody,
        @Part("full_address") fullAddress: RequestBody,
        @Part("lat") lat: RequestBody?,
        @Part("long") long: RequestBody?,
        @Part("status") status: RequestBody,
        @Part image: MultipartBody.Part?,
    ): ApiEnvelope<VendorDto>

    @GET("v1/dsr-collections")
    suspend fun listDsrCollections(
        @Query("vendor_id") vendorId: Int?,
        @Query("date_from") dateFrom: String?,
        @Query("date_to") dateTo: String?,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): PaginatedEnvelope<DsrCollectionDto>

    @POST("v1/dsr-collections")
    suspend fun createDsrCollection(@Body request: CreateDsrCollectionRequestDto): ApiEnvelope<DsrCollectionDto>

    // Raw Response<ResponseBody> - delete's body is never inspected, only the HTTP status
    // matters (403 = role not permitted, see DsrCollectionRepositoryImpl).
    @DELETE("v1/dsr-collections/{id}")
    suspend fun deleteDsrCollection(@Path("id") id: Int): Response<ResponseBody>

    @GET("v1/products/search")
    suspend fun searchProducts(
        @Query("search") search: String?,
        @Query("limit") limit: Int,
    ): ApiEnvelope<List<ProductDto>>

    @GET("v1/products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ApiEnvelope<ProductDto>

    @POST("v1/orders")
    suspend fun createOrder(@Body request: CreateOrderRequestDto): ApiEnvelope<OrderDto>

    @PUT("v1/orders/{id}")
    suspend fun updateOrder(@Path("id") id: Int, @Body request: CreateOrderRequestDto): ApiEnvelope<OrderDto>

    @GET("v1/orders/{id}")
    suspend fun getOrder(@Path("id") id: Int): ApiEnvelope<OrderDto>

    @GET("v1/orders/by-placed-by")
    suspend fun listOrdersByPlacedBy(
        @Query("place_by") placeBy: Int,
        @Query("status_filter") statusFilter: Int?,
        @Query("vendor_filter") vendorFilter: Int?,
        @Query("date_from") dateFrom: String?,
        @Query("date_to") dateTo: String?,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): PaginatedEnvelope<OrderDto>

    // Raw Response<ResponseBody> - cancel's body is never inspected, only the HTTP status
    // matters (403 = not cancellable, see OrderRepositoryImpl).
    @POST("v1/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Int): Response<ResponseBody>

    @GET("v1/admin/sales-performance")
    suspend fun getSalesPerformance(
        @Query("date_from") dateFrom: String?,
        @Query("date_to") dateTo: String?,
        @Query("admin_id") adminId: Int?,
    ): SalesPerformanceResponseDto
}
