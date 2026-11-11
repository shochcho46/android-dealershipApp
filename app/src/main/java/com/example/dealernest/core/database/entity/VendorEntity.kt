package com.example.dealernest.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dealernest.domain.model.Vendor

@Entity(tableName = "vendors")
data class VendorEntity(
    @PrimaryKey val id: Int,
    val shopName: String,
    val contactPerson: String?,
    val email: String?,
    val mobile: String,
    val countryName: String?,
    val fullAddress: String?,
    val status: Boolean,
    val statusText: String,
    val imageUrl: String?,
    val dueBalance: String?,
    val oldDue: String?,
    val totalCredit: String?,
    val totalDebit: String?,
    val locationStatus: String? = null,
    val locationStatusColor: String? = null,
)

fun VendorEntity.toDomain() = Vendor(
    id = id,
    shopName = shopName,
    contactPerson = contactPerson,
    email = email,
    mobile = mobile,
    countryName = countryName,
    fullAddress = fullAddress,
    status = status,
    statusText = statusText,
    imageUrl = imageUrl,
    dueBalance = dueBalance,
    oldDue = oldDue,
    totalCredit = totalCredit,
    totalDebit = totalDebit,
    locationStatus = locationStatus,
    locationStatusColor = locationStatusColor,
)

fun Vendor.toEntity() = VendorEntity(
    id = id,
    shopName = shopName,
    contactPerson = contactPerson,
    email = email,
    mobile = mobile,
    countryName = countryName,
    fullAddress = fullAddress,
    status = status,
    statusText = statusText,
    imageUrl = imageUrl,
    dueBalance = dueBalance,
    oldDue = oldDue,
    totalCredit = totalCredit,
    totalDebit = totalDebit,
    locationStatus = locationStatus,
    locationStatusColor = locationStatusColor,
)
