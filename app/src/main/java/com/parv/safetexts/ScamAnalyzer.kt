package com.parv.safetexts

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
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
        private val JUNK_UI_TEXTS = setOf(
            "settings", "account", "privacy", "avatar", "chats", "notifications", "storage and data",
            "app language", "help", "invite a friend", "wallpaper", "media, links, and docs", "search",
            "mute notifications", "disappearing messages", "more", "view contact", "clear chat", "export chat",
            "add shortcut", "security notifications", "two-step verification", "change number", "request account info",
            "delete my account", "passkeys", "email address", "last seen and online", "profile photo", "about",
            "status", "read receipts", "default message timer", "groups", "live location", "calls", "blocked contacts",
            "fingerprint lock", "chat lock", "theme", "chat backup", "chat history", "enter is send",
            "media visibility", "font size", "keep chats archived", "transfer chats", "conversation tones",
            "vibrate", "popup notification", "light", "high priority notifications", "reaction notifications",
            "manage storage", "network usage", "use less data for calls", "proxy", "media auto-download",
            "when using mobile data", "when connected on wi-fi", "when roaming", "photo upload quality",
            "help center", "contact us", "terms and privacy policy", "app info", "screen reader", "whatsapp",
            "messages", "conversations", "contacts", "archived", "spam and blocked", "security",
            "general", "advanced", "sim card", "automatic preview", "swipe actions"
        )

        fun getInstance(context: Context): ScamAnalyzer {
            return instance ?: synchronized(this) {
                instance ?: ScamAnalyzer(context.applicationContext).also { instance = it }
            }
        }

        fun isTelecomServiceMessage(text: String): Boolean {
            val lower = text.lowercase()
            
            // Never whitelist messages containing links or APK attachments
            if (lower.contains(".apk") || lower.contains("http://") || lower.contains("https://") || lower.contains("bit.ly")) {
                return false
            }

            // 1. Missed call alerts (whether sent by telecom or routed through caller's numeric phone number)
            val isMissedCall = lower.contains("missed call") || 
                               lower.contains("missed a call") || 
                               lower.contains("u missed a call") || 
                               lower.contains("you missed a call") ||
                               lower.contains("call alert") ||
                               lower.contains("tried calling you") ||
                               lower.contains("called you while")

            // 2. Call availability alerts (when caller becomes available to take calls)
            val isCallAvailable = (lower.contains("available to take") || 
                                   lower.contains("available to receive") || 
                                   lower.contains("now available") || 
                                   lower.contains("is available") ||
                                   lower.contains("now reachable") ||
                                   lower.contains("can be reached")) && 
                                  (lower.contains("call") || lower.contains("calling") || lower.contains("phone") || lower.contains("number"))

            // 3. Carrier balance, validity, plan updates
            val isCarrierUpdate = lower.contains("recharge of") || 
                                  lower.contains("plan validity") || 
                                  lower.contains("pack expired") || 
                                  lower.contains("data pack exhausted") || 
                                  lower.contains("daily data limit")

            return isMissedCall || isCallAvailable || isCarrierUpdate
        }

        fun checkFastDeterministicScam(text: String): Pair<String, Float>? {
            val lower = text.lowercase()
            // 0. Whitelist legitimate telecom operator service notifications
            if (isTelecomServiceMessage(text)) {
                return Pair("Safe", 0.0f)
            }
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
                ((lower.contains("otp") || lower.contains("one time password") || lower.contains("verification code")) &&
                 (lower.contains("share") || lower.contains("forward") || lower.contains("send to") || lower.contains("tell me")) &&
                 (!lower.contains("do not share") && !lower.contains("never share") && !lower.contains("don't share") && !lower.contains("dont share")))) {
                return Pair("Alert", 0.98f)
            }
            // 10. Pre-approved loan / refund scam
            if ((lower.contains("loan") || lower.contains("refund") || lower.contains("credit card limit") || lower.contains("cashback")) &&
                (lower.contains("pre-approved") || lower.contains("approved") || lower.contains("credited") || lower.contains("claim now") || lower.contains("apply now")) &&
                (lower.contains("link") || lower.contains("http") || lower.contains("bit.ly") || lower.contains("click"))) {
                return Pair("Alert", 0.95f)
            }
            // 11. Malicious Web Fraud, Fake Govt & Bank URLs in Messages
            val highestRisk = FraudDetectionEngine.findHighestRiskUrlInText(text)
            if (highestRisk != null) {
                val scan = highestRisk.second
                if (scan.score >= 50) {
                    val alertTag = when (scan.fraudCategory) {
                        "GOVERNMENT" -> "Fake Govt WebShield"
                        "BANK" -> "Bank Phishing WebShield"
                        else -> "EdgeGuard WebShield"
                    }
                    return Pair(alertTag, (scan.score / 100f).coerceAtLeast(0.95f))
                }
            }
            return null
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

    fun analyzeMessage(message: MessageEntity, onDuplicateScam: ((MessageEntity) -> Unit)? = null, isApk: Boolean = false, isFakeCredit: Boolean = false, forceRealert: Boolean = false) {
        val senderLower = message.sender.lowercase().trim()
        val contentLower = message.content.lowercase().trim()

        if (contentLower.isEmpty() || 
            senderLower in JUNK_UI_TEXTS || 
            contentLower in JUNK_UI_TEXTS ||
            contentLower.contains("security notifications, change number") ||
            contentLower.contains("theme, wallpapers") ||
            contentLower.contains("network usage, auto-download") ||
            contentLower.contains("help center, contact us") ||
            contentLower.contains("create, edit, profile photo") ||
            contentLower.contains("manage people and groups") ||
            contentLower.contains("block contacts, disappearing messages") ||
            contentLower.contains("message, group & call tones")) {
            return
        }

        if (!isApk && ContactUtils.isTrustedSender(context, message.sender)) {
            Log.d("ScamAnalyzer", "Skipping analysis for trusted sender: ${message.sender}")
            return
        }

        val cleanText = message.content.replace("\n", " ").replace(WHITESPACE_REGEX, " ").trim()
        if (!isApk && isTelecomServiceMessage(cleanText)) {
            Log.d("ScamAnalyzer", "Whitelisted legitimate telecom carrier notification: ${cleanText.take(30)}")
            return
        }

        val msgHash = (message.sender + message.content).hashCode()
        val currentTime = System.currentTimeMillis()

        // Prevent duplicate concurrent processing of the exact same message in flight
        if (!isApk && !inProgressHashes.add(msgHash)) {
            return
        }

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
            val isScam = result == "Alert" || result == "Scam" || result.contains("WebShield") || result.contains("Alert")
            if (isScam) {
                val lastAlert = recentlyAlertedScams[msgHash]
                val throttleTime = if (forceRealert) 1000L else if (isApk) 20000L else 30000L
                val shouldAlert = lastAlert == null || (currentTime - lastAlert) >= throttleTime
                if (shouldAlert) {
                    val alertMessage = message.copy(
                        analysisResult = result,
                        scamProbability = scamProbability
                    )
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        if (showScamWarningOverlay(alertMessage, isApk = isApk, isFakeCredit = isFakeCredit)) {
                            recentlyAlertedScams[msgHash] = currentTime
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            if (showScamWarningOverlay(alertMessage, isApk = isApk, isFakeCredit = isFakeCredit)) {
                                recentlyAlertedScams[msgHash] = currentTime
                            }
                        }
                    }
                }
            }

            // Save into Room DB asynchronously on IO thread (only once per unique message)
            if (!recordedInDb.contains(msgHash)) {
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
            } else {
                inProgressHashes.remove(msgHash)
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
                    val throttleTime = if (forceRealert) 1000L else 30000L
                    val shouldAlert = lastAlert == null || (currentTime - lastAlert) >= throttleTime
                    if (shouldAlert) {
                        val alertMessage = message.copy(
                            analysisResult = result,
                            scamProbability = scamProbability
                        )
                        withContext(Dispatchers.Main) {
                            if (showScamWarningOverlay(alertMessage)) {
                                recentlyAlertedScams[msgHash] = currentTime
                            }
                        }
                    }
                }

                if (!recordedInDb.contains(msgHash)) {
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
                }

                Log.d("ScamAnalyzer", "Recorded message in Room DB for '${message.content.take(20)}...': $result ($scamProbability)")
            } catch (e: Throwable) {
                Log.e("ScamAnalyzer", "Classification/DB failed: ${e.message}", e)
            } finally {
                inProgressHashes.remove(msgHash)
            }
        }
    }

    fun showScamWarningOverlay(message: MessageEntity, isApk: Boolean = false, isFakeCredit: Boolean = false, isBrowserSpam: Boolean = false): Boolean {
        if (ScamAlertActivity.isAlertActive || OverlayUtils.isOverlayShowing) {
            Log.d("ScamAnalyzer", "Overlay/Alert already active. Skipping.")
            return false
        }

        val isWebFraud = isBrowserSpam || message.analysisResult.contains("WebShield") || message.sender.contains("WebShield")
        val category = when {
            message.analysisResult.contains("Govt") || message.sender.contains("Govt") -> "GOVERNMENT"
            message.analysisResult.contains("Bank") || message.sender.contains("Bank") -> "BANK"
            else -> "GENERIC"
        }
        val domainFromContent = if (isWebFraud) {
            (FraudDetectionEngine.findHighestRiskUrlInText(message.content)?.first
                ?: FraudDetectionEngine.findValidUrlInText(message.content))
                ?.let { FraudDetectionEngine.extractDomain(it) }
        } else null

        if (Settings.canDrawOverlays(context)) {
            OverlayUtils.showScamCardOverlay(
                context = context,
                message = message,
                isApk = isApk,
                isFakeCredit = isFakeCredit,
                isBrowserSpam = isBrowserSpam,
                isFakeWebsite = isWebFraud,
                websiteDomain = domainFromContent,
                fraudCategory = category
            )
            return true
        } else {
            Log.w("ScamAnalyzer", "Overlay permission not granted. Showing fallback notification.")
            showScamWarningNotification(message)
            return true
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
            .setStyle(Notification.BigTextStyle().bigText("EdgeGuard intercepted a potential scam from ${message.sender} (${message.appName}). Open EdgeGuard to view details."))
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(message.id, notification)
    }
}
