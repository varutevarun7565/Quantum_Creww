"""
JEEVAN -- MongoDB Atlas Setup Script
Run ONCE to create all collections, indexes, and seed demo accounts.

Usage:
    pip install pymongo dnspython
    python setup_db.py
"""

import sys
# Force UTF-8 output on Windows to avoid cp1252 emoji crash
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from pymongo import MongoClient, ASCENDING, DESCENDING
from pymongo.errors import CollectionInvalid, DuplicateKeyError, OperationFailure
import datetime

# ──────────────────────────────────────────────
# Connection  (explicit options for Atlas SRV)
# ──────────────────────────────────────────────
MONGO_URI = "mongodb+srv://kamateadarsh17_db_user:Adarsh2005@cluster0.oylj9fp.mongodb.net/jeevan?retryWrites=true&w=majority&appName=Cluster0"
DB_NAME = "jeevan"

print("Connecting to MongoDB Atlas...")
print(f"  Cluster : cluster0.oylj9fp.mongodb.net")
print(f"  User    : kamateadarsh17_db_user")
print(f"  DB      : {DB_NAME}")
print()

try:
    client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=15000)
    client.admin.command("ping")
    print("[OK] Connected to MongoDB Atlas!\n")
except OperationFailure as e:
    print(f"[FAIL] Authentication error: {e}")
    print()
    print("Possible fixes:")
    print("  1. Go to MongoDB Atlas -> Database Access")
    print("     Edit user 'kamateadarsh17_db_user' -> reset password")
    print("  2. Go to MongoDB Atlas -> Network Access")
    print("     Add IP 0.0.0.0/0 (Allow from anywhere)")
    print("  3. Make sure the user has 'readWriteAnyDatabase' role")
    sys.exit(1)
except Exception as e:
    print(f"[FAIL] Connection error: {e}")
    print()
    print("Possible fixes:")
    print("  1. Check internet connection")
    print("  2. Run: pip install dnspython")
    print("  3. Check Atlas Network Access -> add IP 0.0.0.0/0")
    sys.exit(1)

db = client[DB_NAME]


def now():
    return datetime.datetime.utcnow().strftime("%d %b %Y, %I:%M %p")


def ensure_collection(name):
    if name not in db.list_collection_names():
        db.create_collection(name)
        print(f"  [NEW] Collection created : {name}")
    else:
        print(f"  [OK]  Collection exists  : {name}")
    return db[name]


# ══════════════════════════════════════════════
# 1. USERS
# ══════════════════════════════════════════════
print("-- Setting up: users --")
users = ensure_collection("users")
users.create_index([("userId", ASCENDING)], unique=True, name="idx_userId_unique")
print("  [IDX] userId (unique)")
print("  [OK]  users ready\n")


# ══════════════════════════════════════════════
# 2. AMBULANCE_DRIVERS
# ══════════════════════════════════════════════
print("-- Setting up: ambulance_drivers --")
drivers = ensure_collection("ambulance_drivers")
drivers.create_index([("userId",      ASCENDING)], unique=True, name="idx_driver_userId")
drivers.create_index([("ambulanceId", ASCENDING)], unique=True, name="idx_ambulanceId")
drivers.create_index([("status",      ASCENDING)], name="idx_status")
print("  [IDX] userId (unique), ambulanceId (unique), status")
print("  [OK]  ambulance_drivers ready\n")


# ══════════════════════════════════════════════
# 3. AMBULANCE_LIVE_LOCATION  (TTL: 24h)
# ══════════════════════════════════════════════
print("-- Setting up: ambulance_live_location --")
live_loc = ensure_collection("ambulance_live_location")
live_loc.create_index([("ambulanceId", ASCENDING)], name="idx_ambId")
live_loc.create_index(
    [("ambulanceId", ASCENDING), ("timestamp", DESCENDING)],
    name="idx_amb_time"
)
live_loc.create_index(
    [("timestamp", ASCENDING)],
    expireAfterSeconds=86400,
    name="idx_ttl_24h"
)
print("  [IDX] ambulanceId, compound(ambulanceId+timestamp), TTL 24h")
print("  [OK]  ambulance_live_location ready\n")


