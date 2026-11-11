package com.example.dealernest.core.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T, val isFromCache: Boolean = false) : NetworkResult<T>()

    data class Error(
        val message: String,
        val errors: Map<String, List<String>>? = null,
        val isUnauthorized: Boolean = false,
    ) : NetworkResult<Nothing>()

    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()
}

/** Maps a [NetworkResult.Success]'s payload (e.g. DTO -> domain model), passing Error/Exception through untouched. */
inline fun <T, R> NetworkResult<T>.mapSuccess(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data), isFromCache)
    is NetworkResult.Error -> this
    is NetworkResult.Exception -> this
}
