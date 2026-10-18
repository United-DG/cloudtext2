package com.frionode.textbee.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.frionode.textbee.helpers.PendingSmsManager

/**
 * BroadcastReceiver triggered by AlarmManager to poll for pending outbound SMS
 * even when the app is in the background or the device is in Doze mode.
 */
class PendingSmsAlarmReceiver : BroadcastReceiver() {
    private companion object {
        const val TAG = "PendingSmsAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm triggered - polling for pending SMS")

        // Poll for pending SMS immediately
        PendingSmsManager.triggerPendingSmsPoll(context)

        // Reschedule the next alarm (2 minutes from now)
        com.frionode.textbee.helpers.PendingSmsAlarmManager.startBackgroundPolling(context)
    }
}
