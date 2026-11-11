package com.example.dealernest.core.session

import com.example.dealernest.domain.model.AdminProfile
import kotlinx.coroutines.flow.Flow

interface SessionManager {

    /** Loading = token saved but profile not yet fetched/saved; see saveToken/saveProfile. */
    val sessionState: Flow<SessionState>

    /**
     * Synchronous best-effort read of the last-known token, for
     * [com.example.dealernest.core.network.AuthInterceptor] only - OkHttp interceptors run
     * synchronously and cannot suspend to read DataStore directly.
     */
    fun currentTokenBlocking(): String?

    suspend fun saveToken(token: String)
    suspend fun saveProfile(profile: AdminProfile)
    suspend fun clearSession()
}
