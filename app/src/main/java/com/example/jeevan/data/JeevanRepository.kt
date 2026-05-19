package com.example.jeevan.data

import android.util.Log
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.*

object JeevanRepository {

    // ── CHANGE THIS based on how you test ──
    // Emulator  : http://10.0.2.2:5000/
    // Real device: http://10.14.2.178:5000/  ← your PC's current IP
    private const val BASE_URL = "http://10.0.2.2:5000/"

    private val gson = GsonBuilder().setLenient().create()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val api: JeevanApiService = retrofit.create(JeevanApiService::class.java)

    // ── Mock SOS state ──
    private var mockState: SosState? = null

    private val mockAmbulances = listOf(
        AmbulanceInfo("AMB-2049", "MH 12 AB 2049", "Rajesh Kumar", 4.9, "ALS", "+919812345678"),
        AmbulanceInfo("AMB-1187", "MH 04 BE 1187", "Sunil Verma",  4.8, "BLS", "+919987654321"),
    )

    // ── Auth — backed by MongoDB via FastAPI ──
    // Fallback seed profiles used ONLY if backend is unreachable
    private val seedUsers = listOf(
        UserProfile("driver1",   "Rajesh Kumar", UserRole.DRIVER,   password = "driver123"),
        UserProfile("hospital1", "City Hospital", UserRole.HOSPITAL, password = "hosp123"),
        UserProfile("admin1",    "Admin",         UserRole.ADMIN,    password = "admin123"),
    )
    // Local fallback list when server is offline
    private val localUsers = mutableListOf<UserProfile>()

