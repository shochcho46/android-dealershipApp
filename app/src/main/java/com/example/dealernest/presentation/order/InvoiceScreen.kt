package com.example.dealernest.presentation.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.dealernest.domain.model.Order
import com.example.dealernest.domain.model.OrderItem
import com.example.dealernest.domain.model.OrderPaymentStatus
import com.example.dealernest.presentation.components.ErrorStateView
import com.example.dealernest.presentation.components.OrderLocationButton
import com.example.dealernest.presentation.components.SectionCard
import com.example.dealernest.presentation.components.SkeletonList
import com.example.dealernest.presentation.components.StatusBadge
import com.example.dealernest.presentation.components.StatusTone
import com.example.dealernest.ui.theme.AppGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    orderId: Int,
    onBack: () -> Unit,
    viewModel: InvoiceViewModel = hiltViewModel(),
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is InvoiceUiState.Loading -> SkeletonList(itemCount = 4, modifier = Modifier.padding(16.dp))
                is InvoiceUiState.Error -> ErrorStateView(message = state.message, onRetry = { viewModel.retry(orderId) })
                is InvoiceUiState.Success -> InvoiceContent(order = state.order)
            }
        }
    }
}

@Composable
private fun InvoiceContent(order: Order) {
    // Confirmed against a real API response: total_amount is already net of discount (it equals
    // the sum of each item's total_price, which is sell_price*qty - discount_price) and is
    // exactly what's payable. The gross, pre-discount total - shown as "Total" above the
    // discount line - is total_amount + total_discount_amount, not total_amount alone.
    val grossTotal = (order.totalAmount.toDoubleOrNull() ?: 0.0) + (order.totalDiscountAmount.toDoubleOrNull() ?: 0.0)

    val (statusLabel, statusTone) = when (order.paymentStatus) {
        OrderPaymentStatus.UNPAID -> "Unpaid" to StatusTone.DANGER
        OrderPaymentStatus.PARTIAL -> "Partial" to StatusTone.WARNING
        OrderPaymentStatus.PAID -> "Paid" to StatusTone.SUCCESS
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppGradients.violet, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(order.invoiceId, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            order.createdAt?.take(10) ?: "-",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    StatusBadge(text = statusLabel, tone = statusTone)
                }
                if (order.placedByName != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sales Rep: ${order.placedByName}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text("Vendor", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            Text(order.vendorShopName, style = MaterialTheme.typography.bodyLarge)
            if (order.vendorFullAddress != null) Text(order.vendorFullAddress, style = MaterialTheme.typography.bodyMedium)
            if (order.vendorMobile != null) Text(order.vendorMobile, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Vendor due balance: ${order.vendorDueBalance ?: "0.00"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (order.googleMapsUrl != null) {
                Spacer(Modifier.height(12.dp))
                OrderLocationButton(distanceText = order.distanceFromVendor, mapsUrl = order.googleMapsUrl)
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text("Items", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            InvoiceTableHeader()
            HorizontalDivider()
            order.items.forEach { item -> InvoiceItemRow(item) }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text("Summary", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            SummaryRow("Total", "%.2f".format(grossTotal))
            SummaryRow("Discount", order.totalDiscountAmount)
            SummaryRow("Payable Amount", order.totalAmount)
            SummaryRow("Paid Amount", order.paidAmount)
            SummaryRow("Remaining Due", order.dueAmount, emphasize = true)
        }
    }
}

@Composable
private fun InvoiceTableHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("Product", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f))
        Text("Qty", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text("Price", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text("Amount", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InvoiceItemRow(item: OrderItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
            val thumbShape = RoundedCornerShape(8.dp)
            if (item.productImageUrl != null) {
                AsyncImage(
                    model = item.productImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(thumbShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.size(36.dp).clip(thumbShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                item.productName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(item.quantity.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(item.sellPrice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(item.totalPrice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasize) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
