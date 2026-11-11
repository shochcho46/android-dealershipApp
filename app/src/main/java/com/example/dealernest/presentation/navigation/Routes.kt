package com.example.dealernest.presentation.navigation

object Routes {
    const val LOCATION_GATE = "location_gate"
    const val SPLASH = "splash"
    const val LOGIN = "login"

    const val DASHBOARD = "dashboard"
    const val ORDERS = "orders"
    const val COLLECTIONS = "collections"
    const val VENDORS = "vendors"
    const val PROFILE = "profile"

    const val VENDOR_DETAILS = "vendor_details/{vendorId}"
    const val VENDOR_CREATE = "vendor_create"
    const val VENDOR_EDIT = "vendor_edit/{vendorId}"

    const val COLLECTION_CREATE = "collection_create"

    const val ORDER_CREATE = "order_create"
    const val ORDER_EDIT = "order_edit/{orderId}"
    const val ORDER_INVOICE = "order_invoice/{orderId}"

    fun vendorDetails(vendorId: Int) = "vendor_details/$vendorId"
    fun vendorEdit(vendorId: Int) = "vendor_edit/$vendorId"
    fun orderEdit(orderId: Int) = "order_edit/$orderId"
    fun orderInvoice(orderId: Int) = "order_invoice/$orderId"
}
