package com.example.dealernest.domain.repository

import com.example.dealernest.core.location.LocationPoint
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.PagedData
import com.example.dealernest.domain.model.Order

data class OrderItemInput(
    val productId: Int,
    val quantity: Int,
    val sellPrice: Double,
    val discountPrice: Double,
)

data class OrderFilters(
    val statusFilter: Int? = null,
    val vendorFilter: Int? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
)

interface OrderRepository {
    // location: GPS captured immediately before submit, same pattern as Vendor create/update -
    // see OrderFormViewModel.submit(). Null when capture fails/permission isn't granted; the
    // request is still sent, just without latitude/longitude.
    suspend fun create(vendorId: Int, placeBy: Int, items: List<OrderItemInput>, location: LocationPoint? = null): NetworkResult<Order>
    suspend fun update(orderId: Int, vendorId: Int, placeBy: Int, items: List<OrderItemInput>, location: LocationPoint? = null): NetworkResult<Order>
    suspend fun getById(id: Int): NetworkResult<Order>

    // placeBy is supplied by the caller (the current user's id) - see the "own vs all" note in
    // .claude/docs/auth-and-permissions.md; there's no "all orders" mode in the API.
    suspend fun listByPlacedBy(placeBy: Int, filters: OrderFilters, page: Int, perPage: Int): NetworkResult<PagedData<Order>>
    suspend fun cancel(id: Int): NetworkResult<Unit>
}
