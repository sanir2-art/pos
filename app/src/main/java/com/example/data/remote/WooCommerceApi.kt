package com.example.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface WooCommerceApi {

    @GET("wp-json/wc/v3/products")
    suspend fun getProducts(
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("status") status: String = "publish"
    ): List<WcProduct>

    @POST("wp-json/wc/v3/orders")
    suspend fun createOrder(
        @Body order: WcCreateOrderRequest
    ): WcOrderResponse

    @PUT("wp-json/wc/v3/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Body body: WcUpdateProductStockRequest
    ): WcProduct

    @GET("wp-json/wc/v3/system_status")
    suspend fun checkSystemStatus(): Response<ResponseBody>

    @GET("wp-json/wc/v3/products")
    suspend fun testConnection(
        @Query("per_page") perPage: Int = 1
    ): Response<List<WcProduct>>
}
