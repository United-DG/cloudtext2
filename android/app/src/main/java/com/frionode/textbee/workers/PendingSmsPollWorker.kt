package com.frionode.textbee.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.frionode.textbee.ApiManager
import com.frionode.textbee.AppConstants
import com.frionode.textbee.dtos.MessagesResponse
import com.frionode.textbee.dtos.SmsMessage
import com.frionode.textbee.helpers.SharedPreferenceHelper
import java.io.IOException

/**
 * Polls the backend for pending SMS messages and enqueues them for sending.
 *
 * This worker removes the FCM dependency for outbound SMS: the backend stores
 * outgoing SMS as "pending", and this worker fetches them directly via the
 * REST API and hands them to SmsSendWorker for delivery through the phone's
 * own SIM card. No third-party push server is required.
 */
class PendingSmsPollWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object {
        private const val TAG = "PendingSmsPollWorker"
        private const val QUEUE_NAME = "pending_sms_poll_queue"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequest.Builder(PendingSmsPollWorker::class.java)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .beginUniqueWork(QUEUE_NAME, ExistingWorkPolicy.REPLACE, workRequest)
                .enqueue()

            Log.d(TAG, "Pending SMS poll enqueued")
        }
    }

    override fun doWork(): Result {
        val context = applicationContext

        val deviceId = SharedPreferenceHelper.getSharedPreferenceString(
            context, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, ""
        ) ?: ""
        val apiKey = SharedPreferenceHelper.getSharedPreferenceString(
            context, AppConstants.SHARED_PREFS_API_KEY_KEY, ""
        ) ?: ""

        if (deviceId.isEmpty() || apiKey.isEmpty()) {
            Log.d(TAG, "Device not registered, skipping pending SMS poll")
            return Result.success()
        }

        val gatewayEnabled = SharedPreferenceHelper.getSharedPreferenceBoolean(
            context, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false
        )
        if (!gatewayEnabled) {
            Log.d(TAG, "Gateway not enabled, skipping pending SMS poll")
            return Result.success()
        }

        return try {
            val response = ApiManager.getApiService().getMessages(
                deviceId, apiKey, 1, 50, "pending"
            ).execute()

            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!.data ?: emptyList<SmsMessage>()
                Log.d(TAG, "Fetched ${messages.size} pending SMS messages from backend")

                for (sms in messages) {
                    // Only process outbound messages awaiting delivery.
                    // The legacy /devices/:id/messages endpoint does not filter
                    // by status (type=pending is ignored), so we filter here to
                    // avoid re-sending messages that are already sent/delivered.
                    if (sms.status == null || sms.status != "pending") {
                        Log.d(TAG, "Skipping SMS ${sms.id} with status '${sms.status}' (not pending)")
                        continue
                    }
                    if (sms.isReceived) {
                        Log.d(TAG, "Skipping received SMS ${sms.id} from ${sms.sender}")
                        continue
                    }

                    val recipient = sms.recipient ?: sms.recipients?.firstOrNull()
                    if (recipient != null && sms.message != null && sms.id != null) {
                        Log.d(TAG, "Enqueuing pending SMS ${sms.id} to $recipient")
                        SmsSendWorker.enqueue(
                            context,
                            recipient,
                            sms.message,
                            sms.id,
                            "",
                            null
                        )
                    } else {
                        Log.w(TAG, "Skipping incomplete pending SMS: recipient=$recipient, message=${sms.message != null}, id=${sms.id}")
                    }
                }

                Result.success()
            } else {
                Log.e(TAG, "Failed to fetch pending SMS. Response code: ${response.code()}, body: ${response.errorBody()?.string()}")
                Result.retry()
            }
        } catch (e: IOException) {
            Log.e(TAG, "API call failed for pending SMS poll: ${e.message}", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error polling pending SMS: ${e.message}", e)
            Result.failure()
        }
    }
}
