package com.example.dealernest.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lightweight bar chart built from plain layout primitives (Box height fractions), not a
 * charting library or raw Canvas math - resolves the Phase 4 chart-library decision by not
 * needing one for a simple 7-bar weekly view. `data` is (day label, value), oldest first.
 */
@Composable
fun SimpleBarChart(
    data: List<Pair<String, Double>>,
    barColor: Color,
    modifier: Modifier = Modifier,
    maxBarHeight: Dp = 120.dp,
) {
    val maxValue = data.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        data.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.height(maxBarHeight).width(28.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val fraction = (value / maxValue).toFloat().coerceIn(0.03f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(fraction)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
