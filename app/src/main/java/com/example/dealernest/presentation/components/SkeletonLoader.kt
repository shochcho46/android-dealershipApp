package com.example.dealernest.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Shimmering rounded-rect placeholder - use in place of content while a screen is loading. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    val color = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = shimmerAlpha)),
    )
}

/** A stack of [SkeletonBox]es shaped like a typical list-item card - shown while a list loads. */
@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier.fillMaxWidth()) {
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(20.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).padding(top = 8.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).padding(top = 8.dp))
    }
}

@Composable
fun SkeletonList(itemCount: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(itemCount) {
            SkeletonListItem(modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
