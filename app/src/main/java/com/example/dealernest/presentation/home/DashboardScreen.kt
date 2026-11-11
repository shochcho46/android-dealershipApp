package com.example.dealernest.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.dealernest.domain.model.DsrCollection
import com.example.dealernest.domain.model.Order
import com.example.dealernest.domain.model.OrderPaymentStatus
import com.example.dealernest.domain.model.SalesPerformance
import com.example.dealernest.domain.model.SalesPerformanceEntry
import com.example.dealernest.domain.model.SalesPerformanceSummary
import com.example.dealernest.presentation.components.DateRangeFilterRow
import com.example.dealernest.presentation.components.ErrorStateView
import com.example.dealernest.presentation.components.SectionCard
import com.example.dealernest.presentation.components.SimpleBarChart
import com.example.dealernest.presentation.components.SkeletonList
import com.example.dealernest.presentation.components.StatCard
import com.example.dealernest.presentation.components.StatusBadge
import com.example.dealernest.presentation.components.StatusTone
import com.example.dealernest.ui.theme.AppGradients
import com.example.dealernest.ui.theme.GradientPaidEnd
import com.example.dealernest.ui.theme.GradientPartialEnd
import com.example.dealernest.ui.theme.GradientTealEnd
import com.example.dealernest.ui.theme.GradientUnpaidEnd
import com.example.dealernest.ui.theme.StatusBlue
import com.example.dealernest.ui.theme.StatusGreen
import com.example.dealernest.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCreateOrderClick: () -> Unit,
    onCreateCollectionClick: () -> Unit,
    onAddVendorClick: () -> Unit,
    onOrderClick: (Int) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val salesPerformanceDateFromMillis by viewModel.salesPerformanceDateFromMillis.collectAsStateWithLifecycle()
    val salesPerformanceDateToMillis by viewModel.salesPerformanceDateToMillis.collectAsStateWithLifecycle()
    val isSalesPerformanceRefreshing by viewModel.isSalesPerformanceRefreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Notification channel is created in DealerNestApp.onCreate(); this is the runtime
    // permission half (Android 13+ requirement) - see NotificationHelper. Push itself stays
    // blocked pending Firebase credentials.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op - nothing to react to until FCM is wired in */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> SkeletonList(itemCount = 4, modifier = Modifier.padding(16.dp))
                is DashboardUiState.Error -> ErrorStateView(message = state.message, onRetry = viewModel::refresh)
                is DashboardUiState.Success -> DashboardContent(
                    state = state,
                    onCreateOrderClick = onCreateOrderClick,
                    onCreateCollectionClick = onCreateCollectionClick,
                    onAddVendorClick = onAddVendorClick,
                    onOrderClick = onOrderClick,
                    salesPerformanceDateFromMillis = salesPerformanceDateFromMillis,
                    salesPerformanceDateToMillis = salesPerformanceDateToMillis,
                    isSalesPerformanceRefreshing = isSalesPerformanceRefreshing,
                    onSalesPerformanceDateFromChange = viewModel::onSalesPerformanceDateFromChange,
                    onSalesPerformanceDateToChange = viewModel::onSalesPerformanceDateToChange,
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    onCreateOrderClick: () -> Unit,
    onCreateCollectionClick: () -> Unit,
    onAddVendorClick: () -> Unit,
    onOrderClick: (Int) -> Unit,
    salesPerformanceDateFromMillis: Long?,
    salesPerformanceDateToMillis: Long?,
    isSalesPerformanceRefreshing: Boolean,
    onSalesPerformanceDateFromChange: (Long?) -> Unit,
    onSalesPerformanceDateToChange: (Long?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Quick Actions sits first, not after the stats/charts, so New Order/New Collection are
        // reachable without scrolling - see .claude/docs/roadmap.md Phase 4 follow-up.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionButton("New Order", Icons.Filled.ShoppingCart, AppGradients.ocean, onCreateOrderClick, Modifier.weight(1f))
            QuickActionButton("New Collection", Icons.Filled.Payments, AppGradients.teal, onCreateCollectionClick, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        QuickActionButton("Add Vendor", Icons.Filled.AddBusiness, AppGradients.violet, onAddVendorClick, Modifier.fillMaxWidth())

        if (state.salesPerformance != null) {
            Spacer(Modifier.height(24.dp))
            SalesPerformanceSection(
                data = state.salesPerformance,
                dateFromMillis = salesPerformanceDateFromMillis,
                dateToMillis = salesPerformanceDateToMillis,
                isRefreshing = isSalesPerformanceRefreshing,
                onDateFromChange = onSalesPerformanceDateFromChange,
                onDateToChange = onSalesPerformanceDateToChange,
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Today", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Orders (yours)",
                value = state.stats.todayOrdersCount.toString(),
                icon = Icons.Filled.ShoppingCart,
                gradient = AppGradients.violet,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Sales (yours)",
                value = "%.2f".format(state.stats.todaySalesTotal),
                icon = Icons.Filled.PointOfSale,
                gradient = AppGradients.ocean,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Collections (all)",
                value = "%.2f".format(state.stats.todayCollectionsTotal),
                icon = Icons.Filled.Payments,
                gradient = AppGradients.teal,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Products Sold (yours)",
                value = state.stats.todayProductsSold.toString(),
                icon = Icons.Filled.Inventory2,
                gradient = AppGradients.sunset,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Weekly Sales", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            SimpleBarChart(data = state.chart.salesByDay, barColor = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))
        Text("Weekly Collections", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            SimpleBarChart(data = state.chart.collectionsByDay, barColor = StatusGreen)
        }

        Spacer(Modifier.height(24.dp))
        Text("Recent Orders", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (state.recentOrders.isEmpty()) {
            Text(
                "No recent orders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.recentOrders.forEach { order -> RecentOrderRow(order, onClick = { onOrderClick(order.id) }) }
        }

        Spacer(Modifier.height(16.dp))
        Text("Recent Collections", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (state.recentCollections.isEmpty()) {
            Text(
                "No recent collections.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.recentCollections.forEach { collection -> RecentCollectionRow(collection) }
        }
    }
}

@Composable
private fun QuickActionButton(label: String, icon: ImageVector, gradient: Brush, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(gradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RecentOrderRow(order: Order, onClick: () -> Unit) {
    val accentColor = when (order.paymentStatus) {
        OrderPaymentStatus.PAID -> GradientPaidEnd
        OrderPaymentStatus.PARTIAL -> GradientPartialEnd
        OrderPaymentStatus.UNPAID -> GradientUnpaidEnd
    }
    SectionCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        contentPadding = 12,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(order.invoiceId, style = MaterialTheme.typography.titleSmall)
                Text(
                    order.vendorShopName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(order.dueAmount, style = MaterialTheme.typography.titleSmall, color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecentCollectionRow(collection: DsrCollection) {
    SectionCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentPadding = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GradientTealEnd))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(collection.vendorShopName, style = MaterialTheme.typography.titleSmall)
                Text(
                    collection.collectionDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(collection.amount, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// The chart is system-wide (meta totals) regardless of tab - the SR/DSR tabs only filter which
// individual entries the detail cards below show, per the user's ask to decouple the two.
@Composable
private fun SalesPerformanceSection(
    data: SalesPerformance,
    dateFromMillis: Long?,
    dateToMillis: Long?,
    isRefreshing: Boolean,
    onDateFromChange: (Long?) -> Unit,
    onDateToChange: (Long?) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Sales Performance", style = MaterialTheme.typography.titleMedium)
        if (isRefreshing) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        }
    }
    Spacer(Modifier.height(8.dp))

    DateRangeFilterRow(
        dateFromMillis = dateFromMillis,
        dateToMillis = dateToMillis,
        onDateFromChange = onDateFromChange,
        onDateToChange = onDateToChange,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem("Sales", StatusBlue)
        LegendItem("Collected", StatusGreen)
        LegendItem("Due", StatusRed)
    }
    Spacer(Modifier.height(12.dp))
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        MetaSummaryBarChart(summary = data.summary, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
    }

    Spacer(Modifier.height(20.dp))
    TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("SR") })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DSR") })
    }
    Spacer(Modifier.height(12.dp))

    val roleKey = if (selectedTab == 0) "sr" else "dsr"
    val entries = data.entries.filter { it.role.equals(roleKey, ignoreCase = true) }

    if (entries.isEmpty()) {
        Text(
            "No ${roleKey.uppercase()} users to show.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        entries.forEach { entry -> SalesPerformanceEntryCard(entry) }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// System-wide totals from the API's own `meta` block (Total Sales/Collected/Due for the
// selected date range) - not derived from, or filtered by, individual entries. Same
// fillMaxHeight-fraction technique as SimpleBarChart, no charting library needed.
@Composable
private fun MetaSummaryBarChart(summary: SalesPerformanceSummary, modifier: Modifier = Modifier) {
    val sales = summary.totalOrdersAmount.toDoubleOrNull() ?: 0.0
    val collected = summary.totalCollectionsCurrentPeriod.toDoubleOrNull() ?: 0.0
    val due = summary.totalDueAmount.toDoubleOrNull() ?: 0.0
    val maxValue = listOf(sales, collected, due).maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        MetaBarColumn(label = "Sales", value = sales, maxValue = maxValue, color = StatusBlue)
        MetaBarColumn(label = "Collected", value = collected, maxValue = maxValue, color = StatusGreen)
        MetaBarColumn(label = "Due", value = due, maxValue = maxValue, color = StatusRed)
    }
}

@Composable
private fun MetaBarColumn(label: String, value: Double, maxValue: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "%.2f".format(value),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        ChartBar(value = value, maxValue = maxValue, color = color, maxBarHeight = 130.dp, barWidth = 32.dp)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChartBar(value: Double, maxValue: Double, color: Color, maxBarHeight: Dp = 100.dp, barWidth: Dp = 14.dp) {
    Box(
        modifier = Modifier.height(maxBarHeight).width(barWidth),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val fraction = (value / maxValue).toFloat().coerceIn(0.02f, 1f)
        Box(
            modifier = Modifier
                .fillMaxHeight(fraction)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
    }
}

@Composable
private fun SalesPerformanceEntryCard(entry: SalesPerformanceEntry) {
    SectionCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentPadding = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (entry.imageUrl != null) {
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = entry.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(text = entry.role.uppercase(), tone = StatusTone.INFO)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AmountFigure(label = "Sales", value = entry.salesAmount, color = StatusBlue)
                    AmountFigure(label = "Collected", value = "%.2f".format(collectedAmount(entry)), color = StatusGreen)
                    AmountFigure(label = "Due", value = entry.dueAmount, color = StatusRed)
                }
                // Only SR users carry a target - DSR users have targetStatus == null and skip
                // this whole block rather than showing a fake 0% target. sales_target itself can
                // still be null even when target_metrics isn't (means "no target set" - amount_
                // remaining is 0 in that case), so it's only shown when an actual figure exists.
                if (entry.targetStatus != null) {
                    Spacer(Modifier.height(8.dp))
                    if (entry.salesTarget != null) {
                        AmountFigure(label = "Target", value = entry.salesTarget, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                    }
                    val fraction = ((entry.targetCompletionPercentage?.toFloatOrNull() ?: 0f) / 100f).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Remaining: ${entry.targetAmountRemaining ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        StatusBadge(text = entry.targetStatus, tone = targetStatusTone(entry.targetStatus))
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountFigure(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun targetStatusTone(status: String): StatusTone = when (status) {
    "Achieved" -> StatusTone.SUCCESS
    "In Progress" -> StatusTone.INFO
    else -> StatusTone.NEUTRAL
}

// individual_collections (their own orders) doesn't cover DSR-only collection activity, and
// sales.collections_received doesn't cover it either (that's collections against their orders
// by anyone) - dsr_collections is the field that does, so both figure into "money this person
// actually collected" regardless of which role they are.
private fun collectedAmount(entry: SalesPerformanceEntry): Double =
    (entry.collectionsReceived.toDoubleOrNull() ?: 0.0) + (entry.dsrCollections.toDoubleOrNull() ?: 0.0)
