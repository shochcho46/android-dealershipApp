package com.example.dealernest.domain.model

data class Product(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val quantityAvailable: Int,
    val sellPrice: Double,
    // Lowest stocks[].sell_price - the SRS's "cannot be lower than minimum price returned by
    // API" floor for order cart rows. See .claude/docs/api-contract.md.
    val minSellPrice: Double,
    val unitName: String?,
)
