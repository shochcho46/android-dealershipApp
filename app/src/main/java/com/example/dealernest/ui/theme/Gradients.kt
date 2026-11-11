package com.example.dealernest.ui.theme

import androidx.compose.ui.graphics.Brush

object AppGradients {
    val violet = Brush.linearGradient(listOf(GradientVioletStart, GradientVioletEnd))
    val teal = Brush.linearGradient(listOf(GradientTealStart, GradientTealEnd))
    val sunset = Brush.linearGradient(listOf(GradientSunsetStart, GradientSunsetEnd))
    val ocean = Brush.linearGradient(listOf(GradientOceanStart, GradientOceanEnd))
    val paid = Brush.linearGradient(listOf(GradientPaidStart, GradientPaidEnd))
    val partial = Brush.linearGradient(listOf(GradientPartialStart, GradientPartialEnd))
    val unpaid = Brush.linearGradient(listOf(GradientUnpaidStart, GradientUnpaidEnd))

    /** Cycles through the four accent gradients by index - used to color a row of StatCards. */
    fun accentByIndex(index: Int): Brush = when (index % 4) {
        0 -> violet
        1 -> teal
        2 -> sunset
        else -> ocean
    }
}
