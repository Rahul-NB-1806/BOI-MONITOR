# BOI Monitor — Android Prototype

Real-time Bank of India notification monitoring with Firebase backend, cheque tracking, and UPI voice announcements.

---

## Architecture Overview

```
BOIMonitor/
├── app/src/main/java/com/boi/monitor/
│   ├── BOIApplication.java          ← App init, Firebase, channels
│   ├── service/
│   │   ├── BOINotificationListenerService.java  ← Core listener
│   │   └── BootReceiver.java                    ← Persist after reboot
│   ├── parser/
│   │   └── NotificationParser.java  ← Regex parsing engine
│   ├── model/
│   │   ├── ChequeTransaction.java   ← Firestore model
│   │   ├── UpiTransaction.java      ← Firestore model
│   │   ├── NotificationLog.java     ← Audit log model
│   │   ├── ParsedNotification.java  ← Intermediate parse result
│   │   └── DashboardStats.java      ← Aggregated UI stats
│   ├── firebase/
│   │   └── FirebaseDataModule.java  ← All Firestore operations
│   ├── voice/
│   │   └── VoiceEngine.java         ← TTS (UPI only)
│   ├── viewmodel/
│   │   └── DashboardViewModel.java  ← MVVM bridge
│   └── ui/
│       ├── dashboard/
│       │   ├── MainActivity.java
│       │   ├── UpiTransactionAdapter.java
│       │   └── ChequeAdapter.java
│       └── admin/
│           ├── AdminLoginActivity.java
│           └── AdminPanelActivity.java
├── firestore.rules                  ← Security rules
└── app/google-services.json        ← Replace with real config
```

---

## Notification Filtering Logic

Every incoming notification is tested against:

```
text.contains("BOI") AND text.contains("XXX004")
```

Notifications that do not pass **both** conditions are silently ignored.

---

## Supported Notification Patterns

### 1. Cheque Cleared
```
BOI - Cheque No. 12345 for Rs 50000 Debited(Clearing) in your A/c XX0004
on 27-04-2026 TO CLG. Avl Bal Rs 25000
```
→ Extracts: cheque_number, amount, date, available_balance  
→ Status: CLEARED  
→ Voice: ❌ None

---

### 2. Cheque Returned
```
BOI-Chq.No.12345 amt 50000, acc XXX004,Fvg. NAGAMMAI PHARMA A UN, RETURNED.
Contact branch for details.
```
→ Extracts: cheque_number, amount, favouring_party  
→ Status: RETURNED  
→ Voice: ❌ None

---

### 3. Cheque Presented
```
BOI-Chq.No. 12345 amt 50000 pertaining to acc XXX004,Fvg. NAGAMMAI PHARMA
A UN is presented in CLEARING today.
```
→ Extracts: cheque_number, amount  
→ Status: PRESENTED  
→ Voice: ❌ None

---

### 4. UPI Credit
```
BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 1200 on 20/05/2026
and debited from a/c no. XXXXXX2101 (UPI Ref noXXXXXXXXX)
```
→ Extracts: amount, date, reference_number, debited_account  
→ Type: UPI_CREDIT  
→ Voice: ✅ **"Received 1200 rupees through UPI."**

---

## Firebase Setup

### Step 1: Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create new project: **BOI Monitor**
3. Add Android app with package: `com.boi.monitor`
4. Download `google-services.json` → place in `app/` folder

### Step 2: Enable Services
- **Authentication** → Sign-in method → Email/Password → Enable
- **Firestore** → Create database → Start in production mode
- **Analytics** → Enable

### Step 3: Create Firestore Collections
Create these empty collections:
- `cheque_transactions`
- `upi_transactions`
- `notification_logs`
- `admin_users`
- `app_settings`

### Step 4: Deploy Security Rules
Copy contents of `firestore.rules` to **Firestore → Rules** tab and publish.

### Step 5: Create Admin User
1. Firebase Console → Authentication → Add user
2. Note the generated UID
3. Firestore → `admin_users` → Add document:
   - Document ID: `<the-uid>`
   - Fields: `{ email: "admin@yourdomain.com", role: "admin" }`

---

## Firestore Document Structure

### cheque_transactions/{CHQ_<cheque_number>}
```json
{
  "chequeNumber": "12345",
  "amount": 50000,
  "status": "CLEARED",
  "availableBalance": 25000,
  "transactionDate": "27-04-2026",
  "favouringParty": "NAGAMMAI PHARMA A UN",
  "timestamp": "<server timestamp>"
}
```
**Note:** Document ID is `CHQ_<chequeNumber>` — subsequent status updates (PRESENTED → CLEARED) merge into the same document via `SetOptions.merge()`.

