package com.example.dealernest.core.session

import com.example.dealernest.domain.model.AdminProfile

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val profile: AdminProfile) : SessionState
}