# ══════════════════════════════════════════════
# 4. SOS_STATE
# ══════════════════════════════════════════════
print("-- Setting up: sos_state --")
sos = ensure_collection("sos_state")
sos.create_index([("active", ASCENDING)], name="idx_active")
sos.create_index([("sosId",  ASCENDING)], name="idx_sosId")
print("  [IDX] active, sosId")
print("  [OK]  sos_state ready\n")


# ══════════════════════════════════════════════
# 5. PATIENTS
# ══════════════════════════════════════════════
print("-- Setting up: patients --")
patients = ensure_collection("patients")
patients.create_index([("ambulanceId", ASCENDING)], name="idx_patient_amb")
patients.create_index([("sosId",       ASCENDING)], name="idx_patient_sos")
print("  [IDX] ambulanceId, sosId")
print("  [OK]  patients ready\n")


# ══════════════════════════════════════════════
# 6. SEED DEMO DATA
# ══════════════════════════════════════════════
print("-- Seeding demo accounts --")

# Driver 1
try:
    drivers.insert_one({
        "userId": "driver1", "name": "Rajesh Kumar", "role": "DRIVER",
        "contactNumber": "9812345678", "password": "driver123", "photoUri": None,
        "ambulanceId": "AMB-2049", "ambulanceNumber": "MH 12 AB 2049",
        "ambulanceType": "ALS", "rating": 4.9, "status": "available",
        "currentSosId": None, "currentLat": 0.0, "currentLng": 0.0,
        "createdAt": now(), "updatedAt": now(),
    })
    print("  [OK]  driver1 / driver123  (AMB-2049) seeded")
except DuplicateKeyError:
    print("  [SKIP] driver1 already exists")

# Driver 2
try:
    drivers.insert_one({
        "userId": "driver2", "name": "Sunil Verma", "role": "DRIVER",
        "contactNumber": "9987654321", "password": "driver123", "photoUri": None,
        "ambulanceId": "AMB-1187", "ambulanceNumber": "MH 04 BE 1187",
        "ambulanceType": "BLS", "rating": 4.8, "status": "available",
        "currentSosId": None, "currentLat": 0.0, "currentLng": 0.0,
        "createdAt": now(), "updatedAt": now(),
    })
    print("  [OK]  driver2 / driver123  (AMB-1187) seeded")
except DuplicateKeyError:
    print("  [SKIP] driver2 already exists")

# Hospital
try:
    users.insert_one({
        "userId": "hospital1", "name": "City Hospital", "role": "HOSPITAL",
        "contactNumber": "011-26925801", "emergencyContact": "MG Road, Delhi",
        "bloodGroup": "", "age": "12", "medicalCondition": "Emergency and Trauma",
        "password": "hosp123", "photoUri": None,
        "createdAt": now(), "updatedAt": now(),
    })
    print("  [OK]  hospital1 / hosp123 seeded")
except DuplicateKeyError:
    print("  [SKIP] hospital1 already exists")

# Admin
try:
    users.insert_one({
        "userId": "admin1", "name": "Admin", "role": "ADMIN",
        "contactNumber": "", "emergencyContact": "", "bloodGroup": "",
        "age": "", "medicalCondition": "", "password": "admin123",
        "photoUri": None, "createdAt": now(), "updatedAt": now(),
    })
    print("  [OK]  admin1 / admin123 seeded")
except DuplicateKeyError:
    print("  [SKIP] admin1 already exists")


# ══════════════════════════════════════════════
# Summary
# ══════════════════════════════════════════════
print()
print("=" * 50)
print("JEEVAN MongoDB Setup Complete!")
print(f"  Database : {DB_NAME}")
print("  Collections:")
for col in sorted(db.list_collection_names()):
    count = db[col].count_documents({})
    print(f"    {col:<35} {count} doc(s)")
print("=" * 50)
