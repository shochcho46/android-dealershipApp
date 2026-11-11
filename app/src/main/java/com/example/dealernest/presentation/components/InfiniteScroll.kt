package com.example.dealernest.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Auto-fires [onLoadMore] once the user scrolls within [buffer] items of the list's end -
 * used everywhere a list/picker paginates, so the app loads more as you scroll instead of
 * requiring a manual "Load more" tap. */
@Composable
fun InfiniteScrollHandler(listState: LazyListState, canLoadMore: Boolean, buffer: Int = 3, onLoadMore: () -> Unit) {
    LaunchedEffect(listState, canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - buffer
        }
            .distinctUntilChanged()
            .collect { reachedEnd -> if (reachedEnd) onLoadMore() }
    }
}
