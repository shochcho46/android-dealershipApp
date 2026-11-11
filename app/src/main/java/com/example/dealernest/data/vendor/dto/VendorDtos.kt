package com.example.dealernest.data.vendor.dto

import com.example.dealernest.domain.model.Vendor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VendorCountryDto(
    // id nullable too, not just name - see the null-safety note on VendorDto below.
    val id: Int? = null,
    val name: String? = null,
    val iso: String? = null,
)

@Serializable
data class VendorAddressDto(
    @SerialName("full_address") val fullAddress: String? = null,
    val lat: String? = null,
    val long: String? = null,
    // Server-computed "does this vendor have coordinates on file" flag + a ready-to-use hex
    // color, so the list can flag vendors needing a location update without the app re-deriving
    // it from lat/long itself. locationStatus is "yes"/"no" (lowercase, per the sample response).
    @SerialName("location_status") val locationStatus: String? = null,
    @SerialName("location_status_color") val locationStatusColor: String? = null,
)

@Serializable
data class VendorImageDto(
    val url: String? = null,
    @SerialName("thumb_url") val thumbUrl: String? = null,
)

@Serializable
data class VendorDto(
    val id: Int,
    val uuid: String? = null,
    // Nullable - Laravel nullable varchar columns can send null even for fields the docs'
    // examples always showed populated (confirmed the hard way on payment_method.name - see
    // .claude/docs/api-contract.md "null-safety audit"). Fallbacks live in toDomain() below.
    @SerialName("shop_name") val shopName: String? = null,
    @SerialName("contact_person") val contactPerson: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val country: VendorCountryDto? = null,
    val address: VendorAddressDto? = null,
    // Confirmed via live testing to be a JSON boolean here too, same as AdminObject.status -
    // this backend uses boolean `status` broadly, not just for admins. See
    // .claude/docs/api-contract.md "null-safety audit".
    val status: Boolean = true,
    @SerialName("status_text") val statusText: String? = null,
    val image: VendorImageDto? = null,
    @SerialName("due_balance") val dueBalance: String? = null,
    @SerialName("old_due") val oldDue: String? = null,
    @SerialName("total_credit") val totalCredit: String? = null,
    @SerialName("total_debit") val totalDebit: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun VendorDto.toDomain(): Vendor = Vendor(
    id = id,
    shopName = shopName ?: "Unnamed Vendor",
    contactPerson = contactPerson,
    email = email,
    mobile = mobile ?: "-",
    countryName = country?.name,
    fullAddress = address?.fullAddress,
    status = status,
    statusText = statusText ?: if (status) "Active" else "Inactive",
    imageUrl = image?.url,
    dueBalance = dueBalance,
    oldDue = oldDue,
    totalCredit = totalCredit,
    totalDebit = totalDebit,
    locationStatus = address?.locationStatus,
    locationStatusColor = address?.locationStatusColor,
)
