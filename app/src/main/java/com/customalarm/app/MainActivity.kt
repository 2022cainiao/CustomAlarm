package com.customalarm.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.customalarm.app.ui.NavRoutes
import com.customalarm.app.ui.alarm.AlarmEditorScreen
import com.customalarm.app.ui.alarm.AlarmEditorViewModel
import com.customalarm.app.ui.home.HomeScreen
import com.customalarm.app.ui.home.HomeViewModel
import com.customalarm.app.ui.routine.RoutineDetailScreen
import com.customalarm.app.ui.routine.RoutineDetailViewModel
import com.customalarm.app.ui.routine.RoutineGroupEditorScreen
import com.customalarm.app.ui.routine.RoutineGroupEditorViewModel
import com.customalarm.app.ui.theme.CustomAlarmTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomAlarmTheme {
                AlarmApp()
            }
        }
    }
}

@Composable
private fun AlarmApp() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = context.appContainer
    var refreshTick by remember { mutableStateOf(0) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTick++
    }

    LaunchedEffect(Unit) {
        launch {
            container.alarmCoordinator.rebuildSchedules()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(NavRoutes.HOME) {
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
                HomeScreen(
                    viewModel = viewModel,
                    contentPadding = paddingValues,
                    exactAlarmEnabled = container.alarmScheduler.canScheduleExactAlarms(),
                    notificationsEnabled = notificationsEnabled(context),
                    onRequestExactAlarmPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onAddNormalAlarm = { navController.navigate(NavRoutes.alarmEditor()) },
                    onAddRoutineGroup = { navController.navigate(NavRoutes.routineEditor()) },
                    onEditAlarm = { alarmId, groupId ->
                        navController.navigate(NavRoutes.alarmEditor(alarmId, groupId))
                    },
                    onOpenRoutineGroup = { groupId ->
                        navController.navigate(NavRoutes.routineDetail(groupId))
                    }
                )
            }

            composable(
                route = "${NavRoutes.ALARM_EDITOR}/{alarmId}/{routineGroupId}",
                arguments = listOf(
                    navArgument("alarmId") { type = NavType.LongType },
                    navArgument("routineGroupId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val alarmId = backStackEntry.arguments?.getLong("alarmId")?.takeIf { it > 0L }
                val routineGroupId = backStackEntry.arguments?.getLong("routineGroupId")?.takeIf { it > 0L }
                val viewModel: AlarmEditorViewModel = viewModel(
                    factory = AlarmEditorViewModel.factory(
                        container = container,
                        alarmId = alarmId,
                        presetRoutineGroupId = routineGroupId
                    )
                )
                AlarmEditorScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "${NavRoutes.ROUTINE_DETAIL}/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
                val viewModel: RoutineDetailViewModel = viewModel(
                    factory = RoutineDetailViewModel.factory(container, groupId)
                )
                RoutineDetailScreen(
                    viewModel = viewModel,
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onEditGroup = { navController.navigate(NavRoutes.routineEditor(groupId)) },
                    onAddAlarm = { navController.navigate(NavRoutes.alarmEditor(routineGroupId = groupId)) },
                    onEditAlarm = { alarmId ->
                        navController.navigate(NavRoutes.alarmEditor(alarmId, groupId))
                    },
                    onDeleted = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "${NavRoutes.ROUTINE_EDITOR}/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId")?.takeIf { it > 0L }
                val viewModel: RoutineGroupEditorViewModel = viewModel(
                    factory = RoutineGroupEditorViewModel.factory(container, groupId)
                )
                RoutineGroupEditorScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun notificationsEnabled(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

