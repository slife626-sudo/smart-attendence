package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.preferences.UserPreferences
import com.example.data.repository.AttendanceRepository
import com.example.ui.admin.AdminShellScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutGoogleAccount
import com.example.ui.staff.StaffPunchScreen
import com.example.ui.staff.StaffViewModel
import com.example.ui.theme.WahidiyaTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object StaffPunchRoute

@Serializable
data object AdminRoute

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WahidiyaTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val authFlow = remember { Firebase.auth.authStateFlow() }
                val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
                val prefs = remember { UserPreferences(applicationContext) }
                val credentialManager = remember(context) { CredentialManager.create(context) }

                val user = currentUser
                if (user == null) {
                    AuthScreen(onAuthSuccess = {})
                } else {
                    val customOrgId by prefs.customOrgId.collectAsStateWithLifecycle(initialValue = "")
                    val isBootstrappedAdmin = user.email.equals(
                        AttendanceRepository.BOOTSTRAPPED_ADMIN_EMAIL,
                        ignoreCase = true
                    ) && user.isEmailVerified
                    val activeOrgId = customOrgId.ifBlank {
                        if (isBootstrappedAdmin) AttendanceRepository.DEFAULT_ADMIN_ORG_ID else user.uid
                    }

                    key(user.uid, activeOrgId) {
                        val navController = rememberNavController()
                        val db = remember {
                            FirebaseFirestore.getInstance(
                                applicationContext.getString(R.string.firestore_database_id)
                            )
                        }
                        val repository = remember(user.uid, activeOrgId) {
                            AttendanceRepository(db, activeOrgId)
                        }
                        val vmKey = "${user.uid}_$activeOrgId"

                        val performGoogleSignOut: () -> Unit = {
                            scope.launch {
                                prefs.setAdminLoggedIn(false)
                            }
                            signOutGoogleAccount(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }

                        NavHost(
                            navController = navController,
                            startDestination = StaffPunchRoute
                        ) {
                            composable<StaffPunchRoute> {
                                val staffVm: StaffViewModel = viewModel(
                                    key = "staff_$vmKey",
                                    factory = StaffViewModel.Factory(repository, prefs)
                                )
                                StaffPunchScreen(
                                    viewModel = staffVm,
                                    signedInEmail = user.email.orEmpty(),
                                    onGoogleSignOut = performGoogleSignOut,
                                    onNavigateToAdmin = {
                                        navController.navigate(AdminRoute) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable<AdminRoute> {
                                val adminVm: AdminViewModel = viewModel(
                                    key = "admin_$vmKey",
                                    factory = AdminViewModel.Factory(repository, prefs)
                                )
                                AdminShellScreen(
                                    viewModel = adminVm,
                                    signedInEmail = user.email.orEmpty(),
                                    signedInUid = user.uid,
                                    onGoogleSignOut = performGoogleSignOut,
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
    }
}
