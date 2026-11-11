package com.example.dealernest.presentation.collection

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dealernest.domain.model.DsrCollection
import com.example.dealernest.domain.model.RoleLevel
import com.example.dealernest.presentation.components.DateRangeFilterRow
import com.example.dealernest.presentation.components.EmptyStateView
import com.example.dealernest.presentation.components.ErrorStateView
import com.example.dealernest.presentation.components.InfiniteScrollHandler
import com.example.dealernest.presentation.components.SkeletonList
import com.example.dealernest.ui.theme.ActionBlue
import com.example.dealernest.ui.theme.GradientTealEnd
import com.example.dealernest.ui.theme.OnActionBlue

@Composable
fun CollectionListScreen(
    onAddCollectionClick: () -> Unit,
    viewModel: CollectionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val roleLevel by viewModel.roleLevel.collectAsStateWithLifecycle()
    val dateFromMillis by viewModel.dateFromMillis.collectAsStateWithLifecycle()
    val dateToMillis by viewModel.dateToMillis.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }

    // Refetches whenever this screen's backstack entry resumes - e.g. popping back after
    // creating a collection - since the ViewModel instance survives that round-trip and its
    // init{} fetch only ran once. Same refresh() the pull-to-refresh gesture already calls.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCollectionClick,
                containerColor = ActionBlue,
                contentColor = OnActionBlue,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New collection")
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
                is CollectionListUiState.Loading -> SkeletonList(modifier = Modifier.padding(horizontal = 16.dp))
                is CollectionListUiState.Empty -> EmptyStateView(
                    icon = Icons.Filled.Payments,
                    title = "No collections found",
                    subtitle = "Try a different date range or record a new collection.",
                )
                is CollectionListUiState.Error -> ErrorStateView(message = state.message, onRetry = viewModel::refresh)
                is CollectionListUiState.Success -> {
                    val listState = rememberLazyListState()
                    val canLoadMore = state.page < state.lastPage
                    InfiniteScrollHandler(listState = listState, canLoadMore = canLoadMore, onLoadMore = viewModel::loadNextPage)
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    ) {
                        items(state.collections, key = { it.id }) { collection ->
                            CollectionCard(
                                collection = collection,
                                canDelete = roleLevel != RoleLevel.NORMAL,
                                onDeleteClick = { pendingDeleteId = collection.id },
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

    val deleteId = pendingDeleteId
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete collection?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCollection(deleteId)
                    pendingDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            },
        )
    }
}

private val CollectionAccent = GradientTealEnd

@Composable
private fun CollectionCard(collection: DsrCollection, canDelete: Boolean, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(CollectionAccent))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(CollectionAccent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Payments, contentDescription = null, tint = CollectionAccent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(collection.vendorShopName, style = MaterialTheme.typography.titleMedium)
                Text(
                    collection.collectionDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (collection.note != null) {
                    Text(
                        collection.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(collection.amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (canDelete) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
