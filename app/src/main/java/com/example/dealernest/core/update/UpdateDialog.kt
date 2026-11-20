package com.example.dealernest.core.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Shows the "new version" prompt on top of whatever screen is visible. */
@Composable
fun UpdateDialogHost(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    when (val s = state) {
        UpdateUiState.None -> Unit
        is UpdateUiState.Available -> UpdateDialog(
            update = s.update,
            confirmLabel = "Update now",
            onConfirm = { viewModel.startDownload(s.update) },
            onDismiss = viewModel::dismiss,
        )
        is UpdateUiState.Failed -> UpdateDialog(
            update = s.update,
            message = "Download failed. Check your connection and try again.",
            confirmLabel = "Retry",
            onConfirm = { viewModel.startDownload(s.update) },
            onDismiss = viewModel::dismiss,
        )
        is UpdateUiState.Ready -> UpdateDialog(
            update = s.update,
            message = "Downloaded. Tap Install to finish updating.",
            confirmLabel = "Install",
            onConfirm = { viewModel.install(s.apk) },
            onDismiss = viewModel::dismiss,
        )
        is UpdateUiState.Downloading -> UpdateDialog(
            update = s.update,
            confirmLabel = null,
            onConfirm = {},
            onDismiss = {},
            progress = s.progress,
        )
    }
}

@Composable
private fun UpdateDialog(
    update: AvailableUpdate,
    confirmLabel: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    progress: Float? = null,
) {
    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = { Text("Update available - v${update.versionName}") },
        text = {
            Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                message?.let { Text(it) }
                if (message == null && progress == null) {
                    Text(update.notes.ifBlank { "A new version of DealerNest is ready to install." })
                }
                if (progress != null) {
                    Text("Downloading...")
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (confirmLabel != null) TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            if (progress == null) TextButton(onClick = onDismiss) { Text("Later") }
        },
    )
}
