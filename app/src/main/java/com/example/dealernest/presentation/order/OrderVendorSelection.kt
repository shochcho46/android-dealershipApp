package com.example.dealernest.presentation.order

import com.example.dealernest.domain.model.Order
import com.example.dealernest.domain.model.Vendor

/** Vendor summary shared by both the "picked from search" and "loaded from an existing order"
 * paths - the Order response only ever gives this subset, not the full Vendor object.
 * [id] is nullable because it comes from Order.vendorId, which the backend has been seen to
 * null-shell on some nested reference objects (see .claude/docs/api-contract.md "null-safety
 * audit") - OrderFormViewModel.submit() must check for this before resubmitting an edit, since
 * silently defaulting a vendor id could send a wrong vendor_id to the API. */
data class OrderVendorSelection(
    val id: Int?,
    val shopName: String,
    val mobile: String?,
    val fullAddress: String?,
    val dueBalance: String?,
)

fun Vendor.toOrderSelection() = OrderVendorSelection(id, shopName, mobile, fullAddress, dueBalance)

fun Order.toVendorSelection() = OrderVendorSelection(vendorId, vendorShopName, vendorMobile, vendorFullAddress, vendorDueBalance)
