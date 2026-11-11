package com.example.dealernest.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.session.SessionManager
import com.example.dealernest.core.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    sessionManager: SessionManager,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
}
