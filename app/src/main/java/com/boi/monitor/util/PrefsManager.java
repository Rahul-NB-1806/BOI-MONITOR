package com.boi.monitor.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * PrefsManager
 *
 * Lightweight SharedPreferences wrapper for storing local app state.
 * Does NOT store sensitive data (tokens/keys go to Firebase Auth only).
 */
public class PrefsManager {

    private static final String PREFS_NAME   = "boi_monitor_prefs";
    private static final String KEY_ONBOARDED = "onboarded";
    private static final String KEY_LAST_SYNC = "last_sync_ts";
    private static final String KEY_SEEN_REFS = "seen_upi_refs";

    private static volatile PrefsManager instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    private PrefsManager(Context ctx) {
        prefs = ctx.getApplicationContext()
                   .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static PrefsManager getInstance(Context ctx) {
        if (instance == null) {
            synchronized (PrefsManager.class) {
                if (instance == null) instance = new PrefsManager(ctx);
            }
        }
        return instance;
    }

    // ── Onboarding ─────────────────────────────────────────────────────────────

    public boolean isOnboarded() {
        return prefs.getBoolean(KEY_ONBOARDED, false);
    }

    public void setOnboarded(boolean v) {
        prefs.edit().putBoolean(KEY_ONBOARDED, v).apply();
    }

    // ── Last Sync Timestamp ───────────────────────────────────────────────────

    public long getLastSyncTimestamp() {
        return prefs.getLong(KEY_LAST_SYNC, 0L);
    }

    public void setLastSyncTimestamp(long ts) {
        prefs.edit().putLong(KEY_LAST_SYNC, ts).apply();
    }

    // ── Seen UPI Refs (duplicate cache) ───────────────────────────────────────

    public void saveSeenRefs(Set<String> refs) {
        String json = gson.toJson(refs);
        prefs.edit().putString(KEY_SEEN_REFS, json).apply();
    }

    public Set<String> loadSeenRefs() {
        String json = prefs.getString(KEY_SEEN_REFS, null);
        if (json == null) return new HashSet<>();
        Type type = new TypeToken<HashSet<String>>(){}.getType();
        Set<String> refs = gson.fromJson(json, type);
        return refs != null ? refs : new HashSet<>();
    }

    // ── Generic helpers ────────────────────────────────────────────────────────

    public void clear() {
        prefs.edit().clear().apply();
    }
}
