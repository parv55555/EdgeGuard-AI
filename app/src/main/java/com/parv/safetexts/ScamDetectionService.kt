// programmed by parv
package com.parv.safetexts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.withContext

class ScamDetectionService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: AppDatabase
    private val messageMutex = Mutex()

    private var appInstallReceiver: AppInstallReceiver? = null
    private var isTelephonyRegistered = false
    private var digitalArrestJob: kotlinx.coroutines.Job? = null

    private val voipAppNames = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "org.telegram.messenger" to "Telegram",
        "com.skype.raider" to "Skype",
        "com.google.android.apps.meetings" to "Meet",
        "com.google.android.apps.tachyon" to "Meet",
        "us.zoom.videomeetings" to "Zoom",
        "org.thoughtcrime.securesms" to "Signal",
        "com.facebook.orca" to "Messenger",
        "com.instagram.android" to "Instagram"
    )

    private fun startDigitalArrestWatchdog() {
        if (digitalArrestJob?.isActive == true) return
        digitalArrestJob = serviceScope.launch {
            while (isActive) {
                val tm = getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                val isTelecomCallActive = tm.callState == android.telephony.TelephonyManager.CALL_STATE_OFFHOOK
                val isVoipCallActive = OtpManager.getActiveVoipCallNumber(applicationContext) != null
                
                if (!isTelecomCallActive && !isVoipCallActive) {
                    break // Stop watchdog if no active calls
                }
                
                if (isVoipCallActive && OtpManager.isVoipCallVideo(applicationContext)) {
                    val videoStartTime = OtpManager.getVoipCallStartTime(applicationContext)
                    val timeSinceVideoStart = System.currentTimeMillis() - videoStartTime
                    
                    if (timeSinceVideoStart > 15L * 60 * 1000) {
                        val lastUnknownTelecomCall = OtpManager.getLastUnknownCallTime(applicationContext)
                        val lastUnknownVoipCall = OtpManager.getLastUnknownVoipCallTime(applicationContext)
                        val lastUnknownCallTime = maxOf(lastUnknownTelecomCall, lastUnknownVoipCall)
                        
                        if (videoStartTime - lastUnknownCallTime <= 24L * 60 * 60 * 1000) {
                            // The phone is now in a High-Risk State for Digital Arrest
                            OtpManager.setDigitalArrestRiskTime(applicationContext)

                            val foregroundApp = getForegroundApp()
                            if (OtpManager.isBankingApp(foregroundApp)) {
                                val alert = MessageEntity(
                                    sender = "System Alert",
                                    content = "Warning triggered for potential Digital Arrest. User opened banking app during a long video call.",
                                    appName = "EdgeGuard AI",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                database.messageDao().insertMessage(alert)
                                withContext(Dispatchers.Main) {
                                    OverlayUtils.showDigitalArrestWarningOverlay(applicationContext)
                                }
                            }
                        }
                    }
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()
        var foregroundPackage: String? = null
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundPackage = event.packageName
            } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED) {
                if (foregroundPackage == event.packageName) {
                    foregroundPackage = null
                }
            }
        }
        return foregroundPackage
    }



    private fun registerTelephonyListener() {
        if (isTelephonyRegistered) return
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                tm.registerTelephonyCallback(
                    mainExecutor,
                    object : android.telephony.TelephonyCallback(), android.telephony.TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            if (state == android.telephony.TelephonyManager.CALL_STATE_IDLE) {
                                OtpManager.clearActiveIncomingNumber(applicationContext)
                            } else if (state == android.telephony.TelephonyManager.CALL_STATE_OFFHOOK) {
                                startDigitalArrestWatchdog()
                            }
                        }
                    }
                )
            } else {
                @Suppress("DEPRECATION")
                tm.listen(object : android.telephony.PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        if (state == android.telephony.TelephonyManager.CALL_STATE_IDLE) {
                            OtpManager.clearActiveIncomingNumber(applicationContext)
                        } else if (state == android.telephony.TelephonyManager.CALL_STATE_OFFHOOK) {
                            startDigitalArrestWatchdog()
                        }
                    }
                }, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
            }
            isTelephonyRegistered = true
        } catch (e: SecurityException) {
            Log.e("ScamDetection", "Failed to register telephony listener", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        ScamAnalyzer.getInstance(this)
        cleanupOldMessages()

        val filter = android.content.IntentFilter(android.content.Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        appInstallReceiver = AppInstallReceiver()
        androidx.core.content.ContextCompat.registerReceiver(this, appInstallReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_EXPORTED)

        registerTelephonyListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        appInstallReceiver?.let {
            unregisterReceiver(it)
        }
    }

    private fun cleanupOldMessages() {
        serviceScope.launch {
            try {
                val sixDaysInMillis = 6L * 24 * 60 * 60 * 1000
                val threshold = System.currentTimeMillis() - sixDaysInMillis
                database.messageDao().deleteMessagesOlderThan(threshold)
                Log.d("ScamDetection", "Cleaned up messages older than 6 days")
            } catch (e: Exception) {
                Log.e("ScamDetection", "Error cleaning up messages: ${e.message}")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        registerTelephonyListener()
        cleanupOldMessages()
        
        val packageName = sbn.packageName
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(applicationContext)
        val voipAppName = voipAppNames[packageName]
        val isVoipApp = voipAppName != null
        val isSms = packageName == defaultSmsPackage || 
                    packageName.contains("messaging") || 
                    packageName.contains("sms")
        val isBrowserApp = packageName == "com.android.chrome" || 
                           packageName == "com.sec.android.app.sbrowser" || 
                           packageName == "com.brave.browser" || 
                           packageName == "com.opera.browser" || 
                           packageName == "org.mozilla.firefox"
        
        if (!isVoipApp && !isSms && !isBrowserApp) return

        // Ignore summary notifications to prevent merged/weird titles and duplicated messages
        val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) return

        val extras = sbn.notification.extras
        var title = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown Sender"
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Extract accurate sender and text from MessagingStyle if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val isMessagingStyle = extras.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$MessagingStyle"
            if (isMessagingStyle) {
                val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                if (!messages.isNullOrEmpty()) {
                    val lastMessage = messages.last() as? android.os.Bundle
                    if (lastMessage != null) {
                        val msgText = lastMessage.getCharSequence("text")?.toString()
                        val senderPerson = lastMessage.getParcelable("sender_person") as? android.app.Person
                        val senderName = senderPerson?.name?.toString()
                        
                        if (!msgText.isNullOrEmpty()) text = msgText
                        if (!senderName.isNullOrEmpty()) title = senderName
                    }
                }
            }
        }

        if (text.isEmpty()) return

        // Instantly eradicate Browser Push Notification Scams
        if (isBrowserApp) {
            if (isBrowserSpam(title, text)) {
                Log.d("ScamDetection", "DETECTED BROWSER SPAM: $title - $text")
                // Notification is left intact in the tray, but we show the warning overlay
                // Log it to the database for analytics, and show the user the standard scam warning overlay
                serviceScope.launch(Dispatchers.IO) {
                    val browserName = when (packageName) {
                        "com.android.chrome" -> "Chrome"
                        "com.sec.android.app.sbrowser" -> "Samsung Internet"
                        "com.brave.browser" -> "Brave"
                        "com.opera.browser" -> "Opera"
                        "org.mozilla.firefox" -> "Firefox"
                        else -> "Browser"
                    }
                    val alert = MessageEntity(
                        sender = browserName,
                        content = "Blocked Browser Scam: $title - $text",
                        appName = "EdgeGuard AI",
                        analysisResult = "Alert",
                        scamProbability = 1.0f
                    )
                    val id = database.messageDao().insertMessage(alert)
                    withContext(Dispatchers.Main) {
                        ScamAnalyzer.getInstance(applicationContext).showScamWarningOverlay(alert.copy(id = id.toInt()), isBrowserSpam = true)
                    }
                }
            }
            return // Stop further processing for browser notifications
        }

        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        
        // Skip official WhatsApp service account, alphanumeric SMS senders, and saved contacts as requested.
        val isWhatsApp = packageName.contains("whatsapp")
        val isOfficialWhatsApp = isWhatsApp && title.lowercase() == "whatsapp"
        val isVerifiedCompany = isWhatsApp && ContactUtils.isWhatsAppCompanyAccount(title)
        val isAlphanumericSms = isSms && ContactUtils.isAlphanumericSmsSender(title)
        val isSavedContact = ContactUtils.isContactExists(applicationContext, title)
        
        // WhatsApp groups, channels, and admin-only communities might bypass the IS_GROUP_CONVERSATION flag.
        val conversationTitle = extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
        val isReallyGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) || !conversationTitle.isNullOrEmpty()
        
        // Use shortcutId which contains the WhatsApp JID to flawlessly detect groups/channels
        val shortcutId = sbn.notification.shortcutId ?: ""
        val isJidGroupOrChannel = isWhatsApp && (shortcutId.contains("@g.us") || shortcutId.contains("@newsletter") || shortcutId.contains("@broadcast"))

        if (isOfficialWhatsApp || isVerifiedCompany || isAlphanumericSms || isSavedContact || (isWhatsApp && isReallyGroup) || isJidGroupOrChannel) {
            return
        }

        // Intercept VoIP calls before system notification filtering
        if (isVoipApp) {
            val isCallCategory = sbn.notification.category == Notification.CATEGORY_CALL
            val lowerText = text.lowercase(Locale.ROOT)
            val isCallText = lowerText.contains("incoming voice call") || 
                             lowerText.contains("incoming video call") || 
                             lowerText.contains("ongoing voice call") || 
                             lowerText.contains("ongoing video call") ||
                             lowerText.contains("whatsapp voice call") ||
                             lowerText.contains("whatsapp video call") ||
                             lowerText.contains("missed video call") ||
                             lowerText.contains("missed voice call") ||
                             lowerText.contains("zoom meeting") ||
                             lowerText.contains("zoom call") ||
                             lowerText.contains("meet call") ||
                             lowerText.contains("ongoing meeting") ||
                             lowerText.contains("ongoing call")
                             
            if (isCallCategory || isCallText || packageName == "us.zoom.videomeetings" || packageName == "com.google.android.apps.meetings" || packageName == "com.google.android.apps.tachyon") {
                val isVideo = lowerText.contains("video call") || packageName == "us.zoom.videomeetings" || packageName == "com.google.android.apps.meetings" || packageName == "com.google.android.apps.tachyon"
                val activeVoipCall = OtpManager.getActiveVoipCallNumber(applicationContext)
                
                if (activeVoipCall != title) {
                    OtpManager.saveActiveVoipCallNumber(applicationContext, title)
                    OtpManager.setVoipCallVideoStatus(applicationContext, isVideo)
                    OtpManager.setVoipCallStartTime(applicationContext, System.currentTimeMillis())
                    
                    if (!ContactUtils.isContactExists(applicationContext, title)) {
                        OtpManager.saveLastUnknownVoipCallTime(applicationContext)
                        
                        // Check for Sextortion Video Call scam
                        if (isVideo) {
                            serviceScope.launch(Dispatchers.IO) {
                                val alert = MessageEntity(
                                    sender = "System Alert",
                                    content = "Warning triggered for potential Sextortion. Unknown video call received.",
                                    appName = "EdgeGuard AI",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                database.messageDao().insertMessage(alert)
                                withContext(Dispatchers.Main) {
                                    OverlayUtils.showSextortionWarningOverlay(applicationContext)
                                }
                            }
                        }
                        
                        // If an OTP was received recently, throw the warning overlay for the VoIP call
                        val lastOtpTime = OtpManager.getLastOtpTime(applicationContext)
                        val sevenMinutesInMillis = 7L * 60 * 1000
                        if (System.currentTimeMillis() - lastOtpTime <= sevenMinutesInMillis) {
                            serviceScope.launch(Dispatchers.IO) {
                                val alert = MessageEntity(
                                    sender = "System Alert",
                                    content = "Warning triggered for potential OTP Theft. Unknown WhatsApp call received shortly after an OTP.",
                                    appName = "EdgeGuard AI",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                database.messageDao().insertMessage(alert)
                                withContext(Dispatchers.Main) {
                                    OverlayUtils.showOtpWarningOverlay(applicationContext)
                                }
                            }
                        }
                    }
                } else {
                    if (isVideo && !OtpManager.isVoipCallVideo(applicationContext)) {
                        OtpManager.setVoipCallVideoStatus(applicationContext, true)
                        OtpManager.setVoipCallStartTime(applicationContext, System.currentTimeMillis())

                        if (!ContactUtils.isContactExists(applicationContext, title)) {
                            serviceScope.launch(Dispatchers.IO) {
                                val alert = MessageEntity(
                                    sender = "System Alert",
                                    content = "Warning triggered for potential Sextortion. Unknown video call received.",
                                    appName = "EdgeGuard AI",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                database.messageDao().insertMessage(alert)
                                withContext(Dispatchers.Main) {
                                    OverlayUtils.showSextortionWarningOverlay(applicationContext)
                                }
                            }
                        }
                    }
                }
                startDigitalArrestWatchdog()
                return // Important: Don't process this call notification as a text message
            }
        }

        if (isSystemNotification(title, text)) return

        // Check for OTPs before filtering out business senders
        val hasOtpKeyword = text.contains(Regex("(?i)\\b(otp|code|verification|password)\\b"))
        val hasNumberCode = text.contains(Regex("\\b\\d{4,8}\\b"))
        
        // Ensure it's a high-value OTP (Bank, Aadhar, PAN, Govt) to ignore delivery OTPs
        val highValueRegex = Regex("(?i)\\b(bank|aadhar|aadhaar|uidai|pan|gov|government|kyc|tax|sbi|hdfc|icici|axis|pnb|bob)\\b")
        val isHighValue = text.contains(highValueRegex) || title.contains(highValueRegex)
        
        val isOtp = hasOtpKeyword && hasNumberCode && isHighValue
        
        if (isOtp) {
            Log.d("ScamDetection", "OTP detected from $title")
            OtpManager.saveLastOtpTime(applicationContext)

            // Alert the user that this is highly sensitive
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    applicationContext,
                    "Highly sensitive OTP. Do not tell it to anyone.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

            val tm = getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            val isTelecomCallActive = tm.callState == android.telephony.TelephonyManager.CALL_STATE_OFFHOOK
            val activeIncomingNumber = OtpManager.getActiveIncomingNumber(applicationContext)
            
            val activeVoipNumber = OtpManager.getActiveVoipCallNumber(applicationContext)
            val isVoipCallActive = activeVoipNumber != null

            val isCallActive = isTelecomCallActive || isVoipCallActive

            val isActiveTelecomUnknown = activeIncomingNumber != null && !ContactUtils.isContactExists(applicationContext, activeIncomingNumber)
            val isActiveVoipUnknown = activeVoipNumber != null && !ContactUtils.isContactExists(applicationContext, activeVoipNumber)
            val isActiveCallUnknown = isActiveTelecomUnknown || isActiveVoipUnknown

            val lastUnknownTelecomCall = OtpManager.getLastUnknownCallTime(applicationContext)
            val lastUnknownVoipCall = OtpManager.getLastUnknownVoipCallTime(applicationContext)
            val lastUnknownCall = maxOf(lastUnknownTelecomCall, lastUnknownVoipCall)
            
            val timeSinceUnknownCall = System.currentTimeMillis() - lastUnknownCall
            
            // If currently on a call AND the active call is unknown (regardless of duration)
            val isActiveScamCall = isCallActive && isActiveCallUnknown
            
            // If not currently on a call, but an unknown call was received within 1 hour
            val isRecentScamCall = !isCallActive && (timeSinceUnknownCall <= 1L * 60 * 60 * 1000)

            if (isActiveScamCall || isRecentScamCall) {
                serviceScope.launch {
                    val alert = MessageEntity(
                        sender = "System Alert",
                        content = "Warning triggered for potential OTP Theft. High-value OTP arrived during/after an unknown call.",
                        appName = "EdgeGuard AI",
                        analysisResult = "Alert",
                        scamProbability = 1.0f
                    )
                    database.messageDao().insertMessage(alert)
                    withContext(Dispatchers.Main) {
                        OverlayUtils.showOtpWarningOverlay(applicationContext)
                    }
                }
            }
        }

        // Ignore SMS from business/alphanumeric senders (only allow purely numeric/phone-number senders) for normal scam processing
        if (isSms) {
            val isPhoneNumber = title.matches(Regex("^[\\d\\s\\+\\-\\(\\)]+\$"))
            if (!isPhoneNumber) {
                Log.d("ScamDetection", "Ignoring SMS from non-numeric sender: $title")
                return
            }
        }

        serviceScope.launch {
            messageMutex.withLock {
                val lowerText = text.lowercase(Locale.ROOT)
                val isApk = lowerText.contains(".apk")
                
                val isFakeCredit = isSms && (
                    (lowerText.contains("credited") || lowerText.contains("received") || lowerText.contains("debited") || lowerText.contains("deducted")) && 
                    (lowerText.contains("rs") || lowerText.contains("inr") || lowerText.contains("amount"))
                )

                val isKnownContact = ContactUtils.isContactExists(applicationContext, title)
                
                // Allow analysis if it hits a deterministic rule (like fake credit/APK).
                // For regular text messages, we only run the PyTorch text analyzer on standard SMS (non-VoIP).
                if (isApk || isFakeCredit || (!isKnownContact && !isVoipApp)) {
                    val message = MessageEntity(
                        sender = title,
                        content = text,
                        appName = voipAppName ?: "SMS"
                    )
                    ScamAnalyzer.getInstance(this@ScamDetectionService).analyzeMessage(message, isApk = isApk, isFakeCredit = isFakeCredit)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (voipAppNames.containsKey(sbn.packageName)) {
            val title = sbn.notification.extras.getString(Notification.EXTRA_TITLE)
            val activeVoipCall = OtpManager.getActiveVoipCallNumber(applicationContext)
            
            // If the removed notification's title matches the currently tracked active call, the call has ended
            if (activeVoipCall != null && activeVoipCall == title) {
                OtpManager.clearActiveVoipCallNumber(applicationContext)
            }
        }
    }


    private fun isSystemNotification(title: String, text: String): Boolean {
        val lowerText = text.lowercase(Locale.ROOT).trim()
        val lowerTitle = title.lowercase(Locale.ROOT).trim()

        // Ignore summary group notifications which usually have "WhatsApp" as the title
        if (lowerTitle == "whatsapp") return true

        val systemPhrases = listOf(
            "checking for new messages",
            "missed voice call",
            "missed video call",
            "incoming voice call",
            "incoming video call",
            "whatsapp web is currently active",
            "backup in progress"
        )

        if (systemPhrases.any { lowerText.contains(it) }) return true

        // Ignore "1 new message", "2 new messages from 2 chats", etc.
        if (lowerText.matches(Regex("^\\d+\\s+new\\s+messages?.*"))) return true

        return false
    }

    private fun isBrowserSpam(title: String, text: String): Boolean {
        val lowerContent = (title + " " + text).lowercase(Locale.ROOT)
        val spamKeywords = listOf(
            // Fake Antivirus
            "mcafee", "norton", "antivirus", "subscription expired", "renew now", "avast", "kaspersky",
            // Fake Infections
            "virus detected", "infected", "hacked", "trojan", "malware", "system damaged", "your device is at risk", "spyware",
            // Fake System Alerts
            "storage full", "clean now", "memory low", "boost speed", "update required", "battery critical", "memory full", "system update", "update now", "software update",
            // Fake Cashback & Payments
            "cashback", "money credited", "gpay reward", "phonepe reward", "claim your amount", "loan approved", "scratch card",
            // Scam Rewards/Ads
            "won an iphone", "spin the wheel", "claim your prize", "lottery winner", "hot girls", "dating", "lucky draw", "single ladies"
        )
        return spamKeywords.any { lowerContent.contains(it) }
    }
}



