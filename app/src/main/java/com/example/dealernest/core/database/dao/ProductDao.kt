package com.example.dealernest.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dealernest.core.database.entity.ProductEntity

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun search(query: String, limit: Int): List<ProductEntity>
}
