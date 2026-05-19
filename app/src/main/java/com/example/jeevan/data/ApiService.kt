package com.example.jeevan.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// ── Retrofit DTOs (matching FastAPI + MongoDB backend) ──

data class SosTriggerRequest(val latitude: Double, val longitude: Double)

data class SosResponse(val message: String?, val state: SosStateDto?)

data class SosStateDto(
    val active: Boolean,
    val stage: String,
    val sosId: String?,
    val etaMin: Int?,
    @SerializedName("totalKm")            val totalKm: Double?,
    val createdAt: String?,
    val user: LocationDto?,
    val ambulance: AmbulanceDto?,
    val hospital: HospitalDto?,
    @SerializedName("user_location")      val userLocation: List<Double>?,
    @SerializedName("ambulance_location") val ambulanceLocation: List<Double>?,
    @SerializedName("hospital_location")  val hospitalLocation: List<Double>?,
)

data class LocationDto(val latitude: Double?, val longitude: Double?)

data class AmbulanceDto(
    val id: String?, val number: String?, val driver: String?,
    val rating: Double?, val type: String?, val phone: String?,
    val latitude: Double?, val longitude: Double?,
)

data class HospitalDto(
    val id: String?, val name: String?, val speciality: String?,
    @SerializedName("distanceKm") val distanceKm: Double?,
    val phone: String?, val beds: Int?, val latitude: Double?, val longitude: Double?,
)

data class AmbulancePositionRequest(val latitude: Double, val longitude: Double, val etaMin: Int?)

data class StageUpdateRequest(val stage: String)

data class PatientAddRequest(
    val name: String, val condition: String, val time: String,
    @SerializedName("ambulanceId") val ambulanceId: String,
    val sosId: String?,
)

data class PatientDto(
    val id: String?, val name: String?, val condition: String?,
    val time: String?,
    @SerializedName("ambulanceId")  val ambulanceId: String?,
    @SerializedName("ambulance_id") val ambulanceId2: String?,
    val sosId: String?,
)

// ── Auth DTOs ──

data class RegisterRequest(
    val userId: String, val name: String, val role: String,
    val contactNumber: String, val emergencyContact: String,
    val bloodGroup: String, val age: String, val medicalCondition: String,
    val password: String, val photoUri: String?,
)

data class LoginRequest(val userId: String, val password: String)

data class UserDto(
    val userId: String,
    val name: String?,
    val role: String?,
    val contactNumber: String?,
    val emergencyContact: String?,
    val bloodGroup: String?,
    val age: String?,
    val medicalCondition: String?,
    val password: String?,
    val photoUri: String?,
)

data class AuthResponse(val message: String?, val user: UserDto?)

// ── Driver DTOs ──

data class DriverRegisterRequest(
    val userId: String, val name: String,
    val contactNumber: String, val password: String,
    val photoUri: String?,
    val ambulanceId: String, val ambulanceNumber: String,
    val ambulanceType: String = "ALS", val rating: Float = 5.0f,
)

data class DriverLocationRequest(
    val ambulanceId: String,
    val driverUserId: String,
    val sosId: String?,
    val latitude: Double,
    val longitude: Double,
    val speed: Float = 0f,
    val heading: Float = 0f,
    val stage: String = "dispatched",
)

data class DriverStatusRequest(
    val ambulanceId: String,
    val status: String,
    val sosId: String? = null,
)

data class DriverDto(
    val userId: String,
    val name: String?,
    val ambulanceId: String?,
    val ambulanceNumber: String?,
    val ambulanceType: String?,
    val rating: Double?,
    val status: String?,
    val currentLatitude: Double?,
    val currentLongitude: Double?,
    val contactNumber: String?,
)

data class LiveLocationDto(
    val ambulanceId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val speed: Double?,
    val heading: Double?,
    val stage: String?,
    val timestampStr: String?,
)

// ── Retrofit API Interface ──

interface JeevanApiService {

    // SOS
    @POST("/sos")
    suspend fun triggerSos(@Body body: SosTriggerRequest): Response<SosResponse>

    @GET("/sos/current")
    suspend fun getCurrentSos(): Response<SosStateDto?>

    @POST("/sos/ambulance-position")
    suspend fun updateAmbulancePosition(@Body body: AmbulancePositionRequest): Response<Any>

    @POST("/sos/stage")
    suspend fun updateStage(@Body body: StageUpdateRequest): Response<Any>

    @POST("/sos/clear")
    suspend fun clearSos(): Response<Any>

    // Patients
    @POST("/patient/add")
    suspend fun addPatient(@Body body: PatientAddRequest): Response<Any>

    @GET("/patient/all")
    suspend fun getAllPatients(): Response<List<PatientDto>>

    // Auth
    @POST("/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @PUT("/auth/profile")
    suspend fun updateProfile(@Body body: RegisterRequest): Response<AuthResponse>

    // Driver
    @POST("/driver/register")
    suspend fun registerDriver(@Body body: DriverRegisterRequest): Response<Any>

    @POST("/driver/location")
    suspend fun pushDriverLocation(@Body body: DriverLocationRequest): Response<Any>

    @PUT("/driver/status")
    suspend fun updateDriverStatus(@Body body: DriverStatusRequest): Response<Any>

    @GET("/driver/{ambulanceId}/location/latest")
    suspend fun getLatestLocation(@Path("ambulanceId") ambulanceId: String): Response<LiveLocationDto>

    @GET("/")
    suspend fun healthCheck(): Response<Any>
}
