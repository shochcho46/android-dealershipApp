package com.example.dealernest.data.collection.dto

import com.example.dealernest.domain.model.DsrCollection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Nullable leaf strings *and* id fields throughout this file, not required - confirmed the hard
// way (twice) that this backend null-shells whole nested reference objects (every field null,
// including id) instead of returning null for the relation itself. See
// .claude/docs/api-contract.md "null-safety audit". Fallbacks live in the toDomain() mappers.
@Serializable
data class CollectionVendorDto(
    val id: Int? = null,
    @SerialName("shop_name") val shopName: String? = null,
    @SerialName("contact_person") val contactPerson: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    @SerialName("full_address") val fullAddress: String? = null,
    @SerialName("due_balance") val dueBalance: String? = null,
)

@Serializable
data class PaymentMethodDto(
    val id: Int? = null,
    val name: String? = null,
    @SerialName("account_name") val accountName: String? = null,
)

@Serializable
data class CollectionAdminDto(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
)

@Serializable
data class DsrCollectionDto(
    val id: Int,
    val vendor: CollectionVendorDto,
    @SerialName("payment_method") val paymentMethod: PaymentMethodDto? = null,
    val amount: String? = null,
    @SerialName("collection_date") val collectionDate: String? = null,
    val note: String? = null,
    @SerialName("created_by") val createdBy: CollectionAdminDto? = null,
    @SerialName("deposite_by") val depositeBy: CollectionAdminDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class CreateDsrCollectionRequestDto(
    @SerialName("vendor_id") val vendorId: Int,
    @SerialName("payment_method_id") val paymentMethodId: Int,
    val amount: Double,
    @SerialName("collection_date") val collectionDate: String,
    val note: String? = null,
)

fun DsrCollectionDto.toDomain(): DsrCollection = DsrCollection(
    id = id,
    vendorId = vendor.id,
    vendorShopName = vendor.shopName ?: "Unnamed Vendor",
    vendorMobile = vendor.mobile,
    vendorDueBalance = vendor.dueBalance,
    paymentMethodName = paymentMethod?.name,
    amount = amount ?: "0.00",
    collectionDate = collectionDate ?: "-",
    note = note,
    createdById = createdBy?.id,
    createdByName = createdBy?.name,
)
