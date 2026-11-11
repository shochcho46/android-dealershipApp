package com.example.dealernest.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(emailOrPhone: String, password: String) {
        if (emailOrPhone.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your email/phone and password")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = authRepository.login(emailOrPhone, password)) {
                is NetworkResult.Success -> LoginUiState.Success
                is NetworkResult.Error -> LoginUiState.Error(result.message)
                is NetworkResult.Exception -> LoginUiState.Error(
                    "Couldn't reach the server. Check your connection and try again.",
                )
            }
        }
    }

    fun resetError() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}
