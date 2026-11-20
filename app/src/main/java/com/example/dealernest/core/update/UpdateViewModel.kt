package com.example.dealernest.core.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateUiState {
    data object None : UpdateUiState
    data class Available(val update: AvailableUpdate) : UpdateUiState
    data class Downloading(val update: AvailableUpdate, val progress: Float) : UpdateUiState
    data class Ready(val update: AvailableUpdate, val apk: File) : UpdateUiState
    data class Failed(val update: AvailableUpdate) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: UpdateManager,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.None)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            updateManager.checkForUpdate()?.let { _state.value = UpdateUiState.Available(it) }
        }
    }

    fun startDownload(update: AvailableUpdate) {
        viewModelScope.launch {
            _state.value = UpdateUiState.Downloading(update, 0f)
            val apk = updateManager.download(update) { p ->
                _state.value = UpdateUiState.Downloading(update, p)
            }
            if (apk == null) {
                _state.value = UpdateUiState.Failed(update)
            } else {
                _state.value = UpdateUiState.Ready(update, apk)
                updateManager.install(apk)
            }
        }
    }

    fun install(apk: File) {
        updateManager.install(apk)
    }

    fun dismiss() {
        _state.value = UpdateUiState.None
    }
}
