package com.example.dealernest.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Fixed 5 tabs per the SRS - always visible for every logged-in user, regardless of role.
 * Role-based restrictions (see domain/model/RoleLevel.kt) gate actions inside a module, not
 * tab visibility - see .claude/docs/auth-and-permissions.md.
 */
enum class BottomNavTab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    Order(Routes.ORDERS, "Order", Icons.Filled.ShoppingCart),
    Collection(Routes.COLLECTIONS, "Collection", Icons.Filled.Payments),
    Vendor(Routes.VENDORS, "Vendor", Icons.Filled.Store),
    Profile(Routes.PROFILE, "Profile", Icons.Filled.Person),
}
