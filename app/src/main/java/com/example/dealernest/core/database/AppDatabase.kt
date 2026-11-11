package com.example.dealernest.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.dealernest.core.database.dao.ProductDao
import com.example.dealernest.core.database.dao.VendorDao
import com.example.dealernest.core.database.entity.ProductEntity
import com.example.dealernest.core.database.entity.VendorEntity

// exportSchema = false - no shipped users yet, nothing to preserve migration history for.
// version 2: added VendorEntity.locationStatus/locationStatusColor - see DatabaseModule's
// fallbackToDestructiveMigration() (a cache table, safe to wipe and resync from the network).
@Database(entities = [VendorEntity::class, ProductEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vendorDao(): VendorDao
    abstract fun productDao(): ProductDao
}
