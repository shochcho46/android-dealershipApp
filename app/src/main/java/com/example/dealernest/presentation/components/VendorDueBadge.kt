package com.example.dealernest.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Due-balance pill used anywhere a vendor is shown (Vendor list/details, Order/Collection vendor
 * pickers) - red whenever there's an outstanding balance, neutral "0.00" when fully settled, so
 * the same rule reads consistently across every screen instead of each module styling it ad hoc. */
@Composable
fun VendorDueBadge(dueBalance: String?, modifier: Modifier = Modifier) {
    val amount = dueBalance?.toDoubleOrNull() ?: 0.0
    StatusBadge(
        text = "Due ${dueBalance ?: "0.00"}",
        tone = if (amount > 0) StatusTone.DANGER else StatusTone.NEUTRAL,
        modifier = modifier,
    )
}
