package com.example.dealernest.data.auth.dto

import com.example.dealernest.domain.model.AdminProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class LoginRequestDto(
    @SerialName("email_or_phone") val emailOrPhone: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    // Confirmed: login always inlines the full admin object - see api-contract.md.
    val admin: AdminProfileDto,
    val token: String,
    @SerialName("token_type") val tokenType: String? = null,
)

@Serializable
data class PictureDto(
    val url: String? = null,
    @SerialName("thumb_url") val thumbUrl: String? = null,
)

// id nullable too, not just name - this backend null-shells whole nested reference objects
// (all fields null) instead of returning null for the relation itself, confirmed on
// created_by/deposite_by. See .claude/docs/api-contract.md "null-safety audit".
@Serializable
data class RoleDto(
    val id: Int? = null,
    val name: String? = null,
    @SerialName("guard_name") val guardName: String? = null,
)

@Serializable
data class PermissionDto(
    val id: Int? = null,
    val name: String? = null,
    @SerialName("guard_name") val guardName: String? = null,
)

@Serializable
data class AdminProfileDto(
    val id: Int,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    // Confirmed via live testing to be a JSON boolean here (unlike Vendor/Product's `status`,
    // which are ints per the API docs' examples) - see .claude/docs/api-contract.md.
    // Parsed tolerantly (true/false, 1/0, "1"/"active") - see isActiveFlag. A strict Boolean
    // plus trusting status_text made active users render as "Inactive".
    val status: JsonElement? = null,
    @SerialName("status_text") val statusText: String? = null,
    val picture: PictureDto? = null,
    val roles: List<RoleDto> = emptyList(),
    val permissions: List<PermissionDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun AdminProfileDto.toDomain(): AdminProfile = AdminProfile(
    id = id,
    name = name ?: "Admin",
    email = email,
    phone = phone,
    status = isActiveFlag(status, statusText),
    statusText = if (isActiveFlag(status, statusText)) "Active" else "Inactive",
    pictureUrl = picture?.url,
    roles = roles.mapNotNull { it.name },
    permissions = permissions.mapNotNull { it.name },
    createdAt = createdAt,
)

/** Accepts true/false, 1/0, "1"/"0", "active"/"inactive"; falls back to status_text, then active. */
private fun isActiveFlag(status: JsonElement?, statusText: String?): Boolean {
    val raw = (status as? JsonPrimitive)?.content?.trim()?.lowercase()
        ?: statusText?.trim()?.lowercase()
    return when (raw) {
        "true", "1", "active" -> true
        "false", "0", "inactive" -> false
        else -> true
    }
}
