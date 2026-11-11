package com.example.dealernest.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dealernest.domain.model.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val imageUrl: String?,
    val quantityAvailable: Int,
    val sellPrice: Double,
    val minSellPrice: Double,
    val unitName: String?,
)

fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    imageUrl = imageUrl,
    quantityAvailable = quantityAvailable,
    sellPrice = sellPrice,
    minSellPrice = minSellPrice,
    unitName = unitName,
)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    imageUrl = imageUrl,
    quantityAvailable = quantityAvailable,
    sellPrice = sellPrice,
    minSellPrice = minSellPrice,
    unitName = unitName,
)
