package com.example.dealernest.presentation.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dealernest.domain.model.Order
import com.example.dealernest.domain.model.OrderPaymentStatus
import com.example.dealernest.presentation.components.DateRangeFilterRow
import com.example.dealernest.presentation.components.EmptyStateView
import com.example.dealernest.presentation.components.ErrorStateView
import com.example.dealernest.presentation.components.InfiniteScrollHandler
import com.example.dealernest.presentation.components.OrderLocationButton
import com.example.dealernest.presentation.components.SkeletonList
import com.example.dealernest.ui.theme.ActionBlue
import com.example.dealernest.ui.theme.GradientPaidEnd
import com.example.dealernest.ui.theme.GradientPartialEnd
import com.example.dealernest.ui.theme.GradientUnpaidEnd
import com.example.dealernest.ui.theme.OnActionBlue

@Composable
fun OrderListScreen(
    onAddOrderClick: () -> Unit,
    onOrderClick: (Int) -> Unit,
    onEditOrderClick: (Int) -> Unit,
    viewModel: OrderListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFromMillis by viewModel.dateFromMillis.collectAsStateWithLifecycle()
    val dateToMillis by viewModel.dateToMillis.collectAsStateWithLifecycle()
    var pendingCancelId by remember { mutableStateOf<Int?>(null) }

    // Refetches whenever this screen's backstack entry resumes - e.g. popping back after
    // creating an order - since the ViewModel instance survives that round-trip and its init{}
    // fetch only ran once. Same refresh() the pull-to-refresh gesture already calls.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddOrderClick,
                containerColor = ActionBlue,
                contentColor = OnActionBlue,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New order")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            DateRangeFilterRow(
                dateFromMillis = dateFromMillis,
                dateToMillis = dateToMillis,
                onDateFromChange = viewModel::onDateFromChange,
                onDateToChange = viewModel::onDateToChange,
                modifier = Modifier.padding(16.dp),
            )

            when (val state = uiState) {
                is OrderListUiState.Loading -> SkeletonList(modifier = Modifier.padding(horizontal = 16.dp))
                is OrderListUiState.Empty -> EmptyStateView(
                    icon = Icons.Filled.ShoppingCart,
                    title = "No orders found",
                    subtitle = "Try a different date range or create a new order.",
                )
                is OrderListUiState.Error -> ErrorStateView(message = state.message, onRetry = viewModel::refresh)
                is OrderListUiState.Success -> {
                    val listState = rememberLazyListState()
                    val canLoadMore = state.page < state.lastPage
                    InfiniteScrollHandler(listState = listState, canLoadMore = canLoadMore, onLoadMore = viewModel::loadNextPage)
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    ) {
                        items(state.orders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onClick = { onOrderClick(order.id) },
                                onEditClick = { onEditOrderClick(order.id) },
                                onCancelClick = { pendingCancelId = order.id },
                            )
                        }
                        if (canLoadMore) {
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

    val cancelId = pendingCancelId
    if (cancelId != null) {
        AlertDialog(
            onDismissRequest = { pendingCancelId = null },
            title = { Text("Cancel this order?") },
            text = { Text("Stock will be restored. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelOrder(cancelId)
                    pendingCancelId = null
                }) { Text("Cancel Order") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelId = null }) { Text("Dismiss") }
            },
        )
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit, onEditClick: () -> Unit, onCancelClick: () -> Unit) {
    val accentColor = when (order.paymentStatus) {
        OrderPaymentStatus.PAID -> GradientPaidEnd
        OrderPaymentStatus.PARTIAL -> GradientPartialEnd
        OrderPaymentStatus.UNPAID -> GradientUnpaidEnd
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(accentColor))
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.invoiceId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        order.vendorShopName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OrderStatusChip(name = order.orderStatusName, colorHex = order.orderStatusColor)
                if (order.isEditable) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit order")
                    }
                }
            }
            if (order.googleMapsUrl != null) {
                Spacer(Modifier.height(8.dp))
                OrderLocationButton(distanceText = order.distanceFromVendor, mapsUrl = order.googleMapsUrl)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AmountColumn(label = "Total", value = order.totalAmount, valueColor = MaterialTheme.colorScheme.onSurface)
                AmountColumn(label = "Paid", value = order.paidAmount, valueColor = MaterialTheme.colorScheme.onSurface)
                AmountColumn(label = "Due", value = order.dueAmount, valueColor = accentColor, emphasize = true)
            }
            if (order.canBeCancelled) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onCancelClick) { Text("Cancel Order") }
            }
        }
    }
}

@Composable
private fun AmountColumn(label: String, value: String, valueColor: Color, emphasize: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            color = valueColor,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun OrderStatusChip(name: String, colorHex: String?) {
    val color = colorHex?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
    } ?: MaterialTheme.colorScheme.primary
    Text(
        name,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
