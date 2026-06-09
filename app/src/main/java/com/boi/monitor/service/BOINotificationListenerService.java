package com.boi.monitor.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.ComponentName;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.boi.monitor.BOIApplication;
import com.boi.monitor.R;
import com.boi.monitor.firebase.FirebaseDataModule;
import com.boi.monitor.model.ParsedNotification;
import com.boi.monitor.parser.NotificationParser;
import com.boi.monitor.ui.dashboard.MainActivity;
import com.boi.monitor.util.PrefsManager;
import com.boi.monitor.voice.VoiceEngine;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BOINotificationListenerService
 *
 * Core Android service that monitors system notifications in real time.
 * - Filters: must contain the configured BOI keyword and account suffix
 * - Deduplicates: uses UPI Reference Number instead of Android notification key
 * - Asynchronous: uses a single-thread executor to avoid blocking the main thread
 * - Foreground-capable: posts a persistent notification to stay alive
 *
 * Processing pipeline:
 *   onNotificationPosted → filter → parse → duplicate check → save Firebase → (UPI only) TTS
 */
public class BOINotificationListenerService extends NotificationListenerService {

    private static final String TAG = "BOINotifService";
    private static final int FOREGROUND_ID = 1001;
    private static final int MAX_SEEN_CACHE = 500;

    // Single-thread executor: ensures serial processing, no race conditions
    private ExecutorService executor;

    // Deduplication using UPI reference numbers
    private final Set<String> processedRefs =
            Collections.synchronizedSet(new HashSet<>());

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();

        executor = Executors.newSingleThreadExecutor();

        Set<String> persisted = PrefsManager.getInstance(this).loadSeenRefs();
        processedRefs.addAll(persisted);
        Log.i(TAG, "Loaded " + persisted.size() + " cached refs for deduplication");

        VoiceEngine.getInstance().init(this);

        startForegroundService();

        Log.i(TAG, "BOI Notification Listener Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        persistSeenRefs();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        VoiceEngine.getInstance().shutdown();

        Log.i(TAG, "BOI Notification Listener Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    // ── NotificationListenerService callbacks ──────────────────────────────────

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification sbn) {

        // Extract text immediately on calling thread
        final String rawText = extractText(sbn);

        Log.d(TAG, "Notification received from package: " + sbn.getPackageName());

        if (rawText == null || rawText.isEmpty())
            return;

        // Fast primary filter
        if (!NotificationParser.passesFilter(rawText)) {

            Log.d(TAG, "Notification rejected");

            return;
        }

        // Hand off to background thread
        executor.submit(() -> processNotification(rawText, sbn));
    }

    @Override
    public void onNotificationRemoved(@NonNull StatusBarNotification sbn) {
        // Nothing needed on removal
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        Log.i(TAG, "Notification Listener connected");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();

        Log.w(TAG, "Notification Listener disconnected — requesting rebind");

        requestRebind(new ComponentName(this, BOINotificationListenerService.class));
    }

    // ── Processing Pipeline ────────────────────────────────────────────────────

    /**
     * Runs on the background executor thread.
     */
    private void processNotification(
            String rawText,
            StatusBarNotification sbn){

        Log.d(TAG, "Processing filtered notification");

        ParsedNotification parsed = null;

        try {

            // Parse notification
            parsed = NotificationParser.parse(rawText);
            parsed.setNotificationTime(
                    sbn.getPostTime()
            );

            if (!parsed.isValid()) {

                Log.w(TAG, "Notification passed filter but no pattern matched");

                // Save unrecognized logs too
                FirebaseDataModule.getInstance().saveNotification(parsed);

                return;
            }

            // ── UPI Duplicate Detection ──────────────────────

            if (parsed.isUpiCredit()) {

                String upiRef = parsed.getReferenceNumber();
                if (upiRef == null) upiRef = extractUpiRef(rawText);

                // Only deduplicate if we have a valid reference number.
                // Without it, two different transactions with the same amount
                // would be incorrectly treated as duplicates.
                if (upiRef != null) {
                    long amount = parsed.getAmount();
                    String uniqueKey = upiRef + "_" + amount;

                    if (processedRefs.contains(uniqueKey)) {

                        Log.d(TAG, "Duplicate UPI skipped");

                        return;
                    }

                    markSeenRef(uniqueKey);
                }
            }

            // ── Save to Firebase ─────────────────────────────

            Log.d(TAG, "Saving notification to Firebase");

            FirebaseDataModule.getInstance().saveNotification(parsed);

            // ── Voice Announcement ───────────────────────────

            if (parsed.isUpiCredit()) {
                VoiceEngine.getInstance()
                        .announceUpiCredit(parsed.getAmount());
            }

            Log.i(TAG, "Successfully processed notification type: " + parsed.getType());

        } catch (Exception e) {

            Log.e(TAG,
                    "Unexpected error processing notification",
                    e);
            FirebaseCrashlytics.getInstance().recordException(
                    new RuntimeException("Notification processing failed for type: "
                            + (parsed != null ? parsed.getType() : "unknown"), e));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Extract notification body text from a StatusBarNotification.
     * Tries extras: EXTRA_BIG_TEXT, EXTRA_TEXT, EXTRA_TITLE.
     */
    private String extractText(StatusBarNotification sbn) {

        if (sbn.getNotification() == null)
            return null;

        android.os.Bundle extras = sbn.getNotification().extras;

        if (extras == null)
            return null;

        // Prefer big text
        CharSequence bigText =
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        if (bigText != null && bigText.length() > 0)
            return bigText.toString().trim();

        CharSequence text =
                extras.getCharSequence(Notification.EXTRA_TEXT);

        if (text != null && text.length() > 0)
            return text.toString().trim();

        CharSequence title =
                extras.getCharSequence(Notification.EXTRA_TITLE);

        if (title != null && title.length() > 0)
            return title.toString().trim();

        CharSequence ticker = sbn.getNotification().tickerText;

        if (ticker != null && ticker.length() > 0) {
            return ticker.toString().trim();
        }

        return null;
    }

    /**
     * Extract UPI Reference Number from notification text.
     */
    private String extractUpiRef(String text) {

        if (text == null)
            return null;

        Pattern pattern = Pattern.compile(
                "(?:UPI\\s*Ref\\s*no|UPI\\s*Ref|Ref\\s*No|UTR)[\\s:\\-]*([0-9]{6,})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            return matcher.group(1);
        }

        return null;
    }

    /**
     * Store processed UPI references.
     */
    private void markSeenRef(String ref) {

        if (processedRefs.size() >= MAX_SEEN_CACHE) {

            String first = processedRefs.iterator().next();

            processedRefs.remove(first);
        }

        processedRefs.add(ref);

        persistSeenRefs();
    }

    private void persistSeenRefs() {
        Set<String> snapshot;
        synchronized (processedRefs) {
            snapshot = new HashSet<>(processedRefs);
        }
        PrefsManager.getInstance(this).saveSeenRefs(snapshot);
    }

    /**
     * Post persistent foreground notification.
     */
    private void startForegroundService() {

        Intent tapIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        BOIApplication.NOTIFICATION_CHANNEL_ID
                )
                        .setContentTitle("BOI Monitor Active")
                        .setContentText("Monitoring Bank of India notifications")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .build();

        startForeground(FOREGROUND_ID, notification);
    }
}
