package com.example.dealernest.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private val errorBodyJson = Json { ignoreUnknownKeys = true }

/**
 * Runs a Retrofit call that returns [ApiEnvelope] and maps it to [NetworkResult]. Assumes a
 * successful envelope always carries a non-null [ApiEnvelope.data] - true for every non-list
 * endpoint used so far (login, profile, vendor CRUD). Endpoints with no meaningful payload
 * (e.g. logout) should not be routed through this helper - call them directly and
 * ignore/best-effort the result instead.
 */
suspend fun <T> safeApiCall(block: suspend () -> ApiEnvelope<T>): NetworkResult<T> {
    return try {
        val envelope = block()
        val data = envelope.data
        if (envelope.success && data != null) {
            NetworkResult.Success(data)
        } else {
            NetworkResult.Error(message = envelope.message ?: "Something went wrong")
        }
    } catch (throwable: Throwable) {
        mapNetworkException(throwable)
    }
}

/** Same as [safeApiCall] but for endpoints that return [PaginatedEnvelope] (real page cursors -
 * Orders-by-placed-by, DSR Collections - not Vendor/Product search, which are limit-only). */
suspend fun <T> safePagedApiCall(block: suspend () -> PaginatedEnvelope<T>): NetworkResult<PagedData<T>> {
    return try {
        val envelope = block()
        val data = envelope.data
        if (envelope.success && data != null) {
            NetworkResult.Success(PagedData(data, envelope.meta ?: PageMeta()))
        } else {
            NetworkResult.Error(message = envelope.message ?: "Something went wrong")
        }
    } catch (throwable: Throwable) {
        mapNetworkException(throwable)
    }
}

// internal, not private - reused by repositories whose envelope shape doesn't fit
// safeApiCall/safePagedApiCall (e.g. SalesPerformanceRepositoryImpl) but still need the same
// HttpException/IOException/SerializationException handling.
internal fun mapNetworkException(throwable: Throwable): NetworkResult<Nothing> =
    when (throwable) {
        is HttpException -> {
            val parsedError = throwable.response()?.errorBody()?.string()?.let { body ->
                runCatching { errorBodyJson.decodeFromString<ApiErrorBody>(body) }.getOrNull()
            }
            NetworkResult.Error(
                message = parsedError?.message ?: "Request failed (HTTP ${throwable.code()})",
                errors = parsedError?.errors,
                isUnauthorized = throwable.code() == 401,
            )
        }
        is IOException -> NetworkResult.Exception(throwable)
        // A response didn't match a DTO's assumed shape (wrong type, unexpectedly missing
        // field, etc.) - this has happened for real more than once against this backend (see
        // .claude/docs/api-contract.md "null-safety audit"). Degrade to a normal error instead
        // of crashing the whole app on the *next* unforeseen shape mismatch too.
        is SerializationException -> NetworkResult.Error(
            message = "The server sent data in an unexpected format. Please try again.",
        )
        else -> throw throwable
    }
