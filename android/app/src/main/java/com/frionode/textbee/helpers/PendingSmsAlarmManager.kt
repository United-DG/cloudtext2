package com.frionode.textbee.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.frionode.textbee.AppConstants
import com.frionode.textbee.receivers.PendingSmsAlarmReceiver

/**
 * Schedules and manages background polling for pending SMS messages using
 * AlarmManager. Unlike WorkManager (which enforces a 15-minute minimum for
 * periodic work), AlarmManager.setExactAndAllowWhileIdle() can wake the
 * device even in Doze mode, so outbound SMS are picked up promptly even when
 * the app is in the background while you use another app (e.g. pal).
 */
object PendingSmsAlarmManager {
    private const val TAG = "PendingSmsAlarmManager"
    private const val ALARM_INTERVAL_MILLIS = 2L * 60 * 1000 // 2 minutes
    private const val ALARM_REQUEST_CODE = 1001

    @JvmStatic
    fun startBackgroundPolling(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(appContext, PendingSmsAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + ALARM_INTERVAL_MILLIS,
            pendingIntent
        )

        Log.d(TAG, "Background pending SMS polling started (2-minute interval)")
    }

    @JvmStatic
    fun stopBackgroundPolling(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(appContext, PendingSmsAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        Log.d(TAG, "Background pending SMS polling stopped")
    }

    @JvmStatic
    fun rescheduleBackgroundPolling(context: Context) {
        stopBackgroundPolling(context)
        startBackgroundPolling(context)
    }
}