---

### upi_transactions/{UPI_<reference>_<amount>_<date>}
```json
{
  "amount": 1200,
  "transactionType": "UPI_CREDIT",
  "accountSuffix": "0004",
  "referenceNumber": "XXXXXXXXX",
  "debitedAccount": "XXXXXX2101",
  "transactionDate": "20/05/2026",
  "voiceAnnounced": false,
  "timestamp": "<server timestamp>"
}
```

---

### notification_logs/{auto-id}
```json
{
  "notificationType": "UPI_CREDIT",
  "processed": true,
  "processingError": null,
  "rawTextStored": false,
  "timestamp": "<server timestamp>"
}
```

Raw notification text is not stored by default to reduce exposure of financial data.

---

## Android Setup

### Build Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Steps
1. Clone / open project in Android Studio
2. Replace `app/google-services.json` with real file
3. Build → `./gradlew assembleDebug`
4. Install on device running Android 8.0+ (API 26+)

### Grant Notification Access
- Settings → Apps → Special app access → Notification access → **BOI Monitor** → Enable

---

## Admin Access

The admin section is hidden from normal users.

**Access path:** Dashboard → ⋮ (overflow menu) → Admin Panel

**Login requires:** Firebase email + password for a Firebase user whose UID is listed in `admin_users`.

**Admin Panel shows:**
- Total notification log count
- Processed vs unprocessed count
- Signed-in admin email
- App version info

---

## Security Architecture

| Layer | Mechanism |
|-------|-----------|
| Notification filter | BOI + XXX004 double check |
| Admin UI | 4-digit PIN gate |
| Admin backend | Firebase Email/Password Auth |
| Firestore writes | Admin-only (checked via `admin_users` collection) |
| Firestore reads | Authenticated users only |
| Duplicate prevention | In-memory HashSet + Firestore merge upsert |

---

## Voice Announcement Rules

| Notification Type | Voice Announcement |
|-------------------|--------------------|
| CHEQUE_CLEARED    | ❌ Never |
| CHEQUE_RETURNED   | ❌ Never |
| CHEQUE_PRESENTED  | ❌ Never |
| UPI_CREDIT        | ✅ Always |

Format: `"Received {amount} rupees through UPI."`

---

## Background Reliability

- `NotificationListenerService` runs in its own process
- `startForeground()` with `IMPORTANCE_LOW` notification prevents OS kill
- `BootReceiver` ensures reconnection after reboot
- `onListenerDisconnected()` → calls `requestRebind()` for auto-recovery
- `ExecutorService` (single thread) handles parsing asynchronously
- Firebase offline persistence caches writes when network is unavailable

---

## Extending the Parser

To add new notification patterns, open `NotificationParser.java`:

1. Define a new `static final Pattern PATTERN_XYZ`
2. Create a `tryParseXYZ(text, out)` method
3. Add it to the `parse()` chain
4. Handle the new type in `FirebaseDataModule.saveNotification()`

---

## Testing the Parser

Use `NotificationParser.parse()` directly in a unit test:

```java
// JUnit test example
String rawText = "BOI UPI - Your a/c no. XXX004 is credited for Rs. 1200 " +
                 "on 20/05/2026 and debited from a/c no. XXXXXX2101 (UPI Ref noABC123)";

assertTrue(NotificationParser.passesFilter(rawText));
ParsedNotification result = NotificationParser.parse(rawText);
assertEquals(NotificationType.UPI_CREDIT, result.getType());
assertEquals(1200.0, result.getAmount(), 0.01);
assertEquals("ABC123", result.getReferenceNumber());
```

---

## Production Checklist

Before going live:

- [ ] Replace `app/google-services.json` with real Firebase config
- [ ] Deploy `firestore.rules` to Firebase
- [ ] Create admin user in Firebase Auth + `admin_users` Firestore doc
- [ ] Test all 4 notification patterns on a real device
- [ ] Enable `minifyEnabled true` in release build
- [ ] Add ProGuard rules for Firebase and Gson
- [ ] Test background behavior: lock screen, low memory, reboot
- [ ] Verify TTS language availability on target devices

---

*BOI Monitor v1.0.0 — Production Prototype*
#   B O I - M O N I T O R  
 