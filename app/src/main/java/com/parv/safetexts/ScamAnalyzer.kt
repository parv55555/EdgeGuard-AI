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
    private val recordedInDb = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    private val classificationCache = java.util.concurrent.ConcurrentHashMap<Int, Pair<String, Float>>()
    private val recentlyAlertedScams = java.util.concurrent.ConcurrentHashMap<Int, Long>()
    private val inProgressHashes = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    private val tfliteClassifier = TFLiteScamClassifier(context)

    init {
        createNotificationChannel()
    }

    fun clearCache() {
        recordedInDb.clear()
        classificationCache.clear()
        recentlyAlertedScams.clear()
        inProgressHashes.clear()
        Log.d("ScamAnalyzer", "Memory caches cleared (DB & classification)")
    }

    fun removeMessageFromCache(sender: String, content: String) {
        val hash = (sender + content).hashCode()
        recordedInDb.remove(hash)
        classificationCache.remove(hash)
        recentlyAlertedScams.remove(hash)
        inProgressHashes.remove(hash)
        Log.d("ScamAnalyzer", "Removed message hash $hash from cache")
    }

    fun markAsFalsePositive(sender: String, content: String) {
        val hash = (sender + content).hashCode()
        classificationCache[hash] = Pair("FALSE_POSITIVE", 0f)
        recordedInDb.add(hash)
        recentlyAlertedScams[hash] = Long.MAX_VALUE
        Log.d("ScamAnalyzer", "Marked message hash $hash as FALSE_POSITIVE")
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

        // 1. If already saved in Room DB in this session, skip immediately
        if (recordedInDb.contains(msgHash)) {
            return
        }

        // 2. Prevent duplicate concurrent processing of the exact same message
        if (!inProgressHashes.add(msgHash)) {
            return
        }

        scope.launch(Dispatchers.Default) {
            try {
                if (isApk || isFakeCredit) {
                    val lastAlert = recentlyAlertedScams[msgHash]
                    val shouldAlert = lastAlert == null || (currentTime - lastAlert) >= 30000
                    if (shouldAlert) {
                        recentlyAlertedScams[msgHash] = currentTime
                    }
                    val scamMessage = message.copy(
                        analysisResult = "Alert",
                        scamProbability = 1.0f
                    )
                    if (shouldAlert) {
                        withContext(Dispatchers.Main) {
                            showScamWarningOverlay(scamMessage, isApk = isApk, isFakeCredit = isFakeCredit)
                        }
                    }
                    withContext(Dispatchers.IO) {
                        val existing = database.messageDao().getMessage(message.sender, message.content)
                        if (existing == null) {
                            database.messageDao().insertMessage(scamMessage)
                        }
                        recordedInDb.add(msgHash)
                    }
                    return@launch
                }

                // Check in-memory classification cache first (0ms latency!)
                val cached = classificationCache[msgHash]
                val (result, scamProbability) = if (cached != null) {
                    cached
                } else {
                    val cleanText = message.content.replace("\n", " ").replace(WHITESPACE_REGEX, " ").trim()
                    val classified = tfliteClassifier.classify(cleanText)
                    classificationCache[msgHash] = classified
                    if (classificationCache.size > 2000) {
                        classificationCache.clear()
                    }
                    classified
                }

                val isScam = result == "Alert" || result == "Scam"
                if (isScam) {
                    val lastAlert = recentlyAlertedScams[msgHash]
                    val shouldAlert = lastAlert == null || (currentTime - lastAlert) >= 30000
                    if (shouldAlert) {
                        recentlyAlertedScams[msgHash] = currentTime
                        val alertMessage = message.copy(
                            analysisResult = result,
                            scamProbability = scamProbability
                        )
                        withContext(Dispatchers.Main) {
                            showScamWarningOverlay(alertMessage)
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    val existing = database.messageDao().getMessage(message.sender, message.content)
                    if (existing == null) {
                        val entity = message.copy(analysisResult = result, scamProbability = scamProbability)
                        database.messageDao().insertMessage(entity)
                    } else if (isScam && existing.analysisResult != "Alert" && existing.analysisResult != "Scam") {
                        database.messageDao().updateMessage(existing.copy(analysisResult = result, scamProbability = scamProbability))
                    }
                    recordedInDb.add(msgHash)
                }

                Log.d("ScamAnalyzer", "Recorded message in Room DB for '${message.content.take(20)}...': $result ($scamProbability)")
            } catch (e: Throwable) {
                Log.e("ScamAnalyzer", "Classification/DB failed: ${e.message}", e)
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
