package com.parv.safetexts

import android.accessibilityservice.AccessibilityService
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray
import java.time.Instant
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.launch

class MessageReaderAccessibilityService : AccessibilityService() {

    private var lastPackageName: String = ""
    private var previousPackageName: String = ""
    private var lastPackageTime: Long = 0L
    private var lastBrowserJumpTime: Long = 0L
    private var lastDMJumpTagTime: Long = 0L
    private var lastPreInstallWarningTime: Long = 0L
    private var lastScreenCheckTime: Long = 0L
    private var cachedPackageName: String = ""
    private var cachedChatTitle: String = ""
    private var cachedIsActualChat: Boolean = false
    private var cachedIsGroup: Boolean = false
    private var cachedIsVerifiedAccount: Boolean = false
    private var cachedIsProfileScreen: Boolean = false
    private var cachedIsNonChatScreen: Boolean = false
    private val knownGroupTitles = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val knownVerifiedAccounts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    companion object {
        var allowNextInstallUntilTime: Long = 0L
        private val BIDI_REGEX = Regex("[\\u200E\\u200F\\u202A-\\u202E]")
        private val TIME_REGEX_1 = Regex("^\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?$")
        private val TIME_REGEX_2 = Regex("^(Yesterday|Today|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)$", RegexOption.IGNORE_CASE)
        private val TIME_REGEX_3 = Regex("^\\d{1,2}/\\d{1,2}/\\d{2,4}$")
        private val TIME_REGEX_4 = Regex("^\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4}$")
        private val TIME_REGEX_5 = Regex("^[A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4}$")
        private val NON_CHAT_TITLES = setOf(
            "whatsapp", "settings", "chats", "privacy", "account", "notifications",
            "storage and data", "help", "app language", "starred messages", "linked devices",
            "payments", "updates", "calls", "communities", "status", "new group", "new broadcast",
            "messages", "search", "conversations", "contacts", "archived", "spam and blocked",
            "security", "two-step verification", "change number", "request account info", "delete my account",
            "avatar", "theme", "chat backup", "chat history", "security notifications"
        )
        private val IGNORED_EXACT_WORDS = setOf(
            "message", "forwarded", "forwarded many times",
            "document", "camera", "gallery", "audio", "location", "contact", "poll",
            "recent", "images", "image", "photo", "photos", "video", "videos", "files",
            "gif", "stickers", "avatar", "payment", "catalogue", "reply", "copy", "delete",
            "share", "info", "report", "block", "cancel", "ok", "yes", "no", "done", "send",
            "attach", "attachment", "recent files", "all files", "downloads", "documents",
            "large files", "oldest", "newest", "sort by", "view", "edit", "select", "albums", "add",
            "chats", "updates", "communities", "calls", "settings", "search", "unread", "favorites", "groups",
            "account", "privacy", "notifications", "storage and data", "app language", "help", "invite a friend"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // --- UNIVERSAL APK CLICK & DOWNLOAD INTERCEPTOR ---
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val apkFound = findApkInEventOrSource(event)
            if (apkFound != null && packageName != "com.parv.safetexts") {
                Log.d("MessageReader", "User clicked on APK: $apkFound in $packageName")
                val appTitle = when {
                    packageName.contains("whatsapp") -> "WhatsApp"
                    packageName.contains("chrome") -> "Chrome"
                    packageName.contains("sbrowser") -> "Samsung Internet"
                    packageName.contains("telegram") -> "Telegram"
                    packageName.contains("myfiles") -> "My Files"
                    else -> "Download Shield"
                }
                val alert = MessageEntity(
                    sender = appTitle,
                    content = "Malicious APK file: $apkFound",
                    appName = packageName,
                    analysisResult = "Alert",
                    scamProbability = 1.0f
                )
                ScamAnalyzer.getInstance(this).analyzeMessage(alert, isApk = true)
                return
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            
            // LAYER 1: Pre-Install Shield for Malicious APKs
            // CRITICAL: Only match actual system package installers!
            // NEVER include "android" (system resolver / open-with sheet) or MyFiles or document viewers!
            val isPackageInstaller = packageName.contains("packageinstaller") &&
                                     packageName != "android" &&
                                     !packageName.contains("myfiles")

            if (isPackageInstaller) {
                val rootNode = rootInActiveWindow
                if (rootNode != null && isInstallScreen(rootNode)) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime < allowNextInstallUntilTime) {
                        // User explicitly bypassed the warning in the last 60 seconds
                        return
                    }

                    if (currentTime - lastPreInstallWarningTime > 3000L) {
                        lastPreInstallWarningTime = currentTime
                        
                        // CRITICAL: Android 12+ prevents overlays from drawing on top of the Package Installer to prevent clickjacking.
                        // Therefore, the installer will hide our overlay.
                        // To fix this, we force close the package installer by pressing BACK automatically!
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        
                        // Log it to the database so the user can review their history later
                        val alert = MessageEntity(
                            sender = "System Alert",
                            content = "Pre-Install Shield triggered: Prevented user from installing an unverified APK outside of Google Play.",
                            appName = "SafeConvos",
                            analysisResult = "Alert",
                            scamProbability = 1.0f
                        )
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
                        }
                        
                        // Add a slight delay to allow the package installer to close before popping the overlay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val isSevere = ContactUtils.isUserOnCall(this@MessageReaderAccessibilityService)
                            OverlayUtils.showPreInstallWarningOverlay(this, isSevere)
                        }, 500)
                    }
                }
            }

            if (OtpManager.isBankingApp(packageName)) {
                val riskTime = OtpManager.getDigitalArrestRiskTime(this)
                val timeSinceRisk = System.currentTimeMillis() - riskTime
                
                // If they open a banking app within 2 hours of a Digital Arrest warning flag
                if (timeSinceRisk <= 2L * 60 * 60 * 1000) {
                    val alert = MessageEntity(
                        sender = "System Alert",
                        content = "Warning triggered for potential Digital Arrest. User opened banking app shortly after a long unknown video call.",
                        appName = "SafeConvos",
                        analysisResult = "Alert",
                        scamProbability = 1.0f
                    )
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
                    }
                    OverlayUtils.showDigitalArrestWarningOverlay(this)
                }
            }

            // --- LAYER 3: Social Media Scam Funnel Interceptor ---
            val isBrowser = packageName == "com.android.chrome" || 
                            packageName == "com.facebook.katana" || 
                            packageName == "com.facebook.lite" ||
                            packageName == "com.instagram.android" ||
                            packageName == "com.instagram.lite" ||
                            packageName == "com.sec.android.app.sbrowser"
                            
            val isWhatsAppOrTelegram = packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b" || packageName == "org.telegram.messenger"
            
            val currentTime = System.currentTimeMillis()
            
            if (isBrowser) {
                lastBrowserJumpTime = currentTime
            }

            if (packageName != lastPackageName && packageName != "com.android.systemui") {
                previousPackageName = lastPackageName
                lastPackageName = packageName
                lastPackageTime = currentTime
            }

            // 1. The WhatsApp/Telegram "Join" Shield
            val wasBrowserPrevious = previousPackageName == "com.android.chrome" || 
                                     previousPackageName == "com.facebook.katana" || 
                                     previousPackageName == "com.facebook.lite" ||
                                     previousPackageName == "com.instagram.android" ||
                                     previousPackageName == "com.instagram.lite" ||
                                     previousPackageName == "com.sec.android.app.sbrowser" ||
                                     previousPackageName == "android" // Intent Resolver

            if (isWhatsAppOrTelegram) {
                val isDirectJump = wasBrowserPrevious && (currentTime - lastPackageTime < 60000L)
                val isRecentBrowserActivity = currentTime - lastBrowserJumpTime < 60000L

                if (isDirectJump || isRecentBrowserActivity) {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        val isJoinScreen = checkForJoinGroupUI(rootNode)
                        if (isJoinScreen) {
                            Log.d("FunnelInterceptor", "Join Shield Triggered! Preventing user from joining Telegram/WhatsApp group from browser jump.")
                            triggerScamFunnelOverlay()
                            lastBrowserJumpTime = 0L // Reset
                            lastPackageTime = 0L
                        } else if (isDirectJump) {
                            // Immediately trigger the DM alert if they jumped directly from an ad/browser to a DM
                            val chatTitle = extractBestEffortChatTitle(rootNode, packageName)
                            if (chatTitle.isNotEmpty() || isChatScreen(rootNode)) {
                                // ONLY trigger if it's an unknown number (not in contacts AND looks like a raw phone number with 7+ digits)
                                val isUnsavedPhoneNumber = !ContactUtils.isContactExists(this@MessageReaderAccessibilityService, chatTitle) && !ContactUtils.isWhatsAppCompanyAccount(chatTitle)
                                if (isUnsavedPhoneNumber) {
                                    Log.d("FunnelInterceptor", "DM Jump Shield Triggered! Preventing user from DMing from browser jump.")
                                    triggerScamFunnelOverlay(isDM = true)
                                    lastBrowserJumpTime = 0L // Reset
                                    lastPackageTime = 0L
                                }
                            }
                        }
                    }
                }
            }

            // 2. The Link Interceptor & Betting Scanner & APK Download Scanner (Browser scanning)
            if (isBrowser) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    checkForBrowserApkDownload(rootNode, packageName)
                    if (checkForScamFunnel(rootNode)) {
                        Log.d("FunnelInterceptor", "Scam Funnel Detected on Screen! Blocking.")
                        triggerScamFunnelOverlay()
                    }
                }

                // 3. The Browser Search Intent (Pre-Emptive Warning)
                val browserPackages = setOf(
                    "com.android.chrome",
                    "org.mozilla.firefox",
                    "com.sec.android.app.sbrowser", // Samsung Internet
                    "com.microsoft.emmx", // Edge
                    "com.opera.browser",
                    "com.opera.mini.native",
                    "com.brave.browser",
                    "com.duckduckgo.mobile.android",
                    "com.vivaldi.browser",
                    "com.UCMobile.intl"
                )
                if (packageName in browserPackages) {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        if (checkForBookingIntent(rootNode)) {
                            val prefs = getSharedPreferences("safetexts_prefs", android.content.Context.MODE_PRIVATE)
                            val lastWarningTime = prefs.getLong("last_booking_warning_time", 0L)
                            val currentTime = System.currentTimeMillis()
                            
                            // Only warn once per 1 hour to avoid spamming while typing
                            if (currentTime - lastWarningTime > 60 * 60 * 1000L) {
                                Handler(Looper.getMainLooper()).post {
                                    OverlayUtils.showPreEmptiveBookingAlert(this@MessageReaderAccessibilityService)
                                }
                                prefs.edit()
                                    .putLong("booking_intent_timestamp", currentTime)
                                    .putLong("last_booking_warning_time", currentTime)
                                    .apply()
                                
                                val alert = com.parv.safetexts.MessageEntity(
                                    sender = "System Alert",
                                    content = "Travel safety alert triggered: User was searching for travel bookings in a web browser.",
                                    appName = "SafeConvos",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
                                }
                                Log.d("FunnelInterceptor", "Booking Intent Logged in Browser")
                            }
                        }
                        if (checkForLoanIntent(rootNode)) {
                            val prefs = getSharedPreferences("safetexts_prefs", android.content.Context.MODE_PRIVATE)
                            val lastLoanWarningTime = prefs.getLong("last_loan_warning_time", 0L)
                            val currentTime = System.currentTimeMillis()
                            
                            // Warn once per 1 hour for loans
                            if (currentTime - lastLoanWarningTime > 60 * 60 * 1000L) {
                                Handler(Looper.getMainLooper()).post {
                                    OverlayUtils.showPreEmptiveLoanAlert(this@MessageReaderAccessibilityService)
                                }
                                prefs.edit()
                                    .putLong("last_loan_warning_time", currentTime)
                                    .apply()

                                val loanAlert = com.parv.safetexts.MessageEntity(
                                    sender = "System Alert",
                                    content = "Loan safety alert triggered: User was searching for loans in a web browser.",
                                    appName = "SafeConvos",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(loanAlert)
                                }
                                Log.d("FunnelInterceptor", "Loan Intent Logged in Browser")
                            }
                        }
                        if (checkForJobIntent(rootNode)) {
                            val prefs = getSharedPreferences("safetexts_prefs", android.content.Context.MODE_PRIVATE)
                            val lastJobWarningTime = prefs.getLong("last_job_warning_time", 0L)
                            val currentTime = System.currentTimeMillis()
                            
                            // Warn once per 1 hour for jobs
                            if (currentTime - lastJobWarningTime > 60 * 60 * 1000L) {
                                Handler(Looper.getMainLooper()).post {
                                    OverlayUtils.showPreEmptiveJobAlert(this@MessageReaderAccessibilityService)
                                }
                                prefs.edit()
                                    .putLong("last_job_warning_time", currentTime)
                                    .apply()

                                val jobAlert = com.parv.safetexts.MessageEntity(
                                    sender = "System Alert",
                                    content = "Job safety alert triggered: User was searching for online jobs or tasks in a web browser.",
                                    appName = "SafeConvos",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(jobAlert)
                                }
                                Log.d("FunnelInterceptor", "Job Intent Logged in Browser")
                            }
                        }
                        if (checkForTradingIntent(rootNode)) {
                            val prefs = getSharedPreferences("safetexts_prefs", android.content.Context.MODE_PRIVATE)
                            val lastTradingWarningTime = prefs.getLong("last_trading_warning_time", 0L)
                            val currentTime = System.currentTimeMillis()
                            
                            // Warn once per 1 hour for trading
                            if (currentTime - lastTradingWarningTime > 60 * 60 * 1000L) {
                                Handler(Looper.getMainLooper()).post {
                                    OverlayUtils.showPreEmptiveTradingAlert(this@MessageReaderAccessibilityService)
                                }
                                prefs.edit()
                                    .putLong("last_trading_warning_time", currentTime)
                                    .apply()

                                val tradingAlert = com.parv.safetexts.MessageEntity(
                                    sender = "System Alert",
                                    content = "Trading safety alert triggered: User was searching for trading or crypto in a web browser.",
                                    appName = "SafeConvos",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(tradingAlert)
                                }
                                Log.d("FunnelInterceptor", "Trading Intent Logged in Browser")
                            }
                        }
                        if (checkForHelplineIntent(rootNode)) {
                            val prefs = getSharedPreferences("safetexts_prefs", android.content.Context.MODE_PRIVATE)
                            val lastHelplineWarningTime = prefs.getLong("last_helpline_warning_time", 0L)
                            val currentTime = System.currentTimeMillis()
                            
                            // Warn once per 1 hour for helpline
                            if (currentTime - lastHelplineWarningTime > 60 * 60 * 1000L) {
                                Handler(Looper.getMainLooper()).post {
                                    OverlayUtils.showPreEmptiveHelplineAlert(this@MessageReaderAccessibilityService)
                                }
                                prefs.edit()
                                    .putLong("last_helpline_warning_time", currentTime)
                                    .apply()

                                val helplineAlert = com.parv.safetexts.MessageEntity(
                                    sender = "System Alert",
                                    content = "Customer care safety alert triggered: User was searching for helplines or customer care in a web browser.",
                                    appName = "SafeConvos",
                                    analysisResult = "Alert",
                                    scamProbability = 1.0f
                                )
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(helplineAlert)
                                }
                                Log.d("FunnelInterceptor", "Helpline Intent Logged in Browser")
                            }
                        }
                    }
                }
            }
        }

        // Filter for messaging apps
        val isWhatsApp = packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b"
        val isTelegram = packageName == "org.telegram.messenger"
        
        // Universally detect the default SMS app for ANY phone model
        val defaultSmsApp = android.provider.Telephony.Sms.getDefaultSmsPackage(this)
        val isSms = packageName == defaultSmsApp || packageName.contains("messaging") || packageName.contains("sms") || packageName.contains("mms")

        if (isWhatsApp || isTelegram || isSms) {
            val rootNode = rootInActiveWindow ?: return
            
            val currentTime = System.currentTimeMillis()
            val currentChatTitle = extractBestEffortChatTitle(rootNode, packageName)
            val isWindowStateChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            val isTitleChanged = currentChatTitle.isNotEmpty() && currentChatTitle != cachedChatTitle
            val isPackageChanged = packageName != cachedPackageName
            val shouldRefreshScreenState = isWindowStateChange || 
                                           isPackageChanged || 
                                           isTitleChanged ||
                                           (currentTime - lastScreenCheckTime > 500L)

            if (shouldRefreshScreenState) {
                cachedPackageName = packageName
                if (isTitleChanged || cachedChatTitle.isEmpty()) {
                    cachedChatTitle = currentChatTitle
                }
                cachedIsActualChat = isChatScreen(rootNode, packageName)
                
                // If this chat title was already verified as a group, KEEP IT AS A GROUP FOREVER!
                val isKnownGroup = (currentChatTitle.isNotEmpty() && knownGroupTitles.contains(currentChatTitle)) ||
                                   (cachedChatTitle.isNotEmpty() && knownGroupTitles.contains(cachedChatTitle))

                if (isKnownGroup) {
                    cachedIsGroup = true
                } else if (cachedIsActualChat) {
                    val detectedGroup = isGroupChat(rootNode, packageName, cachedChatTitle)
                    if (detectedGroup) {
                        cachedIsGroup = true
                        if (cachedChatTitle.isNotEmpty()) knownGroupTitles.add(cachedChatTitle)
                        if (currentChatTitle.isNotEmpty()) knownGroupTitles.add(currentChatTitle)
                    } else if (isTitleChanged || isPackageChanged) {
                        // Only reset to false if user switched conversations or packages!
                        cachedIsGroup = false
                    }
                    // If same conversation, do NOT reset cachedIsGroup from true to false!
                } else {
                    cachedIsGroup = false
                }

                // Check if this chat title was already verified as a verified blue-tick account:
                val isKnownVerified = (currentChatTitle.isNotEmpty() && knownVerifiedAccounts.contains(currentChatTitle)) ||
                                      (cachedChatTitle.isNotEmpty() && knownVerifiedAccounts.contains(cachedChatTitle))

                if (isKnownVerified) {
                    cachedIsVerifiedAccount = true
                } else if (cachedIsActualChat && isWhatsApp) {
                    val detectedVerified = isVerifiedWhatsAppAccount(rootNode, cachedChatTitle.ifEmpty { currentChatTitle })
                    if (detectedVerified) {
                        cachedIsVerifiedAccount = true
                        if (cachedChatTitle.isNotEmpty()) knownVerifiedAccounts.add(cachedChatTitle)
                        if (currentChatTitle.isNotEmpty()) knownVerifiedAccounts.add(currentChatTitle)
                    } else if (isTitleChanged || isPackageChanged) {
                        cachedIsVerifiedAccount = false
                    }
                } else {
                    cachedIsVerifiedAccount = false
                }

                cachedIsProfileScreen = if (cachedIsActualChat) isProfileScreen(rootNode) else false
                cachedIsNonChatScreen = cachedChatTitle.isNotEmpty() && NON_CHAT_TITLES.contains(cachedChatTitle.lowercase().trim())
                lastScreenCheckTime = currentTime

                if (cachedChatTitle.isNotEmpty() && isWhatsApp) {
                    val prefs = getSharedPreferences("safetexts_prefs", android.content.Context.MODE_PRIVATE)
                    val intentTime = prefs.getLong("booking_intent_timestamp", 0L)
                    if (intentTime > 0 && currentTime - intentTime < 30 * 60 * 1000) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val dao = AppDatabase.getDatabase(this@MessageReaderAccessibilityService).chatContextDao()
                            dao.insertOrUpdate(ChatContextEntity(cachedChatTitle, "BOOKING_SCAM_RISK", currentTime))
                        }
                        prefs.edit().remove("booking_intent_timestamp").apply()
                        Log.d("FunnelInterceptor", "Temporal Link: Chat '$cachedChatTitle' tagged as BOOKING_SCAM_RISK")
                    }
                }
            }

            // Real-time WhatsApp verified account detection safety check:
            var isVerified = cachedIsVerifiedAccount || 
                             (cachedChatTitle.isNotEmpty() && knownVerifiedAccounts.contains(cachedChatTitle)) ||
                             (currentChatTitle.isNotEmpty() && knownVerifiedAccounts.contains(currentChatTitle))
            if (isWhatsApp && cachedIsActualChat && !isVerified) {
                if (isVerifiedWhatsAppAccount(rootNode, cachedChatTitle.ifEmpty { currentChatTitle })) {
                    isVerified = true
                    cachedIsVerifiedAccount = true
                    if (cachedChatTitle.isNotEmpty()) knownVerifiedAccounts.add(cachedChatTitle)
                    if (currentChatTitle.isNotEmpty()) knownVerifiedAccounts.add(currentChatTitle)
                }
            }

            // Real-time WhatsApp group detection safety check:
            var isGroup = cachedIsGroup || (cachedChatTitle.isNotEmpty() && knownGroupTitles.contains(cachedChatTitle))
            if (isWhatsApp && cachedIsActualChat && !isGroup && !isVerified) {
                if (isGroupChat(rootNode, packageName, cachedChatTitle)) {
                    isGroup = true
                    cachedIsGroup = true
                    if (cachedChatTitle.isNotEmpty()) knownGroupTitles.add(cachedChatTitle)
                }
            }

            if (cachedIsActualChat && !cachedIsProfileScreen && !cachedIsNonChatScreen) {
                val effectiveTitle = cachedChatTitle.ifEmpty { currentChatTitle }
                val isAlphabetSender = isWhatsApp && effectiveTitle.isNotEmpty() && 
                    (ContactUtils.isAlphabetOrAlphanumeric(effectiveTitle) || ContactUtils.isContactExists(this, effectiveTitle))

                if (isVerified) {
                    // VERIFIED BLUE TICK ACCOUNT: DO NOT SCAN AT ALL!
                    Log.d("MessageReader", "Skipping verified WhatsApp account: '$cachedChatTitle'")
                } else if (isAlphabetSender) {
                    // Sender name is in Alphabet / Alphanumeric instead of phone number -> DO NOT SCAN!
                    // Only scan for malicious .apk files if present!
                    scanForApksOnly(rootNode, packageName, effectiveTitle)
                } else if (isGroup) {
                    // WhatsApp & Telegram groups: ONLY scan for malicious .apk files, NEVER read or alert on chat text!
                    scanForApksOnly(rootNode, packageName, cachedChatTitle)
                } else {
                    extractTextFromNode(rootNode, packageName, cachedChatTitle)
                }
            }
        }
    }

    private fun isGroupChat(rootNode: AccessibilityNodeInfo, packageName: String, chatTitle: String): Boolean {
        if (packageName.contains("whatsapp")) {
            // 1. Check subtitle view IDs across regular WhatsApp and WhatsApp Business
            val subtitleIds = listOf(
                "$packageName:id/conversation_contact_status",
                "com.whatsapp:id/conversation_contact_status",
                "com.whatsapp.w4b:id/conversation_contact_status"
            )
            for (id in subtitleIds) {
                val subtitles = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!subtitles.isNullOrEmpty()) {
                    for (sub in subtitles) {
                        val text = sub.text?.toString()?.lowercase()?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            // In WhatsApp groups, the subtitle is the participants list (e.g. "Alice, Bob, You")
                            // or group status info. A 1-on-1 chat subtitle NEVER contains a comma.
                            if (text.contains(",") ||
                                text.contains("group") ||
                                text.contains("participants") ||
                                text.contains("members") ||
                                text.contains("community") ||
                                text.contains("channel") ||
                                text.contains("announcement") ||
                                text.contains("only admins") ||
                                text.contains("admins can send") ||
                                text.contains("tap here for group") ||
                                text.contains("tap here for info") ||
                                text.startsWith("you,") ||
                                text.startsWith("you and") ||
                                (text.contains(" and ") && (text.contains("other") || text.contains("more"))) ||
                                (text.endsWith("typing...") && text != "typing...") ||
                                (text.endsWith("recording audio...") && text != "recording audio...")
                            ) {
                                subtitles.forEach { it.recycle() }
                                Log.d("MessageReader", "Detected WhatsApp group chat by subtitle: '$text'")
                                return true
                            }
                        }
                    }
                    subtitles.forEach { it.recycle() }
                }
            }

            // 2. Check profile photo content description across WhatsApp and WhatsApp Business
            val photoIds = listOf(
                "$packageName:id/conversation_contact_photo",
                "com.whatsapp:id/conversation_contact_photo",
                "com.whatsapp.w4b:id/conversation_contact_photo"
            )
            for (id in photoIds) {
                val photoNodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!photoNodes.isNullOrEmpty()) {
                    for (photo in photoNodes) {
                        val contentDesc = photo.contentDescription?.toString()?.lowercase() ?: ""
                        if (contentDesc.contains("group") || contentDesc.contains("community") || contentDesc.contains("channel")) {
                            photoNodes.forEach { it.recycle() }
                            Log.d("MessageReader", "Detected WhatsApp group chat by photo desc: '$contentDesc'")
                            return true
                        }
                    }
                    photoNodes.forEach { it.recycle() }
                }
            }

            // 3. Sender name headers on messages (name_in_message_color): 100% exclusive to group chats!
            val nameIds = listOf(
                "$packageName:id/name_in_message_color",
                "com.whatsapp:id/name_in_message_color",
                "com.whatsapp.w4b:id/name_in_message_color"
            )
            for (id in nameIds) {
                val nameNodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nameNodes.isNullOrEmpty()) {
                    nameNodes.forEach { it.recycle() }
                    Log.d("MessageReader", "Detected WhatsApp group chat by name_in_message_color presence")
                    return true
                }
            }

            // 4. Check for group-specific notices / read-only chat info
            val readOnlyIds = listOf(
                "$packageName:id/read_only_chat_info",
                "com.whatsapp:id/read_only_chat_info",
                "com.whatsapp.w4b:id/read_only_chat_info"
            )
            for (id in readOnlyIds) {
                val readOnlyNodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!readOnlyNodes.isNullOrEmpty()) {
                    readOnlyNodes.forEach { it.recycle() }
                    Log.d("MessageReader", "Detected WhatsApp group chat by read_only_chat_info")
                    return true
                }
            }

            // 5. Check if any message bubble has a sender name starting with tilde (~)
            if (hasTildeSenderName(rootNode)) {
                Log.d("MessageReader", "Detected WhatsApp group chat by tilde sender name (~)")
                return true
            }

            // 6. Fast hierarchy scan in case view IDs were not fully indexed in accessibility cache
            if (detectWhatsAppGroupInHierarchy(rootNode)) {
                Log.d("MessageReader", "Detected WhatsApp group chat via hierarchy inspection")
                return true
            }

        } else if (packageName.contains("telegram")) {
            val subtitles = rootNode.findAccessibilityNodeInfosByViewId("org.telegram.messenger:id/action_bar_subtitle")
            if (!subtitles.isNullOrEmpty()) {
                val text = subtitles[0].text?.toString()?.lowercase() ?: ""
                subtitles.forEach { it.recycle() }
                if (text.contains("members") || text.contains("subscribers") || text.contains(",")) {
                    return true
                }
            }
        } else if (packageName.contains("messaging") || packageName.contains("sms") || packageName.contains("mms")) {
            if (chatTitle.contains(",")) {
                return true
            }
        }
        return false
    }

    private fun hasTildeSenderName(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 12) return false
        val text = node.text?.toString()?.trim() ?: ""
        if (text.startsWith("~")) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (hasTildeSenderName(child, depth + 1)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun detectWhatsAppGroupInHierarchy(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 12) return false

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.endsWith("name_in_message_color") || viewId.endsWith("read_only_chat_info")) {
            return true
        }

        val nodeText = node.text?.toString()?.trim() ?: ""
        if (nodeText.startsWith("~")) {
            return true
        }

        if (viewId.endsWith("conversation_contact_status")) {
            val text = node.text?.toString()?.lowercase()?.trim() ?: ""
            if (text.contains(",") || text.contains("group") || text.contains("participants") ||
                text.contains("members") || text.contains("community") || text.contains("channel") ||
                text.contains("announcement") || text.contains("admins") || text.contains("others")) {
                return true
            }
        }

        if (viewId.endsWith("conversation_contact_photo")) {
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (desc.contains("group") || desc.contains("community") || desc.contains("channel")) {
                return true
            }
        }

        // Group system message check: "Messages and calls are end-to-end encrypted in this group"
        if (node.text != null) {
            val text = node.text.toString().lowercase()
            if (text.contains("in this group") || 
                text.contains("only admins can send") ||
                text.contains("tap here for group info") ||
                text.contains("you joined using this group") ||
                text.contains("you were added to this group")) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (detectWhatsAppGroupInHierarchy(child, depth + 1)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }

        return false
    }

    private fun isVerifiedWhatsAppAccount(rootNode: AccessibilityNodeInfo, chatTitle: String): Boolean {
        // 1. Check known verified accounts set
        if (chatTitle.isNotEmpty() && knownVerifiedAccounts.contains(chatTitle)) {
            return true
        }

        // 2. Check toolbar subtitle for "official business account" or "verified business"
        val subtitleIds = listOf(
            "com.whatsapp:id/conversation_contact_status",
            "com.whatsapp.w4b:id/conversation_contact_status"
        )
        for (id in subtitleIds) {
            val subtitles = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (!subtitles.isNullOrEmpty()) {
                for (sub in subtitles) {
                    val text = sub.text?.toString()?.lowercase()?.trim() ?: ""
                    val desc = sub.contentDescription?.toString()?.lowercase()?.trim() ?: ""
                    if (text.contains("official business account") || text.contains("verified business") ||
                        desc.contains("official business account") || desc.contains("verified business")) {
                        subtitles.forEach { it.recycle() }
                        return true
                    }
                }
                subtitles.forEach { it.recycle() }
            }
        }

        // 3. Check for verified badge views in toolbar
        val badgeIds = listOf(
            "com.whatsapp:id/verified_badge",
            "com.whatsapp.w4b:id/verified_badge",
            "com.whatsapp:id/w4b_verified_badge",
            "com.whatsapp:id/official_badge",
            "com.whatsapp.w4b:id/official_badge",
            "com.whatsapp:id/verified_icon",
            "com.whatsapp.w4b:id/verified_icon"
        )
        for (id in badgeIds) {
            val badgeNodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (!badgeNodes.isNullOrEmpty()) {
                badgeNodes.forEach { it.recycle() }
                return true
            }
        }

        // 4. Recursive inspection of toolbar / header for verified content descriptions or official business text
        return scanForVerifiedBadge(rootNode)
    }

    private fun scanForVerifiedBadge(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 12) return false

        val desc = node.contentDescription?.toString()?.lowercase()?.trim() ?: ""
        if (desc.contains("verified") || desc.contains("official business account") || desc.contains("official business")) {
            return true
        }

        val text = node.text?.toString()?.lowercase()?.trim() ?: ""
        if (text.contains("official business account") || text.contains("this is an official business account") || text.contains("verified business account")) {
            return true
        }

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("verified") || viewId.contains("official_badge")) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (scanForVerifiedBadge(child, depth + 1)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun isChatScreen(node: AccessibilityNodeInfo, packageName: String = ""): Boolean {
        if (packageName.contains("whatsapp")) {
            val whatsappChatIndicators = listOf(
                "com.whatsapp:id/entry",
                "com.whatsapp.w4b:id/entry",
                "$packageName:id/entry",
                "com.whatsapp:id/conversation_contact_name",
                "com.whatsapp.w4b:id/conversation_contact_name",
                "$packageName:id/conversation_contact_name",
                "com.whatsapp:id/voice_note_btn",
                "com.whatsapp.w4b:id/voice_note_btn",
                "$packageName:id/voice_note_btn"
            )
            for (id in whatsappChatIndicators) {
                val found = node.findAccessibilityNodeInfosByViewId(id)
                if (!found.isNullOrEmpty()) {
                    found.forEach { it.recycle() }
                    return true
                }
            }
            // For WhatsApp, if none of these conversation elements exist, it is 100% Home Screen, Settings, Status, or Calls!
            return false
        } else if (packageName.contains("telegram")) {
            val telegramChatIndicators = listOf(
                "org.telegram.messenger:id/chat_text_edit",
                "org.telegram.messenger:id/chat_write_box",
                "org.telegram.messenger:id/chat_send_button"
            )
            for (id in telegramChatIndicators) {
                val found = node.findAccessibilityNodeInfosByViewId(id)
                if (!found.isNullOrEmpty()) {
                    found.forEach { it.recycle() }
                    return true
                }
            }
            return false
        } else {
            // For SMS: check for compose message box in bottom half of screen (not top search bars)
            return hasBottomComposeBox(node)
        }
    }

    private fun hasBottomComposeBox(node: AccessibilityNodeInfo): Boolean {
        val screenHeight = resources.displayMetrics.heightPixels
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (node.isEditable && bounds.top > screenHeight * 0.45f) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (hasBottomComposeBox(child)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun isProfileScreen(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null) {
            val text = node.text.toString().lowercase().trim()
            if (text == "media, links, and docs" || 
                text == "mute notifications" || 
                text == "custom notifications" || 
                text == "media visibility" || 
                text == "encryption" || 
                text == "disappearing messages" ||
                text == "chat lock" ||
                text == "contact info") {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (isProfileScreen(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private data class NodeScanContext(
        val isInsideQuote: Boolean,
        val isSent: Boolean,
        val isInsideGroup: Boolean = false
    )

    private fun inspectNodeContext(node: AccessibilityNodeInfo, screenWidth: Int, isWhatsApp: Boolean): NodeScanContext {
        val nodeBounds = android.graphics.Rect()
        node.getBoundsInScreen(nodeBounds)

        // Ultra-fast geometry sent detection: In WhatsApp & SMS, user's sent messages are right-aligned.
        // If detected as sent by geometry, exit instantly (0ms) without any Binder IPC parent traversals!
        if (screenWidth > 0 && nodeBounds.width() > 0) {
            val distanceToLeft = nodeBounds.left
            val distanceToRight = screenWidth - nodeBounds.right
            if (distanceToRight < distanceToLeft - (screenWidth * 0.05f) && distanceToRight <= (screenWidth * 0.30f)) {
                return NodeScanContext(isInsideQuote = false, isSent = true, isInsideGroup = false)
            }
        }

        var isQuote = false
        var isSent = false
        var isGroup = false
        var current: AccessibilityNodeInfo? = node
        var count = 0

        while (current != null && count < 5) {
            val viewId = current.viewIdResourceName?.lowercase() ?: ""
            if (viewId.contains("quote") || viewId.contains("reply")) {
                isQuote = true
            }

            if (isWhatsApp && (viewId.endsWith("name_in_message_color") || viewId.endsWith("read_only_chat_info"))) {
                isGroup = true
            }

            val contentDesc = current.contentDescription?.toString()?.lowercase() ?: ""
            if (contentDesc.startsWith("you:") || contentDesc.contains("you: ")) {
                return NodeScanContext(isInsideQuote = isQuote, isSent = true, isInsideGroup = isGroup)
            }

            // Check if current node is a bubble container that has a sender name child (name_in_message_color)
            if (isWhatsApp && !isGroup && count <= 3) {
                for (c in 0 until current.childCount) {
                    val child = current.getChild(c)
                    if (child != null) {
                        val cId = child.viewIdResourceName?.lowercase() ?: ""
                        if (cId.endsWith("name_in_message_color")) {
                            isGroup = true
                        }
                        child.recycle()
                    }
                    if (isGroup) break
                }
            }

            val className = current.className?.toString() ?: ""
            if (className.contains("RecyclerView") || className.contains("ListView") || className.contains("ScrollView")) {
                break
            }

            current = current.parent
            count++
        }

        return NodeScanContext(isInsideQuote = isQuote, isSent = isSent, isInsideGroup = isGroup)
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo, packageName: String, chatTitle: String) {
        if (node.text != null && node.text.isNotBlank()) {
            val text = node.text.toString()
            val cleanText = text.trim().replace(BIDI_REGEX, "")
            val lowerText = cleanText.lowercase()

            val isTimeOrDate = cleanText.matches(TIME_REGEX_1) ||
                               cleanText.matches(TIME_REGEX_2) ||
                               cleanText.matches(TIME_REGEX_3) ||
                               cleanText.matches(TIME_REGEX_4) ||
                               cleanText.matches(TIME_REGEX_5)

            val isEditable = node.isEditable

            if (!isEditable && !isTimeOrDate && cleanText.isNotEmpty() && 
                !IGNORED_EXACT_WORDS.contains(lowerText.trim()) &&
                !lowerText.contains("type a message") && 
                !lowerText.contains("search") &&
                !lowerText.contains("type an sms message") &&
                !lowerText.contains("end-to-end encrypted") &&
                !lowerText.contains("end to end encrypted") &&
                !lowerText.contains("messages and calls are end-to-end encrypted") &&
                !lowerText.contains("call log was deleted") &&
                !lowerText.contains("this message was deleted") &&
                !lowerText.contains("you deleted this message") &&
                !lowerText.contains("not a contact") &&
                !lowerText.contains("business account") &&
                !lowerText.contains("tap for more info") &&
                !lowerText.contains("this business uses a secure service") &&
                !lowerText.contains("waiting for this message") &&
                !lowerText.contains("you're both in") &&
                !lowerText.contains("groups including") &&
                !lowerText.contains("tap here for group info") &&
                !lowerText.contains("only admins can send") &&
                !lowerText.contains("in this group")) {
                
                val viewId = node.viewIdResourceName ?: ""
                if (packageName.contains("whatsapp")) {
                    val effectiveSender = chatTitle.ifEmpty { cachedChatTitle }
                    val isAlphabetSender = effectiveSender.isNotEmpty() && 
                        (ContactUtils.isAlphabetOrAlphanumeric(effectiveSender) || ContactUtils.isContactExists(this, effectiveSender))
                    if (isAlphabetSender && !lowerText.contains(".apk")) {
                        return
                    }

                    if (effectiveSender.isEmpty() && !lowerText.contains(".apk")) {
                        return
                    }

                    // Strict verified account exclusion: NEVER read messages from verified accounts!
                    val isVerifiedChat = cachedIsVerifiedAccount || (chatTitle.isNotEmpty() && knownVerifiedAccounts.contains(chatTitle))
                    if (isVerifiedChat) {
                        return
                    }

                    // 1. If text starts with ~, it is a member sender name in a group!
                    if (cleanText.startsWith("~") || viewId.endsWith("name_in_message_color")) {
                        cachedIsGroup = true
                        if (chatTitle.isNotEmpty()) knownGroupTitles.add(chatTitle)
                        return
                    }

                    // 2. Strict group exclusion: If screen is currently a group or chat title is in known groups, NEVER read messages!
                    val isGroupChat = cachedIsGroup || (chatTitle.isNotEmpty() && knownGroupTitles.contains(chatTitle))
                    if (isGroupChat && !lowerText.contains(".apk")) {
                        return
                    }
                }

                val isApk = lowerText.contains(".apk")

                // Ignore document file attachment metadata (PDF names, page counts, slide counts, file sizes)
                val isDocMetadata = lowerText.endsWith(".pdf") || 
                                    lowerText.endsWith(".docx") || 
                                    lowerText.endsWith(".doc") || 
                                    lowerText.endsWith(".xlsx") || 
                                    lowerText.endsWith(".xls") || 
                                    lowerText.endsWith(".pptx") || 
                                    lowerText.endsWith(".ppt") || 
                                    lowerText.endsWith(".txt") ||
                                    cleanText.matches(Regex("""^\d+(\.\d+)?\s*(kb|mb|gb|b|bytes)$""", RegexOption.IGNORE_CASE)) ||
                                    cleanText.matches(Regex("""^\d+\s*(pages?|slides?|sheets?)$""", RegexOption.IGNORE_CASE)) ||
                                    cleanText == "•" || cleanText == "·"
                if (isDocMetadata && !isApk) {
                    return
                }
                val isFakeCredit = (packageName.contains("whatsapp") || packageName.contains("telegram")) &&
                    (lowerText.contains("credited") || lowerText.contains("received") || lowerText.contains("successful") || lowerText.contains("debited")) &&
                    (lowerText.contains("rs") || lowerText.contains("inr") || lowerText.contains("₹") || lowerText.contains("amount")) &&
                    (lowerText.contains("account") || lowerText.contains("wallet") || lowerText.contains("upi") || lowerText.contains("a/c") || lowerText.contains("paytm") || lowerText.contains("phonepe") || lowerText.contains("gpay"))

                val isKnownBadId = !isApk && !isFakeCredit && (
                    viewId.endsWith("name_in_message_color") || 
                    viewId.endsWith("date") ||
                    viewId.endsWith("conversations_row_message") ||
                    viewId.endsWith("conversation_contact_name") ||
                    viewId.endsWith("conversation_contact_status") ||
                    viewId.endsWith("action_bar_title") ||
                    viewId.endsWith("title") ||
                    viewId.endsWith("subtitle") ||
                    viewId.endsWith("search_src_text") ||
                    viewId.endsWith("search_box_text") ||
                    viewId.endsWith("tab")
                )
                                   
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                val screenWidth = resources.displayMetrics.widthPixels

                val nodeCenter = bounds.centerX()
                val screenCenter = screenWidth / 2
                val isCenterAligned = Math.abs(nodeCenter - screenCenter) < (screenWidth * 0.05f) && bounds.width() < (screenWidth * 0.5f)

                if ((!isKnownBadId && !isCenterAligned) || isApk) {
                    val contextInfo = inspectNodeContext(node, screenWidth, packageName.contains("whatsapp"))

                    // STRICT WHATSAPP GROUP PROTECTION:
                    // NEVER scan, analyze, or alert on group chat messages!
                    // Only dangerous .apk file downloads are allowed to alert.
                    if (contextInfo.isInsideGroup && !isApk) {
                        return
                    }

                    if (!contextInfo.isInsideQuote && !contextInfo.isSent && (!contextInfo.isInsideGroup || isApk)) {
                        if (chatTitle.isEmpty() || text.trim() != chatTitle.trim()) {
                            val finalSender = if (chatTitle.isNotEmpty()) chatTitle else "Screen Reader"
                            val message = MessageEntity(
                                sender = finalSender,
                                content = text.trim(),
                                appName = packageName
                            )
                            ScamAnalyzer.getInstance(this).analyzeMessage(message, isApk = isApk, isFakeCredit = if (contextInfo.isInsideGroup) false else isFakeCredit)
                        }
                    }
                }
            }
        }

        // Traverse children in reverse order (bottom-most newest messages processed first!)
        for (i in node.childCount - 1 downTo 0) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                extractTextFromNode(childNode, packageName, chatTitle)
                childNode.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.d("MessageReader", "Accessibility Service Interrupted")
    }

    private fun isUninstallScreen(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (text.contains("uninstall") || desc.contains("uninstall") || 
            text.contains("delete") || desc.contains("delete") || 
            text.contains("remove") || desc.contains("remove")) {
            return true
        }
        
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (isUninstallScreen(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun isInstallScreen(node: AccessibilityNodeInfo): Boolean {
        // If it's an uninstall screen, it is definitely not an install screen
        if (isUninstallScreen(node)) return false
        
        // Otherwise, look for explicit install/update keywords
        return hasInstallKeywords(node)
    }

    private fun hasInstallKeywords(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        // Specific package installer prompts
        if (text.contains("do you want to install this app") ||
            text.contains("do you want to install this application") ||
            text.contains("do you want to install an update") ||
            text.contains("install unknown apps") ||
            desc.contains("do you want to install this app") ||
            desc.contains("do you want to install this application")) {
            return true
        }

        // Action button with exact "Install" or "Update" on a package installer screen
        val isExactInstallButton = (node.isClickable || node.className?.contains("Button") == true) &&
            (text == "install" || text == "update" || desc == "install" || desc == "update")
        if (isExactInstallButton) {
            return true
        }
        
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (hasInstallKeywords(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun triggerScamFunnelOverlay(isDM: Boolean = false) {
        val alert = MessageEntity(
            sender = "System Alert",
            content = if (isDM) "Scam Funnel Interceptor triggered: User was blocked from a fraudulent DM conversation." else "Scam Funnel Interceptor triggered: User was blocked from joining a fraudulent group or illegal betting site.",
            appName = "SafeConvos",
            analysisResult = "Alert",
            scamProbability = 1.0f
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
        }
        OverlayUtils.showScamFunnelWarningOverlay(this, isDM)
    }

    private fun checkForJoinGroupUI(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null || node.contentDescription != null) {
            val text = node.text?.toString()?.lowercase()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase()?.trim() ?: ""
            
            if (text.contains("join group") || text.contains("join channel") || 
                text == "join" || text.contains("join chat") || 
                text.contains("request to join") || text == "join now" || text.contains("apply to join") ||
                text.contains("view group") || text.contains("view channel") || text.contains("view community") ||
                desc.contains("join group") || desc.contains("join channel") || 
                desc == "join" || desc.contains("join chat") || 
                desc.contains("request to join") || desc == "join now" || desc.contains("apply to join") ||
                desc.contains("view group") || desc.contains("view channel") || desc.contains("view community")) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForJoinGroupUI(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun normalizeText(input: String): String {
        return input.lowercase()
            .replace("!", "i")
            .replace("@", "a")
            .replace("$", "s")
            .replace("0", "o")
            .replace("3", "e")
            .replace("4", "a")
            .replace(" ", "") // Remove spaces to defeat 'w i n' or 'c a s i n o'
    }

    private fun checkForScamFunnel(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null) {
            val originalText = node.text.toString().lowercase()
            
            // Check for structural funnels (Link Interceptor)
            if (originalText.contains("wa.me") || 
                originalText.contains("chat.whatsapp.com") || 
                originalText.contains("t.me") || 
                originalText.contains("telegram.me")) {
                
                // If it contains a funnel link AND scam keywords
                if (originalText.contains("vip") || originalText.contains("trading") || 
                    originalText.contains("stock") || originalText.contains("tips") || 
                    originalText.contains("signals") || originalText.contains("profit") || 
                    originalText.contains("investment") || originalText.contains("crypto")) {
                    return true
                }
            }

            // The De-Leetification Engine (Obfuscation Bypass)
            val normalizedText = normalizeText(node.text.toString())
            
            val hasBettingAction = normalizedText.contains("betnow") || 
                                   normalizedText.contains("playcasino") || 
                                   normalizedText.contains("aviator") || 
                                   normalizedText.contains("teenpatti") || 
                                   normalizedText.contains("slotmachine") || 
                                   normalizedText.contains("winbig") ||
                                   normalizedText.contains("prediction")
                                   
            val hasBettingMechanic = normalizedText.contains("deposit") || 
                                     normalizedText.contains("bonus") || 
                                     normalizedText.contains("withdraw")

            if (hasBettingAction && hasBettingMechanic) {
                return true
            }
        }
        
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForScamFunnel(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun isSearchBar(node: AccessibilityNodeInfo): Boolean {
        val id = node.viewIdResourceName?.lowercase() ?: ""
        if (id.endsWith(":id/url_bar") || 
            id.endsWith(":id/search_src_text") || 
            id.endsWith(":id/search_box_text") || 
            id.endsWith(":id/address_bar") || 
            id.endsWith(":id/omnibox")) {
            return true
        }
        // Only consider it a search bar if it is an actual EditText widget
        if (node.isEditable && node.className == "android.widget.EditText") {
            return true
        }
        return false
    }

    private fun checkForBookingIntent(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null && isSearchBar(node)) {
            val text = node.text.toString().lowercase()
            val regex = Regex("\\b(hotel|resort|flight|villa|dhams|stays|train|bus|booking|call girl|massage service|escort)\\b")
            if (regex.containsMatchIn(text)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForBookingIntent(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun checkForHelplineIntent(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null && isSearchBar(node)) {
            val text = node.text.toString().lowercase()
            val regex = Regex("\\b(complaint care|helpline|customer care|customer service|support number|toll free|consumer helpline|contact number|customer support)\\b")
            if (regex.containsMatchIn(text)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForHelplineIntent(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun checkForLoanIntent(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null && isSearchBar(node)) {
            val text = node.text.toString().lowercase()
            val regex = Regex("\\b(loan|loans)\\b")
            if (regex.containsMatchIn(text)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForLoanIntent(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun checkForJobIntent(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null && isSearchBar(node)) {
            val text = node.text.toString().lowercase()
            val regex = Regex("\\b(job|jobs|hiring|vacancy|career|careers|recruitment|earn money online|wfh|wfh jobs|work from home|work from home jobs|part time|part time jobs|part-time jobs|full time|online tasks|like videos for money|online jobs|data entry|freelance|freelance jobs|make money online|remote jobs|remote work|work online|job search|naukri|indeed|linkedin jobs|placement|urgent hiring)\\b")
            if (regex.containsMatchIn(text)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForJobIntent(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun checkForTradingIntent(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null && isSearchBar(node)) {
            val text = node.text.toString().lowercase()
            val regex = Regex("\\b(trading|crypto|bitcoin|forex|stock tips|options trading|intraday|binance)\\b")
            if (regex.containsMatchIn(text)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForTradingIntent(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun checkForPaymentRequest(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null) {
            val text = node.text.toString().lowercase()
            if (text.contains("photo") || text.contains("qr code") || text.contains("scanner") || 
                text.contains("upi") || text.contains("@ybl") || text.contains("@paytm") || text.contains("@okicici") ||
                text.contains("pay now") || text.contains("pay")) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForPaymentRequest(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun triggerFakeBookingOverlay(chatTitle: String) {
        val alert = MessageEntity(
            sender = "System Alert",
            content = "Fake Booking Scam Interceptor triggered: User received a payment request in a chat linked to a recent hotel search.",
            appName = "SafeConvos",
            analysisResult = "Alert",
            scamProbability = 1.0f
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
        }
        OverlayUtils.showFakeBookingWarningOverlay(this, chatTitle)
    }

    private fun extractBestEffortChatTitle(rootNode: AccessibilityNodeInfo, packageName: String): String {
        if (packageName.contains("whatsapp")) {
            val whatsappTitleIds = listOf(
                "com.whatsapp:id/conversation_contact_name",
                "com.whatsapp.w4b:id/conversation_contact_name",
                "$packageName:id/conversation_contact_name"
            )
            for (id in whatsappTitleIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    val title = nodes[0].text?.toString()?.trim() ?: ""
                    nodes.forEach { it.recycle() }
                    if (title.isNotEmpty() && !NON_CHAT_TITLES.contains(title.lowercase())) return title
                }
            }
            // For WhatsApp, if not inside an active conversation, do not fall back to action bar titles (e.g. Settings, WhatsApp)
            return ""
        }

        val candidateIds = listOf(
            "$packageName:id/conversation_title",
            "$packageName:id/chat_name",
            "$packageName:id/contact_name",
            "$packageName:id/action_bar_title",
            "$packageName:id/title"
        )
        for (id in candidateIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val title = nodes[0].text?.toString()?.trim() ?: ""
                nodes.forEach { it.recycle() }
                if (title.isNotEmpty() && !NON_CHAT_TITLES.contains(title.lowercase())) return title
            }
        }

        if (packageName.contains("telegram")) {
            val title = findFirstTextNode(rootNode).trim()
            if (title.isNotEmpty() && !NON_CHAT_TITLES.contains(title.lowercase())) {
                return title
            }
        }
        
        return ""
    }

    private fun findFirstTextNode(node: AccessibilityNodeInfo): String {
        if (node.text != null && node.text.isNotBlank()) {
            return node.text.toString()
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                val found = findFirstTextNode(childNode)
                childNode.recycle()
                if (found.isNotBlank()) return found
            }
        }
        return ""
    }

    private fun checkForDMScamKeywords(node: AccessibilityNodeInfo): Boolean {
        if (node.text != null) {
            val text = node.text.toString().lowercase()
            // Job & Crypto DM Scams
            if (text.contains("part time") || text.contains("part-time") || text.contains("youtube") || 
                text.contains("like and subscribe") || text.contains("daily income") || text.contains("task") ||
                text.contains("crypto") || text.contains("trading") || text.contains("profit") || 
                text.contains("signals") || text.contains("investment") || text.contains("returns")) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (checkForDMScamKeywords(childNode)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }
        return false
    }

    private fun findApkInEventOrSource(event: AccessibilityEvent): String? {
        val eventTexts = event.text
        if (!eventTexts.isNullOrEmpty()) {
            for (t in eventTexts) {
                val str = t?.toString() ?: ""
                if (isNonApkDocument(str)) return null
                if (str.contains(".apk", ignoreCase = true)) {
                    return str
                }
            }
        }
        val source = event.source ?: return null
        val sourceText = source.text?.toString() ?: ""
        val sourceDesc = source.contentDescription?.toString() ?: ""

        if (isNonApkDocument(sourceText) || isNonApkDocument(sourceDesc)) {
            return null
        }

        if (sourceText.contains(".apk", ignoreCase = true)) {
            return sourceText
        }
        if (sourceDesc.contains(".apk", ignoreCase = true)) {
            return sourceDesc
        }

        // In WhatsApp & Browsers, the click might be on a download arrow/button next to the APK filename
        val parent = source.parent
        if (parent != null) {
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChild(i)
                if (sibling != null) {
                    val sText = sibling.text?.toString() ?: ""
                    val sDesc = sibling.contentDescription?.toString() ?: ""
                    sibling.recycle()
                    if (isNonApkDocument(sText) || isNonApkDocument(sDesc)) {
                        parent.recycle()
                        return null
                    }
                    if (sText.contains(".apk", ignoreCase = true)) {
                        parent.recycle()
                        return sText
                    }
                    if (sDesc.contains(".apk", ignoreCase = true)) {
                        parent.recycle()
                        return sDesc
                    }
                }
            }
            parent.recycle()
        }
        return null
    }

    private fun isNonApkDocument(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()
        val isNonApk = lower.contains(".pdf") || lower.contains(".docx") || lower.contains(".doc") ||
                       lower.contains(".xlsx") || lower.contains(".xls") || lower.contains(".pptx") ||
                       lower.contains(".ppt") || lower.contains(".txt") || lower.contains(".jpg") ||
                       lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".mp3") ||
                       lower.contains(".mp4") || lower.contains(".zip") || lower.contains(".rar")
        return isNonApk && !lower.contains(".apk")
    }

    private fun checkForBrowserApkDownload(node: AccessibilityNodeInfo, packageName: String): Boolean {
        if (node.text != null) {
            val text = node.text.toString().lowercase()
            // Catch Chrome download prompt: "File might be harmful. Do you want to download xyz.apk anyway?"
            // Or web page download button: "Download APK", "Install APK"
            if (text.contains(".apk") && (text.contains("harmful") || text.contains("download") || text.contains("anyway") || text.contains("install") || text.contains("file"))) {
                val alert = MessageEntity(
                    sender = if (packageName.contains("sbrowser")) "Samsung Internet" else "Chrome",
                    content = "Malicious APK Download: ${node.text}",
                    appName = packageName,
                    analysisResult = "Alert",
                    scamProbability = 1.0f
                )
                ScamAnalyzer.getInstance(this).analyzeMessage(alert, isApk = true)
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (checkForBrowserApkDownload(child, packageName)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun scanForApksOnly(node: AccessibilityNodeInfo, packageName: String, chatTitle: String) {
        if (node.text != null && node.text.isNotBlank()) {
            val text = node.text.toString()
            val lowerText = text.lowercase()
            if (lowerText.contains(".apk") && !node.isEditable) {
                val finalSender = if (chatTitle.isNotEmpty()) chatTitle else "Group APK"
                val message = MessageEntity(
                    sender = finalSender,
                    content = text.trim(),
                    appName = packageName
                )
                ScamAnalyzer.getInstance(this).analyzeMessage(message, isApk = true, isFakeCredit = false)
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scanForApksOnly(child, packageName, chatTitle)
                child.recycle()
            }
        }
    }
}
















