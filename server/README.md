# JEEVAN Backend — Setup & Run

## MongoDB Atlas
Connection URI: `mongodb+srv://kamateadarsh17_db_user:Adarsh2005@cluster0.oylj9fp.mongodb.net/`
Database name: `jeevan`
Collections auto-created: `users`, `sos_state`, `patients`

## Python Setup (run once)
```bash
cd server
pip install -r requirements.txt
```

## Start Server
```bash
cd server
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## Android App Connection
- **Emulator**: `http://10.0.2.2:8000/`  (already configured in JeevanRepository.kt)
- **Real device**: Change `BASE_URL` in `JeevanRepository.kt` to your PC's local IP:
  ```kotlin
  private const val BASE_URL = "http://192.168.x.x:8000/"
  ```
  Find your IP: `ipconfig` → IPv4 address

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /sos | Trigger SOS → saves to MongoDB |
| GET  | /sos/current | Get active SOS state |
| POST | /sos/ambulance-position | Update ambulance GPS |
| POST | /sos/stage | Update dispatch stage |
| POST | /sos/clear | End SOS session |
| POST | /auth/register | Register new user |
| POST | /auth/login | Login → fetch profile from MongoDB |
| PUT  | /auth/profile | Update profile |
| POST | /patient/add | Save patient record |
| GET  | /patient/all | Get all patient history |

## Seed Demo Users
Run this once after server starts to add demo Driver/Hospital accounts:
```bash
curl -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"userId":"driver1","name":"Rajesh Kumar","role":"DRIVER","contactNumber":"9812345678","emergencyContact":"","bloodGroup":"B+","age":"32","medicalCondition":"","password":"driver123","photoUri":null}'

curl -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"userId":"hospital1","name":"City Hospital","role":"HOSPITAL","contactNumber":"011-26925801","emergencyContact":"MG Road, Delhi","bloodGroup":"","age":"12","medicalCondition":"Emergency and Trauma","password":"hosp123","photoUri":null}'
```
