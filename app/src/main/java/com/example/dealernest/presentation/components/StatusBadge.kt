package com.example.dealernest.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dealernest.ui.theme.StatusBlue
import com.example.dealernest.ui.theme.StatusBlueContainer
import com.example.dealernest.ui.theme.StatusGreen
import com.example.dealernest.ui.theme.StatusGreenContainer
import com.example.dealernest.ui.theme.StatusOrange
import com.example.dealernest.ui.theme.StatusOrangeContainer
import com.example.dealernest.ui.theme.StatusRed
import com.example.dealernest.ui.theme.StatusRedContainer

enum class StatusTone { SUCCESS, WARNING, DANGER, INFO, NEUTRAL }

/** Small color-coded pill for status text (paid/partial/unpaid, active/inactive, etc.). */
@Composable
fun StatusBadge(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val (container, content) = when (tone) {
        StatusTone.SUCCESS -> StatusGreenContainer to StatusGreen
        StatusTone.WARNING -> StatusOrangeContainer to StatusOrange
        StatusTone.DANGER -> StatusRedContainer to StatusRed
        StatusTone.INFO -> StatusBlueContainer to StatusBlue
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        color = content,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
