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
    private val recentlySeenMessages = java.util.concurrent.ConcurrentHashMap<Int, Long>()
    private val tfliteClassifier = TFLiteScamClassifier(context)

    init {
        createNotificationChannel()
    }

    companion object {
        @Volatile
        private var instance: ScamAnalyzer? = null

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
        val lastSeen = recentlySeenMessages[msgHash]
        
        // Debounce: If we have seen this exact message within the last 10 seconds, ignore the event.
        if (lastSeen != null && (currentTime - lastSeen) < 10000) {
            return
        }
        recentlySeenMessages[msgHash] = currentTime
        
        // Cleanup old hashes to prevent memory leaks
        if (recentlySeenMessages.size > 100) {
            recentlySeenMessages.entries.removeIf { (currentTime - it.value) > 60000 }
        }

        scope.launch {
            // De-duplication check for persistent storage
            val existingMessage = database.messageDao().getMessage(message.sender, message.content)
            if (existingMessage != null) {
                Log.d("ScamAnalyzer", "Duplicate message found: ${message.sender} - ${message.content}")
                if (existingMessage.analysisResult == "Scam" || existingMessage.analysisResult == "Alert") {
                    withContext(Dispatchers.Main) {
                        onDuplicateScam?.invoke(existingMessage)
                    }
                }
                return@launch
            }

            if (isApk || isFakeCredit) {
                val scamMessage = message.copy(
                    analysisResult = "Alert",
                    scamProbability = 1.0f
                )
                val id = database.messageDao().insertMessage(scamMessage)
                withContext(Dispatchers.Main) {
                    showScamWarningOverlay(scamMessage.copy(id = id.toInt()), isApk = isApk, isFakeCredit = isFakeCredit)
                }
                return@launch
            }

            val id = database.messageDao().insertMessage(message)
            analyzeKeywords(message.copy(id = id.toInt()))
        }
    }

    private suspend fun analyzeKeywords(message: MessageEntity) {
        try {
            val cleanText = message.content.replace("\n", " ").replace(Regex("\\s+"), " ")
            
            // Run TFLite inference
            val (result, scamProbability) = tfliteClassifier.classify(cleanText)

            val analyzedMessage = message.copy(
                analysisResult = result,
                scamProbability = scamProbability
            )
            database.messageDao().updateMessage(analyzedMessage)
            
            if (result == "Alert" || result == "Scam") {
                withContext(Dispatchers.Main) {
                    showScamWarningOverlay(analyzedMessage)
                }
            }
            
            Log.d("ScamAnalyzer", "TFLite Result for '${message.content}': $result ($scamProbability)")
        } catch (e: Throwable) {
            Log.e("ScamAnalyzer", "TFLite Inference failed: ${e.message}")
            val errorMsg = e.message?.take(30) ?: e.javaClass.simpleName
            database.messageDao().updateMessage(message.copy(analysisResult = "Failed: $errorMsg"))
        }
    }

    fun showScamWarningOverlay(message: MessageEntity, isApk: Boolean = false, isFakeCredit: Boolean = false, isBrowserSpam: Boolean = false) {
        if (ScamAlertActivity.isAlertActive || OverlayUtils.isOverlayShowing) {
            Log.d("ScamAnalyzer", "Overlay/Alert already active. Skipping.")
            return
        }

        if (!Settings.canDrawOverlays(context)) {
            Log.w("ScamAnalyzer", "Overlay permission not granted. Showing fallback notification.")
            showScamWarningNotification(message)
            return
        }

        OverlayUtils.isOverlayShowing = true

        try {
            val intent = Intent(context, ScamAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(ScamAlertActivity.EXTRA_ALERT_TYPE, ScamAlertActivity.TYPE_SCAM)
                putExtra(ScamAlertActivity.EXTRA_SENDER, message.sender)
                putExtra(ScamAlertActivity.EXTRA_CONTENT, message.content)
                putExtra(ScamAlertActivity.EXTRA_APP_NAME, message.appName)
                putExtra(ScamAlertActivity.EXTRA_IS_APK, isApk)
                putExtra(ScamAlertActivity.EXTRA_IS_FAKE_CREDIT, isFakeCredit)
                putExtra(ScamAlertActivity.EXTRA_IS_BROWSER_SPAM, isBrowserSpam)
                putExtra(ScamAlertActivity.EXTRA_MESSAGE_ID, message.id)
            }

            // Post High-Priority FullScreenIntent Notification
            val pendingIntent = PendingIntent.getActivity(
                context,
                message.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, "SCAM_ALERTS")
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(if (isBrowserSpam) "⚠️ PHISHING ALERT" else "🚨 SCAM ALERT: ${message.sender}")
                .setContentText("Potential scam message intercepted from ${message.sender}")
                .setStyle(Notification.BigTextStyle().bigText("SafeConvos intercepted a potential scam from ${message.sender} (${message.appName}). Open SafeConvos to view details."))
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(message.id, notification)

            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    val method = options.javaClass.getMethod("setPendingIntentBackgroundActivityStartMode", Int::class.javaPrimitiveType)
                    method.invoke(options, 1)
                } catch (e: Throwable) {}
            }
            context.startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Log.e("ScamAnalyzer", "Failed to launch ScamAlertActivity: ${e.message}", e)
            showScamWarningNotification(message)
            OverlayUtils.isOverlayShowing = false
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
