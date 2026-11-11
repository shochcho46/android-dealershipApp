package com.example.dealernest.presentation.vendor

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.dealernest.domain.model.Vendor
import com.example.dealernest.presentation.components.ErrorStateView
import com.example.dealernest.presentation.components.InfoRow
import com.example.dealernest.ui.theme.StatusRed
import com.example.dealernest.presentation.components.SectionCard
import com.example.dealernest.presentation.components.SkeletonList
import com.example.dealernest.presentation.components.StatusBadge
import com.example.dealernest.presentation.components.StatusTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDetailsScreen(
    vendorId: Int,
    onEditClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: VendorDetailsViewModel = hiltViewModel(),
) {
    LaunchedEffect(vendorId) { viewModel.load(vendorId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendor Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(vendorId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit vendor")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is VendorDetailsUiState.Loading -> SkeletonList(
                    itemCount = 3,
                    modifier = Modifier.padding(16.dp),
                )
                is VendorDetailsUiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.retry(vendorId) },
                )
                is VendorDetailsUiState.Success -> VendorDetailsContent(vendor = state.vendor)
            }
        }
    }
}

@Composable
private fun VendorDetailsContent(vendor: Vendor) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (vendor.imageUrl != null) {
                    AsyncImage(
                        model = vendor.imageUrl,
                        contentDescription = vendor.shopName,
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.padding(start = 12.dp))
                Column {
                    Text(vendor.shopName, style = MaterialTheme.typography.titleLarge)
                    StatusBadge(
                        text = vendor.statusText,
                        tone = if (vendor.status) StatusTone.SUCCESS else StatusTone.DANGER,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text("Contact", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            InfoRow(label = "Contact Person", value = vendor.contactPerson ?: "-")
            InfoRow(label = "Mobile", value = vendor.mobile)
            InfoRow(label = "Email", value = vendor.email ?: "-")
            InfoRow(label = "Address", value = vendor.fullAddress ?: "-")
            InfoRow(label = "Country", value = vendor.countryName ?: "-")
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text("Balance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            InfoRow(label = "Due Balance", value = vendor.dueBalance ?: "0.00", valueColor = StatusRed)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Collections and orders for this vendor will appear here once Phases 2/3 are built.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
