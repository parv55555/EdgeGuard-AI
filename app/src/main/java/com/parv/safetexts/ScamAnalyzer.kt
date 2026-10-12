package com.parv.safetexts

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
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
        val senderLower = message.sender.lowercase().trim()
        val contentLower = message.content.lowercase().trim()

        if (contentLower.isEmpty() || senderLower == "settings" || senderLower == "whatsapp" || senderLower == "chats") {
            return
        }

        val msgHash = (message.sender + message.content).hashCode()
        val currentTime = System.currentTimeMillis()

        // 1. If already saved in Room DB in this session, skip immediately (unless it's an APK threat)
        if (!isApk && recordedInDb.contains(msgHash)) {
            return
        }

        // 2. Prevent duplicate concurrent processing of the exact same message
        if (!isApk && !inProgressHashes.add(msgHash)) {
            return
        }

        val cleanText = message.content.replace("\n", " ").replace(WHITESPACE_REGEX, " ").trim()
        val cached = classificationCache[msgHash]
        val fastScam = if (!isApk && !isFakeCredit && cached == null) checkFastDeterministicScam(cleanText) else null

        val immediateClassification = when {
            isApk || isFakeCredit -> Pair("Alert", 1.0f)
            cached != null -> cached
            fastScam != null -> fastScam
            else -> null
        }

        // ULTRA-FAST ZERO-LATENCY PATH:
        // If message is APK, fake credit, in classification cache, or matches deterministic rule engine (<0.1ms),
        // trigger the overlay IMMEDIATELY on the main thread without any coroutine scheduling jitter!
        if (immediateClassification != null) {
            val (result, scamProbability) = immediateClassification
            val isScam = result == "Alert" || result == "Scam"
            if (isScam) {
                val lastAlert = recentlyAlertedScams[msgHash]
                val throttleTime = if (isApk) 3000L else 30000L
                val shouldAlert = lastAlert == null || (currentTime - lastAlert) >= throttleTime
                if (shouldAlert) {
                    recentlyAlertedScams[msgHash] = currentTime
                    val alertMessage = message.copy(
                        analysisResult = result,
                        scamProbability = scamProbability
                    )
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        showScamWarningOverlay(alertMessage, isApk = isApk, isFakeCredit = isFakeCredit)
                    } else {
                        scope.launch(Dispatchers.Main) {
                            showScamWarningOverlay(alertMessage, isApk = isApk, isFakeCredit = isFakeCredit)
                        }
                    }
                }
            }

            // Save into Room DB asynchronously on IO thread without holding up screen responsiveness
            scope.launch(Dispatchers.IO) {
                try {
                    val existing = database.messageDao().getMessage(message.sender, message.content)
                    if (existing == null) {
                        val entity = message.copy(analysisResult = result, scamProbability = scamProbability)
                        database.messageDao().insertMessage(entity)
                    } else if (isScam && existing.analysisResult != "Alert" && existing.analysisResult != "Scam") {
                        database.messageDao().updateMessage(existing.copy(analysisResult = result, scamProbability = scamProbability))
                    }
                    recordedInDb.add(msgHash)
                } catch (e: Throwable) {
                    Log.e("ScamAnalyzer", "DB record failed: ${e.message}")
                } finally {
                    inProgressHashes.remove(msgHash)
                }
            }
            return
        }

        // Transformer inference path for uncatalogued complex text patterns
        scope.launch(Dispatchers.Default) {
            try {
                val classified = tfliteClassifier.classify(cleanText)
                classificationCache[msgHash] = classified
                if (classificationCache.size > 2000) {
                    classificationCache.clear()
                }

                val (result, scamProbability) = classified
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

    private fun checkFastDeterministicScam(text: String): Pair<String, Float>? {
        val lower = text.lowercase()
        // 1. APK files
        if (lower.contains(".apk")) {
            return Pair("Alert", 1.0f)
        }
        // 2. Electricity bill disconnection scam
        if ((lower.contains("electricity") || lower.contains("bijli") || lower.contains("power")) &&
            (lower.contains("disconnect") || lower.contains("cut") || lower.contains("bill") || lower.contains("tonight") || lower.contains("9:30") || lower.contains("unpaid"))) {
            return Pair("Alert", 0.98f)
        }
        // 3. Bank / PAN / KYC Block scam
        if ((lower.contains("kyc") || lower.contains("pan card") || lower.contains("pancard") || lower.contains("aadhaar") || lower.contains("account") || lower.contains("debit card") || lower.contains("sim card") || lower.contains("sim block") || lower.contains("yono") || lower.contains("sbi")) &&
            (lower.contains("block") || lower.contains("suspend") || lower.contains("update immediately") || lower.contains("deactivate") || lower.contains("expire") || lower.contains("click here") || lower.contains("update your"))) {
            return Pair("Alert", 0.97f)
        }
        // 4. Fake Lottery / KBC / Prize
        if ((lower.contains("kbc") || lower.contains("lottery") || lower.contains("congratulations") || lower.contains("prize") || lower.contains("winner")) &&
            (lower.contains("lakh") || lower.contains("crore") || lower.contains("won") || lower.contains("claim") || lower.contains("lucky"))) {
            return Pair("Alert", 0.96f)
        }
        // 5. Fake Job / YouTube task
        if ((lower.contains("part time") || lower.contains("daily income") || lower.contains("youtube") || lower.contains("like and subscribe") || lower.contains("telegram")) &&
            (lower.contains("per day") || lower.contains("earn") || lower.contains("salary") || lower.contains("daily") || lower.contains("rs.") || lower.contains("inr") || lower.contains("work from home"))) {
            return Pair("Alert", 0.95f)
        }
        // 6. Fake Traffic Challan
        if ((lower.contains("challan") || lower.contains("echallan") || lower.contains("traffic")) &&
            (lower.contains("pay") || lower.contains("court") || lower.contains(".apk") || lower.contains("fine") || lower.contains("vehicle"))) {
            return Pair("Alert", 0.95f)
        }
        // 7. Sextortion / Digital Arrest
        if ((lower.contains("cbi") || lower.contains("narcotics") || lower.contains("cyber crime") || lower.contains("police") || lower.contains("digital arrest")) &&
            (lower.contains("warrant") || lower.contains("arrest") || lower.contains("illegal") || lower.contains("court") || lower.contains("summon") || lower.contains("case file"))) {
            return Pair("Alert", 0.99f)
        }
        // 8. India Post / Courier delivery address update scam
        if ((lower.contains("india post") || lower.contains("post office") || lower.contains("courier") || lower.contains("parcel") || lower.contains("shipment") || lower.contains("package") || lower.contains("delivery")) &&
            (lower.contains("address") || lower.contains("failed") || lower.contains("suspend") || lower.contains("update") || lower.contains("reschedule") || lower.contains("undelivered"))) {
            return Pair("Alert", 0.96f)
        }
        // 9. Remote access tools / OTP fraud
        if ((lower.contains("anydesk") || lower.contains("teamviewer") || lower.contains("quicksupport") || lower.contains("rustdesk")) ||
            ((lower.contains("otp") || lower.contains("one time password") || lower.contains("verification code")) && (lower.contains("share") || lower.contains("forward") || lower.contains("send to") || lower.contains("tell me")))) {
            return Pair("Alert", 0.98f)
        }
        // 10. Pre-approved loan / refund scam
        if ((lower.contains("loan") || lower.contains("refund") || lower.contains("credit card limit") || lower.contains("cashback")) &&
            (lower.contains("pre-approved") || lower.contains("approved") || lower.contains("credited") || lower.contains("claim now") || lower.contains("apply now")) &&
            (lower.contains("link") || lower.contains("http") || lower.contains("bit.ly") || lower.contains("click"))) {
            return Pair("Alert", 0.95f)
        }
        return null
    }
}
