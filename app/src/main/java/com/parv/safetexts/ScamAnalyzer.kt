package com.parv.safetexts

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScamAnalyzer private constructor(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val knownSafeMessages = java.util.concurrent.ConcurrentHashMap<Int, Long>()
    private val recentlyAlertedScams = java.util.concurrent.ConcurrentHashMap<Int, Long>()
    private val inProgressHashes = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    private val tfliteClassifier = TFLiteScamClassifier(context)

    init {
        createNotificationChannel()
    }

    companion object {
        @Volatile
        private var instance: ScamAnalyzer? = null
        private val WHITESPACE_REGEX = Regex("\\s+")

        fun getInstance(context: Context): ScamAnalyzer {
            return instance ?: synchronized(this) {
                instance ?: ScamAnalyzer(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scam Alerts"
            val descriptionText = "Notifications for detected scam messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("SCAM_ALERTS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun analyzeMessage(message: MessageEntity, onDuplicateScam: ((MessageEntity) -> Unit)? = null, isApk: Boolean = false, isFakeCredit: Boolean = false) {
        val msgHash = (message.sender + message.content).hashCode()
        val currentTime = System.currentTimeMillis()

        // 1. Instant Drop: If already known safe, return in 0ms!
        if (knownSafeMessages.containsKey(msgHash)) {
            return
        }

        // 2. Prevent duplicate alerts within 60s for the same scam message
        val lastAlert = recentlyAlertedScams[msgHash]
        if (lastAlert != null && (currentTime - lastAlert) < 60000) {
            return
        }

        // 3. Prevent duplicate concurrent processing of the exact same message
        if (!inProgressHashes.add(msgHash)) {
            return
        }

        scope.launch(Dispatchers.Default) {
            try {
                if (isApk || isFakeCredit) {
                    recentlyAlertedScams[msgHash] = currentTime
                    val scamMessage = message.copy(
                        analysisResult = "Alert",
                        scamProbability = 1.0f
                    )
                    // Trigger alert IMMEDIATELY on Main thread
                    withContext(Dispatchers.Main) {
                        showScamWarningOverlay(scamMessage, isApk = isApk, isFakeCredit = isFakeCredit)
                    }
                    // Persist to DB asynchronously
                    withContext(Dispatchers.IO) {
                        val existing = database.messageDao().getMessage(message.sender, message.content)
                        if (existing == null) {
                            database.messageDao().insertMessage(scamMessage)
                        }
                    }
                    return@launch
                }

                // Run fast on-device TFLite inference FIRST (on Dispatchers.Default CPU thread pool)
                val cleanText = message.content.replace("\n", " ").replace(WHITESPACE_REGEX, " ").trim()
                val (result, scamProbability) = tfliteClassifier.classify(cleanText)

                val isScam = result == "Alert" || result == "Scam"
                if (isScam) {
                    recentlyAlertedScams[msgHash] = System.currentTimeMillis()
                    val alertMessage = message.copy(
                        analysisResult = result,
                        scamProbability = scamProbability
                    )
                    // Trigger alert IMMEDIATELY on Main thread without waiting for SQLite disk writes!
                    withContext(Dispatchers.Main) {
                        showScamWarningOverlay(alertMessage)
                    }
                } else {
                    knownSafeMessages[msgHash] = System.currentTimeMillis()
                    // Bound the cache size to avoid memory growth
                    if (knownSafeMessages.size > 1500) {
                        val threshold = System.currentTimeMillis() - 3600000L
                        knownSafeMessages.entries.removeIf { it.value < threshold }
                    }
                }

                // Record into Room database asynchronously on Dispatchers.IO (non-blocking)
                withContext(Dispatchers.IO) {
                    val existing = database.messageDao().getMessage(message.sender, message.content)
                    if (existing == null) {
                        val entity = message.copy(analysisResult = result, scamProbability = scamProbability)
                        database.messageDao().insertMessage(entity)
                    } else if (isScam && existing.analysisResult != "Alert" && existing.analysisResult != "Scam") {
                        database.messageDao().updateMessage(existing.copy(analysisResult = result, scamProbability = scamProbability))
                    }
                }

                Log.d("ScamAnalyzer", "TFLite Result for '${message.content.take(20)}...': $result ($scamProbability)")
            } catch (e: Throwable) {
                Log.e("ScamAnalyzer", "Classification failed: ${e.message}", e)
            } finally {
                inProgressHashes.remove(msgHash)
            }
        }
    }

    fun showScamWarningOverlay(message: MessageEntity, isApk: Boolean = false, isFakeCredit: Boolean = false, isBrowserSpam: Boolean = false) {
        if (ScamAlertActivity.isAlertActive || OverlayUtils.isOverlayShowing) {
            Log.d("ScamAnalyzer", "Overlay/Alert already active. Skipping.")
            return
        }

        if (Settings.canDrawOverlays(context)) {
            OverlayUtils.showScamCardOverlay(context, message, isApk, isFakeCredit, isBrowserSpam)
        } else {
            Log.w("ScamAnalyzer", "Overlay permission not granted. Showing fallback notification.")
            showScamWarningNotification(message)
        }
    }

    private fun showScamWarningNotification(message: MessageEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, "SCAM_ALERTS")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ SCAM WARNING: ${message.sender}")
            .setContentText("Potential scam intercepted from ${message.sender}")
            .setStyle(Notification.BigTextStyle().bigText("SafeConvos intercepted a potential scam from ${message.sender} (${message.appName}). Open SafeConvos to view details."))
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(message.id, notification)
    }
}
