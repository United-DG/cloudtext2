package com.frionode.textbee.helpers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.frionode.textbee.AppConstants
import com.frionode.textbee.workers.PendingSmsPollWorker
import java.util.concurrent.TimeUnit

/**
 * Schedules and manages periodic polling for pending SMS messages.
 *
 * The poller runs on a dedicated short interval (minimum 2 minutes) independent
 * of the heartbeat, so queued outbound SMS are picked up promptly even when the
 * app is already running. Requires network connectivity.
 */
object PendingSmsManager {
    private const val TAG = "PendingSmsManager"
    private const val MIN_INTERVAL_MINUTES = 2
    private const val DEFAULT_INTERVAL_MINUTES = 5
    private const val UNIQUE_WORK_NAME = "pending_sms_poll_unique_work"

    @JvmStatic
    fun schedulePendingSmsPoll(context: Context) {
        val appContext = context.applicationContext

        // Use a dedicated short interval for pending SMS polling, independent
        // of the heartbeat interval. WorkManager enforces a 15-minute minimum
        // for periodic work on most devices, so we also rely on triggerPendingSmsPoll()
        // from onResume and after send for more frequent checks.
        var intervalMinutes = SharedPreferenceHelper.getSharedPreferenceInt(
            appContext, AppConstants.SHARED_PREFS_PENDING_SMS_POLL_INTERVAL_MINUTES_KEY, DEFAULT_INTERVAL_MINUTES
        )
        if (intervalMinutes < MIN_INTERVAL_MINUTES) {
            Log.w(TAG, "Interval $intervalMinutes minutes is less than minimum $MIN_INTERVAL_MINUTES minutes, using minimum")
            intervalMinutes = MIN_INTERVAL_MINUTES
        }

        Log.d(TAG, "Scheduling pending SMS poll with interval: $intervalMinutes minutes")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val pollWork = PeriodicWorkRequest.Builder(
            PendingSmsPollWorker::class.java,
            intervalMinutes.toLong(),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(AppConstants.PENDING_SMS_POLL_TAG)
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                pollWork
            )
        Log.d(TAG, "Pending SMS poll scheduled successfully")
    }

    @JvmStatic
    fun cancelPendingSmsPoll(context: Context) {
        Log.d(TAG, "Cancelling pending SMS poll work")
        val appContext = context.applicationContext
        WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(appContext).cancelAllWorkByTag(AppConstants.PENDING_SMS_POLL_TAG)
    }

    @JvmStatic
    fun triggerPendingSmsPoll(context: Context) {
        Log.d(TAG, "Triggering immediate pending SMS poll")
        PendingSmsPollWorker.enqueue(context)
    }
}
