package com.boi.monitor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
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
        Log.i(TAG, "Retrofit networking initialized");
    }

    /**
     * Authenticate anonymously via the REST API so the app always has
     * an active session before performing data operations.
     */
    private void initAnonymousAuth() {
        AuthManager authManager = AuthManager.getInstance();
        if (authManager.isLoggedIn()) {
            Log.i(TAG, "Already authenticated: userId=" + authManager.getUserId());
            ApiDataModule dataModule = ApiDataModule.getInstance();
            dataModule.onAuthReady();
            dataModule.flushPendingQueue();
            return;
        }

        authManager.anonymousAuth(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String userId, String token) {
                Log.i(TAG, "Anonymous auth established: " + userId);
                ApiDataModule dataModule = ApiDataModule.getInstance();
                dataModule.onAuthReady();
                dataModule.flushPendingQueue();
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
