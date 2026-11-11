package com.example.dealernest.domain.model

enum class RoleLevel { NORMAL, ADMIN, SUPER_ADMIN }

// Exact role-name strings are unconfirmed against the real backend - the SRS just says
// "SuperAdmin"/"Admin"/normal user, the API doc doesn't show example role names. See
// .claude/docs/auth-and-permissions.md "Role model" - adjust this matching once confirmed.
fun List<String>.toRoleLevel(): RoleLevel = when {
    any { it.replace("_", "").replace("-", "").equals("superadmin", ignoreCase = true) } ->
        RoleLevel.SUPER_ADMIN
    any { it.equals("admin", ignoreCase = true) } -> RoleLevel.ADMIN
    else -> RoleLevel.NORMAL
}

// Separate from RoleLevel above (which only distinguishes Normal/Admin/SuperAdmin for other
// features) - Sales Performance's "see all" gate also includes Director and Subadmin, two role
// names this app otherwise never needed to recognize. See .claude/docs/auth-and-permissions.md
// "Role model" and api-contract.md "Sales Performance".
private val ELEVATED_SALES_PERFORMANCE_ROLES = setOf("director", "admin", "superadmin", "subadmin")

fun List<String>.canSeeAllSalesPerformance(): Boolean = any {
    it.replace("_", "").replace("-", "").replace(" ", "").lowercase() in ELEVATED_SALES_PERFORMANCE_ROLES
}
