package com.example.jeevan.data

// ── Existing models ──

data class LatLng(val lat: Double, val lng: Double)

data class AmbulanceInfo(
    val id: String      = "AMB-2049",
    val number: String  = "MH 12 AB 2049",
    val driver: String  = "Rajesh Kumar",
    val rating: Double  = 4.9,
    val type: String    = "ALS",
    val phone: String   = "+919812345678",
    val lat: Double     = 0.0,
    val lng: Double     = 0.0,
)

data class HospitalInfo(
    val id: String         = "H-001",
    val name: String       = "Nearest Hospital",
    val speciality: String = "General",
    val distanceKm: Double = 2.0,
    val phone: String      = "",
    val beds: Int          = 8,
    val lat: Double        = 0.0,
    val lng: Double        = 0.0,
)

data class SosState(
    val active: Boolean           = false,
    val stage: String             = "idle",
    val sosId: String?            = null,
    val user: LatLng?             = null,
    val ambulance: AmbulanceInfo? = null,
    val hospital: HospitalInfo?   = null,
    val etaMin: Int               = 6,
    val etaSec: Int               = 0,      // ← NEW: second-level countdown
    val totalKm: Double           = 2.0,
    val createdAt: String?        = null,
)

data class PatientRecord(
    val id: String       = "",
    val name: String     = "",
    val condition: String = "",
    val time: String     = "",
    val ambulanceId: String = "",
    val sosId: String?   = null,
)

enum class AgentStatus { PENDING, ACTIVE, SUCCESS, ERROR }

data class AgentStep(
    val key: String,
    val agentName: String,
    val description: String,
    val detail: String      = "",
    val status: AgentStatus = AgentStatus.PENDING,
)

// ── NEW: Auth models ──

enum class UserRole { USER, DRIVER, HOSPITAL, ADMIN }

data class UserProfile(
    val userId: String,
    val name: String          = "",
    val role: UserRole        = UserRole.USER,
    val contactNumber: String = "",
    val emergencyContact: String = "",
    val bloodGroup: String    = "",
    val age: String           = "",
    val medicalCondition: String = "",
    val password: String      = "",   // stored plain for prototype; hash in production
    val photoUri: String?     = null,
)

data class AuthState(
    val isLoggedIn: Boolean      = false,
    val currentUser: UserProfile? = null,
)

// ── NEW: SOS History ──

data class SosHistoryEntry(
    val id: String,
    val timestamp: String,
    val userAddress: String,
    val ambulanceNumber: String,
    val hospitalName: String,
    val etaMin: Int,
    val distanceKm: Double,
    val stage: String = "completed",
)
