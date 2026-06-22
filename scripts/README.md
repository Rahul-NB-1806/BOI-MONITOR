# Firestore to MongoDB Migration

Migrates user data from Firestore subcollections into MongoDB Atlas collections.

## Collections Migrated

| Firestore Path | MongoDB Collection | Notes |
|---|---|---|
| `users/{uid}/upi_transactions` | `upitxns` | `userId` field added from parent doc ID |
| `users/{uid}/cheque_transactions` | `chequetxns` | `userId` field added from parent doc ID |
| `users/{uid}/notification_logs` | `notificationlogs` | `userId` field added from parent doc ID |
| `admin_users` | `adminusers` | Top-level collection, no flattening needed |

## Prerequisites

1. **Firebase service account key** — download from Firebase Console → Project Settings → Service Accounts → Generate New Private Key. Save as `serviceAccountKey.json` in the project root (or set `GOOGLE_APPLICATION_CREDENTIALS` env var).
2. **MongoDB Atlas connection string** — from Atlas Dashboard → Database → Connect → Drivers. Set `MONGODB_URI` env var.
3. **Node.js 18+**

## Install

```bash
cd scripts
npm install
```

## Run

```bash
# Dry run (logs what would be migrated, no writes)
npm run dry-run

# Actual migration
npm run migrate
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GOOGLE_APPLICATION_CREDENTIALS` | Yes | — | Path to Firebase service account JSON |
| `MONGODB_URI` | Yes | — | MongoDB Atlas connection string |
| `DRY_RUN` | No | `false` | Set `true` to log without writing |
| `BATCH_SIZE` | No | `100` | Documents per insert batch |
