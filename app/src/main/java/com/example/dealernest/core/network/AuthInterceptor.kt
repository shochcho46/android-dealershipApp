package com.example.dealernest.core.network

import com.example.dealernest.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

private val NO_AUTH_PATHS = setOf("v1/admin/login")

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.trimStart('/')
        if (NO_AUTH_PATHS.any { path.endsWith(it) }) {
            return chain.proceed(request)
        }

        val token = sessionManager.currentTokenBlocking()
        val authorizedRequest = if (!token.isNullOrBlank()) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(authorizedRequest)
    }
}
