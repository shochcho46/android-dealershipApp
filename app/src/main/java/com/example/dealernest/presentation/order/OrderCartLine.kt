package com.example.dealernest.presentation.order

import com.example.dealernest.domain.model.Product

/**
 * Text fields, not Double, so a text field always shows exactly what was typed (no
 * reformat-on-every-keystroke). Clamping (qty >= 1, price >= product.minSellPrice, discount >=
 * 0) happens lazily in the computed properties, used for the displayed line total and at
 * submit time - not by rewriting what the user typed.
 */
data class OrderCartLine(
    val product: Product,
    val quantityText: String = "1",
    val sellPriceText: String,
    val discountText: String = "0",
) {
    val quantity: Int get() = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val sellPrice: Double get() = (sellPriceText.toDoubleOrNull() ?: product.minSellPrice).coerceAtLeast(product.minSellPrice)
    val discountPrice: Double get() = (discountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val lineTotal: Double get() = (sellPrice * quantity) - discountPrice
    val isBelowMinPrice: Boolean get() = product.minSellPrice > 0 && (sellPriceText.toDoubleOrNull() ?: 0.0) < product.minSellPrice
}

val List<OrderCartLine>.subtotal: Double get() = sumOf { it.sellPrice * it.quantity }
val List<OrderCartLine>.totalDiscount: Double get() = sumOf { it.discountPrice }
val List<OrderCartLine>.grandTotal: Double get() = sumOf { it.lineTotal }
