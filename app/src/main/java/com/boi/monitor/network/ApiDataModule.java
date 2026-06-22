package com.boi.monitor.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.boi.monitor.model.ChequeTransaction;
import com.boi.monitor.model.DashboardStats;
import com.boi.monitor.model.NotificationLog;
import com.boi.monitor.model.ParsedNotification;
import com.boi.monitor.model.UpiTransaction;
import com.boi.monitor.util.Constants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Data module that replaces FirebaseDataModule for the REST API migration.
 * All Firestore reads/writes are routed through the REST API via Retrofit.
 * Provides LiveData for UI consumption and callback-based async pattern.
 */
public class ApiDataModule {

    private static final String TAG = "ApiDataModule";

    private static volatile ApiDataModule instance;

    private final BoiApiService apiService;
    private final AuthManager authManager;

    private final MutableLiveData<List<UpiTransaction>> upiTransactionsLive = new MutableLiveData<>();
    private final MutableLiveData<List<ChequeTransaction>> chequeTransactionsLive = new MutableLiveData<>();
    private final MutableLiveData<DashboardStats> dashboardStatsLive = new MutableLiveData<>();
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();

    private List<UpiTransaction> latestUpiList = new ArrayList<>();
    private List<ChequeTransaction> latestChequeList = new ArrayList<>();

    private volatile boolean authReady = false;
    private boolean pendingRefresh = false;

    private ApiDataModule() {
        this.apiService = ApiClient.getInstance().getApiService();
        this.authManager = AuthManager.getInstance();
    }

    public void onAuthReady() {
        authReady = true;
        flushPendingQueue();
        if (pendingRefresh) {
            pendingRefresh = false;
            refreshAll();
        }
    }

    public static ApiDataModule getInstance() {
        if (instance == null) {
            synchronized (ApiDataModule.class) {
                if (instance == null) instance = new ApiDataModule();
            }
        }
        return instance;
    }

    // ── LiveData Accessors ─────────────────────────────────────────────────────

    public LiveData<List<UpiTransaction>> getUpiTransactions() { return upiTransactionsLive; }
    public LiveData<List<ChequeTransaction>> getChequeTransactions() { return chequeTransactionsLive; }
    public LiveData<DashboardStats> getDashboardStats() { return dashboardStatsLive; }
    public LiveData<String> getError() { return errorLive; }

    // ── Data Loading ───────────────────────────────────────────────────────────

    /**
     * Refresh all data from the server. Call on pull-to-refresh or app start.
     * Data loading is deferred until anonymous auth completes.
     */
    public void refreshAll() {
        if (!authReady) {
            pendingRefresh = true;
            return;
        }
        refreshUpiTransactions(Constants.UPI_QUERY_LIMIT);
        refreshChequeTransactions(Constants.CHEQUE_QUERY_LIMIT);
    }

    public void refreshUpiTransactions(int limit) {
        apiService.getUpiTransactions(limit).enqueue(new Callback<List<UpiTransaction>>() {
            @Override
            public void onResponse(@NonNull Call<List<UpiTransaction>> call,
                                   @NonNull Response<List<UpiTransaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    latestUpiList = response.body();
                    upiTransactionsLive.postValue(latestUpiList);
                    recomputeStats(latestUpiList, latestChequeList);
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Failed to load UPI transactions: " + error);
                    errorLive.postValue("UPI load failed: " + error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UpiTransaction>> call, @NonNull Throwable t) {
                Log.e(TAG, "UPI transactions network error", t);
                errorLive.postValue("UPI load failed: " + t.getMessage());
            }
        });
    }

    public void refreshChequeTransactions(int limit) {
        apiService.getChequeTransactions(limit).enqueue(new Callback<List<ChequeTransaction>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChequeTransaction>> call,
                                   @NonNull Response<List<ChequeTransaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChequeTransaction> deduped = deduplicateCheques(response.body());
                    latestChequeList = deduped;
                    chequeTransactionsLive.postValue(latestChequeList);
                    recomputeStats(latestUpiList, latestChequeList);
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Failed to load cheque transactions: " + error);
                    errorLive.postValue("Cheque load failed: " + error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChequeTransaction>> call, @NonNull Throwable t) {
                Log.e(TAG, "Cheque transactions network error", t);
                errorLive.postValue("Cheque load failed: " + t.getMessage());
            }
        });
    }

