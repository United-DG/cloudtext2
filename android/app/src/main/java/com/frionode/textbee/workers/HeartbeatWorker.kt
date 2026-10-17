package com.frionode.textbee.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.frionode.textbee.AppConstants
import com.frionode.textbee.helpers.HeartbeatHelper
import com.frionode.textbee.helpers.PendingSmsManager
import com.frionode.textbee.helpers.SharedPreferenceHelper

class HeartbeatWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object {
        private const val TAG = "HeartbeatWorker"
    }

    override fun doWork(): Result {
        val context = applicationContext

        if (!HeartbeatHelper.isDeviceEligibleForHeartbeat(context)) {
            Log.d(TAG, "Device not eligible for heartbeat, skipping")
            return Result.success()
        }

        val deviceId = SharedPreferenceHelper.getSharedPreferenceString(
            context, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, ""
        ) ?: ""
        val apiKey = SharedPreferenceHelper.getSharedPreferenceString(
            context, AppConstants.SHARED_PREFS_API_KEY_KEY, ""
        ) ?: ""

        return if (HeartbeatHelper.sendHeartbeat(context, deviceId, apiKey)) {
            PendingSmsManager.triggerPendingSmsPoll(context)
            Result.success()
        } else {
            Log.e(TAG, "Heartbeat failed, but still triggering pending SMS poll")
            PendingSmsManager.triggerPendingSmsPoll(context)
            Result.retry()
        }
    }
}
