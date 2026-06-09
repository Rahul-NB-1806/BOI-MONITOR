package com.boi.monitor.firebase;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Date;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.boi.monitor.model.ChequeTransaction;
import com.boi.monitor.model.DashboardStats;
import com.boi.monitor.model.ParsedNotification;
import com.boi.monitor.model.UpiTransaction;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FirebaseDataModule
 *
 * Singleton responsible for all Firestore reads and writes.
 * All data is scoped per-user under users/{uid}/ subcollections.
 * Provides reactive LiveData for UI consumption.
 */
public class FirebaseDataModule {

    private static final String TAG = "FirebaseDataModule";

    private static volatile FirebaseDataModule instance;
    private final FirebaseFirestore db;

    private final MutableLiveData<List<UpiTransaction>>    upiTransactionsLive    = new MutableLiveData<>();
    private final MutableLiveData<List<ChequeTransaction>> chequeTransactionsLive = new MutableLiveData<>();
    private final MutableLiveData<DashboardStats>          dashboardStatsLive     = new MutableLiveData<>();
    private final MutableLiveData<String>                  errorLive              = new MutableLiveData<>();

    private ListenerRegistration upiListenerReg;
    private ListenerRegistration chequeListenerReg;

    // Cache the latest lists to strictly avoid race conditions
    private List<UpiTransaction>    latestUpiList    = new ArrayList<>();
    private List<ChequeTransaction> latestChequeList = new ArrayList<>();

    // Track whether we've attempted fallback to old flat collections
    private boolean upiFallbackAttempted;
    private boolean chequeFallbackAttempted;

    // One-shot migration guard per user
    private String migrationTargetUid;
    private boolean migrationAttempted;

    private FirebaseAuth.AuthStateListener authStateListener;

