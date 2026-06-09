package com.boi.monitor.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BootReceiver
 *
 * Receives BOOT_COMPLETED to ensure the notification listener reconnects
 * after device restart. The NotificationListenerService is automatically
 * bound by Android if the permission is granted, but this receiver ensures
 * the app process starts up and Firebase is initialized.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BOIBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.i(TAG, "Boot/update received — BOI Monitor will reconnect");
            // The NotificationListenerService is managed by Android OS.
            // Simply starting the app process ensures Firebase initializes.
            // The OS will rebind the NotificationListenerService automatically.
        }
    }
}
