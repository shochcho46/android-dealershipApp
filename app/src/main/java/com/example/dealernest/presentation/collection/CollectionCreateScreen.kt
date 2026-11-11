package com.example.dealernest.presentation.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dealernest.core.qr.QrScannerScreen
import com.example.dealernest.domain.model.Vendor
import com.example.dealernest.presentation.components.AppSearchField
import com.example.dealernest.presentation.components.InfiniteScrollHandler
import com.example.dealernest.presentation.components.PrimaryButton
import com.example.dealernest.presentation.components.SectionCard
import com.example.dealernest.presentation.components.SkeletonList
import com.example.dealernest.presentation.components.VendorDueBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionCreateScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: CollectionCreateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }

    if (showScanner) {
        QrScannerScreen(
            onResult = { value ->
                showScanner = false
                viewModel.onQrScanned(value)
            },
            onClose = { showScanner = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Collection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val vendor = uiState.selectedVendor
        if (vendor == null) {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
                Text("Step 1 · Select vendor", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                AppSearchField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Search vendors",
                    onQrClick = { showScanner = true },
                )
                Spacer(Modifier.height(12.dp))
                if (uiState.errorMessage != null) {
                    Text(
                        uiState.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (uiState.isSearching) {
                    SkeletonList(itemCount = 3)
                } else {
                    val vendorListState = rememberLazyListState()
                    InfiniteScrollHandler(
                        listState = vendorListState,
                        canLoadMore = uiState.canLoadMoreVendors,
                        onLoadMore = viewModel::loadMoreVendors,
                    )
                    LazyColumn(state = vendorListState, contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(uiState.vendorResults, key = { it.id }) { result ->
                            VendorLiteRow(vendor = result, onClick = { viewModel.onVendorSelected(result) })
                        }
                        if (uiState.canLoadMoreVendors) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
                Text("Step 2 · Collection details", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vendor.shopName, style = MaterialTheme.typography.titleMedium)
                            VendorDueBadge(dueBalance = vendor.dueBalance, modifier = Modifier.padding(top = 4.dp))
                        }
                        TextButton(onClick = viewModel::onChangeVendor) { Text("Change") }
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Amount *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Note (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        uiState.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(24.dp))
                PrimaryButton(text = "Save Collection", onClick = viewModel::submit, loading = uiState.isSaving)
            }
        }
    }
}

@Composable
private fun VendorLiteRow(vendor: Vendor, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vendor.shopName, style = MaterialTheme.typography.titleMedium)
                Text(
                    vendor.mobile ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            VendorDueBadge(dueBalance = vendor.dueBalance)
        }
    }
}
