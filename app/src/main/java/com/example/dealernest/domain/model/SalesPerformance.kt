package com.example.dealernest.domain.model

data class SalesPerformanceEntry(
    val id: Int,
    val name: String,
    val role: String,
    val imageUrl: String?,
    val salesTarget: String?,
    val salesAmount: String,
    val orderCount: Int,
    val collectionsReceived: String,
    val dueAmount: String,
    val collectionPercentage: String,
    val dsrCollections: String,
    // Null for DSR users - only SR users have a target, per the API docs.
    val targetCompletionPercentage: String?,
    val targetAmountRemaining: String?,
    val targetStatus: String?,
)

data class SalesPerformanceSummary(
    val dateFrom: String,
    val dateTo: String,
    val totalUsers: Int,
    val totalOrdersAmount: String,
    val totalCollectionsCurrentPeriod: String,
    val totalCollectionsPreviousPeriod: String,
    val totalDueAmount: String,
    val collectionPercentageCurrentPeriod: String,
    val duePercentage: String,
)

data class SalesPerformance(
    val entries: List<SalesPerformanceEntry>,
    val summary: SalesPerformanceSummary,
)
