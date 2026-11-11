package com.example.dealernest.core.qr

private val VENDOR_ID_LINE = Regex("(?im)^\\s*vendor\\s*id\\s*:\\s*(.+?)\\s*$")

/**
 * Vendor QR codes encode multiple labeled lines (Vendor ID/Name/Mobile/Contact Person/...). When
 * a "Vendor ID:" line is present, search only by that ID instead of dumping the whole multi-line
 * payload into the search box. Any other scanned text (or manually typed input) is used as-is.
 */
fun extractVendorSearchTerm(rawScannedText: String): String =
    VENDOR_ID_LINE.find(rawScannedText)?.groupValues?.get(1)?.trim() ?: rawScannedText.trim()
