package com.example.dealernest.domain.model

data class Order(
    val id: Int,
    val invoiceId: String,
    // Nullable - see the null-safety note on DsrCollection.vendorId; same reasoning, same
    // backend, and this field is likewise unused elsewhere in the app.
    val vendorId: Int?,
    val vendorShopName: String,
    val vendorFullAddress: String?,
    val vendorMobile: String?,
    val vendorDueBalance: String?,
    val placedByName: String?,
    val orderStatusId: Int,
    val orderStatusName: String,
    val orderStatusColor: String?,
    val totalAmount: String,
    val paidAmount: String,
    val dueAmount: String,
    val totalQuantity: Int,
    val totalDiscountAmount: String,
    val canBeCancelled: Boolean,
    val items: List<OrderItem>,
    val createdAt: String?,
    // Server-computed - the app never calculates distance or builds this URL itself. Null
    // googleMapsUrl means there's nothing to show (no order/vendor coordinates on file).
    val hasLocationData: Boolean = false,
    val distanceFromVendor: String? = null,
    val googleMapsUrl: String? = null,
) {
    // SRS-stated IDs (2=Pending, 3=Confirmed editable; 4=Shipped, 5=Delivered read-only) - not
    // confirmed against the real order_statuses table, since the API docs don't enumerate it.
    // See .claude/docs/api-contract.md.
    val isEditable: Boolean get() = orderStatusId == 2 || orderStatusId == 3

    val paymentStatus: OrderPaymentStatus
        get() {
            val paid = paidAmount.toDoubleOrNull() ?: 0.0
            val due = dueAmount.toDoubleOrNull() ?: 0.0
            return when {
                paid <= 0.0 -> OrderPaymentStatus.UNPAID
                due > 0.0 -> OrderPaymentStatus.PARTIAL
                else -> OrderPaymentStatus.PAID
            }
        }
}

/** Derived from paid/due amounts, not a backend field - drives the color (green/amber/red) every
 * order card is shown in, so list, dashboard, and invoice all agree on the same paid/partial/
 * unpaid call. */
enum class OrderPaymentStatus { PAID, PARTIAL, UNPAID }

data class OrderItem(
    val id: Int,
    val productId: Int,
    val productName: String,
    val productImageUrl: String?,
    val quantity: Int,
    val sellPrice: String,
    val totalPrice: String,
    val discountPrice: String,
)
