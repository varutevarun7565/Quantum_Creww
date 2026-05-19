package com.example.jeevan

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jeevan.data.*
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale

// ── Navigation routes ──
sealed class Screen(val route: String) {
    object Home             : Screen("home")
    object Processing       : Screen("processing")
    object Tracking         : Screen("tracking")
    object Driver           : Screen("driver")
    object Hospital         : Screen("hospital")
    object Admin            : Screen("admin")
    object History          : Screen("history")
    object LiveTracking     : Screen("live_tracking")
    object Profile          : Screen("profile")
    object ProfileEdit      : Screen("profile_edit")
    object Login            : Screen("login")
    object Signup           : Screen("signup")
    // Role-specific dashboards (no SOS button)
    object DriverDashboard  : Screen("driver_dashboard")
    object DriverTracking   : Screen("driver_tracking")
    object HospitalDashboard: Screen("hospital_dashboard")
    object HospitalTracking : Screen("hospital_tracking")
}

// ── App Stage ──
enum class AppStage { HOME, PROCESSING, TRACKING, STATUS }

// ── UI State ──
data class UiState(
    val stage: AppStage              = AppStage.HOME,
    val userLoc: LatLng?             = null,
    val locationAddress: String      = "Detecting location…",
    val locStatus: String            = "detecting",
    val sosState: SosState?          = null,
    val nearbyHospitals: List<HospitalInfo> = emptyList(),
    val agentSteps: List<AgentStep>  = emptyList(),
    val processingProgress: Float    = 0f,
    val error: String                = "",
    // Auth
    val authState: AuthState         = AuthState(),
    // History
    val sosHistory: List<SosHistoryEntry> = emptyList(),
    // ETA countdown (seconds remaining)
    val etaTotalSec: Int             = 360,
    val etaRemainSec: Int            = 360,
)

class JeevanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollJob: Job?            = null
    private var timerJob: Job?           = null
    private var movementJob: Job?        = null
    private var locationPushJob: Job?    = null   // pushes GPS to MongoDB every 5 sec

    private val initialAgentSteps = listOf(
        AgentStep("sos",      "SOS Agent",      "Analyzing location",  "Geocode processing…",         AgentStatus.PENDING),
        AgentStep("dispatch", "Dispatch Agent",  "Finding ambulance",   "Scanning unit availability…", AgentStatus.PENDING),
        AgentStep("routing",  "Routing Agent",   "Optimizing path",     "Calculating traffic data…",   AgentStatus.PENDING),
        AgentStep("hospital", "Hospital Agent",  "Securing ER bed",     "Contacting hospital…",        AgentStatus.PENDING),
    )

    // ────────────────────────────────────────────
    // LOCATION
    // ────────────────────────────────────────────
    fun startLocationUpdates(context: Context) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L).build()
        try {
            fusedClient.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    onLocationReceived(context, loc)
                    fusedClient.removeLocationUpdates(this)
                }
            }, Looper.getMainLooper())
        } catch (e: SecurityException) {
            _uiState.update { it.copy(locStatus = "error") }
        }
    }

    private fun onLocationReceived(context: Context, loc: Location) {
        val latLng   = LatLng(loc.latitude, loc.longitude)
        val address  = reverseGeocode(context, loc.latitude, loc.longitude)
        val hospitals = JeevanRepository.getMockNearbyHospitals(loc.latitude, loc.longitude)
        _uiState.update {
            it.copy(userLoc = latLng, locationAddress = address, locStatus = "ready", nearbyHospitals = hospitals)
        }
    }

    private fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder  = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                buildString {
                    addr.subLocality?.let { append("$it, ") }
                    addr.locality?.let   { append("$it, ") }
                    addr.adminArea?.let  { append(it) }
                }.trimEnd(',', ' ').ifEmpty { "Your Location" }
            } else "Your Location"
        } catch (_: Exception) { "Your Location" }
    }

    // ────────────────────────────────────────────
    // SOS TRIGGER
    // ────────────────────────────────────────────
    fun triggerSOS() {
        val loc = _uiState.value.userLoc ?: LatLng(28.6139, 77.2090) // Delhi fallback for emulator
        _uiState.update {
            it.copy(stage = AppStage.PROCESSING, agentSteps = initialAgentSteps, processingProgress = 0f, error = "")
        }

        viewModelScope.launch {
            val stepMs = 1000L

            delay(500);     activateStep(0)
            delay(stepMs);  completeStep(0, "Geocode Confirmed")

            val sosJob = async { JeevanRepository.triggerSos(loc.lat, loc.lng) }

            activateStep(1); delay(stepMs); completeStep(1, "Unit 402 Dispatched")
            activateStep(2); delay(stepMs); completeStep(2, "Route Calculated")
            activateStep(3); delay(stepMs)

            val sosState = sosJob.await()
            completeStep(3, "${sosState.hospital?.name ?: "Hospital"} Ready")

            val totalSec = sosState.etaMin * 60
            _uiState.update {
                it.copy(
                    stage             = AppStage.TRACKING,
                    sosState          = sosState,
                    processingProgress = 1f,
                    etaTotalSec       = totalSec,
                    etaRemainSec      = totalSec,
                )
            }

            startPolling()
            startEtaCountdown()
            startAmbulanceMovement(sosState)
        }
    }

    private fun activateStep(index: Int) {
        _uiState.update { state ->
            val steps = state.agentSteps.toMutableList()
            if (index < steps.size) steps[index] = steps[index].copy(status = AgentStatus.ACTIVE)
            state.copy(agentSteps = steps, processingProgress = index / 4f)
        }
    }

    private fun completeStep(index: Int, detail: String) {
        _uiState.update { state ->
            val steps = state.agentSteps.toMutableList()
            if (index < steps.size) steps[index] = steps[index].copy(status = AgentStatus.SUCCESS, detail = detail)
            state.copy(agentSteps = steps, processingProgress = (index + 1) / 4f)
        }
    }

    // ────────────────────────────────────────────
    // AMBULANCE MOVEMENT — Two-phase, road-following
    //
    // Phase 1: ambulance start → patient (accident spot)
    // Phase 2: patient → hospital
    // Both phases walk real OSRM road waypoints step by step
    // ────────────────────────────────────────────
    private fun startAmbulanceMovement(initial: SosState) {
        movementJob?.cancel()
        movementJob = viewModelScope.launch {
            val ambStart = initial.ambulance ?: return@launch
            val user     = initial.user      ?: return@launch
            val hospital = initial.hospital  ?: return@launch

            // ── Phase 1: Ambulance → Patient ──
            _uiState.update { it.copy(sosState = it.sosState?.copy(stage = "to_user")) }

            val phase1Route = JeevanRepository.getOsrmRoute(
                LatLng(ambStart.lat, ambStart.lng), user
            )

            walkWaypoints(phase1Route, speedKmH = 40.0) { lat, lng, etaMin ->
                JeevanRepository.updateMockAmbulancePosition(lat, lng, etaMin)
                _uiState.update { state ->
                    val sos = state.sosState ?: return@update state
                    state.copy(sosState = sos.copy(
                        ambulance = sos.ambulance?.copy(lat = lat, lng = lng),
                        etaMin    = etaMin,
                    ))
                }
            }

            if (!isActive) return@launch

            // ── Snap to patient position ──
            _uiState.update { state ->
                val sos = state.sosState ?: return@update state
                state.copy(sosState = sos.copy(
                    ambulance = sos.ambulance?.copy(lat = user.lat, lng = user.lng),
                    stage     = "at_user",
                    etaMin    = 0,
                ))
            }

            delay(3000) // simulate patient pickup pause

            if (!isActive) return@launch

            // ── Phase 2: Patient → Hospital ──
            val phase2DistKm = JeevanRepository.haversineKm(
                user.lat, user.lng, hospital.lat, hospital.lng
            )
            val phase2EtaSec = ((phase2DistKm / 50.0) * 3600).toInt()

            _uiState.update { state ->
                val sos = state.sosState ?: return@update state
                state.copy(
                    etaTotalSec  = phase2EtaSec,
                    etaRemainSec = phase2EtaSec,
                    sosState     = sos.copy(
                        stage   = "to_hospital",
                        etaMin  = phase2EtaSec / 60,
                        totalKm = (phase2DistKm * 10).toLong() / 10.0,
                    ),
                )
            }

            val phase2Route = JeevanRepository.getOsrmRoute(
                user, LatLng(hospital.lat, hospital.lng)
            )

            walkWaypoints(phase2Route, speedKmH = 50.0) { lat, lng, etaMin ->
                JeevanRepository.updateMockAmbulancePosition(lat, lng, etaMin)
                _uiState.update { state ->
                    val sos = state.sosState ?: return@update state
                    state.copy(sosState = sos.copy(
                        ambulance = sos.ambulance?.copy(lat = lat, lng = lng),
                        etaMin    = etaMin,
                    ))
                }
            }

            if (!isActive) return@launch

            // ── Reached hospital ──
            _uiState.update { state ->
                val sos = state.sosState ?: return@update state
                state.copy(sosState = sos.copy(
                    ambulance = sos.ambulance?.copy(lat = hospital.lat, lng = hospital.lng),
                    stage     = "at_hospital",
                    etaMin    = 0,
                ))
            }
        }
    }

    // ────────────────────────────────────────────
    // Walks OSRM waypoints step-by-step.
    // Delay per waypoint is proportional to segment
    // length so speed stays consistent (40-50 km/h).
    // ────────────────────────────────────────────
    private suspend fun walkWaypoints(
        waypoints: List<LatLng>,
        speedKmH: Double,
        onStep: suspend (lat: Double, lng: Double, etaMin: Int) -> Unit,
    ) {
        if (waypoints.size < 2) return

        // Total route distance for ETA calculation
        var totalDistKm = 0.0
        for (i in 0 until waypoints.size - 1) {
            totalDistKm += JeevanRepository.haversineKm(
                waypoints[i].lat, waypoints[i].lng,
                waypoints[i + 1].lat, waypoints[i + 1].lng,
            )
        }

        var distTravelled = 0.0

        for (i in 0 until waypoints.size - 1) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) return

            val from  = waypoints[i]
            val to    = waypoints[i + 1]
            val segKm = JeevanRepository.haversineKm(from.lat, from.lng, to.lat, to.lng)

            distTravelled += segKm
            val distRemaining = maxOf(0.0, totalDistKm - distTravelled)
            val etaMin = maxOf(0, (distRemaining / speedKmH * 60).toInt())

            onStep(to.lat, to.lng, etaMin)

            // Step delay: how long to travel this segment at speedKmH
            // Clamped 600ms–3000ms so it feels real but not too slow
            val stepMs = (segKm / speedKmH * 3_600_000L).toLong().coerceIn(600L, 3000L)
            delay(stepMs)
        }
    }



    // ────────────────────────────────────────────
    // ETA COUNTDOWN TIMER (every second)
    // ────────────────────────────────────────────
    private fun startEtaCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { state ->
                    val remaining = maxOf(0, state.etaRemainSec - 1)
                    val etaMin = remaining / 60
                    val etaSec = remaining % 60
                    state.copy(
                        etaRemainSec = remaining,
                        sosState = state.sosState?.copy(etaMin = etaMin, etaSec = etaSec),
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────
    // BACKEND POLLING
    // ────────────────────────────────────────────
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(2000)
                val state = JeevanRepository.getCurrentSos()
                if (state != null && _uiState.value.stage == AppStage.TRACKING) {
                    // Only update non-position fields from backend (movement sim handles position)
                    _uiState.update { ui ->
                        val current = ui.sosState ?: return@update ui
                        ui.copy(sosState = current.copy(
                            active = state.active,
                            stage  = state.stage,
                        ))
                    }
                }
            }
        }
    }

    // ────────────────────────────────────────────
    // CANCEL / RESET
    // ────────────────────────────────────────────
    fun cancelSOS() {
        val sos = _uiState.value.sosState
        // 1. Stop all jobs immediately
        stopAllJobs()
        // 2. Reset UI instantly (no waiting for network)
        _uiState.update {
            it.copy(
                stage        = AppStage.HOME,
                sosState     = null,
                agentSteps   = emptyList(),
                processingProgress = 0f,
                etaRemainSec = 0,
                sosHistory   = if (sos != null) {
                    val entry = SosHistoryEntry(
                        id              = sos.sosId ?: "SOS-cancelled",
                        timestamp       = JeevanRepository.nowFormatted(),
                        userAddress     = it.locationAddress,
                        ambulanceNumber = sos.ambulance?.number ?: "—",
                        hospitalName    = sos.hospital?.name ?: "—",
                        etaMin          = sos.etaMin,
                        distanceKm      = sos.totalKm,
                        stage           = "cancelled",
                    ).also { entry -> JeevanRepository.addHistory(entry) }
                    JeevanRepository.getHistory()
                } else it.sosHistory,
            )
        }
        // 3. Network clear in background (fire-and-forget)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { JeevanRepository.clearSos() } catch (_: Exception) {}
        }
    }

    fun markArrived() {
        viewModelScope.launch {
            val sos = _uiState.value.sosState
            if (sos != null) {
                val entry = SosHistoryEntry(
                    id              = sos.sosId ?: "SOS-${System.currentTimeMillis()}",
                    timestamp       = JeevanRepository.nowFormatted(),
                    userAddress     = _uiState.value.locationAddress,
                    ambulanceNumber = sos.ambulance?.number ?: "—",
                    hospitalName    = sos.hospital?.name ?: "—",
                    etaMin          = sos.etaMin,
                    distanceKm      = sos.totalKm,
                    stage           = "completed",
                )
                JeevanRepository.addHistory(entry)
            }
            stopAllJobs()
            _uiState.update {
                it.copy(stage = AppStage.STATUS, sosHistory = JeevanRepository.getHistory())
            }
        }
    }

    fun resetToHome() {
        viewModelScope.launch {
            stopAllJobs()
            JeevanRepository.clearSos()
            _uiState.update {
                it.copy(
                    stage        = AppStage.HOME,
                    sosState     = null,
                    agentSteps   = emptyList(),
                    processingProgress = 0f,
                    etaRemainSec = 0,
                    sosHistory   = JeevanRepository.getHistory(),
                )
            }
        }
    }

    // ────────────────────────────────────────────
    // AUTH  (suspend → MongoDB via FastAPI)
    // ────────────────────────────────────────────
    fun login(userId: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = JeevanRepository.login(userId, password)
            if (user != null) {
                _uiState.update { it.copy(authState = AuthState(isLoggedIn = true, currentUser = user)) }
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun register(profile: UserProfile, onResult: (JeevanRepository.RegisterResult) -> Unit) {
        viewModelScope.launch {
            val result = JeevanRepository.register(profile)
            if (result == JeevanRepository.RegisterResult.SUCCESS) {
                _uiState.update { it.copy(authState = AuthState(isLoggedIn = true, currentUser = profile)) }
            }
            onResult(result)
        }
    }

    fun logout() {
        _uiState.update { it.copy(authState = AuthState()) }
    }

    fun updateProfile(updated: UserProfile) {
        viewModelScope.launch {
            JeevanRepository.updateProfile(updated)
            _uiState.update { it.copy(authState = it.authState.copy(currentUser = updated)) }
        }
    }

    // ────────────────────────────────────────────
    // HISTORY
    // ────────────────────────────────────────────
    fun refreshHistory() {
        _uiState.update { it.copy(sosHistory = JeevanRepository.getHistory()) }
    }

    // ────────────────────────────────────────────
    // DRIVER: DECLINE → REASSIGN TO NEXT DRIVER
    // ────────────────────────────────────────────
    fun declineSOS() {
        val sos = _uiState.value.sosState ?: return

        // Mock list of backup ambulance drivers (simulates DB lookup)
        val backupDrivers = listOf(
            AmbulanceInfo(
                id = "AMB-2050", number = "MH 12 AB 2050",
                driver = "Amit Sharma", rating = 4.8, type = "BLS",
                phone  = "+919823456789",
                lat    = sos.ambulance?.lat?.plus(0.008) ?: 28.62,
                lng    = sos.ambulance?.lng?.plus(0.005) ?: 77.21,
            ),
            AmbulanceInfo(
                id = "AMB-2051", number = "MH 12 AB 2051",
                driver = "Suresh Patil", rating = 4.7, type = "ALS",
                phone  = "+919834567890",
                lat    = sos.ambulance?.lat?.minus(0.006) ?: 28.61,
                lng    = sos.ambulance?.lng?.plus(0.009) ?: 77.22,
            ),
        )

        // Pick next driver (rotate based on attempt count using sosId hash)
        val idx     = (sos.sosId?.hashCode() ?: 0).and(0x7FFFFFFF) % backupDrivers.size
        val nextAmb = backupDrivers[idx]

        // Recalculate ETA for new driver distance
        val distKm = sos.user?.let {
            JeevanRepository.haversineKm(nextAmb.lat, nextAmb.lng, it.lat, it.lng)
        } ?: 3.0
        val newEta = ((distKm / 40.0) * 60).toInt().coerceAtLeast(2) // 40 km/h speed

        val newSos = sos.copy(
            ambulance = nextAmb,
            etaMin    = newEta,
            totalKm   = Math.round(distKm * 10) / 10.0,
            stage     = "to_user",
        )

        _uiState.update { it.copy(sosState = newSos) }

        // Restart ambulance movement with new driver position
        movementJob?.cancel()
        startAmbulanceMovement(newSos)
    }
    private fun stopAllJobs() {
        pollJob?.cancel();          pollJob = null
        timerJob?.cancel();         timerJob = null
        movementJob?.cancel();      movementJob = null
        locationPushJob?.cancel();  locationPushJob = null
    }

    // ────────────────────────────────────────────
    // DRIVER LIVE LOCATION PUSH (every 5 seconds)
    // Call this when the driver accepts an SOS.
    // Stops automatically when stopAllJobs() is called.
    // ────────────────────────────────────────────
    fun startDriverLocationPush() {
        locationPushJob?.cancel()
        locationPushJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000)  // push every 5 seconds
                val state  = _uiState.value.sosState ?: continue
                val amb    = state.ambulance ?: continue
                val driver = _uiState.value.authState.currentUser ?: continue
                JeevanRepository.pushDriverLocation(
                    ambulanceId  = amb.id,
                    driverUserId = driver.userId,
                    lat          = amb.lat,
                    lng          = amb.lng,
                    sosId        = state.sosId,
                    stage        = state.stage,
                )
            }
        }
    }

    fun stopDriverLocationPush() {
        locationPushJob?.cancel()
        locationPushJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAllJobs()
    }
}
