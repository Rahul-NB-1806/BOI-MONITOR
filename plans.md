# Plan: Storage Control Page

## Overview
Add a dedicated storage management page showing per-category usage with delete controls, storage warnings, and date-wise deletion.

---

## Menu
- New menu item **"Clear Data"** in the overflow menu
- Opens `StorageControlActivity`

---

## Page Layout

```
┌──────────────────────────────────────┐
│  📊 Storage Control                  │
├──────────────────────────────────────┤
│                                      │
│  ⚠ Storage is 92% full — Delete old │  ← Red banner when > 80%
│                                      │
│  ┌─ UPI Transactions ──────────────┐ │
│  │ Count: 42   |   2.1 MB  / 5.0 MB│ │
│  │ ██████████████░░░░░░░░░  42%    │ │
│  │ [🗑 Delete All UPI]             │ │
│  └──────────────────────────────────┘ │
│                                      │
│  ┌─ Cheque Transactions ───────────┐ │
│  │ Count: 15   |   0.8 MB  / 5.0 MB│ │
│  │ ████████████░░░░░░░░░░░  16%    │ │
│  │ [🗑 Delete All Cheques]         │ │
│  └──────────────────────────────────┘ │
│                                      │
│  ┌─ Notification Logs ─────────────┐ │
│  │ Count: 128  |   3.4 MB  / 5.0 MB│ │
│  │ ██████████████████░░░░░  68%    │ │
│  │ [🗑 Delete All Logs]           │ │
│  └──────────────────────────────────┘ │
│                                      │
│  ┌─ Total ─────────────────────────┐ │
│  │ 6.3 MB / 100 MB                 │ │
│  │ ██████░░░░░░░░░░░░░░░░   6%     │ │
│  └──────────────────────────────────┘ │
│                                      │
│  ─── Date-wise Delete ──────────     │
│                                      │
│  Delete records older than           │
│  [  30  ] days                       │
│  or pick date: [📅 Select]           │
│                                      │
│  [🗑 Delete Selected]                 │
│                                      │
└──────────────────────────────────────┘
```

---

## Storage Warning Thresholds

| Usage | Bar Color | Banner | Message |
|-------|-----------|--------|---------|
| < 80% | Default | None | — |
| 80% - 89% | Yellow/Amber | Show | "Storage nearly full — consider deleting old data" |
| 90%+ | Red | Show (bold) | "⚠ Storage almost full! Delete old records soon" |

- Each category has its own bar + warning
- Top banner appears when **any** category or total exceeds threshold

---

## What Each Section Shows

| Section | Data Source | Delete Action |
|---------|-------------|---------------|
| UPI Transactions | `GET /api/upi` (count) | `DELETE /api/upi` |
| Cheque Transactions | `GET /api/cheques` (count) | `DELETE /api/cheques` |
| Notification Logs | `GET /api/logs` (count) | `DELETE /api/logs` |
| Total | Sum of all 3 | — |

Each section has:
- Document count
- Estimated storage in MB (`count × avgDocSize`)
- Progress bar (relative to a per-section cap)
- "Delete All" button with confirmation dialog

---

## Date-wise Delete

- Text input for **number of days**
- **Date picker** button to pick a specific cutoff date
- "Delete Selected" button deletes records **older than** the chosen date from **all 3 collections**
- Shows estimated affected count before confirming

---

## Server Changes

| File | Change |
|------|--------|
| `server/src/services/dataService.js` | Add `deleteAll()`, `deleteOlderThan()`, `count()` for all 3 services |
| `server/src/routes/logs.js` | Add `DELETE /api/logs`, `DELETE /api/logs/older-than` |
| `server/src/routes/upi.js` | Add `DELETE /api/upi`, `DELETE /api/upi/older-than` |
| `server/src/routes/cheques.js` | Add `DELETE /api/cheques`, `DELETE /api/cheques/older-than` |
| **New** `server/src/routes/storage.js` | `GET /api/storage/stats` |
| `server/src/index.js` | Register storage route |

### Storage Stats Endpoint

```
GET /api/storage/stats
Response:
{
  "upi":    { "count": 42,  "estimatedMB": 2.1 },
  "cheques": { "count": 15,  "estimatedMB": 0.8 },
  "logs":   { "count": 128, "estimatedMB": 3.4 },
  "total":  { "count": 185, "estimatedMB": 6.3 }
}
```

Avg doc sizes used for estimation: UPI ~250B, Cheque ~300B, Log ~200B.

---

## Android Changes

| File | Change |
|------|--------|
| `app/.../network/BoiApiService.java` | Add 7 new Retrofit endpoints |
| `app/.../network/ApiDataModule.java` | Add delete/count methods + `getStorageStats()` |
| **New** `app/.../ui/storage/StorageControlActivity.java` | Full storage UI with 4 sections, progress bars, date picker |
| **New** `app/res/layout/activity_storage_control.xml` | Layout |
| `app/.../ui/dashboard/MainActivity.java` | Add "Clear Data" menu handler |
| `app/res/menu/main_menu.xml` | Add "Clear Data" item |

### Confirmations

Every delete action:
1. Show AlertDialog with affected count + warning
2. On confirm → call API → refresh stats on success → show Toast
