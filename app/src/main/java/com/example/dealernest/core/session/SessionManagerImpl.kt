package com.example.dealernest.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.dealernest.domain.model.AdminProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val TOKEN_KEY = stringPreferencesKey("auth_token")
private val PROFILE_KEY = stringPreferencesKey("admin_profile")

// Persistence-only mirror of AdminProfile. Kept private to this file (rather than reusing the
// domain model directly) so `domain/` stays free of serialization annotations - see
// .claude/docs/architecture.md.
@Serializable
private data class StoredProfile(
    val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val status: Boolean = true,
    val statusText: String = "Active",
    val pictureUrl: String? = null,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val createdAt: String? = null,
)

private fun StoredProfile.toDomain() = AdminProfile(
    id = id,
    name = name,
    email = email,
    phone = phone,
    status = status,
    statusText = statusText,
    pictureUrl = pictureUrl,
    roles = roles,
    permissions = permissions,
    createdAt = createdAt,
)

private fun AdminProfile.toStored() = StoredProfile(
    id = id,
    name = name,
    email = email,
    phone = phone,
    status = status,
    statusText = statusText,
    pictureUrl = pictureUrl,
    roles = roles,
    permissions = permissions,
    createdAt = createdAt,
)

@Singleton
class SessionManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionManager {

    private val json = Json { ignoreUnknownKeys = true }

    // Interceptors can't suspend, so the token is mirrored into memory for
    // currentTokenBlocking() by this background collector.
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedToken: String? = null

    init {
        dataStore.data
            .map { it[TOKEN_KEY] }
            .onEach { cachedToken = it }
            .launchIn(backgroundScope)
    }

    override val sessionState: Flow<SessionState> = dataStore.data.map { prefs ->
        val token = prefs[TOKEN_KEY]
        val profileJson = prefs[PROFILE_KEY]
        when {
            token.isNullOrBlank() -> SessionState.LoggedOut
            profileJson.isNullOrBlank() -> SessionState.Loading
            else -> runCatching { json.decodeFromString<StoredProfile>(profileJson).toDomain() }
                .fold(
                    onSuccess = { SessionState.LoggedIn(it) },
                    onFailure = { SessionState.LoggedOut },
                )
        }
    }

    override fun currentTokenBlocking(): String? = cachedToken

    override suspend fun saveToken(token: String) {
        dataStore.edit { it[TOKEN_KEY] = token }
    }

    override suspend fun saveProfile(profile: AdminProfile) {
        dataStore.edit { it[PROFILE_KEY] = json.encodeToString(profile.toStored()) }
    }

    override suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}
