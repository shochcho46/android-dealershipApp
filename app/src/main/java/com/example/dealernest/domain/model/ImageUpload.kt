package com.example.dealernest.domain.model

/** Domain-safe stand-in for a picked image file - presentation converts a Uri to this before
 * calling a repository, keeping android.net.Uri out of domain/data. */
data class ImageUpload(val bytes: ByteArray, val fileName: String, val mimeType: String)