    suspend fun login(userId: String, password: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val res = api.login(LoginRequest(userId, password))
            if (res.isSuccessful) return@withContext res.body()?.user?.toProfile()
        } catch (e: Exception) {
            Log.w("JeevanRepo", "Login API failed, using local: ${e.message}")
        }
        // Offline fallback: check seed + locally-registered users
        (seedUsers + localUsers).find { it.userId == userId && it.password == password }
    }

    enum class RegisterResult { SUCCESS, DUPLICATE, NETWORK_ERROR }

    suspend fun register(profile: UserProfile): RegisterResult = withContext(Dispatchers.IO) {
        try {
            val res = api.register(profile.toRequest())
            return@withContext when {
                res.isSuccessful          -> RegisterResult.SUCCESS
                res.code() == 409         -> RegisterResult.DUPLICATE   // HTTP 409 = user already exists
                else                      -> RegisterResult.NETWORK_ERROR
            }
        } catch (e: Exception) {
            // Backend unreachable — register locally so the user can still use the app
            Log.w("JeevanRepo", "Register API unreachable, saving locally: ${e.message}")
            if (localUsers.any { it.userId == profile.userId } ||
                seedUsers.any  { it.userId == profile.userId }) {
                return@withContext RegisterResult.DUPLICATE
            }
            localUsers.add(profile)
            return@withContext RegisterResult.SUCCESS
        }
    }

    suspend fun updateProfile(updated: UserProfile) = withContext(Dispatchers.IO) {
        try { api.updateProfile(updated.toRequest()) }
        catch (e: Exception) { Log.w("JeevanRepo", "UpdateProfile failed: ${e.message}") }
    }

    // ── Driver: push live GPS every 5 seconds ──
    suspend fun pushDriverLocation(
        ambulanceId: String,
        driverUserId: String,
        latitude: Double,
        longitude: Double,
        sosId: String?,
        stage: String,
    ) = withContext(Dispatchers.IO) {
        try {
            api.pushDriverLocation(
                DriverLocationRequest(
                    ambulanceId  = ambulanceId,
                    driverUserId = driverUserId,
                    sosId        = sosId,
                    latitude     = latitude,
                    longitude    = longitude,
                    stage        = stage,
                )
            )
        } catch (e: Exception) {
            Log.w("JeevanRepo", "Location push failed (offline ok): ${e.message}")
        }
    }

    private fun UserDto.toProfile() = UserProfile(
        userId           = userId,
        name             = name ?: "",
        role             = when (role?.uppercase()) {
            "DRIVER"   -> UserRole.DRIVER
            "HOSPITAL" -> UserRole.HOSPITAL
            "ADMIN"    -> UserRole.ADMIN
            else       -> UserRole.USER
        },
        contactNumber    = contactNumber ?: "",
        emergencyContact = emergencyContact ?: "",
        bloodGroup       = bloodGroup ?: "",
        age              = age ?: "",
        medicalCondition = medicalCondition ?: "",
        password         = password ?: "",
        photoUri         = photoUri,
    )

    private fun UserProfile.toRequest() = RegisterRequest(
        userId           = userId,
        name             = name,
        role             = role.name,
        contactNumber    = contactNumber,
        emergencyContact = emergencyContact,
        bloodGroup       = bloodGroup,
        age              = age,
        medicalCondition = medicalCondition,
        password         = password,
        photoUri         = photoUri,
    )

    // ── SOS History (in-memory) ──
    private val sosHistory = mutableListOf<SosHistoryEntry>()

    fun addHistory(entry: SosHistoryEntry) { sosHistory.add(0, entry) }
    fun getHistory(): List<SosHistoryEntry> = sosHistory.toList()

    // ── DTO normalization ──
    private fun SosStateDto.toDomain(): SosState {
        val userLoc = when {
            userLocation?.size == 2 -> LatLng(userLocation[0], userLocation[1])
            user?.latitude != null && user.longitude != null -> LatLng(user.latitude, user.longitude)
            else -> null
        }
        val ambLoc  = if (ambulanceLocation?.size == 2) LatLng(ambulanceLocation[0], ambulanceLocation[1]) else null
        val hospLoc = if (hospitalLocation?.size == 2) LatLng(hospitalLocation[0], hospitalLocation[1]) else null

        val amb = ambulance?.let {
            AmbulanceInfo(
                id     = it.id ?: "AMB-2049",
                number = it.number ?: "MH 12 AB 2049",
                driver = it.driver ?: "Rajesh Kumar",
                rating = it.rating ?: 4.9,
                type   = it.type ?: "ALS",
                phone  = it.phone ?: "",
                lat    = ambLoc?.lat ?: it.latitude ?: 0.0,
                lng    = ambLoc?.lng ?: it.longitude ?: 0.0,
            )
        }
        val hosp = hospital?.let {
            HospitalInfo(
                id         = it.id ?: "H-001",
                name       = it.name ?: "Nearest Hospital",
                speciality = it.speciality ?: "General",
                distanceKm = it.distanceKm ?: 2.0,
                phone      = it.phone ?: "",
                beds       = it.beds ?: 8,
                lat        = hospLoc?.lat ?: it.latitude ?: 0.0,
                lng        = hospLoc?.lng ?: it.longitude ?: 0.0,
            )
        }
        return SosState(
            active    = active,
            stage     = stage,
            sosId     = sosId,
            user      = userLoc,
            ambulance = amb,
            hospital  = hosp,
            etaMin    = etaMin ?: 6,
            totalKm   = totalKm ?: 2.0,
            createdAt = createdAt,
        )
    }

    // ── API calls ──

    suspend fun triggerSos(latitude: Double, longitude: Double): SosState = withContext(Dispatchers.IO) {
        Log.d("JeevanRepo", "[API Request] /sos trigger with: Lat=$latitude, Lng=$longitude")
        try {
            val res = api.triggerSos(SosTriggerRequest(latitude, longitude))
            if (res.isSuccessful) {
                Log.d("JeevanRepo", "[API Response] /sos response received successfully.")
                val state = res.body()?.state?.toDomain() ?: buildMockSosState(latitude, longitude)
                mockState = state
                return@withContext state
            }
        } catch (e: Exception) {
            Log.e("JeevanRepo", "[API Error] /sos trigger failed: ${e.message}")
        }
        val mock = buildMockSosState(latitude, longitude)
        mockState = mock
        mock
    }

    suspend fun getCurrentSos(): SosState? = withContext(Dispatchers.IO) {
        try {
            val res = api.getCurrentSos()
            if (res.isSuccessful) {
                val dto = res.body()
                if (dto == null || !dto.active) return@withContext mockState
                val state = dto.toDomain()
                mockState = state
                return@withContext state
            }
        } catch (e: Exception) {
            Log.w("JeevanRepo", "Poll fallback to mock")
        }
        mockState
    }

    // Update ambulance position in mock state (called by ViewModel every 2s)
    fun updateMockAmbulancePosition(newLat: Double, newLng: Double, newEtaMin: Int) {
        mockState = mockState?.copy(
            ambulance = mockState!!.ambulance?.copy(lat = newLat, lng = newLng),
            etaMin    = newEtaMin,
        )
    }

    suspend fun clearSos() = withContext(Dispatchers.IO) {
        mockState = null
        try { api.clearSos() } catch (_: Exception) {}
    }

    suspend fun updateStage(stage: String) = withContext(Dispatchers.IO) {
        mockState = mockState?.copy(stage = stage)
        try { api.updateStage(StageUpdateRequest(stage)) } catch (_: Exception) {}
    }

    suspend fun addPatient(req: PatientAddRequest): Boolean = withContext(Dispatchers.IO) {
        try { api.addPatient(req).isSuccessful } catch (_: Exception) { false }
    }

    suspend fun getAllPatients(): List<PatientDto> = withContext(Dispatchers.IO) {
        try { api.getAllPatients().body() ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    // ── Mock state builder ──
    private fun buildMockSosState(lat: Double, lng: Double): SosState {
        val amb = mockAmbulances[0]
        return SosState(
            active    = true,
            stage     = "to_user",
            sosId     = "SOS-${System.currentTimeMillis()}",
            user      = LatLng(lat, lng),
            ambulance = amb.copy(lat = lat + 0.018, lng = lng + 0.018),
            hospital  = HospitalInfo(
                id = "H-mock", name = "City General Hospital",
                speciality = "Multi-Specialty", distanceKm = 3.2,
                beds = 12, phone = "108",
                lat = lat + 0.03, lng = lng + 0.01,
            ),
            etaMin    = 6,
            etaSec    = 0,
            totalKm   = 3.5,
            createdAt = java.time.Instant.now().toString(),
        )
    }

    fun getMockNearbyHospitals(lat: Double, lng: Double): List<HospitalInfo> = listOf(
        HospitalInfo("H-001", "City General Hospital",  "Multi-Specialty", 1.2, "108",         14, lat+0.01,  lng+0.008),
        HospitalInfo("H-002", "Apollo Medical Centre",  "Cardiology",      2.1, "011-4600000", 22, lat-0.015, lng+0.02),
        HospitalInfo("H-003", "Fortis Healthcare",      "Trauma & Burns",  2.8, "011-4625000", 18, lat+0.025, lng-0.01),
        HospitalInfo("H-004", "AIIMS Emergency",        "General Surgery", 3.5, "011-2659346",  8, lat-0.03,  lng+0.025),
    )

    // ── OSRM Routing ──
    // Fetches actual road-following coordinates from OSRM public API
    suspend fun getOsrmRoute(from: LatLng, to: LatLng): List<LatLng> = withContext(Dispatchers.IO) {
        try {
            val url = "http://router.project-osrm.org/route/v1/driving/" +
                "${from.lng},${from.lat};${to.lng},${to.lat}" +
                "?overview=full&geometries=geojson"
            val request = Request.Builder().url(url)
                .addHeader("User-Agent", "JEEVAN-App")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext listOf(from, to)
            val json = JSONObject(body)
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@withContext listOf(from, to)
            val coords = routes.getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")
            (0 until coords.length()).map { i ->
                val pt = coords.getJSONArray(i)
                LatLng(pt.getDouble(1), pt.getDouble(0)) // OSRM gives [lng, lat]
            }
        } catch (e: Exception) {
            Log.w("OSRM", "Route fetch failed, using straight line: ${e.message}")
            listOf(from, to)
        }
    }

    // ── Haversine distance (km) ──
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng/2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // ── Format timestamp for history ──
    fun nowFormatted(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
}
