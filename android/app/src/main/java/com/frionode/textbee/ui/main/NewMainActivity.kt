package com.frionode.textbee.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.frionode.textbee.AppConstants
import com.frionode.textbee.activities.MainActivity
import com.frionode.textbee.helpers.HeartbeatManager
import com.frionode.textbee.helpers.PendingSmsManager
import com.frionode.textbee.helpers.SharedPreferenceHelper
import com.frionode.textbee.ui.dashboard.DashboardScreen
import com.frionode.textbee.ui.messages.ComposeScreen
import com.frionode.textbee.ui.messages.MessagesScreen
import com.frionode.textbee.ui.onboarding.OnboardingActivity
import com.frionode.textbee.ui.settings.SMSFilterScreen
import com.frionode.textbee.ui.settings.SettingsScreen
import com.frionode.textbee.ui.theme.TextbeeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class MainDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    MESSAGES("Messages", Icons.Default.Message),
    SETTINGS("Settings", Icons.Default.Settings)
}

class NewMainActivity : ComponentActivity() {

    private var pendingSmsPollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Poll for pending outbound SMS on app start
        PendingSmsManager.triggerPendingSmsPoll(this)

        // Keep a periodic pending-SMS poll scheduled (WorkManager) for when
        // the app is backgrounded/closed.
        PendingSmsManager.schedulePendingSmsPoll(this)

        setContent {
            TextbeeTheme {
                val navController = rememberNavController()
                MainScaffold(
                    navController = navController,
                    onSwitchToLegacy = {
                        SharedPreferenceHelper.setSharedPreferenceBoolean(
                            this, AppConstants.SHARED_PREFS_USE_NEW_UI_KEY, false
                        )
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    },
                    onDisconnect = {
                        listOf(
                            AppConstants.SHARED_PREFS_DEVICE_ID_KEY,
                            AppConstants.SHARED_PREFS_API_KEY_KEY,
                            AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY,
                            AppConstants.SHARED_PREFS_DEVICE_NAME_KEY,
                            AppConstants.SHARED_PREFS_LAST_HEARTBEAT_MS_KEY
                        ).forEach { key ->
                            SharedPreferenceHelper.clearSharedPreference(this, key)
                        }
                        HeartbeatManager.cancelHeartbeat(this)
                        startActivity(
                            Intent(this, OnboardingActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Immediate poll when app comes to foreground
        PendingSmsManager.triggerPendingSmsPoll(this)

        // Repeat pending-SMS poll every 30 seconds while the app is in the
        // foreground, so messages submitted via API/dashboard while the app
        // is already open get picked up without requiring a restart.
        pendingSmsPollJob?.cancel()
        pendingSmsPollJob = lifecycleScope.launch {
            while (true) {
                delay(30000)
                PendingSmsManager.triggerPendingSmsPoll(this@NewMainActivity)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pendingSmsPollJob?.cancel()
        pendingSmsPollJob = null
    }
}

@Composable
private fun MainScaffold(
    navController: NavHostController,
    onSwitchToLegacy: () -> Unit,
    onDisconnect: () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != "compose" && currentRoute != "filters"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    MainDestination.values().forEach { dest ->
                        val selected = currentRoute == dest.name
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.name) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    dest.icon,
                                    contentDescription = dest.label,
                                    modifier = Modifier.size(if (selected) 26.dp else 22.dp)
                                )
                            },
                            label = {
                                Text(
                                    dest.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainDestination.DASHBOARD.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainDestination.DASHBOARD.name) {
                DashboardScreen()
            }
            composable(MainDestination.MESSAGES.name) {
                MessagesScreen(
                    onNavigateToCompose = { navController.navigate("compose") }
                )
            }
            composable(MainDestination.SETTINGS.name) {
                SettingsScreen(
                    onSwitchToLegacy = onSwitchToLegacy,
                    onNavigateToFilters = { navController.navigate("filters") },
                    onDisconnect = onDisconnect
                )
            }
            composable("compose") {
                ComposeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("filters") {
                SMSFilterScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
