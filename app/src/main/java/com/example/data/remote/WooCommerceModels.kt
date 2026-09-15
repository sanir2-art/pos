package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WcProduct(
    val id: Long,
    val name: String,
    val sku: String? = "",
    val price: String? = "0",
    @Json(name = "regular_price") val regularPrice: String? = "0",
    @Json(name = "sale_price") val salePrice: String? = "",
    @Json(name = "stock_quantity") val stockQuantity: Int? = 0,
    @Json(name = "manage_stock") val manageStock: Boolean? = false,
    val categories: List<WcCategory>? = emptyList(),
    val images: List<WcImage>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class WcCategory(
    val id: Long = 0,
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class WcImage(
    val id: Long = 0,
    val src: String = ""
)

@JsonClass(generateAdapter = true)
data class WcCreateOrderRequest(
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "payment_method_title") val paymentMethodTitle: String,
    @Json(name = "set_paid") val setPaid: Boolean = true,
    val status: String = "completed",
    val billing: WcBilling,
    @Json(name = "line_items") val lineItems: List<WcLineItem>,
    @Json(name = "customer_note") val customerNote: String = ""
)

@JsonClass(generateAdapter = true)
data class WcBilling(
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val address_1: String = ""
)

@JsonClass(generateAdapter = true)
data class WcLineItem(
    @Json(name = "product_id") val productId: Long,
    val quantity: Int,
    val total: String? = null
)

@JsonClass(generateAdapter = true)
data class WcOrderResponse(
    val id: Long,
    val status: String = "",
    val total: String = ""
)

@JsonClass(generateAdapter = true)
data class WcUpdateProductStockRequest(
    @Json(name = "stock_quantity") val stockQuantity: Int,
    @Json(name = "regular_price") val regularPrice: String? = null
)
