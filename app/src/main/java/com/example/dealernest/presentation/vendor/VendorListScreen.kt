package com.example.dealernest.presentation.vendor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.dealernest.core.qr.QrScannerScreen
import com.example.dealernest.domain.model.Vendor
import com.example.dealernest.presentation.components.AppSearchField
import com.example.dealernest.presentation.components.EmptyStateView
import com.example.dealernest.presentation.components.ErrorStateView
import com.example.dealernest.presentation.components.InfiniteScrollHandler
import com.example.dealernest.presentation.components.SkeletonList
import com.example.dealernest.presentation.components.VendorDueBadge
import com.example.dealernest.ui.theme.ActionBlue
import com.example.dealernest.ui.theme.GradientPaidEnd
import com.example.dealernest.ui.theme.GradientUnpaidEnd
import com.example.dealernest.ui.theme.GradientVioletEnd
import com.example.dealernest.ui.theme.OnActionBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorListScreen(
    onVendorClick: (Int) -> Unit,
    onAddVendorClick: () -> Unit,
    viewModel: VendorListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }

    // Refetches whenever this screen's backstack entry resumes - e.g. popping back after
    // creating a vendor - since the ViewModel instance survives that round-trip and its init{}
    // fetch only ran once. Same refresh() the pull-to-refresh gesture already calls.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddVendorClick,
                containerColor = ActionBlue,
                contentColor = OnActionBlue,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add vendor")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AppSearchField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search vendors",
                onQrClick = { showScanner = true },
                modifier = Modifier.padding(16.dp),
            )

            when (val state = uiState) {
                is VendorListUiState.Loading -> SkeletonList(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                is VendorListUiState.Empty -> EmptyStateView(
                    icon = Icons.Filled.Store,
                    title = "No vendors found",
                    subtitle = "Try a different search or scan a vendor's QR code.",
                )
                is VendorListUiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = viewModel::refresh,
                )
                is VendorListUiState.Success -> {
                    val listState = rememberLazyListState()
                    InfiniteScrollHandler(
                        listState = listState,
                        canLoadMore = state.canLoadMore,
                        onLoadMore = viewModel::loadMore,
                    )
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        ) {
                            if (state.isFromCache) {
                                item {
                                    Text(
                                        "Showing cached results — you're offline.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                }
                            }
                            items(state.vendors, key = { it.id }) { vendor ->
                                VendorCard(vendor = vendor, onClick = { onVendorClick(vendor.id) })
                            }
                            if (state.canLoadMore) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val VendorAccent = GradientVioletEnd

@Composable
private fun VendorCard(vendor: Vendor, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(VendorAccent))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            if (vendor.imageUrl != null) {
                AsyncImage(
                    model = vendor.imageUrl,
                    contentDescription = vendor.shopName,
                    modifier = Modifier.size(52.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(VendorAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Store, contentDescription = null, tint = VendorAccent)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(vendor.shopName, style = MaterialTheme.typography.titleMedium)
                Text(
                    vendor.mobile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (vendor.fullAddress != null) {
                    Text(
                        vendor.fullAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                VendorDueBadge(dueBalance = vendor.dueBalance)
                if (vendor.locationStatus != null) {
                    Spacer(Modifier.height(6.dp))
                    LocationStatusChip(hasLocation = vendor.hasLocationSet, colorHex = vendor.locationStatusColor)
                }
            }
        }
    }
}

// Server-computed "yes"/"no" + hex color (VendorAddressDto.location_status/location_status_color)
// - lets the list flag at a glance which vendors still need a GPS update, without opening each
// one. Falls back to a plain green/red only if the backend didn't send a color.
@Composable
private fun LocationStatusChip(hasLocation: Boolean, colorHex: String?) {
    val color = colorHex?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
    } ?: if (hasLocation) GradientPaidEnd else GradientUnpaidEnd
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            if (hasLocation) "Yes" else "No",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
