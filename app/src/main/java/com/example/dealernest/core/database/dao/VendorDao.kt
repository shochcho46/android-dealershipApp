package com.example.dealernest.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dealernest.core.database.entity.VendorEntity

@Dao
interface VendorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(vendors: List<VendorEntity>)

    @Query("SELECT * FROM vendors WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): VendorEntity?

    @Query(
        "SELECT * FROM vendors WHERE shopName LIKE '%' || :query || '%' " +
            "OR mobile LIKE '%' || :query || '%' LIMIT :limit",
    )
    suspend fun search(query: String, limit: Int): List<VendorEntity>
}
