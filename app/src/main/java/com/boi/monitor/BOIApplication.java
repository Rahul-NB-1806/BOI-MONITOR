package com.boi.monitor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.multidex.MultiDex;

import com.boi.monitor.network.ApiClient;
import com.boi.monitor.network.ApiDataModule;
import com.boi.monitor.network.AuthManager;

/**
 * BOI Monitor Application class.
 * Initializes Retrofit/OkHttp networking and global app configurations.
 */
public class BOIApplication extends Application {

    private static final String TAG = "BOIApplication";
    private static final int MAX_AUTH_RETRIES = 3;
    private static final long AUTH_BACKOFF_BASE_MS = 2000;

    public static final String NOTIFICATION_CHANNEL_ID = "boi_monitor_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "BOI Monitor Service";

    private static BOIApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        MultiDex.install(this);

        initNetworking();
        initAnonymousAuth();
        createNotificationChannel();

        Log.i(TAG, "BOI Monitor Application initialized");
    }

    public static BOIApplication getInstance() {
        return instance;
    }

    /**
     * Initialize Retrofit/OkHttp API client and AuthManager.
     */
    private void initNetworking() {
        ApiClient.getInstance(this);
        AuthManager.getInstance(this);
        Log.i(TAG, "[initNetworking] Retrofit networking initialized");
    }

    /**
     * Authenticate anonymously via the REST API so the app always has
     * an active session before performing data operations.
     * Retries up to MAX_AUTH_RETRIES times with exponential backoff on failure.
     */
    private void initAnonymousAuth() {
        AuthManager authManager = AuthManager.getInstance();
        if (authManager.isLoggedIn()) {
            Log.d(TAG, "[initAnonymousAuth] Cached token found, userId=" + authManager.getUserId());
            ApiDataModule dataModule = ApiDataModule.getInstance();
            dataModule.onAuthReady();
            dataModule.flushPendingQueue();
            return;
        }

        Log.d(TAG, "[initAnonymousAuth] No cached token, starting anonymous auth");
        attemptAnonymousAuth(authManager, 1);
    }

    private void attemptAnonymousAuth(AuthManager authManager, int attempt) {
        Log.d(TAG, "[initAnonymousAuth] Auth attempt " + attempt + "/" + MAX_AUTH_RETRIES);
        authManager.anonymousAuth(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String userId, String token) {
                Log.i(TAG, "[initAnonymousAuth] Auth established: userId=" + userId);
                ApiDataModule dataModule = ApiDataModule.getInstance();
                dataModule.onAuthReady();
                dataModule.flushPendingQueue();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.w(TAG, "[initAnonymousAuth] Attempt " + attempt + "/" + MAX_AUTH_RETRIES
                        + " failed: " + errorMessage);
                if (attempt < MAX_AUTH_RETRIES) {
                    long backoff = AUTH_BACKOFF_BASE_MS * (1L << (attempt - 1));
                    Log.d(TAG, "[initAnonymousAuth] Retrying in " + backoff + "ms");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        attemptAnonymousAuth(authManager, attempt + 1);
                    }, backoff);
                } else {
                    Log.e(TAG, "[initAnonymousAuth] All " + MAX_AUTH_RETRIES
                            + " attempts exhausted: " + errorMessage);
                    Toast.makeText(BOIApplication.this,
                            "Sign-in failed after " + MAX_AUTH_RETRIES
                                    + " attempts: " + errorMessage,
                            Toast.LENGTH_LONG).show();
                    ApiDataModule dataModule = ApiDataModule.getInstance();
                    dataModule.onAuthReady();
                }
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
