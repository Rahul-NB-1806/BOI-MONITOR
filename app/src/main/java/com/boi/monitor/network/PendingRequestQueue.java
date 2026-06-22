package com.boi.monitor.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.boi.monitor.model.ChequeTransaction;
import com.boi.monitor.model.NotificationLog;
import com.boi.monitor.model.UpiTransaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PendingRequestQueue {

    private static final String TAG = "PendingReqQueue";
    private static final String PREFS_NAME = "boi_pending_queue";
    private static final String KEY_UPI = "pending_upi";
    private static final String KEY_CHEQUE = "pending_cheque";
    private static final String KEY_LOG = "pending_log";

    private static volatile PendingRequestQueue instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    private PendingRequestQueue(Context ctx) {
        prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static PendingRequestQueue getInstance(Context ctx) {
        if (instance == null) {
            synchronized (PendingRequestQueue.class) {
                if (instance == null) instance = new PendingRequestQueue(ctx);
            }
        }
        return instance;
    }

    public void enqueueUpi(UpiTransaction tx) {
        List<UpiTransaction> list = loadUpi();
        list.add(tx);
        saveUpi(list);
        Log.d(TAG, "UPI queued for retry, total=" + list.size());
    }

    public void enqueueCheque(ChequeTransaction tx) {
        List<ChequeTransaction> list = loadCheque();
        list.add(tx);
        saveCheque(list);
        Log.d(TAG, "Cheque queued for retry, total=" + list.size());
    }

    public void enqueueLog(NotificationLog log) {
        List<NotificationLog> list = loadLogs();
        list.add(log);
        saveLogs(list);
        Log.d(TAG, "Log queued for retry, total=" + list.size());
    }

    public int retryAll() {
        int total = 0;
        total += retryUpi();
        total += retryCheque();
        total += retryLogs();
        if (total > 0) Log.i(TAG, "Retried " + total + " pending requests");
        return total;
    }

    private int retryUpi() {
        List<UpiTransaction> list = loadUpi();
        if (list.isEmpty()) return 0;
        ApiDataModule api = ApiDataModule.getInstance();
        List<UpiTransaction> failed = new ArrayList<>();
        for (UpiTransaction tx : list) {
            try {
                api.saveUpiTransactionSync(tx);
            } catch (Exception e) {
                failed.add(tx);
            }
        }
        saveUpi(failed);
        return list.size() - failed.size();
    }

    private int retryCheque() {
        List<ChequeTransaction> list = loadCheque();
        if (list.isEmpty()) return 0;
        ApiDataModule api = ApiDataModule.getInstance();
        List<ChequeTransaction> failed = new ArrayList<>();
        for (ChequeTransaction tx : list) {
            try {
                api.saveChequeTransactionSync(tx);
            } catch (Exception e) {
                failed.add(tx);
            }
        }
        saveCheque(failed);
        return list.size() - failed.size();
    }

    private int retryLogs() {
        List<NotificationLog> list = loadLogs();
        if (list.isEmpty()) return 0;
        ApiDataModule api = ApiDataModule.getInstance();
        List<NotificationLog> failed = new ArrayList<>();
        for (NotificationLog log : list) {
            try {
                api.saveLogSync(log);
            } catch (Exception e) {
                failed.add(log);
            }
        }
        saveLogs(failed);
        return list.size() - failed.size();
    }

    public boolean hasPending() {
        return !loadUpi().isEmpty() || !loadCheque().isEmpty() || !loadLogs().isEmpty();
    }

    private List<UpiTransaction> loadUpi() {
        String json = prefs.getString(KEY_UPI, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<UpiTransaction>>(){}.getType();
        List<UpiTransaction> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    private void saveUpi(List<UpiTransaction> list) {
        prefs.edit().putString(KEY_UPI, gson.toJson(list)).apply();
    }

    private List<ChequeTransaction> loadCheque() {
        String json = prefs.getString(KEY_CHEQUE, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<ChequeTransaction>>(){}.getType();
        List<ChequeTransaction> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    private void saveCheque(List<ChequeTransaction> list) {
        prefs.edit().putString(KEY_CHEQUE, gson.toJson(list)).apply();
    }

    private List<NotificationLog> loadLogs() {
        String json = prefs.getString(KEY_LOG, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<NotificationLog>>(){}.getType();
        List<NotificationLog> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    private void saveLogs(List<NotificationLog> list) {
        prefs.edit().putString(KEY_LOG, gson.toJson(list)).apply();
    }
}
