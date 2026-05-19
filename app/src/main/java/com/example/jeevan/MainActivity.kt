package com.example.jeevan

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jeevan.data.JeevanRepository
import com.example.jeevan.data.PatientAddRequest
import com.example.jeevan.data.UserRole
import com.example.jeevan.ui.screens.*
import com.example.jeevan.ui.theme.JeevanTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private val viewModel: JeevanViewModel by viewModels()

    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.startLocationUpdates(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))
        setContent {
            JeevanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JeevanApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun JeevanApp(viewModel: JeevanViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Admin patient list polling
    var patients     by remember { mutableStateOf<List<com.example.jeevan.data.PatientDto>>(emptyList()) }
    var adminLoading by remember { mutableStateOf(false) }
    fun loadAdminData() {
        scope.launch {
            adminLoading = true
            patients = JeevanRepository.getAllPatients()
            adminLoading = false
        }
    }
    LaunchedEffect(Unit) { while (true) { loadAdminData(); delay(2000) } }

    // ── Stage → navigation for USER role only ──
    val role = uiState.authState.currentUser?.role
    LaunchedEffect(uiState.stage) {
        // Only drive SOS navigation for User role (or when not logged in)
        if (role == null || role == UserRole.USER) {
            when (uiState.stage) {
                AppStage.HOME -> navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
                AppStage.PROCESSING -> navController.navigate(Screen.Processing.route) { launchSingleTop = true }
                AppStage.TRACKING   -> navController.navigate(Screen.Tracking.route)   { launchSingleTop = true }
                AppStage.STATUS     -> navController.navigate("status")                 { launchSingleTop = true }
            }
        }
    }

    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }

    NavHost(
        navController    = navController,
        startDestination = Screen.Home.route,
        enterTransition  = { fadeIn() + slideInHorizontally { it / 4 } },
        exitTransition   = { fadeOut() + slideOutHorizontally { -it / 4 } },
    ) {

        // ════════════════════════════════════════
        // USER SCREENS
        // ════════════════════════════════════════
        composable(Screen.Home.route) {
            HomeScreen(
                uiState    = uiState,
                onSOS      = { viewModel.triggerSOS() },
                onNavigate = { navigate(it) }
            )
        }

        composable(Screen.Processing.route) {
            ProcessingScreen(uiState = uiState, onCancel = { viewModel.cancelSOS() })
        }

        composable(Screen.Tracking.route) {
            LiveTrackingScreen(
                uiState       = uiState,
                onCancel      = { viewModel.cancelSOS() },
                onMarkArrived = { viewModel.markArrived() }
            )
        }

        composable("status") {
            StatusScreen(onDone = { viewModel.resetToHome() })
        }

        composable(Screen.LiveTracking.route) {
            LiveTrackingTabScreen(
                uiState       = uiState,
                onSOS         = { viewModel.triggerSOS() },
                onMarkArrived = { viewModel.markArrived() },
                onNavigate    = { navigate(it) }
            )
        }

        composable(Screen.History.route) {
            LaunchedEffect(Unit) { viewModel.refreshHistory() }
            HistoryScreen(
                history    = uiState.sosHistory,
                onNavigate = { navigate(it) }
            )
        }

        composable(Screen.Chatbot.route) {
            ChatbotScreen(onBack = { navController.popBackStack() })
        }

        // ════════════════════════════════════════
        // PROFILE (shared by all roles)
        // ════════════════════════════════════════
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel   = viewModel,
                isLoggedIn  = uiState.authState.isLoggedIn,
                currentUser = uiState.authState.currentUser,
                onNavigate  = { navigate(it) }
            )
        }

        composable(Screen.ProfileEdit.route) {
            val user = uiState.authState.currentUser
            if (user != null) {
                ProfileEditScreen(currentUser = user, viewModel = viewModel, onBack = { navController.popBackStack() })
            } else {
                navigate(Screen.Profile.route)
            }
        }

        // ════════════════════════════════════════
        // AUTH SCREENS
        // ════════════════════════════════════════
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onSuccess = { loggedInRole ->
                    val dest = when (loggedInRole) {
                        UserRole.DRIVER   -> Screen.DriverDashboard.route
                        UserRole.HOSPITAL -> Screen.HospitalDashboard.route
                        UserRole.ADMIN    -> Screen.Admin.route
                        else              -> Screen.Home.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ════════════════════════════════════════
        // DRIVER SCREENS (no SOS button)
        // ════════════════════════════════════════
        composable(Screen.DriverDashboard.route) {
            DriverDashboardScreen(
                uiState   = uiState,
                onAccept  = { /* Driver accepted — navigate to tracking handled inside card */ },
                onDecline = { viewModel.declineSOS() },
                onNavigate = { navigate(it) }
            )
        }

        composable(Screen.DriverTracking.route) {
            DriverTrackingScreen(
                uiState       = uiState,
                onMarkArrived = { viewModel.markArrived() },
                onNavigate    = { navigate(it) }
            )
        }

        // ════════════════════════════════════════
        // HOSPITAL SCREENS (no SOS button)
        // ════════════════════════════════════════
        composable(Screen.HospitalDashboard.route) {
            HospitalDashboardScreen(
                uiState           = uiState,
                onPatientReceived = { viewModel.markArrived() },
                onNavigate        = { navigate(it) }
            )
        }

        composable(Screen.HospitalTracking.route) {
            HospitalTrackingScreen(
                uiState    = uiState,
                onNavigate = { navigate(it) }
            )
        }

        // ════════════════════════════════════════
        // ADMIN SCREENS (legacy)
        // ════════════════════════════════════════
        composable(Screen.Driver.route) {
            DriverScreen(
                sosState      = uiState.sosState,
                onMarkArrived = { viewModel.markArrived() },
                onNavigate    = { navigate(it) }
            )
        }

        composable(Screen.Hospital.route) {
            HospitalScreen(
                sosState        = uiState.sosState,
                onSubmitPatient = { name, condition, ambId ->
                    scope.launch {
                        JeevanRepository.addPatient(
                            PatientAddRequest(
                                name        = name,
                                condition   = condition,
                                time        = java.time.Instant.now().toString(),
                                ambulanceId = ambId,
                                sosId       = uiState.sosState?.sosId,
                            )
                        )
                    }
                },
                onMarkArrived = { viewModel.markArrived() },
                onNavigate    = { navigate(it) }
            )
        }

        composable(Screen.Admin.route) {
            AdminScreen(
                sosState   = uiState.sosState,
                patients   = patients,
                isLoading  = adminLoading,
                onRefresh  = { loadAdminData() },
                onNavigate = { navigate(it) }
            )
        }
    }
}
