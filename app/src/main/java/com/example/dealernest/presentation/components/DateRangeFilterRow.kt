package com.example.dealernest.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dealernest.core.common.toApiDateString
import com.example.dealernest.ui.theme.ActionBlue

/** From/To date filter chips shared by Collection and Order lists - opens a Material 3 date
 * picker per chip, clearable once set. Callers receive/emit epoch millis. */
@Composable
fun DateRangeFilterRow(
    dateFromMillis: Long?,
    dateToMillis: Long?,
    onDateFromChange: (Long?) -> Unit,
    onDateToChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DateFilterChip(
            label = "From date",
            selectedMillis = dateFromMillis,
            onDateSelected = onDateFromChange,
            modifier = Modifier.weight(1f),
        )
        DateFilterChip(
            label = "To date",
            selectedMillis = dateToMillis,
            onDateSelected = onDateToChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterChip(
    label: String,
    selectedMillis: Long?,
    onDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val hasValue = selectedMillis != null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (hasValue) ActionBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = if (hasValue) ActionBlue.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(50),
            )
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(
            Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = if (hasValue) ActionBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            selectedMillis?.toApiDateString() ?: label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal,
            color = if (hasValue) ActionBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false),
            maxLines = 1,
        )
        if (hasValue) {
            IconButton(onClick = { onDateSelected(null) }, modifier = Modifier.size(20.dp).padding(start = 4.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Clear $label", tint = ActionBlue, modifier = Modifier.size(14.dp))
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(state.selectedDateMillis)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
