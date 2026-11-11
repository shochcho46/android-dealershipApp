package com.example.dealernest.data.order.dto

import com.example.dealernest.domain.model.Order
import com.example.dealernest.domain.model.OrderItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Nullable leaf strings *and* id fields throughout this file (except OrderItemProductDto.id -
// see the note on OrderItemDto below), not required - this backend null-shells whole nested
// reference objects (every field null, including id) instead of returning null for the relation
// itself, confirmed on created_by/deposite_by. See .claude/docs/api-contract.md "null-safety
// audit". Fallbacks live in the toDomain() mappers below.
@Serializable
data class OrderAdminDto(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
)

@Serializable
data class OrderVendorDto(
    val id: Int? = null,
    @SerialName("shop_name") val shopName: String? = null,
    @SerialName("contact_person") val contactPerson: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    @SerialName("full_address") val fullAddress: String? = null,
    @SerialName("due_balance") val dueBalance: String? = null,
)

@Serializable
data class OrderStatusDto(
    val id: Int? = null,
    val name: String? = null,
    val color: String? = null,
)

// Only the fields the UI actually needs are mapped here - order_latitude/order_longitude/
// vendor_latitude/vendor_longitude/distance_in_meters are also in the response but their types
// aren't confirmed against the backend (Vendor's address lat/long are strings, but that's no
// guarantee these are); leaving them unparsed avoids a deserialization crash if they turn out to
// be numeric. Add them (with the right type) if a future feature needs raw coordinates.
@Serializable
data class OrderLocationDto(
    @SerialName("has_location_data") val hasLocationData: Boolean = false,
    @SerialName("distance_from_vendor") val distanceFromVendor: String? = null,
    @SerialName("google_maps_url") val googleMapsUrl: String? = null,
)

// id deliberately left required (unlike its siblings above): OrderItem.productId is actively
// used to resubmit product_id when editing/updating an order (see OrderRepositoryImpl). A
// silent fallback (e.g. 0) here could send a bogus product reference to the API and corrupt an
// edit instead of just failing loudly. If this ever null-shells in practice, the right fix is to
// exclude that line from being editable, not to default its id.
@Serializable
data class OrderItemProductDto(
    val id: Int,
    val name: String? = null,
    val image: String? = null,
)

@Serializable
data class OrderItemDto(
    val id: Int,
    val product: OrderItemProductDto,
    val quantity: Int,
    @SerialName("purchase_price") val purchasePrice: String? = null,
    @SerialName("sell_price") val sellPrice: String? = null,
    @SerialName("total_price") val totalPrice: String? = null,
    @SerialName("discount_price") val discountPrice: String? = null,
    val profit: String? = null,
)

@Serializable
data class OrderDto(
    val id: Int,
    @SerialName("invoice_id") val invoiceId: String? = null,
    val admin: OrderAdminDto? = null,
    val vendor: OrderVendorDto,
    val location: OrderLocationDto? = null,
    @SerialName("placed_by") val placedBy: OrderAdminDto? = null,
    @SerialName("order_status") val orderStatus: OrderStatusDto? = null,
    @SerialName("total_amount") val totalAmount: String? = null,
    @SerialName("paid_amount") val paidAmount: String? = null,
    @SerialName("due_amount") val dueAmount: String? = null,
    @SerialName("total_quantity") val totalQuantity: Int = 0,
    @SerialName("total_discount_amount") val totalDiscountAmount: String? = null,
    @SerialName("payment_status") val paymentStatus: Int = 0,
    @SerialName("payment_status_text") val paymentStatusText: String? = null,
    @SerialName("total_profit") val totalProfit: String? = null,
    @SerialName("can_be_cancelled") val canBeCancelled: Boolean = false,
    val items: List<OrderItemDto> = emptyList(),
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class OrderItemRequestDto(
    @SerialName("product_id") val productId: Int,
    val quantity: Int,
    @SerialName("sell_price") val sellPrice: Double,
    @SerialName("discount_price") val discountPrice: Double,
)

@Serializable
data class CreateOrderRequestDto(
    @SerialName("vendor_id") val vendorId: Int,
    @SerialName("place_by") val placeBy: Int,
    val items: List<OrderItemRequestDto>,
    // GPS capture, not manual entry - same SRS rule as Vendor create/update (see
    // VendorFormViewModel). Field naming (latitude/longitude) mirrors the response's
    // order_latitude/order_longitude (OrderLocationDto) rather than Vendor's lat/long - not
    // independently confirmed against the backend for this endpoint. Omitted (not sent) when
    // location capture fails/permission isn't granted, since these are nullable.
    val latitude: String? = null,
    val longitude: String? = null,
)

fun OrderDto.toDomain(): Order = Order(
    id = id,
    invoiceId = invoiceId ?: id.toString(),
    vendorId = vendor.id,
    vendorShopName = vendor.shopName ?: "Unnamed Vendor",
    vendorFullAddress = vendor.fullAddress,
    vendorMobile = vendor.mobile,
    vendorDueBalance = vendor.dueBalance,
    placedByName = placedBy?.name,
    orderStatusId = orderStatus?.id ?: 0,
    orderStatusName = orderStatus?.name ?: "Unknown",
    orderStatusColor = orderStatus?.color,
    totalAmount = totalAmount ?: "0.00",
    paidAmount = paidAmount ?: "0.00",
    dueAmount = dueAmount ?: "0.00",
    totalQuantity = totalQuantity,
    totalDiscountAmount = totalDiscountAmount ?: "0.00",
    canBeCancelled = canBeCancelled,
    items = items.map { it.toDomain() },
    createdAt = createdAt,
    hasLocationData = location?.hasLocationData ?: false,
    distanceFromVendor = location?.distanceFromVendor,
    googleMapsUrl = location?.googleMapsUrl,
)

fun OrderItemDto.toDomain(): OrderItem = OrderItem(
    id = id,
    productId = product.id,
    productName = product.name ?: "Unknown product",
    productImageUrl = product.image,
    quantity = quantity,
    sellPrice = sellPrice ?: "0.00",
    totalPrice = totalPrice ?: "0.00",
    discountPrice = discountPrice ?: "0.00",
)
