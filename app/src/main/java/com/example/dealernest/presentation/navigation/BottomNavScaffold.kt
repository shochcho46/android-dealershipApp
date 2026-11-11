package com.example.dealernest.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.dealernest.ui.theme.ActionBlue

/** Fixed 5-tab Material 3 bottom nav shell - replaces the Phase 0 drawer (MainScaffold). Uses
 * the stock NavigationBar/NavigationBarItem (Material's own vetted spacing/animation/ripple)
 * with just the brand blue applied to the selected state - a hand-rolled bar kept looking worse,
 * not better. */
@Composable
fun BottomNavScaffold(
    navController: NavHostController,
    currentTab: BottomNavTab,
    content: @Composable () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                BottomNavTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab != currentTab) {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ActionBlue,
                            selectedTextColor = ActionBlue,
                            indicatorColor = ActionBlue.copy(alpha = 0.14f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
