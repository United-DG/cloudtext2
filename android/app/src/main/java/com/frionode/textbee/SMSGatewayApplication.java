package com.frionode.textbee;

import android.app.Application;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.frionode.textbee.helpers.PendingSmsAlarmManager;

public class SMSGatewayApplication extends Application implements Configuration.Provider {
    private static final String TAG = "SMSGatewayApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize WorkManager early to ensure it's ready for background work
        try {
            WorkManager.initialize(this, getWorkManagerConfiguration());
            Log.d(TAG, "WorkManager initialized successfully");
        } catch (IllegalStateException e) {
            Log.d(TAG, "WorkManager already initialized or will be initialized automatically");
        }

        // Start background pending SMS polling via AlarmManager.
        // This ensures outbound SMS are picked up even when the app is in the
        // background (e.g. user is using another app like pal) and the device
        // is in Doze mode. Polls every 2 minutes via setExactAndAllowWhileIdle().
        PendingSmsAlarmManager.startBackgroundPolling(this);
        Log.d(TAG, "Background pending SMS polling started from Application.onCreate");
    }
    
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build();
    }
} 