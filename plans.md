# Plan: Migrate from Firestore to MongoDB Atlas

## Current Architecture
- **Android App (Java)** talking directly to **Google Cloud Firestore** via Firebase SDK
- **Firebase Auth**: Anonymous auth (normal users) + Email/Password (admin users)
- **Collections**: `users/{uid}/upi_transactions`, `users/{uid}/cheque_transactions`, `users/{uid}/notification_logs`, `admin_users`
- **Key files**: `FirebaseDataModule.java` (644 lines), `FirebaseAuthManager.java`, `BOIApplication.java`

## Target Architecture
```
Android App --Retrofit/HTTP--> Node.js/Express API --Mongoose--> MongoDB Atlas
                                    + Atlas App Services (auth)
```

---

## Phase 1: MongoDB Atlas Setup

### 1.1 Create Atlas Cluster
- Create account at cloud.mongodb.com
- Create a cluster (M0 free tier for dev, M10+ for production)
- Create database: `boi_monitor`

### 1.2 MongoDB Collections (replace Firestore subcollections)

| Firestore Path | MongoDB Collection | Document Schema |
|---|---|---|
| `users/{uid}/upi_transactions` | `upi_transactions` | `{ userId, amount, transactionType, accountSuffix, referenceNumber, debitedAccount, transactionDate, voiceAnnounced, timestamp }` |
| `users/{uid}/cheque_transactions` | `cheque_transactions` | `{ userId, chequeNumber, amount, status, availableBalance, transactionDate, favouringParty, timestamp }` |
| `users/{uid}/notification_logs` | `notification_logs` | `{ userId, notificationType, processed, processingError, rawTextStored, packageName, timestamp }` |
| `admin_users` | `admin_users` | `{ userId, email, createdAt }` |

### 1.3 Create Indexes
```javascript
db.upi_transactions.createIndex({ userId: 1, timestamp: -1 })
db.upi_transactions.createIndex({ userId: 1, referenceNumber: 1 })
db.cheque_transactions.createIndex({ userId: 1, timestamp: -1 })
db.cheque_transactions.createIndex({ userId: 1, chequeNumber: 1 })
db.notification_logs.createIndex({ userId: 1, timestamp: -1 })
db.admin_users.createIndex({ userId: 1 }, { unique: true })
```

---

## Phase 2: Atlas App Services (Auth)

### 2.1 Enable App Services
- Enable Atlas App Services in the Atlas project
- Enable Email/Password authentication provider
- Use API Key authentication for anonymous users (Atlas lacks native anonymous auth)

### 2.2 Auth Strategy
- **Option A (Recommended):** API Key auth -- fixed API key per app build, backend creates/retrieves user sessions
- **Option B:** Custom JWT -- backend generates JWTs for anonymous users

---

## Phase 3: Backend API Server (Node.js + Express)

### 3.1 Project Structure
```
server/
├── package.json
├── src/
│   ├── index.js              # Express app entry
│   ├── config.js             # MongoDB URI, Atlas config
│   ├── middleware/
│   │   ├── auth.js           # API key + user ID verification
│   │   └── errorHandler.js
│   ├── models/
│   │   ├── UpiTransaction.js
│   │   ├── ChequeTransaction.js
│   │   ├── NotificationLog.js
│   │   └── AdminUser.js
│   ├── routes/
│   │   ├── upi.js            # /api/upi/*
│   │   ├── cheques.js        # /api/cheques/*
│   │   ├── logs.js           # /api/logs/*
│   │   ├── admin.js          # /api/admin/*
│   │   └── auth.js           # /api/auth/*
│   └── services/
│       ├── dataService.js    # CRUD operations
│       └── statsService.js   # Dashboard aggregation
```

### 3.2 API Endpoints

| Method | Endpoint | Purpose | Replaces |
|--------|----------|---------|----------|
| POST | `/api/auth/register` | Register user | `FirebaseAuthManager.signIn()` |
| POST | `/api/auth/login` | Login user, return JWT | `FirebaseAuthManager.signIn()` |
| POST | `/api/auth/anonymous` | Create anonymous session | `signInAnonymously()` |
| GET | `/api/upi` | List UPI transactions | Firestore snapshot listener |
| POST | `/api/upi` | Save UPI transaction | `saveUpiCredit()` |
| GET | `/api/cheques` | List cheque transactions | Firestore snapshot listener |
| POST | `/api/cheques` | Save/update cheque (upsert) | `saveChequeCleared/Returned/Presented()` |
| GET | `/api/logs` | List notification logs | Firestore snapshot listener |
| POST | `/api/logs` | Save notification log | `saveLog()` |
| DELETE | `/api/user/data` | Delete all user data | `deleteAllUserData()` |
| GET | `/api/admin/stats` | Admin dashboard stats | `collectionGroup` queries |
| POST | `/api/admin/migrate` | Trigger data migration | `migrateOldDataIfNeeded()` |

### 3.3 Key Implementation: Cheque Upsert
Replaces Firestore's `SetOptions.merge()` pattern:
```javascript
await ChequeTransaction.findOneAndUpdate(
  { userId, chequeNumber: cleanNum },
  { $set: data },
  { upsert: true }
);
```

---

## Phase 4: Android App Changes

### 4.1 Dependency Changes (`app/build.gradle`)
```gradle
// REMOVE:
// implementation platform('com.google.firebase:firebase-bom:32.7.0')
// implementation 'com.google.firebase:firebase-auth'
// implementation 'com.google.firebase:firebase-firestore'

// ADD:
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
```
Keep `firebase-analytics` and `firebase-crashlytics` if desired.

