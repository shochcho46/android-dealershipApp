package com.example.dealernest.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dealernest.presentation.auth.LoginScreen
import com.example.dealernest.presentation.collection.CollectionCreateScreen
import com.example.dealernest.presentation.collection.CollectionListScreen
import com.example.dealernest.presentation.home.DashboardScreen
import com.example.dealernest.presentation.locationgate.LocationGateScreen
import com.example.dealernest.presentation.order.InvoiceScreen
import com.example.dealernest.presentation.order.OrderFormScreen
import com.example.dealernest.presentation.order.OrderListScreen
import com.example.dealernest.presentation.profile.ProfileScreen
import com.example.dealernest.presentation.splash.SplashScreen
import com.example.dealernest.presentation.vendor.VendorDetailsScreen
import com.example.dealernest.presentation.vendor.VendorFormScreen
import com.example.dealernest.presentation.vendor.VendorListScreen

private const val TRANSITION_DURATION_MS = 220

@Composable
fun DealerNestNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOCATION_GATE,
        enterTransition = {
            fadeIn(tween(TRANSITION_DURATION_MS)) +
                slideInHorizontally(tween(TRANSITION_DURATION_MS)) { it / 8 }
        },
        exitTransition = { fadeOut(tween(TRANSITION_DURATION_MS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_DURATION_MS)) },
        popExitTransition = {
            fadeOut(tween(TRANSITION_DURATION_MS)) +
                slideOutHorizontally(tween(TRANSITION_DURATION_MS)) { it / 8 }
        },
    ) {
        composable(Routes.LOCATION_GATE) {
            LocationGateScreen(
                onGranted = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(Routes.LOCATION_GATE) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.DASHBOARD) {
            BottomNavScaffold(navController = navController, currentTab = BottomNavTab.Dashboard) {
                DashboardScreen(
                    onCreateOrderClick = { navController.navigate(Routes.ORDER_CREATE) },
                    onCreateCollectionClick = { navController.navigate(Routes.COLLECTION_CREATE) },
                    onAddVendorClick = { navController.navigate(Routes.VENDOR_CREATE) },
                    onOrderClick = { orderId -> navController.navigate(Routes.orderInvoice(orderId)) },
                )
            }
        }
        composable(Routes.ORDERS) {
            BottomNavScaffold(navController = navController, currentTab = BottomNavTab.Order) {
                OrderListScreen(
                    onAddOrderClick = { navController.navigate(Routes.ORDER_CREATE) },
                    onOrderClick = { orderId -> navController.navigate(Routes.orderInvoice(orderId)) },
                    onEditOrderClick = { orderId -> navController.navigate(Routes.orderEdit(orderId)) },
                )
            }
        }
        composable(Routes.COLLECTIONS) {
            BottomNavScaffold(navController = navController, currentTab = BottomNavTab.Collection) {
                CollectionListScreen(onAddCollectionClick = { navController.navigate(Routes.COLLECTION_CREATE) })
            }
        }
        composable(Routes.VENDORS) {
            BottomNavScaffold(navController = navController, currentTab = BottomNavTab.Vendor) {
                VendorListScreen(
                    onVendorClick = { vendorId ->
                        navController.navigate(Routes.vendorDetails(vendorId))
                    },
                    onAddVendorClick = { navController.navigate(Routes.VENDOR_CREATE) },
                )
            }
        }
        composable(Routes.PROFILE) {
            BottomNavScaffold(navController = navController, currentTab = BottomNavTab.Profile) {
                ProfileScreen(
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable(
            route = Routes.VENDOR_DETAILS,
            arguments = listOf(navArgument("vendorId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val vendorId = backStackEntry.arguments?.getInt("vendorId") ?: return@composable
            VendorDetailsScreen(
                vendorId = vendorId,
                onEditClick = { navController.navigate(Routes.vendorEdit(vendorId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.VENDOR_CREATE) {
            VendorFormScreen(
                vendorId = null,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.VENDOR_EDIT,
            arguments = listOf(navArgument("vendorId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val vendorId = backStackEntry.arguments?.getInt("vendorId") ?: return@composable
            VendorFormScreen(
                vendorId = vendorId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.COLLECTION_CREATE) {
            CollectionCreateScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ORDER_CREATE) {
            OrderFormScreen(
                orderId = null,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.ORDER_EDIT,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            OrderFormScreen(
                orderId = orderId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.ORDER_INVOICE,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            InvoiceScreen(orderId = orderId, onBack = { navController.popBackStack() })
        }
    }
}
