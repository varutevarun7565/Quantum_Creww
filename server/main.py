"""
JEEVAN Emergency SOS — FastAPI Backend v3.0
MongoDB Atlas: cluster0.oylj9fp.mongodb.net
Collections:
  users                   — USER role profiles
  ambulance_drivers       — DRIVER profiles + vehicle info
  ambulance_live_location — GPS snapshots every 5 sec (TTL 24h)
  sos_state               — Active SOS tracking
  patients                — Hospital patient records
Run: uvicorn main:app --reload --host 0.0.0.0 --port 8000
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient
from pydantic import BaseModel
from typing import Optional, List
import math, datetime, uuid, logging, os

# Structured logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger("jeevan")

# ──────────────────────────────────────────────
# MongoDB Atlas connection
# Reads from env var on cloud, falls back to local dev string
# ──────────────────────────────────────────────
MONGO_URI = os.getenv(
    "MONGO_URI",
    "mongodb+srv://kamateadarsh17_db_user:Adarsh2005@cluster0.oylj9fp.mongodb.net/jeevan?retryWrites=true&w=majority&appName=Cluster0"
)
DB_NAME = os.getenv("DB_NAME", "jeevan")

client = AsyncIOMotorClient(MONGO_URI)
db     = client[DB_NAME]

# ── Collections ──────────────────────────────
sos_col           = db["sos_state"]
users_col         = db["users"]
patients_col      = db["patients"]
drivers_col       = db["ambulance_drivers"]
live_loc_col      = db["ambulance_live_location"]

app = FastAPI(title="JEEVAN Emergency SOS API", version="3.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

# ──────────────────────────────────────────────
# Startup: create indexes (runs once on boot)
# ──────────────────────────────────────────────
@app.on_event("startup")
async def startup():
    # 1. Verify MongoDB Atlas connection
    try:
        await client.admin.command("ping")
        log.info("[DB] Connected to MongoDB Atlas successfully")
        log.info(f"[DB] Database : {DB_NAME}")
        cols = await db.list_collection_names()
        log.info(f"[DB] Collections: {cols}")
    except Exception as e:
        log.error(f"[DB] MongoDB connection FAILED: {e}")

    # 2. Create indexes (safe — ignores if already exist)
    try:
        await users_col.create_index("userId", unique=True)
        await drivers_col.create_index("userId", unique=True)
        await drivers_col.create_index("ambulanceId", unique=True)
        await drivers_col.create_index("status")
        await live_loc_col.create_index("ambulanceId")
        await live_loc_col.create_index("timestamp", expireAfterSeconds=86400)
        await sos_col.create_index("active")
        await sos_col.create_index("sosId")
        await patients_col.create_index("ambulanceId")
        await patients_col.create_index("sosId")
        log.info("[DB] Indexes verified")
    except Exception as e:
        log.info(f"[DB] Index note (already exists): {e}")


# ──────────────────────────────────────────────
# Pydantic Schemas
# ──────────────────────────────────────────────

# ── Shared ──
class LocationModel(BaseModel):
    lat: float
    lng: float

# ── SOS ──
class SosTriggerRequest(BaseModel):
    lat: float
    lng: float

class AmbulancePositionRequest(BaseModel):
    lat: float
    lng: float
    etaMin: Optional[int] = None

# ── Patients ──
class PatientAddRequest(BaseModel):
    name: str
    condition: str
    time: str
    ambulanceId: str
    sosId: Optional[str] = None

# ── USER Registration ──
class UserRegisterRequest(BaseModel):
    userId: str
    name: str
    role: str = "USER"
    contactNumber: str = ""
    emergencyContact: str = ""
    bloodGroup: str = ""
    age: str = ""
    medicalCondition: str = ""
    password: str
    photoUri: Optional[str] = None

class LoginRequest(BaseModel):
    userId: str
    password: str

# ── DRIVER Registration ──
class DriverRegisterRequest(BaseModel):
    userId: str
    name: str
    contactNumber: str = ""
    password: str
    photoUri: Optional[str] = None
    # Ambulance details
    ambulanceId: str
    ambulanceNumber: str
    ambulanceType: str = "ALS"   # ALS | BLS
    rating: float = 5.0

# ── DRIVER Live Location (pushed every 5 sec) ──
class DriverLocationRequest(BaseModel):
    ambulanceId: str
    driverUserId: str
    sosId: Optional[str] = None
    lat: float
    lng: float
    speed: Optional[float] = 0.0    # km/h
    heading: Optional[float] = 0.0  # degrees 0-360
    stage: Optional[str] = "available"

# ── DRIVER Status Update ──
class DriverStatusRequest(BaseModel):
    ambulanceId: str
    status: str   # available | dispatched | at_patient | to_hospital | offline
    sosId: Optional[str] = None


# ──────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────

def haversine_km(lat1, lng1, lat2, lng2) -> float:
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlng = math.radians(lng2 - lng1)
    a = (math.sin(dlat/2)**2 +
         math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlng/2)**2)
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

def now_str() -> str:
    return datetime.datetime.utcnow().strftime("%d %b %Y, %I:%M %p")

def now_iso() -> datetime.datetime:
    return datetime.datetime.utcnow()

def serialize(doc: dict) -> dict:
    doc.pop("_id", None)
    return doc

def mock_ambulance(user_lat: float, user_lng: float) -> dict:
    return {
        "id": "AMB-2049", "number": "MH 12 AB 2049",
        "driver": "Rajesh Kumar", "rating": 4.9,
        "type": "ALS", "phone": "+919812345678",
        "lat": user_lat + 0.012, "lng": user_lng + 0.008,
    }

def mock_hospital(user_lat: float, user_lng: float) -> dict:
    return {
        "id": "H-001", "name": "Apollo Hospital",
        "speciality": "Emergency & Trauma",
        "distanceKm": 3.2, "phone": "011-26925801",
        "beds": 8,
        "lat": user_lat - 0.02, "lng": user_lng + 0.015,
    }


# ──────────────────────────────────────────────
# ROUTES
# ──────────────────────────────────────────────

@app.get("/")
async def health():
    return {"status": "JEEVAN backend running v3.0", "db": DB_NAME}


# ═══════════════════════════════════════════════
# SOS  (unchanged from v2)
# ═══════════════════════════════════════════════

@app.post("/sos")
async def trigger_sos(body: SosTriggerRequest):
    # Try to find a real available driver first
    real_driver = await drivers_col.find_one({"status": "available"})
    if real_driver:
        amb = {
            "id":     real_driver["ambulanceId"],
            "number": real_driver["ambulanceNumber"],
            "driver": real_driver["name"],
            "rating": real_driver.get("rating", 4.9),
            "type":   real_driver.get("ambulanceType", "ALS"),
            "phone":  real_driver.get("contactNumber", ""),
            "lat":    real_driver.get("currentLat", body.lat + 0.012),
            "lng":    real_driver.get("currentLng", body.lng + 0.008),
        }
        # Mark driver as dispatched
        await drivers_col.update_one(
            {"ambulanceId": real_driver["ambulanceId"]},
            {"$set": {"status": "dispatched", "updatedAt": now_str()}}
        )
    else:
        amb = mock_ambulance(body.lat, body.lng)

    hosp     = mock_hospital(body.lat, body.lng)
    dist_amb = haversine_km(body.lat, body.lng, amb["lat"],  amb["lng"])
    dist_h   = haversine_km(body.lat, body.lng, hosp["lat"], hosp["lng"])
    total_km = round(dist_amb + dist_h, 2)
    eta_min  = max(2, int((dist_amb / 40) * 60))

    sos_id = str(uuid.uuid4())[:8].upper()
    state  = {
        "active": True, "stage": "to_user",
        "sosId": sos_id, "etaMin": eta_min, "totalKm": total_km,
        "createdAt": now_str(),
        "user":      {"lat": body.lat, "lng": body.lng},
        "ambulance": amb,
        "hospital":  hosp,
    }
    await sos_col.replace_one({"active": True}, state, upsert=True)
    return {"message": "SOS dispatched", "state": state}


@app.get("/sos/current")
async def get_current_sos():
    doc = await sos_col.find_one({"active": True})
    return serialize(doc) if doc else None


@app.post("/sos/ambulance-position")
async def update_ambulance_position(body: AmbulancePositionRequest):
    update = {"ambulance.lat": body.lat, "ambulance.lng": body.lng}
    if body.etaMin is not None:
        update["etaMin"] = body.etaMin
    await sos_col.update_one({"active": True}, {"$set": update})
    return {"message": "Position updated"}


@app.post("/sos/stage")
async def update_stage(payload: dict):
    stage = payload.get("stage", "to_user")
    await sos_col.update_one({"active": True}, {"$set": {"stage": stage}})
    return {"message": f"Stage set to {stage}"}


@app.post("/sos/clear")
async def clear_sos():
    await sos_col.update_many({"active": True}, {"$set": {"active": False, "stage": "completed"}})
    # Free up the dispatched driver
    await drivers_col.update_many(
        {"status": {"$in": ["dispatched", "at_patient", "to_hospital"]}},
        {"$set": {"status": "available", "currentSosId": None, "updatedAt": now_str()}}
    )
    return {"message": "SOS cleared"}


# ═══════════════════════════════════════════════
# PATIENTS  (unchanged)
# ═══════════════════════════════════════════════

@app.post("/patient/add")
async def add_patient(body: PatientAddRequest):
    doc = body.dict()
    doc["createdAt"] = now_str()
    await patients_col.insert_one(doc)
    return {"message": "Patient record saved"}


@app.get("/patient/all")
async def get_all_patients():
    docs = await patients_col.find().sort("createdAt", -1).to_list(100)
    return [serialize(d) for d in docs]


# ═══════════════════════════════════════════════
# AUTH — USER role  (unchanged from v2)
# ═══════════════════════════════════════════════

@app.post("/auth/register")
async def register_user(body: UserRegisterRequest):
    log.info(f"[REGISTER] Incoming signup: userId={body.userId}, role={body.role}")

    # Duplicate check — users collection
    existing_user = await users_col.find_one({"userId": body.userId})
    if existing_user:
        log.warning(f"[REGISTER] Duplicate userId in users: {body.userId}")
        raise HTTPException(status_code=409, detail="User ID already taken")

    # Duplicate check — drivers collection
    existing_driver = await drivers_col.find_one({"userId": body.userId})
    if existing_driver:
        log.warning(f"[REGISTER] userId already taken by a driver: {body.userId}")
        raise HTTPException(status_code=409, detail="User ID already taken by a driver")

    # Build document
    doc = body.dict()
    doc["createdAt"] = now_str()
    doc["updatedAt"] = now_str()

    log.info(f"[REGISTER] Inserting into 'users' collection: {doc}")

    # Insert into MongoDB
    result = await users_col.insert_one(doc)
    inserted_id = str(result.inserted_id)

    log.info(f"[REGISTER] SUCCESS — inserted_id: {inserted_id}, userId: {body.userId}")

    # Return without _id
    doc.pop("_id", None)
    return {
        "message": "Registered successfully",
        "insertedId": inserted_id,
        "user": doc,
    }


@app.post("/auth/login")
async def login(body: LoginRequest):
    log.info(f"[LOGIN] Attempt: userId={body.userId}")

    # Check users collection first
    user = await users_col.find_one({"userId": body.userId, "password": body.password})
    if user:
        log.info(f"[LOGIN] SUCCESS from users collection: {body.userId}, role={user.get('role')}")
        return {"message": "Login successful", "user": serialize(user)}

    # Check drivers collection
    driver = await drivers_col.find_one({"userId": body.userId, "password": body.password})
    if driver:
        log.info(f"[LOGIN] SUCCESS from ambulance_drivers: {body.userId}")
        profile = {
            "userId":           driver["userId"],
            "name":             driver.get("name", ""),
            "role":             "DRIVER",
            "contactNumber":    driver.get("contactNumber", ""),
            "emergencyContact": "",
            "bloodGroup":       "",
            "age":              "",
            "medicalCondition": "",
            "password":        driver.get("password", ""),
            "photoUri":        driver.get("photoUri"),
        }
        return {"message": "Login successful", "user": profile}
    raise HTTPException(status_code=401, detail="Invalid credentials")


@app.put("/auth/profile")
async def update_profile(body: UserRegisterRequest):
    doc = body.dict()
    doc["updatedAt"] = now_str()
    await users_col.replace_one({"userId": body.userId}, doc, upsert=True)
    return {"message": "Profile updated", "user": doc}


@app.get("/auth/users")
async def list_users():
    docs = await users_col.find().to_list(200)
    return [serialize(d) for d in docs]


# ═══════════════════════════════════════════════
# DRIVER  — NEW endpoints
# ═══════════════════════════════════════════════

@app.post("/driver/register")
async def register_driver(body: DriverRegisterRequest):
    """
    Register ambulance driver.
    Saves to ambulance_drivers collection.
    Also cross-checks users collection so userId stays unique globally.
    """
    if await drivers_col.find_one({"userId": body.userId}):
        raise HTTPException(status_code=409, detail="Driver userId already registered")
    if await drivers_col.find_one({"ambulanceId": body.ambulanceId}):
        raise HTTPException(status_code=409, detail="Ambulance ID already registered")
    if await users_col.find_one({"userId": body.userId}):
        raise HTTPException(status_code=409, detail="User ID already taken by a user account")

    doc = {
        "userId":          body.userId,
        "name":            body.name,
        "role":            "DRIVER",
        "contactNumber":   body.contactNumber,
        "password":        body.password,
        "photoUri":        body.photoUri,
        "ambulanceId":     body.ambulanceId,
        "ambulanceNumber": body.ambulanceNumber,
        "ambulanceType":   body.ambulanceType,
        "rating":          body.rating,
        "status":          "available",   # available | dispatched | at_patient | to_hospital | offline
        "currentSosId":    None,
        "currentLat":      0.0,
        "currentLng":      0.0,
        "createdAt":       now_str(),
        "updatedAt":       now_str(),
    }
    await drivers_col.insert_one(doc)
    return {"message": "Driver registered", "driver": {k: v for k, v in doc.items() if k != "_id"}}


@app.get("/driver/all")
async def list_drivers():
    docs = await drivers_col.find().to_list(100)
    return [serialize(d) for d in docs]


@app.get("/driver/{ambulance_id}")
async def get_driver(ambulance_id: str):
    doc = await drivers_col.find_one({"ambulanceId": ambulance_id})
    if not doc:
        raise HTTPException(status_code=404, detail="Driver not found")
    return serialize(doc)


@app.put("/driver/status")
async def update_driver_status(body: DriverStatusRequest):
    """Update driver availability status."""
    update = {"status": body.status, "updatedAt": now_str()}
    if body.sosId is not None:
        update["currentSosId"] = body.sosId
    result = await drivers_col.update_one(
        {"ambulanceId": body.ambulanceId}, {"$set": update}
    )
    if result.matched_count == 0:
        raise HTTPException(status_code=404, detail="Ambulance not found")
    return {"message": f"Status updated to {body.status}"}


# ═══════════════════════════════════════════════
# LIVE LOCATION — saves every 5 seconds
# ═══════════════════════════════════════════════

@app.post("/driver/location")
async def push_driver_location(body: DriverLocationRequest):
    """
    Called by driver's Android app every 5 seconds.
    1. Inserts a new GPS snapshot into ambulance_live_location (TTL 24h)
    2. Updates the driver's currentLat/Lng in ambulance_drivers
    3. Updates ambulance position in active sos_state (if sosId present)
    """
    # 1. Insert GPS snapshot
    snapshot = {
        "ambulanceId":   body.ambulanceId,
        "driverUserId":  body.driverUserId,
        "sosId":         body.sosId,
        "lat":           body.lat,
        "lng":           body.lng,
        "speed":         body.speed,
        "heading":       body.heading,
        "stage":         body.stage,
        "timestamp":     now_iso(),   # datetime for TTL index
        "timestampStr":  now_str(),   # human-readable
    }
    await live_loc_col.insert_one(snapshot)

    # 2. Update driver's current position in ambulance_drivers
    await drivers_col.update_one(
        {"ambulanceId": body.ambulanceId},
        {"$set": {
            "currentLat": body.lat,
            "currentLng": body.lng,
            "updatedAt":  now_str(),
        }}
    )

    # 3. If this driver is on an active SOS, update sos_state too
    if body.sosId:
        await sos_col.update_one(
            {"sosId": body.sosId, "active": True},
            {"$set": {"ambulance.lat": body.lat, "ambulance.lng": body.lng}}
        )

    return {"message": "Location saved"}


@app.get("/driver/{ambulance_id}/location/latest")
async def get_latest_location(ambulance_id: str):
    """Return the most recent GPS snapshot for an ambulance."""
    doc = await live_loc_col.find_one(
        {"ambulanceId": ambulance_id},
        sort=[("timestamp", -1)]
    )
    if not doc:
        raise HTTPException(status_code=404, detail="No location data found")
    return serialize(doc)


@app.get("/driver/{ambulance_id}/location/trail")
async def get_location_trail(ambulance_id: str, limit: int = 60):
    """Return last N GPS snapshots (default 60 = last 5 minutes at 5-sec interval)."""
    docs = await live_loc_col.find(
        {"ambulanceId": ambulance_id}
    ).sort("timestamp", -1).limit(limit).to_list(limit)
    return [serialize(d) for d in docs]
