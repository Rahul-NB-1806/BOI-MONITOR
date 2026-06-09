package com.boi.monitor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.multidex.MultiDex;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import com.boi.monitor.firebase.FirebaseAuthManager;

/**
 * BOI Monitor Application class.
 * Initializes Firebase and global app configurations.
 */
public class BOIApplication extends Application {

    private static final String TAG = "BOIApplication";

    public static final String NOTIFICATION_CHANNEL_ID = "boi_monitor_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "BOI Monitor Service";

    private static BOIApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        MultiDex.install(this);

        initFirebase();
        createNotificationChannel();
        initAnonymousAuth();

        Log.i(TAG, "BOI Monitor Application initialized");
    }

    public static BOIApplication getInstance() {
        return instance;
    }

    /**
     * Initialize Firebase with offline persistence enabled.
     */
    private void initFirebase() {
        FirebaseApp.initializeApp(this);

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();

        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        Log.i(TAG, "Firebase initialized with offline persistence");
    }

    /**
     * Sign in anonymously so the app always has an authenticated user
     * before performing Firestore operations.
     */
    private void initAnonymousAuth() {
        FirebaseAuthManager.getInstance().signInAnonymously(new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.i(TAG, "Anonymous auth established: " + user.getUid());
                FirebaseCrashlytics.getInstance().setUserId(user.getUid());
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Anonymous auth failed: " + errorMessage);
                Toast.makeText(BOIApplication.this,
                        "Sign-in failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Create foreground service notification channel (Android 8+).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("BOI notification monitoring service");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
