package com.example.dealernest.ui.theme

import androidx.compose.ui.graphics.Color

// Brand seed colors - vivid blue primary (trust/commerce), slate secondary, amber accent.
val Brand80 = Color(0xFFA9C7FF)
val Slate80 = Color(0xFFC5CAE9)
val Amber80 = Color(0xFFFFCC80)

val Brand40 = Color(0xFF2563EB)
val Slate40 = Color(0xFF5B6472)
val Amber40 = Color(0xFFB45309)

// Explicit "action" blue/white - every Create/Save/primary-action button uses these two colors
// directly (not the theme's `primary` role), so they stay identical across light/dark mode.
val ActionBlue = Color(0xFF2563EB)
val ActionBluePressed = Color(0xFF1D4ED8)
val OnActionBlue = Color(0xFFFFFFFF)

// Harmonized container roles for the blue/slate/amber seeds above - explicit rather than left to
// Material's baseline-purple defaults, so containers read as part of the same palette.
val BrandContainerLight = Color(0xFFDBEAFE)
val OnBrandContainerLight = Color(0xFF1E3A8A)
val BrandContainerDark = Color(0xFF1E3A5F)
val OnBrandContainerDark = Color(0xFFDBEAFE)

// Semantic status colors (paid/partial/unpaid, success/warning/danger) - used by StatusBadge
// and anywhere a colored state needs to read consistently across the app.
val StatusGreen = Color(0xFF16A34A)
val StatusGreenContainer = Color(0xFFDCFCE7)
val StatusOrange = Color(0xFFEA580C)
val StatusOrangeContainer = Color(0xFFFFEDD5)
val StatusRed = Color(0xFFDC2626)
val StatusRedContainer = Color(0xFFFEE2E2)
val StatusBlue = Color(0xFF2563EB)
val StatusBlueContainer = Color(0xFFDBEAFE)

// Vivid accent colors for gradient cards/backgrounds (Dashboard stat cards, hero surfaces) -
// distinct from the Material color-scheme roles above, used only inside Brush gradients.
val GradientVioletStart = Color(0xFF6D28D9)
val GradientVioletEnd = Color(0xFF4F46E5)
val GradientTealStart = Color(0xFF0D9488)
val GradientTealEnd = Color(0xFF06B6D4)
val GradientSunsetStart = Color(0xFFEA580C)
val GradientSunsetEnd = Color(0xFFDB2777)
val GradientOceanStart = Color(0xFF2563EB)
val GradientOceanEnd = Color(0xFF7C3AED)

// Order-card payment-status gradients (whole-card background, not just an accent) - paid/
// partial/unpaid need to be tellable apart at a glance, so each gets its own hue family.
val GradientPaidStart = Color(0xFF22C55E)
val GradientPaidEnd = Color(0xFF15803D)
val GradientPartialStart = Color(0xFFFBBF24)
val GradientPartialEnd = Color(0xFFD97706)
val GradientUnpaidStart = Color(0xFFF87171)
val GradientUnpaidEnd = Color(0xFFB91C1C)
