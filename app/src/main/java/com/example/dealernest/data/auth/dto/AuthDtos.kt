package com.example.dealernest.data.auth.dto

import com.example.dealernest.domain.model.AdminProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val status: Boolean = true,
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
    status = status,
    statusText = statusText ?: if (status) "Active" else "Inactive",
    pictureUrl = picture?.url,
    roles = roles.mapNotNull { it.name },
    permissions = permissions.mapNotNull { it.name },
    createdAt = createdAt,
)