    private FirebaseDataModule() {
        db = FirebaseFirestore.getInstance();
        authStateListener = this::onAuthStateChanged;
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    private void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Log.i(TAG, "Auth state changed: user=" + user.getUid());
            restartListening();
            migrateOldDataIfNeeded(user.getUid());
        }
    }

    // ── Data Migration ────────────────────────────────────────────────────────────

    /**
     * One-time migration from old flat collections to user-scoped paths.
     * Runs once per user — guards redundant Firestore queries on auth re-fire.
     */
    private void migrateOldDataIfNeeded(String uid) {
        if (migrationAttempted && uid.equals(migrationTargetUid)) return;
        migrationTargetUid = uid;
        migrationAttempted = true;

        String userPath = "users/" + uid;
        db.collection(userPath + "/upi_transactions").limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) return; // already migrated
                    migrateCollection("upi_transactions", userPath + "/upi_transactions", uid);
                });
        db.collection(userPath + "/cheque_transactions").limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) return;
                    migrateCollection("cheque_transactions", userPath + "/cheque_transactions", uid);
                });
        db.collection(userPath + "/notification_logs").limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) return;
                    migrateCollection("notification_logs", userPath + "/notification_logs", uid);
                });
    }

    private void migrateCollection(String oldCollection, String newPath, String uid) {
        db.collection(oldCollection).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        Log.i(TAG, "No old data to migrate from " + oldCollection);
                        return;
                    }
                    Log.i(TAG, "Migrating " + snapshot.size() + " docs from " + oldCollection);
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.remove("rawNotification");
                            data.remove("rawText");
                            // Convert old double amounts to paise if needed
                            fixAmountField(data, "amount");
                            fixAmountField(data, "availableBalance");
                            fixAmountField(data, "totalUpiReceived");
                            fixAmountField(data, "totalClearedAmount");
                            fixAmountField(data, "totalReturnedAmount");
                            fixAmountField(data, "totalProcessingAmount");
                            db.collection(newPath).document(doc.getId()).set(data)
                                    .addOnFailureListener(e ->
                                            Log.w(TAG, "Migration write failed for " + doc.getId(), e));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Migration read failed for " + oldCollection, e));
    }

    private void fixAmountField(Map<String, Object> data, String field) {
        if (!data.containsKey(field)) return;
        Object val = data.get(field);
        if (val instanceof Double) {
            data.put(field, Math.round((Double) val * 100.0));
        }
    }

    public static FirebaseDataModule getInstance() {
        if (instance == null) {
            synchronized (FirebaseDataModule.class) {
                if (instance == null) instance = new FirebaseDataModule();
            }
        }
        return instance;
    }

    public LiveData<List<UpiTransaction>>    getUpiTransactions()    { return upiTransactionsLive; }
    public LiveData<List<ChequeTransaction>> getChequeTransactions() { return chequeTransactionsLive; }
    public LiveData<DashboardStats>          getDashboardStats()     { return dashboardStatsLive; }
    public LiveData<String>                  getError()              { return errorLive; }

    // ── Auth helpers ───────────────────────────────────────────────────────────

    private String getUserUid() {
        FirebaseUser user = FirebaseAuthManager.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user for Firestore operation");
            return null;
        }
        return user.getUid();
    }

    // ── Listener lifecycle ──────────────────────────────────────────────────────

    public void startListening() {
        startUpiListener();
        startChequeListener();
    }

    public void stopListening() {
        if (upiListenerReg    != null) { upiListenerReg.remove();    upiListenerReg    = null; }
        if (chequeListenerReg != null) { chequeListenerReg.remove(); chequeListenerReg = null; }
    }

    public void restartListening() {
        upiFallbackAttempted = false;
        chequeFallbackAttempted = false;
        stopListening();
        startListening();
    }

    // ── Data Deletion (Privacy) ──────────────────────────────────────────────────

    public Task<Void> deleteAllUserData() {
        String uid = getUserUid();
        if (uid == null) {
            Log.w(TAG, "deleteAllUserData: no authenticated user");
            return Tasks.forException(new Exception("No authenticated user"));
        }
        String basePath = "users/" + uid;
        Task<Void> deleteUpi = deleteCollection(db.collection(basePath + "/upi_transactions"));
        Task<Void> deleteCheque = deleteCollection(db.collection(basePath + "/cheque_transactions"));
        Task<Void> deleteLogs = deleteCollection(db.collection(basePath + "/notification_logs"));
        return Tasks.whenAll(deleteUpi, deleteCheque, deleteLogs);
    }

    private Task<Void> deleteCollection(com.google.firebase.firestore.CollectionReference collection) {
        return collection.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(task.getException() != null
                        ? task.getException() : new Exception("Failed to fetch documents"));
            }
            List<Task<Void>> deleteTasks = new ArrayList<>();
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                deleteTasks.add(doc.getReference().delete());
            }
            return Tasks.whenAll(deleteTasks);
        });
    }

    // ── Listeners ───────────────────────────────────────────────────────────────

    private void startUpiListener() {
        String uid = getUserUid();
        if (uid == null) {
            Log.w(TAG, "startUpiListener: no authenticated user, skipping");
            return;
        }
        String path = "users/" + uid + "/upi_transactions";
        upiListenerReg = db.collection(path)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1000)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "UPI listener error", e);
                        errorLive.postValue("UPI data error: " + e.getMessage());
                        return;
                    }
                    if (snapshot == null) return;

                    List<UpiTransaction> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        UpiTransaction tx = doc.toObject(UpiTransaction.class);
                        if (tx != null) {
                            tx.setDocumentId(doc.getId());
                            list.add(tx);
                        }
                    }

                    // Fallback to old flat collection if user-scoped path is empty
                    if (list.isEmpty() && !upiFallbackAttempted) {
                        upiFallbackAttempted = true;
                        fetchOldUpiTransactions(uid);
                        return;
                    }

                    latestUpiList = list;
                    upiTransactionsLive.postValue(list);
                    recomputeStats(latestUpiList, latestChequeList);
                });
    }

    private void fetchOldUpiTransactions(String uid) {
        db.collection("upi_transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1000)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        Log.i(TAG, "Old UPI collection also empty");
                        return;
                    }
                    List<UpiTransaction> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;
                        UpiTransaction tx = mapToUpiTransaction(doc.getId(), data);
                        if (tx != null) list.add(tx);
                    }
                    if (!list.isEmpty()) {
                        latestUpiList = list;
                        upiTransactionsLive.postValue(list);
                        recomputeStats(latestUpiList, latestChequeList);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to read old UPI collection", e));
    }

    private UpiTransaction mapToUpiTransaction(String docId, Map<String, Object> data) {
        UpiTransaction tx = new UpiTransaction();
        tx.setDocumentId(docId);
        tx.setAmount(toLongPaise(data.get("amount")));
        tx.setTransactionType(toStr(data.get("transactionType"), UpiTransaction.TYPE_UPI_CREDIT));
        tx.setAccountSuffix(toStr(data.get("accountSuffix"), null));
        tx.setReferenceNumber(toStr(data.get("referenceNumber"), null));
        tx.setDebitedAccount(toStr(data.get("debitedAccount"), null));
        tx.setTransactionDate(toStr(data.get("transactionDate"), null));
        if (data.get("timestamp") instanceof Timestamp) {
            tx.setTimestamp((Timestamp) data.get("timestamp"));
        }
        return tx;
    }

    private void startChequeListener() {
        String uid = getUserUid();
        if (uid == null) {
            Log.w(TAG, "startChequeListener: no authenticated user, skipping");
            return;
        }
        String path = "users/" + uid + "/cheque_transactions";
        chequeListenerReg = db.collection(path)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1000)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Cheque listener error", e);
                        errorLive.postValue("Cheque data error: " + e.getMessage());
                        return;
                    }
                    if (snapshot == null) return;

                    List<ChequeTransaction> rawList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ChequeTransaction tx = doc.toObject(ChequeTransaction.class);
                        if (tx != null) {
                            tx.setDocumentId(doc.getId());
                            rawList.add(tx);
                        }
                    }

                    // Fallback to old flat collection if user-scoped path is empty
                    if (rawList.isEmpty() && !chequeFallbackAttempted) {
                        chequeFallbackAttempted = true;
                        fetchOldChequeTransactions(uid);
                        return;
                    }

                    // Deduplicate before posting to UI (fixes Cheque Page duplicates)
                    List<ChequeTransaction> deduped = deduplicateCheques(rawList);
                    latestChequeList = deduped;

                    chequeTransactionsLive.postValue(deduped);
                    recomputeStats(latestUpiList, latestChequeList);
                });
    }

    private void fetchOldChequeTransactions(String uid) {
        db.collection("cheque_transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1000)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        Log.i(TAG, "Old Cheque collection also empty");
                        return;
                    }
                    List<ChequeTransaction> rawList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;
                        ChequeTransaction tx = mapToChequeTransaction(doc.getId(), data);
                        if (tx != null) rawList.add(tx);
                    }
                    if (!rawList.isEmpty()) {
                        List<ChequeTransaction> deduped = deduplicateCheques(rawList);
                        latestChequeList = deduped;
                        chequeTransactionsLive.postValue(deduped);
                        recomputeStats(latestUpiList, latestChequeList);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to read old Cheque collection", e));
    }

    private ChequeTransaction mapToChequeTransaction(String docId, Map<String, Object> data) {
        ChequeTransaction tx = new ChequeTransaction();
        tx.setDocumentId(docId);
        tx.setChequeNumber(toStr(data.get("chequeNumber"), ""));
        tx.setAmount(toLongPaise(data.get("amount")));
        tx.setStatus(toStr(data.get("status"), ChequeTransaction.STATUS_PRESENTED));
        tx.setAvailableBalance(toLongPaise(data.get("availableBalance")));
        tx.setTransactionDate(toStr(data.get("transactionDate"), null));
        if (data.get("timestamp") instanceof Timestamp) {
            tx.setTimestamp((Timestamp) data.get("timestamp"));
        }
        return tx;
    }

    private long toLongPaise(Object val) {
        if (val instanceof Long) return (Long) val;
        if (val instanceof Double) return Math.round((Double) val * 100.0);
        if (val instanceof Number) return ((Number) val).longValue();
        return 0;
    }

    private String toStr(Object val, String fallback) {
        return val instanceof String ? (String) val : fallback;
    }

    /**
     * Deduplicates cheques by normalized number.
     * Prefers later timestamp; among same number, picks best status
     * (CLEARED > RETURNED > PRESENTED), ties broken by timestamp.
     */
    private List<ChequeTransaction> deduplicateCheques(List<ChequeTransaction> rawList) {
        if (rawList == null) return new ArrayList<>();

        Map<String, ChequeTransaction> map = new LinkedHashMap<>();

        for (ChequeTransaction tx : rawList) {
            String cleanNum = normalizeChequeNumber(tx.getChequeNumber());

            ChequeTransaction existing = map.get(cleanNum);
            if (existing == null) {
                map.put(cleanNum, tx);
                continue;
            }

            if (shouldReplace(existing, tx)) {
                map.put(cleanNum, tx);
            }
        }
        return new ArrayList<>(map.values());
    }

    /** Returns true if {@code candidate} should replace {@code current} for the same cheque number. */
    private boolean shouldReplace(ChequeTransaction current, ChequeTransaction candidate) {
        int curScore = statusScore(current.getStatus());
        int candScore = statusScore(candidate.getStatus());

        if (candScore != curScore) return candScore > curScore;

        // Same status — prefer later timestamp
        Timestamp curTs = current.getTimestamp();
        Timestamp candTs = candidate.getTimestamp();
        if (curTs == null) return candTs != null;
        if (candTs == null) return false;
        return candTs.compareTo(curTs) > 0;
    }

    private static int statusScore(String status) {
        if (ChequeTransaction.STATUS_CLEARED.equals(status))   return 3;
        if (ChequeTransaction.STATUS_RETURNED.equals(status))  return 2;
        if (ChequeTransaction.STATUS_PRESENTED.equals(status)) return 1;
        return 0;
    }

    // ── Public save entry point ─────────────────────────────────────────────────

    public void saveNotification(ParsedNotification parsed) {
        String uid = getUserUid();
        if (uid == null) {
            Log.w(TAG, "saveNotification: no authenticated user, skipping write");
            return;
        }
        switch (parsed.getType()) {
            case CHEQUE_CLEARED:   saveChequeCleared(parsed, uid);   break;
            case CHEQUE_RETURNED:  saveChequeReturned(parsed, uid);  break;
            case CHEQUE_PRESENTED: saveChequePresented(parsed, uid); break;
            case UPI_CREDIT:       saveUpiCredit(parsed, uid);       break;
            default:               saveUnrecognizedLog(uid);
        }
    }

    private String normalizeChequeNumber(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceFirst("^0+(?!$)", "");
    }

    // ── Cheque writes ───────────────────────────────────────────────────────────

    private void saveChequeCleared(ParsedNotification parsed, String uid) {
        String cleanNum = normalizeChequeNumber(parsed.getChequeNumber());
        String docId = "CHQ_" + cleanNum;

        Map<String, Object> data = new HashMap<>();
        data.put("chequeNumber",      cleanNum);
        data.put("amount",            parsed.getAmount());
        data.put("status",            ChequeTransaction.STATUS_CLEARED);
        data.put("availableBalance",  parsed.getAvailableBalance());
        data.put("transactionDate",   parsed.getTransactionDate());
        data.put("timestamp",         new Timestamp(new Date(parsed.getNotificationTime())));

        db.collection("users/" + uid + "/cheque_transactions").document(docId).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.i(TAG, "Saved cheque transaction: CLEARED"))
                .addOnFailureListener(e -> handleWriteFailure("cheque cleared", e));
        saveLog("CHEQUE_CLEARED", true, null, uid);
    }

    private void saveChequeReturned(ParsedNotification parsed, String uid) {
        String cleanNum = normalizeChequeNumber(parsed.getChequeNumber());
        String docId = "CHQ_" + cleanNum;

        Map<String, Object> data = new HashMap<>();
        data.put("chequeNumber",    cleanNum);
        data.put("amount",          parsed.getAmount());
        data.put("status",          ChequeTransaction.STATUS_RETURNED);
        if (parsed.getFavouringParty() != null) data.put("favouringParty", parsed.getFavouringParty());
        data.put("timestamp",       new Timestamp(new Date(parsed.getNotificationTime())));

        db.collection("users/" + uid + "/cheque_transactions").document(docId).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.i(TAG, "Saved cheque transaction: RETURNED"))
                .addOnFailureListener(e -> handleWriteFailure("cheque returned", e));
        saveLog("CHEQUE_RETURNED", true, null, uid);
    }

    private void saveChequePresented(ParsedNotification parsed, String uid) {
        String cleanNum = normalizeChequeNumber(parsed.getChequeNumber());
        String docId = "CHQ_" + cleanNum;

        Map<String, Object> data = new HashMap<>();
        data.put("chequeNumber",    cleanNum);
        data.put("amount",          parsed.getAmount());
        data.put("status",          ChequeTransaction.STATUS_PRESENTED);
        if (parsed.getFavouringParty() != null) data.put("favouringParty", parsed.getFavouringParty());
        data.put("timestamp",       new Timestamp(new Date(parsed.getNotificationTime())));

        db.collection("users/" + uid + "/cheque_transactions").document(docId).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.i(TAG, "Saved cheque transaction: PRESENTED"))
                .addOnFailureListener(e -> handleWriteFailure("cheque presented", e));
        saveLog("CHEQUE_PRESENTED", true, null, uid);
    }

    // ── UPI writes ──────────────────────────────────────────────────────────────

    private void saveUpiCredit(ParsedNotification parsed, String uid) {
        UpiTransaction tx = new UpiTransaction(
                parsed.getAmount(), UpiTransaction.TYPE_UPI_CREDIT,
                parsed.getAccountSuffix(), parsed.getReferenceNumber(),
                parsed.getDebitedAccount(), parsed.getTransactionDate()
        );
        tx.setTimestamp(new Timestamp(new Date(parsed.getNotificationTime())));
        db.collection("users/" + uid + "/upi_transactions").document(buildUpiDocumentId(parsed)).set(tx, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.i(TAG, "Saved UPI transaction"))
                .addOnFailureListener(e -> handleWriteFailure("UPI transaction", e));
        saveLog("UPI_CREDIT", true, null, uid);
    }

    private String buildUpiDocumentId(ParsedNotification parsed) {
        String ref = parsed.getReferenceNumber() != null ? parsed.getReferenceNumber() : "NO_REF";
        String date = parsed.getTransactionDate() != null ? parsed.getTransactionDate() : "NO_DATE";
        String amount = String.valueOf(parsed.getAmount());
        return ("UPI_" + ref + "_" + amount + "_" + date).replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private void saveUnrecognizedLog(String uid) {
        saveLog("UNRECOGNIZED", false, "No matching pattern found", uid);
    }

    private void saveLog(String type, boolean processed, String error, String uid) {
        Map<String, Object> log = new HashMap<>();
        log.put("notificationType", type);
        log.put("processed",        processed);
        log.put("processingError",  error);
        log.put("rawTextStored",    false);
        log.put("timestamp",        Timestamp.now());
        db.collection("users/" + uid + "/notification_logs").add(log)
                .addOnSuccessListener(ref -> Log.i(TAG, "Saved notification log: " + type))
                .addOnFailureListener(e -> handleWriteFailure("notification log", e));
    }

    private void handleWriteFailure(String operation, Exception e) {
        Log.e(TAG, "Firestore write failed: " + operation, e);
        FirebaseCrashlytics.getInstance().recordException(e);
        errorLive.postValue("Failed to save " + operation);
    }

    // ── Dashboard Stats ─────────────────────────────────────────────────────────

    private void recomputeStats(List<UpiTransaction> upiList, List<ChequeTransaction> chequeList) {
        DashboardStats stats = new DashboardStats();

        if (upiList != null && !upiList.isEmpty()) {
            long upiTotal = 0;
            int upiCount = 0;
            Calendar now = Calendar.getInstance();
            int currentYear = now.get(Calendar.YEAR);
            int currentDay = now.get(Calendar.DAY_OF_YEAR);

            for (UpiTransaction tx : upiList) {
                Timestamp t = tx.getTimestamp();
                if (t != null) {
                    Calendar txCal = Calendar.getInstance();
                    txCal.setTime(t.toDate());
                    if (txCal.get(Calendar.YEAR) == currentYear &&
                        txCal.get(Calendar.DAY_OF_YEAR) == currentDay) {
                        upiTotal += tx.getAmount();
                        upiCount++;
                    }
                }
            }
            stats.setTotalUpiReceived(upiTotal);
            stats.setTotalUpiCount(upiCount);
        }

        if (chequeList != null && !chequeList.isEmpty()) {
            long clearedAmt = 0; long returnedAmt = 0; long processingAmt = 0;
            int clearedCnt = 0; int returnedCnt = 0; int processingCnt = 0;

            for (ChequeTransaction tx : chequeList) {
                String status = tx.getStatus();
                if (ChequeTransaction.STATUS_CLEARED.equals(status)) {
                    clearedAmt += tx.getAmount();
                    clearedCnt++;
                } else if (ChequeTransaction.STATUS_RETURNED.equals(status)) {
                    returnedAmt += tx.getAmount();
                    returnedCnt++;
                } else {
                    processingAmt += tx.getAmount();
                    processingCnt++;
                }
            }
            stats.setTotalClearedAmount(clearedAmt);
            stats.setTotalReturnedAmount(returnedAmt);
            stats.setTotalProcessingAmount(processingAmt);
            stats.setTotalClearedCount(clearedCnt);
            stats.setTotalReturnedCount(returnedCnt);
            stats.setTotalPresentedCount(processingCnt);
        }

        dashboardStatsLive.postValue(stats);
    }

    private void recomputeStats() {
        recomputeStats(latestUpiList, latestChequeList);
    }
}
