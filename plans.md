## Hotfix: App Shows No Data After Firestore Restructure

### Problem

Existing data stored in old flat collections (`cheque_transactions`, `upi_transactions`, `notification_logs`) is invisible after the code was restructured to read/write from `users/{uid}/...` paths. Three root causes:

### Root Cause 1: Firestore Rules Block Old Collections

The rules allow access only to `users/{userId}/{subcollection}/{docId}` and `admin_users/{userId}`. The catch-all `match /{document=**} { allow read, write: if false; }` denies all access to the old flat collections. The migration code (`migrateOldDataIfNeeded`) tries to read from them but is denied → migration silently fails.

**Fix:**
Add read-only rules for the old flat collections so the migration can copy data to user-scoped paths:

```text
// Old flat collections — read-only for migration (temporary)
match /cheque_transactions/{docId} {
  allow read: if isAuthenticated();
  allow write: if false;
}
match /upi_transactions/{docId} {
  allow read: if isAuthenticated();
  allow write: if false;
}
match /notification_logs/{docId} {
  allow read: if isAuthenticated();
  allow write: if false;
}
```

### Root Cause 2: Listeners Only Watch User-Scoped Paths

`startUpiListener()` and `startChequeListener()` query `users/{uid}/upi_transactions` etc. If migration has not run (or failed), those paths are empty, and existing data in flat collections is never shown.

**Fix:**
In `FirebaseDataModule`, fall back to reading from the old flat collection path when the user-scoped path returns empty results.

### Root Cause 3: Auth Race Condition

`DashboardViewModel.startListening()` is called in its constructor. If `initAnonymousAuth()` has not completed yet, `getUserUid()` returns `null` and listeners are skipped. The `AuthStateListener` (added in a previous fix) restarts listeners when auth completes, but if anonymous auth fails silently (e.g., not enabled in Firebase Console), no data ever loads.

**Fix:**
- Verify **Anonymous Authentication** is enabled in Firebase Console → Authentication → Sign-in method.
- Listeners already restart via `AuthStateListener` when auth succeeds — no code change needed if auth is enabled.
- Add a Toast/user-facing error when anonymous sign-in fails so the user knows auth is broken.

### Execution

1. Update `firestore.rules` — add read-only rules for old flat collections.
2. Update `FirebaseDataModule.java` — fall back to flat collection paths when user-scoped paths are empty.
3. Rebuild and run.
4. After confirmation that all data has migrated, optionally remove the fallback logic and old-collection rules.
