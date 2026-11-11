package com.example.dealernest.presentation.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.dealernest.core.qr.QrScannerScreen
import com.example.dealernest.domain.model.Product
import com.example.dealernest.domain.model.Vendor
import com.example.dealernest.presentation.components.AppSearchField
import com.example.dealernest.presentation.components.InfiniteScrollHandler
import com.example.dealernest.presentation.components.InfoRow
import com.example.dealernest.presentation.components.PrimaryButton
import com.example.dealernest.presentation.components.SectionCard
import com.example.dealernest.presentation.components.SkeletonList
import com.example.dealernest.presentation.components.VendorDueBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    orderId: Int?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: OrderFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(orderId) { orderId?.let { viewModel.loadForEdit(it) } }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showVendorScanner by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }

    if (showVendorScanner) {
        QrScannerScreen(
            onResult = { value ->
                showVendorScanner = false
                viewModel.onVendorQrScanned(value)
            },
            onClose = { showVendorScanner = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (orderId == null) "New Order" else "Edit Order") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoadingExisting) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (uiState.isReadOnly) {
                Text(
                    "This order can no longer be edited - its status is locked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(16.dp))
            }

            Text("Step 1 · Vendor", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val vendor = uiState.vendor
            if (vendor == null) {
                AppSearchField(
                    value = uiState.vendorQuery,
                    onValueChange = viewModel::onVendorQueryChange,
                    placeholder = "Search vendors",
                    onQrClick = { showVendorScanner = true },
                )
                Spacer(Modifier.height(8.dp))
                if (uiState.isSearchingVendor) {
                    SkeletonList(itemCount = 2)
                } else if (uiState.vendorResults.isNotEmpty()) {
                    val vendorListState = rememberLazyListState()
                    InfiniteScrollHandler(
                        listState = vendorListState,
                        canLoadMore = uiState.canLoadMoreVendors,
                        onLoadMore = viewModel::loadMoreVendors,
                    )
                    LazyColumn(state = vendorListState, modifier = Modifier.heightIn(max = 320.dp)) {
                        items(uiState.vendorResults, key = { it.id }) { result ->
                            VendorResultRow(result, onClick = { viewModel.onVendorSelected(result) })
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
            } else {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vendor.shopName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                vendor.mobile ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            VendorDueBadge(dueBalance = vendor.dueBalance, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (!uiState.isReadOnly) {
                            TextButton(onClick = viewModel::onChangeVendor) { Text("Change") }
                        }
                    }
                }
            }

            if (!uiState.isReadOnly) {
                Spacer(Modifier.height(24.dp))
                Text("Step 2 · Products", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.productQuery,
                    onValueChange = viewModel::onProductQueryChange,
                    label = { Text("Search products") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (uiState.isSearchingProduct) {
                    SkeletonList(itemCount = 2)
                } else if (uiState.productResults.isNotEmpty()) {
                    if (uiState.productResultsFromCache) {
                        Text(
                            "Showing cached results — you're offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    val productListState = rememberLazyListState()
                    InfiniteScrollHandler(
                        listState = productListState,
                        canLoadMore = uiState.canLoadMoreProducts,
                        onLoadMore = viewModel::loadMoreProducts,
                    )
                    LazyColumn(state = productListState, modifier = Modifier.heightIn(max = 320.dp)) {
                        items(uiState.productResults, key = { it.id }) { product ->
                            ProductResultRow(product, onClick = { viewModel.onAddProduct(product) })
                        }
                        if (uiState.canLoadMoreProducts) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.cartLines.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column {
                    uiState.cartLines.forEach { line ->
                        CartLineRow(
                            line = line,
                            readOnly = uiState.isReadOnly,
                            onQuantityChange = { viewModel.onQuantityTextChange(line.product.id, it) },
                            onSellPriceChange = { viewModel.onSellPriceTextChange(line.product.id, it) },
                            onDiscountChange = { viewModel.onDiscountTextChange(line.product.id, it) },
                            onRemove = { viewModel.onRemoveLine(line.product.id) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Summary", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
                    InfoRow(label = "Subtotal", value = "%.2f".format(uiState.cartLines.subtotal))
                    InfoRow(label = "Total Discount", value = "%.2f".format(uiState.cartLines.totalDiscount))
                    InfoRow(label = "Grand Total", value = "%.2f".format(uiState.cartLines.grandTotal))
                }
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    uiState.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (!uiState.isReadOnly) {
                Spacer(Modifier.height(24.dp))
                PrimaryButton(
                    text = if (orderId == null) "Create Order" else "Save Changes",
                    onClick = viewModel::submit,
                    loading = uiState.isSaving,
                    enabled = uiState.vendor != null && uiState.cartLines.isNotEmpty(),
                )
            }
        }
    }
}

@Composable
private fun VendorResultRow(vendor: Vendor, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vendor.shopName, style = MaterialTheme.typography.titleMedium)
                Text(
                    vendor.mobile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            VendorDueBadge(dueBalance = vendor.dueBalance)
        }
    }
}

@Composable
private fun ProductResultRow(product: Product, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductThumbnail(imageUrl = product.imageUrl)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Stock: ${product.quantityAvailable} · Price: ${"%.2f".format(product.sellPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProductThumbnail(imageUrl: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = modifier.size(48.dp).clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.size(48.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CartLineRow(
    line: OrderCartLine,
    readOnly: Boolean,
    onQuantityChange: (String) -> Unit,
    onSellPriceChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    SectionCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductThumbnail(imageUrl = line.product.imageUrl)
            Spacer(Modifier.width(12.dp))
            Text(line.product.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (!readOnly) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (readOnly) {
            InfoRow(label = "Qty × Price", value = "${line.quantity} × ${"%.2f".format(line.sellPrice)}")
            InfoRow(label = "Discount", value = "%.2f".format(line.discountPrice))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = line.quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = line.sellPriceText,
                    onValueChange = onSellPriceChange,
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = line.discountText,
                    onValueChange = onDiscountChange,
                    label = { Text("Discount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            if (line.isBelowMinPrice) {
                Text(
                    "Minimum price is ${"%.2f".format(line.product.minSellPrice)} - using that instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Line total: ${"%.2f".format(line.lineTotal)}", style = MaterialTheme.typography.titleSmall)
    }
}