### 4.2 File Changes

| File | Action |
|------|--------|
| `FirebaseDataModule.java` | Rewrite to use Retrofit API calls instead of Firestore |
| `FirebaseAuthManager.java` | Rewrite to use JWT auth with SharedPreferences token storage |
| `BOIApplication.java` | Remove Firestore init, add Retrofit/OkHttp init |
| `ChequeTransaction.java` | Remove `@DocumentId`, `@ServerTimestamp`, `@IgnoreExtraProperties`. Add `userId`. |
| `UpiTransaction.java` | Same as above |
| `NotificationLog.java` | Same as above |
| `Constants.java` | Remove Firestore collection names, add API base URL |
| `DashboardViewModel.java` | No change (still uses LiveData) |
| `AdminPanelActivity.java` | Update to use API |
| `AdminLoginActivity.java` | Update to use API auth |

### 4.3 New Files to Create

| File | Purpose |
|------|---------|
| `app/.../network/ApiDataModule.java` | New data layer (replaces FirebaseDataModule) |
| `app/.../network/BoiApiService.java` | Retrofit interface |
| `app/.../network/AuthManager.java` | New JWT auth manager |
| `app/.../network/AuthInterceptor.java` | OkHttp interceptor for auth tokens |
| `app/.../network/ApiClient.java` | Retrofit client setup |

### 4.4 Retrofit Interface
```java
public interface BoiApiService {
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/anonymous")
    Call<AuthResponse> anonymousAuth(@Header("X-API-Key") String apiKey);

    @GET("api/upi")
    Call<List<UpiTransaction>> getUpiTransactions(
        @Header("Authorization") String token,
        @Query("limit") int limit);

    @POST("api/upi")
    Call<Void> saveUpiTransaction(
        @Header("Authorization") String token,
        @Body UpiTransaction tx);

    @GET("api/cheques")
    Call<List<ChequeTransaction>> getChequeTransactions(
        @Header("Authorization") String token,
        @Query("limit") int limit);

    @POST("api/cheques")
    Call<Void> saveChequeTransaction(
        @Header("Authorization") String token,
        @Body ChequeTransaction tx);

    @POST("api/logs")
    Call<Void> saveLog(
        @Header("Authorization") String token,
        @Body NotificationLog log);

    @DELETE("api/user/data")
    Call<Void> deleteAllData(@Header("Authorization") String token);
}
```

### 4.5 Real-Time Updates Strategy
- **Current:** Firestore `addSnapshotListener()` for real-time data
- **New:** Polling at 30-second intervals using `Handler.postDelayed()`
- **Future upgrade:** Add SSE (Server-Sent Events) endpoint on backend for push updates

### 4.6 Offline Support
- Firestore offline persistence is lost with REST API
- Mitigation: OkHttp cache interceptor + Room/SQLite local cache if needed
- Initial migration: polling is sufficient

---

## Phase 5: Data Migration

### 5.1 Migration Script (`scripts/firestore-to-mongodb.js`)
1. Connect to Firestore via Firebase Admin SDK
2. Read all user data from `users/{uid}/...` subcollections
3. Transform: flatten into documents with `userId` field
4. Insert into MongoDB Atlas via Mongoose

### 5.2 Migration Strategy
- Run migration ONCE before deploying new app version
- Keep Firestore data as backup for 30 days
- New app uses MongoDB Atlas from day one

---

## Phase 6: Admin Panel Changes

Replace `collectionGroup("notification_logs")` with MongoDB aggregation:
```javascript
// GET /api/admin/stats
db.notification_logs.aggregate([
  { $group: { _id: "$notificationType", count: { $sum: 1 } } }
])
```

---

## Phase 7: Testing & Deployment

1. Write unit tests for all API endpoints
2. Test Android app against local dev server
3. Deploy backend (Render, Railway, or VPS)
4. Update `Constants.java` with production API URL
5. Build and test Android app
6. Run Firestore-to-MongoDB migration script
7. Monitor for 24-48 hours before decommissioning Firestore

---

## Summary of Changes

### Files to Modify (11)
- `app/build.gradle`
- `BOIApplication.java`
- `FirebaseDataModule.java` (rewrite)
- `FirebaseAuthManager.java` (rewrite)
- `ChequeTransaction.java`
- `UpiTransaction.java`
- `NotificationLog.java`
- `Constants.java`
- `AdminPanelActivity.java`
- `AdminLoginActivity.java`
- `firestore.rules` (can be removed)

### Files to Create (~15)
- `server/package.json`
- `server/src/index.js`
- `server/src/config.js`
- `server/src/models/*.js` (4 files)
- `server/src/routes/*.js` (5 files)
- `server/src/middleware/auth.js`
- `server/src/services/*.js` (2 files)
- `scripts/firestore-to-mongodb.js`
- `app/.../network/ApiDataModule.java`
- `app/.../network/BoiApiService.java`
- `app/.../network/AuthManager.java`
- `app/.../network/AuthInterceptor.java`
- `app/.../network/ApiClient.java`

### Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Lose real-time Firestore listeners | Dashboard won't auto-refresh | 30-second polling fallback |
| Lose offline persistence | App needs network for data | OkHttp cache + optional Room DB |
| Auth migration complexity | Users lose existing sessions | Force re-authentication on first launch |
| Data migration failures | Missing transaction history | Run migration script with rollback plan |
