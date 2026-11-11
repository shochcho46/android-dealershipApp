package com.example.dealernest.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class ApiErrorBody(
    val success: Boolean = false,
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
)
