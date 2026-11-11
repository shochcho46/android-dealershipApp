package com.example.dealernest.data.salesperformance.dto

import com.example.dealernest.domain.model.SalesPerformanceEntry
import com.example.dealernest.domain.model.SalesPerformanceSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// This is its own envelope shape, not ApiEnvelope/PaginatedEnvelope - meta here carries
// system-wide totals, not page cursors (see .claude/docs/api-contract.md "Sales Performance").
@Serializable
data class SalesPerformanceResponseDto(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<SalesPerformanceEntryDto>? = null,
    val meta: SalesPerformanceMetaDto? = null,
)

@Serializable
data class SalesPerformanceSalesDto(
    val amount: String = "0.00",
    @SerialName("order_count") val orderCount: Int = 0,
    @SerialName("collections_received") val collectionsReceived: String = "0.00",
    @SerialName("due_amount") val dueAmount: String = "0.00",
    @SerialName("collection_percentage") val collectionPercentage: String = "0.00",
)

@Serializable
data class SalesPerformanceIndividualCollectionsDto(
    @SerialName("from_current_period_orders") val fromCurrentPeriodOrders: String = "0.00",
    @SerialName("from_previous_period_orders") val fromPreviousPeriodOrders: String = "0.00",
)

@Serializable
data class SalesPerformanceTargetMetricsDto(
    @SerialName("completion_percentage") val completionPercentage: String = "0.00",
    @SerialName("amount_remaining") val amountRemaining: String = "0.00",
    val status: String = "No Target Set",
)

@Serializable
data class SalesPerformanceEntryDto(
    val id: Int,
    val name: String,
    val role: String,
    val image: String? = null,
    @SerialName("sales_target") val salesTarget: String? = null,
    val sales: SalesPerformanceSalesDto = SalesPerformanceSalesDto(),
    @SerialName("individual_collections")
    val individualCollections: SalesPerformanceIndividualCollectionsDto = SalesPerformanceIndividualCollectionsDto(),
    @SerialName("dsr_collections") val dsrCollections: String = "0.00",
    @SerialName("target_metrics") val targetMetrics: SalesPerformanceTargetMetricsDto? = null,
)

@Serializable
data class SalesPerformanceMetaDto(
    @SerialName("date_from") val dateFrom: String? = null,
    @SerialName("date_to") val dateTo: String? = null,
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("total_orders_amount") val totalOrdersAmount: String = "0.00",
    @SerialName("total_collections_current_period") val totalCollectionsCurrentPeriod: String = "0.00",
    @SerialName("total_collections_previous_period") val totalCollectionsPreviousPeriod: String = "0.00",
    @SerialName("total_due_amount") val totalDueAmount: String = "0.00",
    @SerialName("collection_percentage_current_period") val collectionPercentageCurrentPeriod: String = "0.00",
    @SerialName("due_percentage") val duePercentage: String = "0.00",
)

fun SalesPerformanceEntryDto.toDomain(): SalesPerformanceEntry = SalesPerformanceEntry(
    id = id,
    name = name,
    role = role,
    imageUrl = image,
    salesTarget = salesTarget,
    salesAmount = sales.amount,
    orderCount = sales.orderCount,
    collectionsReceived = sales.collectionsReceived,
    dueAmount = sales.dueAmount,
    collectionPercentage = sales.collectionPercentage,
    dsrCollections = dsrCollections,
    targetCompletionPercentage = targetMetrics?.completionPercentage,
    targetAmountRemaining = targetMetrics?.amountRemaining,
    targetStatus = targetMetrics?.status,
)

fun SalesPerformanceMetaDto.toDomain(): SalesPerformanceSummary = SalesPerformanceSummary(
    dateFrom = dateFrom ?: "-",
    dateTo = dateTo ?: "-",
    totalUsers = totalUsers,
    totalOrdersAmount = totalOrdersAmount,
    totalCollectionsCurrentPeriod = totalCollectionsCurrentPeriod,
    totalCollectionsPreviousPeriod = totalCollectionsPreviousPeriod,
    totalDueAmount = totalDueAmount,
    collectionPercentageCurrentPeriod = collectionPercentageCurrentPeriod,
    duePercentage = duePercentage,
)