    // ── Save Operations ────────────────────────────────────────────────────────

    public void saveUpiTransaction(UpiTransaction tx) {
        apiService.saveUpiTransaction(tx).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "UPI transaction saved");
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Failed to save UPI transaction: " + error);
                    errorLive.postValue("UPI save failed: " + error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "UPI save network error, queuing for retry", t);
                errorLive.postValue("UPI save failed: " + t.getMessage());
                PendingRequestQueue.getInstance(ApiClient.getInstance().getContext()).enqueueUpi(tx);
            }
        });
    }

    public void saveChequeTransaction(ChequeTransaction tx) {
        apiService.saveChequeTransaction(tx).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Cheque transaction saved");
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Failed to save cheque transaction: " + error);
                    errorLive.postValue("Cheque save failed: " + error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Cheque save network error, queuing for retry", t);
                errorLive.postValue("Cheque save failed: " + t.getMessage());
                PendingRequestQueue.getInstance(ApiClient.getInstance().getContext()).enqueueCheque(tx);
            }
        });
    }

    public void saveLog(NotificationLog log) {
        apiService.saveLog(log).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Notification log saved");
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Failed to save log: " + error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Log save network error, queuing for retry", t);
                PendingRequestQueue.getInstance(ApiClient.getInstance().getContext()).enqueueLog(log);
            }
        });
    }

    // ── Synchronous saves for retry queue ─────────────────────────────────────

    public void saveUpiTransactionSync(UpiTransaction tx) {
        try {
            Response<Void> resp = apiService.saveUpiTransaction(tx).execute();
            if (resp.isSuccessful()) {
                Log.i(TAG, "UPI transaction saved (sync retry)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveChequeTransactionSync(ChequeTransaction tx) {
        try {
            Response<Void> resp = apiService.saveChequeTransaction(tx).execute();
            if (resp.isSuccessful()) {
                Log.i(TAG, "Cheque transaction saved (sync retry)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveLogSync(NotificationLog log) {
        try {
            Response<Void> resp = apiService.saveLog(log).execute();
            if (resp.isSuccessful()) {
                Log.i(TAG, "Notification log saved (sync retry)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── Flush pending queue ────────────────────────────────────────────────────

    public void flushPendingQueue() {
        if (!authReady) {
            pendingRefresh = true;
            return;
        }
        int flushed = PendingRequestQueue.getInstance(ApiClient.getInstance().getContext()).retryAll();
        if (flushed > 0) {
            Log.i(TAG, "Flushed " + flushed + " pending requests");
        }
    }

    public void getLogs(int limit, ApiCallback<List<NotificationLog>> callback) {
        apiService.getLogs(limit).enqueue(new Callback<List<NotificationLog>>() {
            @Override
            public void onResponse(@NonNull Call<List<NotificationLog>> call,
                                   @NonNull Response<List<NotificationLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<NotificationLog>> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    // ── Notification Processing ─────────────────────────────────────────────────

    public void saveNotification(ParsedNotification parsed) {
        if (parsed == null) return;

        if (parsed.isUpiCredit()) {
            UpiTransaction tx = new UpiTransaction(
                parsed.getAmount(),
                UpiTransaction.TYPE_UPI_CREDIT,
                parsed.getAccountSuffix(),
                parsed.getReferenceNumber(),
                parsed.getDebitedAccount(),
                parsed.getTransactionDate()
            );
            saveUpiTransaction(tx);
        } else if (parsed.isCheque()) {
            ChequeTransaction tx = new ChequeTransaction(
                parsed.getChequeNumber(),
                parsed.getAmount(),
                statusFromParsedType(parsed.getType()),
                parsed.getAvailableBalance(),
                parsed.getTransactionDate()
            );
            saveChequeTransaction(tx);
        }

        NotificationLog log = new NotificationLog(
            "com.boi.monitor",
            parsed.getType().name(),
            parsed.isValid(),
            null
        );
        log.setRawTextStored(parsed.getRawText());
        saveLog(log);
    }

    private String statusFromParsedType(ParsedNotification.NotificationType type) {
        switch (type) {
            case CHEQUE_CLEARED: return "CLEARED";
            case CHEQUE_RETURNED: return "RETURNED";
            case CHEQUE_PRESENTED: return "PRESENTED";
            default: return "UNKNOWN";
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    public void deleteAllUserData() {
        apiService.deleteAllData().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "All user data deleted");
                    latestUpiList = new ArrayList<>();
                    latestChequeList = new ArrayList<>();
                    upiTransactionsLive.postValue(latestUpiList);
                    chequeTransactionsLive.postValue(latestChequeList);
                    recomputeStats(latestUpiList, latestChequeList);
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Failed to delete user data: " + error);
                    errorLive.postValue("Delete failed: " + error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Delete network error", t);
                errorLive.postValue("Delete failed: " + t.getMessage());
            }
        });
    }

    // ── Dashboard Stats ────────────────────────────────────────────────────────

    private void recomputeStats(List<UpiTransaction> upiList, List<ChequeTransaction> chequeList) {
        DashboardStats stats = new DashboardStats();

        if (upiList != null && !upiList.isEmpty()) {
            long upiTotal = 0;
            int upiCount = 0;
            for (UpiTransaction tx : upiList) {
                upiTotal += tx.getAmount();
                upiCount++;
            }
            stats.setTotalUpiReceived(upiTotal);
            stats.setTotalUpiCount(upiCount);
        }

        if (chequeList != null && !chequeList.isEmpty()) {
            long clearedAmt = 0, returnedAmt = 0, processingAmt = 0;
            int clearedCnt = 0, returnedCnt = 0, processingCnt = 0;

            for (ChequeTransaction tx : chequeList) {
                String status = tx.getStatus();
                if (com.boi.monitor.util.Constants.STATUS_CLEARED.equals(status)) {
                    clearedAmt += tx.getAmount();
                    clearedCnt++;
                } else if (com.boi.monitor.util.Constants.STATUS_RETURNED.equals(status)) {
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

    // ── Cheque Deduplication ──────────────────────────────────────────────────

    @VisibleForTesting
    List<ChequeTransaction> deduplicateCheques(List<ChequeTransaction> rawList) {
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

    @VisibleForTesting
    boolean shouldReplace(ChequeTransaction current, ChequeTransaction candidate) {
        int curScore = statusScore(current.getStatus());
        int candScore = statusScore(candidate.getStatus());

        if (candScore != curScore) return candScore > curScore;

        String curTs = current.getTransactionDate();
        String candTs = candidate.getTransactionDate();
        if (curTs == null) return candTs != null;
        if (candTs == null) return false;
        return candTs.compareTo(curTs) > 0;
    }

    @VisibleForTesting
    String normalizeChequeNumber(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceFirst("^0+(?!$)", "");
    }

    private static int statusScore(String status) {
        if (com.boi.monitor.util.Constants.STATUS_CLEARED.equals(status)) return 3;
        if (com.boi.monitor.util.Constants.STATUS_RETURNED.equals(status)) return 2;
        if (com.boi.monitor.util.Constants.STATUS_PRESENTED.equals(status)) return 1;
        return 0;
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return response.errorBody().string();
            }
        } catch (Exception ignored) {}
        return "HTTP " + response.code() + " " + response.message();
    }

    /**
     * Generic callback interface for async API results.
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }
}
