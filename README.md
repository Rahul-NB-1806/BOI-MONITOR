# BOI Monitor

Real-time Bank of India notification monitoring with voice announcements and MongoDB storage.

## Architecture

```
Android App ──Retrofit/HTTP──> Node.js/Express API ──Mongoose──> MongoDB Atlas
                                    ↑
                              Hosted on Render
```

### Project Structure

```
BOIMonitor/
├── app/src/main/java/com/boi/monitor/
│   ├── service/BOINotificationListenerService.java   ← Listens to all notifications
│   ├── parser/NotificationParser.java                ← Filters BOI msgs + parses them
│   ├── network/                                      ← Retrofit API client, auth, offline queue
│   ├── voice/VoiceEngine.java                        ← TTS engine for UPI announcements
│   ├── viewmodel/DashboardViewModel.java
│   └── ui/                                           ← Dashboard, UPI list, Cheque list, Admin
└── server/                                           ← Node.js backend
    ├── src/index.js                                  ← Express server + MongoDB connection
    ├── src/models/                                   ← Mongoose schemas (UPI, Cheque, Logs, Admin)
    ├── src/routes/                                   ← REST API routes
    └── render.yaml                                   ← Render deployment config
```

## Features

- **Notification filter** — Only processes messages containing "BOI" and account "XXX004"
- **Parses 4 patterns** — UPI Credit, Cheque Cleared, Cheque Returned, Cheque Presented
- **Voice announcements** — TTS speaks UPI credit amounts (e.g. "Received 1200 rupees through UPI.")
- **MongoDB storage** — All transactions and logs saved via REST API
- **Offline queue** — Failed API calls are stored locally and auto-retried on next notification or app open
- **Swipe to refresh** — Pull down on dashboard to reload data
- **Configurable server URL** — Menu → Server Settings (no rebuild needed for different environments)

## Notification Patterns

| Type | SMS Pattern | Voice |
|------|-------------|-------|
| UPI Credit | `a/c no. XXXX0004 is credited for Rs. XXXX` | ✅ |
| Cheque Cleared | `Cheque No. XXXX Debited(Clearing)` | ❌ |
| Cheque Returned | `Chq.No. XXXX RETURNED` | ❌ |
| Cheque Presented | `Chq.No. XXXX presented in CLEARING` | ❌ |

## Quick Start

### Backend (local)
```bash
cd server
npm install
node src/index.js
```

### Android
1. Open in Android Studio, build and install
2. Enable Notification Access (Settings → Notification Access)
3. Set server URL via Menu → Server Settings

### Deploy to Render
1. Push to GitHub
2. Create Web Service → root dir: `server`
3. Add env vars: `MONGODB_URI`, `JWT_SECRET`, `API_KEY`

## API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/auth/anonymous` | Anonymous login |
| GET/POST | `/api/upi` | List/save UPI transactions |
| GET/POST | `/api/cheques` | List/save cheque transactions |
| GET/POST | `/api/logs` | List/save notification logs |
| DELETE | `/api/user/data` | Delete all user data |
| GET | `/api/admin/stats` | Admin dashboard stats |
