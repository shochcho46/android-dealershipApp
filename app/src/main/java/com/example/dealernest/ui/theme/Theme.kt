package com.example.dealernest.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Brand80,
    onPrimary = Color(0xFF0A2472),
    primaryContainer = BrandContainerDark,
    onPrimaryContainer = OnBrandContainerDark,
    secondary = Slate80,
    tertiary = Amber80,
    background = Color(0xFF10101B),
    surface = Color(0xFF16162B),
    surfaceVariant = Color(0xFF1E1E30)
)

private val LightColorScheme = lightColorScheme(
    primary = Brand40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BrandContainerLight,
    onPrimaryContainer = OnBrandContainerLight,
    secondary = Slate40,
    tertiary = Amber40,
    background = Color(0xFFF7F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEEDF9)
)

// Rounder, more modern corners app-wide - anything not given an explicit shape (dialogs, chips,
// default-shape cards/fields) picks these up instead of Material's tighter baseline radii.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun DealerNestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the brand palette above is consistent across devices/wallpapers.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}