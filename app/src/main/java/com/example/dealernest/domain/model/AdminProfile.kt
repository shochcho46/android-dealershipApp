package com.example.dealernest.domain.model

// Confirmed fields only - the API doesn't return Employee ID/Address/Joining Date/Last Login
// the SRS's Profile screen asks for. See .claude/docs/auth-and-permissions.md "Profile fields
// gap". createdAt is shown as a "member since" line, not a real joining-date field.
data class AdminProfile(
    val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val status: Boolean,
    val statusText: String,
    val pictureUrl: String? = null,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val createdAt: String? = null,
) {
    val roleLevel: RoleLevel get() = roles.toRoleLevel()
}
