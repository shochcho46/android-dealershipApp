package com.example.dealernest.data.order

import com.example.dealernest.core.location.LocationPoint
import com.example.dealernest.core.network.ApiService
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.PagedData
import com.example.dealernest.core.network.mapSuccess
import com.example.dealernest.core.network.safeApiCall
import com.example.dealernest.core.network.safePagedApiCall
import com.example.dealernest.data.order.dto.CreateOrderRequestDto
import com.example.dealernest.data.order.dto.OrderItemRequestDto
import com.example.dealernest.data.order.dto.toDomain
import com.example.dealernest.domain.model.Order
import com.example.dealernest.domain.repository.OrderFilters
import com.example.dealernest.domain.repository.OrderItemInput
import com.example.dealernest.domain.repository.OrderRepository
import java.io.IOException
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : OrderRepository {

    override suspend fun create(
        vendorId: Int,
        placeBy: Int,
        items: List<OrderItemInput>,
        location: LocationPoint?,
    ): NetworkResult<Order> =
        safeApiCall { apiService.createOrder(buildRequest(vendorId, placeBy, items, location)) }
            .mapSuccess { it.toDomain() }

    override suspend fun update(
        orderId: Int,
        vendorId: Int,
        placeBy: Int,
        items: List<OrderItemInput>,
        location: LocationPoint?,
    ): NetworkResult<Order> =
        safeApiCall { apiService.updateOrder(orderId, buildRequest(vendorId, placeBy, items, location)) }
            .mapSuccess { it.toDomain() }

    override suspend fun getById(id: Int): NetworkResult<Order> =
        safeApiCall { apiService.getOrder(id) }.mapSuccess { it.toDomain() }

    override suspend fun listByPlacedBy(
        placeBy: Int,
        filters: OrderFilters,
        page: Int,
        perPage: Int,
    ): NetworkResult<PagedData<Order>> = safePagedApiCall {
        apiService.listOrdersByPlacedBy(
            placeBy = placeBy,
            statusFilter = filters.statusFilter,
            vendorFilter = filters.vendorFilter,
            dateFrom = filters.dateFrom,
            dateTo = filters.dateTo,
            page = page,
            perPage = perPage,
        )
    }.mapSuccess { paged -> PagedData(paged.items.map { it.toDomain() }, paged.meta) }

    override suspend fun cancel(id: Int): NetworkResult<Unit> = try {
        val response = apiService.cancelOrder(id)
        if (response.isSuccessful) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(
                message = "Request failed (HTTP ${response.code()})",
                isUnauthorized = response.code() == 401,
            )
        }
    } catch (e: IOException) {
        NetworkResult.Exception(e)
    }

    private fun buildRequest(vendorId: Int, placeBy: Int, items: List<OrderItemInput>, location: LocationPoint?) = CreateOrderRequestDto(
        vendorId = vendorId,
        placeBy = placeBy,
        items = items.map {
            OrderItemRequestDto(
                productId = it.productId,
                quantity = it.quantity,
                sellPrice = it.sellPrice,
                discountPrice = it.discountPrice,
            )
        },
        latitude = location?.latitude?.toString(),
        longitude = location?.longitude?.toString(),
    )
}
