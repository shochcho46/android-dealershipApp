package com.example.dealernest.data.di

import android.content.Context
import androidx.room.Room
import com.example.dealernest.core.database.AppDatabase
import com.example.dealernest.core.database.dao.ProductDao
import com.example.dealernest.core.database.dao.VendorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "dealernest.db")
            // Read-through cache only - safe to drop and resync from the network on a schema
            // bump rather than writing real migrations for it.
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    @Singleton
    fun provideVendorDao(database: AppDatabase): VendorDao = database.vendorDao()

    @Provides
    @Singleton
    fun provideProductDao(database: AppDatabase): ProductDao = database.productDao()
}
