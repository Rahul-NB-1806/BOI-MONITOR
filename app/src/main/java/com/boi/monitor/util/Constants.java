package com.boi.monitor.util;

/**
 * Constants
 *
 * Single source of truth for string keys, collection names, and config values
 * used across multiple modules.
 */
public final class Constants {

    private Constants() {}

    // ── REST API configuration ────────────────────────────────────────────────
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:3001/";
    public static final String API_KEY  = "boi-monitor-api-key-a1b2c3d4e5";

    // ── Primary notification filter strings ────────────────────────────────────
    public static final String FILTER_BOI_KEYWORD     = "BOI";
    public static final String FILTER_ACCOUNT_SUFFIX  = "XXX004";

    // ── Firestore collection names (kept as reference, no longer used) ─────────
    // public static final String COL_CHEQUE_TRANSACTIONS = "cheque_transactions";
    // public static final String COL_UPI_TRANSACTIONS    = "upi_transactions";
    // public static final String COL_NOTIFICATION_LOGS   = "notification_logs";
    // public static final String COL_ADMIN_USERS         = "admin_users";
    // public static final String COL_APP_SETTINGS        = "app_settings";

    // ── Cheque document ID prefix ─────────────────────────────────────────────
    public static final String CHEQUE_DOC_PREFIX = "CHQ_";

    // ── Cheque statuses ────────────────────────────────────────────────────────
    public static final String STATUS_CLEARED   = "CLEARED";
    public static final String STATUS_RETURNED  = "RETURNED";
    public static final String STATUS_PRESENTED = "PRESENTED";

    // ── UPI transaction type ──────────────────────────────────────────────────
    public static final String TYPE_UPI_CREDIT = "UPI_CREDIT";

    // ── Notification service ──────────────────────────────────────────────────
    public static final int    FOREGROUND_NOTIFICATION_ID = 1001;
    public static final int    DEDUP_CACHE_MAX_SIZE       = 500;

    // ── Dashboard live query limits ───────────────────────────────────────────
    public static final int UPI_QUERY_LIMIT    = 50;
    public static final int CHEQUE_QUERY_LIMIT = 100;
    public static final int DASHBOARD_RECENT   = 10;  // items shown in UI lists
}
