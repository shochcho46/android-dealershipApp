package com.example.dealernest.data.di

import com.example.dealernest.data.auth.AuthRepositoryImpl
import com.example.dealernest.data.collection.DsrCollectionRepositoryImpl
import com.example.dealernest.data.order.OrderRepositoryImpl
import com.example.dealernest.data.product.ProductRepositoryImpl
import com.example.dealernest.data.salesperformance.SalesPerformanceRepositoryImpl
import com.example.dealernest.data.vendor.VendorRepositoryImpl
import com.example.dealernest.domain.repository.AuthRepository
import com.example.dealernest.domain.repository.DsrCollectionRepository
import com.example.dealernest.domain.repository.OrderRepository
import com.example.dealernest.domain.repository.ProductRepository
import com.example.dealernest.domain.repository.SalesPerformanceRepository
import com.example.dealernest.domain.repository.VendorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindVendorRepository(impl: VendorRepositoryImpl): VendorRepository

    @Binds
    @Singleton
    abstract fun bindDsrCollectionRepository(impl: DsrCollectionRepositoryImpl): DsrCollectionRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindSalesPerformanceRepository(impl: SalesPerformanceRepositoryImpl): SalesPerformanceRepository
}
