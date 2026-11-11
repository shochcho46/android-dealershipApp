package com.example.dealernest.data.product.dto

import com.example.dealernest.domain.model.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductLookupDto(
    // id nullable too, not just name - see the null-safety note on ProductDto below.
    val id: Int? = null,
    val name: String? = null,
)

@Serializable
data class ProductImageDto(
    val url: String? = null,
    @SerialName("thumb_url") val thumbUrl: String? = null,
)

@Serializable
data class ProductStockDto(
    @SerialName("stock_id") val stockId: Int,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    @SerialName("batch_id") val batchId: String? = null,
    @SerialName("purchase_price") val purchasePrice: String? = null,
    @SerialName("sell_price") val sellPrice: String? = null,
    @SerialName("available_quantity") val availableQuantity: Int = 0,
    @SerialName("manufacture_date") val manufactureDate: String? = null,
    @SerialName("expire_date") val expireDate: String? = null,
)

// Nullable leaf strings, not required - see .claude/docs/api-contract.md "null-safety audit".
// Fallbacks live in toDomain() below.
@Serializable
data class ProductDto(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val color: ProductLookupDto? = null,
    val company: ProductLookupDto? = null,
    val unit: ProductLookupDto? = null,
    @SerialName("measurement_unit_name") val measurementUnitName: String? = null,
    @SerialName("measurement_unit_number") val measurementUnitNumber: String? = null,
    @SerialName("package_unit_name") val packageUnitName: String? = null,
    @SerialName("package_unit_quantity") val packageUnitQuantity: String? = null,
    @SerialName("discount_type") val discountType: String? = null,
    @SerialName("discount_amount") val discountAmount: String? = null,
    // Boolean, not Int - confirmed on Admin and Vendor's status fields, this backend uses
    // boolean `status` broadly. Not mapped into the Product domain model (unused), but still
    // has to parse correctly regardless. See .claude/docs/api-contract.md "null-safety audit".
    val status: Boolean = true,
    @SerialName("status_text") val statusText: String? = null,
    val image: ProductImageDto? = null,
    @SerialName("quantity_available") val quantityAvailable: Int = 0,
    @SerialName("sell_price") val sellPrice: String? = null,
    val stocks: List<ProductStockDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun ProductDto.toDomain(): Product {
    val minStockPrice = stocks.mapNotNull { it.sellPrice?.toDoubleOrNull() }.minOrNull()
    val listedPrice = sellPrice?.toDoubleOrNull()
    return Product(
        id = id,
        name = name ?: "Unnamed Product",
        imageUrl = image?.url,
        quantityAvailable = quantityAvailable,
        sellPrice = listedPrice ?: minStockPrice ?: 0.0,
        minSellPrice = minStockPrice ?: listedPrice ?: 0.0,
        unitName = measurementUnitName,
    )
}
