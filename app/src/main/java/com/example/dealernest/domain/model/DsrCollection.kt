package com.example.dealernest.domain.model

data class DsrCollection(
    val id: Int,
    // Nullable - this backend null-shells some nested reference objects (all-null fields)
    // instead of returning null for the relation itself; confirmed on created_by, and this
    // field is currently unused elsewhere in the app so there's no cost to guarding it too.
    // See .claude/docs/api-contract.md "null-safety audit".
    val vendorId: Int?,
    val vendorShopName: String,
    val vendorMobile: String?,
    val vendorDueBalance: String?,
    val paymentMethodName: String?,
    val amount: String,
    val collectionDate: String,
    val note: String?,
    val createdById: Int?,
    val createdByName: String?,
)
