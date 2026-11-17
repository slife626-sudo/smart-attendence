package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.db.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.AttendanceRepository
import com.example.ui.admin.AdminShellScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.staff.StaffPunchScreen
import com.example.ui.staff.StaffViewModel
import com.example.ui.theme.WahidiyaTheme
import kotlinx.serialization.Serializable

@Serializable
data object StaffPunchRoute

@Serializable
data object AdminRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WahidiyaTheme {
                val navController = rememberNavController()
                val db = remember { AppDatabase.getInstance(applicationContext) }
                val repository = remember { AttendanceRepository(db) }
                val prefs = remember { UserPreferences(applicationContext) }

                NavHost(
                    navController = navController,
                    startDestination = StaffPunchRoute
                ) {
                    composable<StaffPunchRoute> {
                        val staffVm: StaffViewModel = viewModel(
                            factory = StaffViewModel.Factory(repository, prefs)
                        )
                        StaffPunchScreen(
                            viewModel = staffVm,
                            onNavigateToAdmin = {
                                navController.navigate(AdminRoute) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable<AdminRoute> {
                        val adminVm: AdminViewModel = viewModel(
                            factory = AdminViewModel.Factory(repository, prefs)
                        )
                        AdminShellScreen(
                            viewModel = adminVm,
                            onNavigateBackToStaff = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
