package com.carmelitasoftworks.motointercom.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carmelitasoftworks.motointercom.R
import com.carmelitasoftworks.motointercom.feature.group.GroupScreen
import com.carmelitasoftworks.motointercom.feature.ride.RideScreen
import com.carmelitasoftworks.motointercom.feature.settings.SettingsScreen

private enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    GROUP("group", R.string.nav_group, Icons.Filled.Home),
    RIDE("ride", R.string.nav_ride, Icons.Filled.Call),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}

@Composable
fun MotoIntercomApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.GROUP.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.GROUP.route) { GroupScreen() }
            composable(TopLevelDestination.RIDE.route) { RideScreen() }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
