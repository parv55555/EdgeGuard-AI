package com.parv.safetexts

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.launch
import java.util.Locale

class MessageReaderAccessibilityService : AccessibilityService() {

    private var lastPackageName: String = ""
    private var previousPackageName: String = ""
    private var lastPackageTime: Long = 0L
    private var lastBrowserJumpTime: Long = 0L
    private var lastDMJumpTagTime: Long = 0L
    private var lastPreInstallWarningTime: Long = 0L
    private var lastApkDownloadWarningTime: Long = 0L
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

    // Viewport tracking for re-alerting when messages enter the screen
    private var lastEventTimestamp: Long = 0L
    private val DEBOUNCE_INTERVAL_MS = 200L
    private val previouslyVisibleMessageHashes = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

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
        private val DEDICATED_BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.UCMobile.intl"
        )
        private val SOCIAL_AD_PACKAGES = setOf(
            "com.facebook.katana",
            "com.facebook.lite",
            "com.instagram.android",
            "com.instagram.lite"
        )
        private val BROWSER_PACKAGES = DEDICATED_BROWSER_PACKAGES + SOCIAL_AD_PACKAGES
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = serviceInfo
            info.packageNames = null // Dynamically ensure listening to all packages globally
            info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                         android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                         android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            serviceInfo = info
            Log.d("MessageReader", "Accessibility service connected with global package listening and full view visibility")
        } catch (e: Exception) {
            Log.e("MessageReader", "Error configuring service info", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val isBrowser = packageName in BROWSER_PACKAGES
        val isDedicatedBrowser = packageName in DEDICATED_BROWSER_PACKAGES

        // INTERCEPT ON CLICK: Click events bypass debounce and check for APK download/install clicks across all apps
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            handleViewClickedForApkDownload(event, packageName)
            return
        }

        // Debounce check (200ms) only for high-frequency content/state change events
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEventTimestamp < DEBOUNCE_INTERVAL_MS) {
            return
        }
        lastEventTimestamp = currentTime

        // Restrict structural scam checks to window state and content changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            previouslyVisibleMessageHashes.clear()
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            // LAYER 1: Pre-Install Shield for Malicious APKs
            val isPackageInstaller = packageName.contains("packageinstaller") &&
                                     packageName != "android" &&
                                     !packageName.contains("myfiles")

            if (isPackageInstaller) {
                Log.d("MessageReader", "PackageInstaller detected: $packageName, checking install screen...")
                val rootNode = rootInActiveWindow ?: return
                val isInstalling = isInstallScreen(rootNode)
                if (isInstalling) {
                    Log.d("MessageReader", "PackageInstaller installation screen active! Checking overlay state: isOverlayShowing=${OverlayUtils.isOverlayShowing}, isAlertActive=${ScamAlertActivity.isAlertActive}")
                    if (currentTime < allowNextInstallUntilTime) {
                        Log.d("MessageReader", "Install whitelisted until $allowNextInstallUntilTime, skipping")
                        return
                    }

                    if (currentTime - lastPreInstallWarningTime > 2000L) {
                        lastPreInstallWarningTime = currentTime
                        
                        // Enforce strictly read-only behavior: Zero gesture/action injection
                        val alert = MessageEntity(
                            sender = "System Alert",
                            content = "Pre-Install Shield triggered: Prevented user from installing an unverified APK outside of Google Play.",
                            appName = "EdgeGuard",
                            analysisResult = "Alert",
                            scamProbability = 1.0f
                        )
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
                        }
                        
                        Handler(Looper.getMainLooper()).post {
                            val isSevere = ContactUtils.isUserOnCall(this@MessageReaderAccessibilityService)
                            OverlayUtils.showPreInstallWarningOverlay(this, isSevere)
                        }
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
                        appName = "EdgeGuard",
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
            val isWhatsAppOrTelegram = packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b" || packageName == "org.telegram.messenger"
            
            val currentTime = System.currentTimeMillis()
            
            if (isBrowser) {
                lastBrowserJumpTime = currentTime
            }

            if (packageName != lastPackageName && packageName != "com.android.systemui") {
                previousPackageName = lastPackageName
                lastPackageName = packageName
                lastPackageTime = currentTime
                previouslyVisibleMessageHashes.clear()
            }

            // 1. The WhatsApp/Telegram "Join" Shield
            val wasBrowserPrevious = previousPackageName in BROWSER_PACKAGES

            if (isWhatsAppOrTelegram) {
                val isDirectJump = wasBrowserPrevious && (currentTime - lastPackageTime < 15000L)

                if (isDirectJump) {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        val alreadyInChat = isChatScreen(rootNode, packageName)
                        if (!alreadyInChat) {
                            val isJoinScreen = checkForJoinGroupUI(rootNode)
                            if (isJoinScreen) {
                                Log.d("FunnelInterceptor", "Join Shield Triggered! Preventing user from joining Telegram/WhatsApp group from browser jump.")
                                triggerScamFunnelOverlay()
                                lastBrowserJumpTime = 0L // Reset
                                lastPackageTime = 0L
                            }
                        } else {
                            // Immediately trigger the DM alert if they jumped directly from an ad/browser to an unsaved DM
                            val chatTitle = extractBestEffortChatTitle(rootNode, packageName)
                            if (chatTitle.isNotEmpty()) {
                                // ONLY trigger if it's an unknown number (not in contacts AND looks like a raw phone number with 7+ digits)
                                val isUnsavedPhoneNumber = ContactUtils.isRawPhoneNumber(chatTitle) && 
                                                           !ContactUtils.isContactExists(this@MessageReaderAccessibilityService, chatTitle) && 
                                                           !ContactUtils.isWhatsAppCompanyAccount(chatTitle)
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

            // 2. The Link Interceptor & Betting Scanner & APK Download Dialog Scanner (Browser & Downloader scanning)
            if (isBrowser || packageName == "android" || packageName.contains("download") || packageName.contains("browser") || packageName.contains("packageinstaller")) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    checkForBrowserApkDownloadDialog(rootNode, packageName)
                    if (isBrowser && checkForScamFunnel(rootNode)) {
                        Log.d("FunnelInterceptor", "Scam Funnel Detected on Screen! Blocking.")
                        triggerScamFunnelOverlay()
                    }
                    if (isDedicatedBrowser) {
                        checkBrowserFraudWebsite(rootNode)
                    }
                }

                // 3. The Browser Search Intent (Pre-Emptive Warning)
                if (isDedicatedBrowser) {
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
                                    appName = "EdgeGuard",
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
                                    appName = "EdgeGuard",
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
                                    appName = "EdgeGuard",
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
                                    appName = "EdgeGuard",
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
                                    appName = "EdgeGuard",
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

            // --- LAYER 5: Storage & File Manager APK Interceptor (File Lists, Downloads & Storage Explorers) ---
            if (isFileManagerOrDownloadApp(packageName)) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    scanVisibleApkFilesInFileManager(rootNode, packageName)
                }
            }
        }

        // Never analyze EdgeGuard itself
        if (packageName == this.packageName) {
            return
        }

        // Never analyze system settings or preference apps
        if (packageName.contains("settings") || packageName.contains("preference")) {
            cachedPackageName = packageName
            cachedChatTitle = ""
            cachedIsActualChat = false
            return
        }

        // Filter for messaging apps
        val isWhatsApp = packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b"
        val isTelegram = packageName == "org.telegram.messenger"
        
        // Universally detect the default SMS app for ANY phone model
        val defaultSmsApp = android.provider.Telephony.Sms.getDefaultSmsPackage(this)
        val isSms = packageName == defaultSmsApp || packageName.contains("messaging") || packageName.contains("sms") || packageName.contains("mms")

        if (isWhatsApp || isTelegram || isSms) {
            val rootNode = rootInActiveWindow ?: return

            // 1. HARD BLOCK ON SETTINGS: If user opened Settings within messaging app, abort immediately!
            if (isSettingsScreen(rootNode, packageName)) {
                Log.d("MessageReader", "Settings screen detected in $packageName. Skipping message analysis.")
                cachedChatTitle = ""
                cachedIsActualChat = false
                previouslyVisibleMessageHashes.clear()
                return
            }

            // 2. FRESH CHECK: Is this ACTUALLY a chat conversation screen right now?
            val isActualChat = isChatScreen(rootNode, packageName)
            cachedIsActualChat = isActualChat
            if (!isActualChat) {
                cachedChatTitle = ""
                previouslyVisibleMessageHashes.clear()
                return
            }

            // 3. EXTRACT CHAT TITLE FROM HEADER:
            val currentChatTitle = extractBestEffortChatTitle(rootNode, packageName)
            if (currentChatTitle.isEmpty() || NON_CHAT_TITLES.contains(currentChatTitle.lowercase().trim())) {
                cachedChatTitle = ""
                previouslyVisibleMessageHashes.clear()
                return
            }
            if (currentChatTitle != cachedChatTitle) {
                previouslyVisibleMessageHashes.clear()
            }
            cachedChatTitle = currentChatTitle

            // 4. CHECK FOR PROFILE / CONTACT INFO SCREEN:
            if (isProfileScreen(rootNode)) {
                return
            }

            // 5. GROUP AND VERIFIED ACCOUNT CHECKS:
            if (ContactUtils.isTrustedSender(this, currentChatTitle)) {
                Log.d("MessageReader", "Skipping trusted sender: '$currentChatTitle'")
                return
            }

            val isKnownVerified = knownVerifiedAccounts.contains(currentChatTitle)
            val isVerified = isKnownVerified || (isWhatsApp && isVerifiedWhatsAppAccount(rootNode, currentChatTitle))
            if (isVerified) {
                knownVerifiedAccounts.add(currentChatTitle)
                Log.d("MessageReader", "Skipping verified WhatsApp account: '$currentChatTitle'")
                return
            }

            val isKnownGroup = knownGroupTitles.contains(currentChatTitle)
            val isGroup = isKnownGroup || isGroupChat(rootNode, packageName, currentChatTitle)
            if (isGroup) {
                knownGroupTitles.add(currentChatTitle)
            }

            val isAlphabetSender = (isWhatsApp || isSms) && 
                (ContactUtils.isAlphabetOrAlphanumeric(currentChatTitle) || ContactUtils.isContactExists(this, currentChatTitle) || !ContactUtils.isRawPhoneNumber(currentChatTitle))

            // 6. STRICTLY SCAN CHAT MESSAGES ONLY (ONLY THE CHAT PART):
            // Toolbars, popup menus (3 dots), compose input bars, and settings are NEVER scanned!
            val messageList = findMessageListContainer(rootNode, packageName)
            if (messageList != null) {
                if (isAlphabetSender || isGroup) {
                    // Alphabet senders or groups: ONLY scan for malicious .apk files, never regular chat text!
                    scanContainerForApksOnly(messageList, packageName, currentChatTitle)
                } else {
                    scanContainerForMessages(messageList, packageName, currentChatTitle)
                }
                if (messageList != rootNode) {
                    messageList.recycle()
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

    private fun isSettingsScreen(rootNode: AccessibilityNodeInfo, packageName: String): Boolean {
        if (packageName.contains("settings") || packageName.contains("preferences")) {
            return true
        }

        val titleNodes = mutableListOf<AccessibilityNodeInfo>()
        val titleIds = listOf(
            "$packageName:id/action_bar_title",
            "$packageName:id/title",
            "$packageName:id/toolbar",
            "android:id/title",
            "android:id/action_bar_title"
        )
        for (id in titleIds) {
            val found = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (!found.isNullOrEmpty()) {
                titleNodes.addAll(found)
            }
        }

        for (node in titleNodes) {
            val text = node.text?.toString()?.lowercase()?.trim() ?: ""
            if (text == "settings" || text == "messages settings" || text == "chat settings" ||
                text == "app settings" || text == "paramètres" || text == "ajustes" || text == "einstellungen") {
                titleNodes.forEach { it.recycle() }
                return true
            }
        }
        titleNodes.forEach { it.recycle() }

        val hasEntry = hasViewId(rootNode, "$packageName:id/entry") || 
                       hasViewId(rootNode, "com.whatsapp:id/entry") || 
                       hasViewId(rootNode, "com.whatsapp.w4b:id/entry") ||
                       hasViewId(rootNode, "org.telegram.messenger:id/chat_text_edit")

        if (!hasEntry) {
            var settingsKeywordCount = 0
            val checkTexts = mutableListOf<String>()
            collectTexts(rootNode, checkTexts, maxDepth = 6)
            for (t in checkTexts) {
                val lower = t.lowercase().trim()
                if (lower == "account" || lower == "privacy" || lower == "chats" || 
                    lower == "notifications" || lower == "storage and data" || lower == "app language" || 
                    lower == "help" || lower == "invite a friend" || lower == "avatar" || lower == "settings") {
                    settingsKeywordCount++
                    if (settingsKeywordCount >= 2) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun hasViewId(rootNode: AccessibilityNodeInfo, id: String): Boolean {
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
        val has = !nodes.isNullOrEmpty()
        nodes?.forEach { it.recycle() }
        return has
    }

    private fun isChatScreen(node: AccessibilityNodeInfo, packageName: String = ""): Boolean {
        if (packageName.contains("whatsapp")) {
            val hasEntry = hasViewId(node, "$packageName:id/entry") || 
                           hasViewId(node, "com.whatsapp:id/entry") || 
                           hasViewId(node, "com.whatsapp.w4b:id/entry")

            val hasContact = hasViewId(node, "$packageName:id/conversation_contact_name") || 
                             hasViewId(node, "com.whatsapp:id/conversation_contact_name") || 
                             hasViewId(node, "com.whatsapp.w4b:id/conversation_contact_name")

            // In WhatsApp, BOTH the message entry box and conversation contact header MUST exist
            return hasEntry && hasContact
        } else if (packageName.contains("telegram")) {
            val hasEdit = hasViewId(node, "org.telegram.messenger:id/chat_text_edit") || 
                          hasViewId(node, "org.telegram.messenger:id/chat_write_box")
            return hasEdit
        } else {
            val screenHeight = resources.displayMetrics.heightPixels
            val hasCompose = hasBottomComposeBox(node, screenHeight)
            if (!hasCompose) return false

            val candidateTitle = extractBestEffortChatTitle(node, packageName)
            return candidateTitle.isNotEmpty() && !NON_CHAT_TITLES.contains(candidateTitle.lowercase().trim())
        }
    }

    private fun hasBottomComposeBox(node: AccessibilityNodeInfo, screenHeight: Int = resources.displayMetrics.heightPixels): Boolean {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (node.isEditable && bounds.top > screenHeight * 0.45f) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (hasBottomComposeBox(child, screenHeight)) {
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

    private fun inspectNodeContext(
        node: AccessibilityNodeInfo,
        screenWidth: Int,
        effectiveRight: Int = 0,
        effectiveLeft: Int = 0,
        isWhatsApp: Boolean = false
    ): NodeScanContext {
        val nodeBounds = android.graphics.Rect()
        node.getBoundsInScreen(nodeBounds)

        var isQuote = false
        var isSent = false
        var isGroup = false

        val rightRef = if (effectiveRight > 0) effectiveRight else screenWidth
        val leftRef = effectiveLeft
        val widthRef = if (screenWidth > 0) screenWidth else (rightRef - leftRef)

        // 1. Text node geometry check (right-aligned check)
        if (widthRef > 0 && nodeBounds.width() > 0) {
            val distLeft = nodeBounds.left - leftRef
            val distRight = rightRef - nodeBounds.right
            if (distRight < distLeft - (widthRef * 0.05f) && distRight <= (widthRef * 0.35f)) {
                isSent = true
            }
        }

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
            if (contentDesc.startsWith("you:") || contentDesc.contains("you: ") ||
                contentDesc.contains(", read") || contentDesc.contains(", delivered") ||
                contentDesc.contains(", sent") || contentDesc.contains(", pending")) {
                isSent = true
            }

            // 2. Check bubble container bounds (count 1..3 represents the message bubble/card)
            if (!isSent && count in 1..3 && widthRef > 0) {
                val pBounds = android.graphics.Rect()
                current.getBoundsInScreen(pBounds)
                if (pBounds.width() > 0 && pBounds.width() < widthRef) {
                    val pDistLeft = pBounds.left - leftRef
                    val pDistRight = rightRef - pBounds.right
                    // Sent bubble is snug against right margin and indented from left margin
                    if (pDistRight < pDistLeft && pDistRight <= (widthRef * 0.22f) && pDistLeft >= (widthRef * 0.12f)) {
                        isSent = true
                    }
                }
            }

            // 3. Inspect siblings inside the bubble/row container for status ticks or sender info
            if (count in 1..3) {
                val childCount = current.childCount
                for (c in 0 until childCount) {
                    val child = current.getChild(c) ?: continue
                    val cId = child.viewIdResourceName?.lowercase() ?: ""
                    val cDesc = child.contentDescription?.toString()?.lowercase() ?: ""

                    if (isWhatsApp && !isGroup && cId.endsWith("name_in_message_color")) {
                        isGroup = true
                    }

                    // Delivery status icon: tick marks (only outgoing/sent messages have status icons in WhatsApp, Telegram, SMS!)
                    if (cId.endsWith("status") || cId.endsWith("msg_status") || cId.endsWith("status_icon") || 
                        cId.endsWith("status_indicator") || cId.contains("message_status")) {
                        isSent = true
                    }
                    if (cDesc == "sent" || cDesc == "delivered" || cDesc == "read" || cDesc == "pending" || cDesc == "played" ||
                        cDesc.contains("delivered to") || cDesc.contains("read by") || cDesc.contains("sent to")) {
                        isSent = true
                    }

                    child.recycle()
                    if (isSent && (isGroup || !isWhatsApp)) break
                }
            }

            val className = current.className?.toString() ?: ""
            if (className.contains("RecyclerView") || className.contains("ListView") || className.contains("ScrollView")) {
                break
            }

            val parent = current.parent
            if (current != node) {
                current.recycle()
            }
            current = parent
            count++
        }

        if (current != null && current != node) {
            current.recycle()
        }

        return NodeScanContext(isInsideQuote = isQuote, isSent = isSent, isInsideGroup = isGroup)
    }

    private fun findMessageListContainer(rootNode: AccessibilityNodeInfo, packageName: String): AccessibilityNodeInfo? {
        if (packageName.contains("whatsapp")) {
            val listIds = listOf(
                "android:id/list",
                "$packageName:id/conversation_layout",
                "com.whatsapp:id/conversation_layout",
                "com.whatsapp.w4b:id/conversation_layout"
            )
            for (id in listIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    val container = nodes[0]
                    for (i in 1 until nodes.size) nodes[i].recycle()
                    return container
                }
            }
            return findScrollableList(rootNode)
        } else if (packageName.contains("telegram")) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId("org.telegram.messenger:id/chat_list")
            if (!nodes.isNullOrEmpty()) {
                val container = nodes[0]
                for (i in 1 until nodes.size) nodes[i].recycle()
                return container
            }
            return findScrollableList(rootNode)
        } else {
            val smsListIds = listOf(
                "$packageName:id/conversation_list",
                "$packageName:id/message_list",
                "$packageName:id/message_recycler_view",
                "android:id/list"
            )
            for (id in smsListIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    val container = nodes[0]
                    for (i in 1 until nodes.size) nodes[i].recycle()
                    return container
                }
            }
            return findScrollableList(rootNode)
        }
    }

    private fun findScrollableList(node: AccessibilityNodeInfo, depth: Int = 0): AccessibilityNodeInfo? {
        if (depth > 8) return null
        val className = node.className?.toString() ?: ""
        if (className.contains("ListView") || className.contains("RecyclerView")) {
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            if (!viewId.contains("menu") && !viewId.contains("popup") && !viewId.contains("setting") && !viewId.contains("toolbar")) {
                return node
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableList(child, depth + 1)
            if (found != null) {
                if (found != child) child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun scanContainerForMessages(container: AccessibilityNodeInfo, packageName: String, chatTitle: String) {
        val containerBounds = android.graphics.Rect()
        container.getBoundsInScreen(containerBounds)
        val screenWidth = resources.displayMetrics.widthPixels
        val effectiveRight = if (containerBounds.right > 0) containerBounds.right else screenWidth
        val effectiveLeft = if (containerBounds.left >= 0) containerBounds.left else 0
        val isWhatsApp = packageName.contains("whatsapp")
        val currentScanHashes = HashSet<Int>()
        extractTextFromMessageContainer(
            container, 
            packageName, 
            chatTitle, 
            screenWidth, 
            effectiveRight, 
            effectiveLeft, 
            isWhatsApp, 
            depth = 0, 
            currentScanHashes = currentScanHashes
        )

        // Update previously visible hashes:
        // Messages visible right now are retained;
        // if a message leaves the visible screen and is brought back later, it will trigger isNewlyAppeared = true!
        previouslyVisibleMessageHashes.clear()
        previouslyVisibleMessageHashes.addAll(currentScanHashes)
    }

    private fun extractTextFromMessageContainer(
        node: AccessibilityNodeInfo,
        packageName: String,
        chatTitle: String,
        screenWidth: Int,
        effectiveRight: Int,
        effectiveLeft: Int,
        isWhatsApp: Boolean,
        depth: Int,
        currentScanHashes: MutableSet<Int>
    ) {
        if (depth > 12) return
        if (ContactUtils.isTrustedSender(this, chatTitle)) return

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        
        // Strictly exclude non-chat elements (toolbars, menus, popup options, compose inputs, settings)
        if (viewId.contains("toolbar") || viewId.contains("action_bar") || 
            viewId.contains("menu") || viewId.contains("popup") || 
            viewId.contains("entry") || viewId.contains("compose") || 
            viewId.contains("setting") || viewId.contains("header")) {
            return
        }

        if (node.text != null && node.text.isNotBlank()) {
            val text = node.text.toString()
            val cleanText = text.trim().replace(BIDI_REGEX, "")
            val lowerText = cleanText.lowercase(Locale.ROOT)

            val isTimeOrDate = cleanText.matches(TIME_REGEX_1) ||
                               cleanText.matches(TIME_REGEX_2) ||
                               cleanText.matches(TIME_REGEX_3) ||
                               cleanText.matches(TIME_REGEX_4) ||
                               cleanText.matches(TIME_REGEX_5)

            val isEditable = node.isEditable

            if (!isEditable && !isTimeOrDate && cleanText.isNotEmpty() && 
                !IGNORED_EXACT_WORDS.contains(lowerText.trim()) &&
                !JUNK_UI_TEXTS.contains(lowerText.trim()) &&
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

                val isApk = lowerText.contains(".apk")

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

                if (!isDocMetadata || isApk) {
                    val isFakeCredit = (isWhatsApp || packageName.contains("telegram")) &&
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
                    val nodeCenter = bounds.centerX()
                    val screenCenter = screenWidth / 2
                    val isCenterAligned = Math.abs(nodeCenter - screenCenter) < (screenWidth * 0.05f) && bounds.width() < (screenWidth * 0.5f)

                    val isUrlFraud = FraudDetectionEngine.findValidUrlInText(text)?.let {
                        FraudDetectionEngine.analyze(it, text).score >= 50
                    } == true

                    if ((!isKnownBadId && !isCenterAligned) || isApk || isUrlFraud) {
                        val contextInfo = inspectNodeContext(node, screenWidth, effectiveRight, effectiveLeft, isWhatsApp)

                        // STRICT PRIVACY RULE: Messages sent by the user are NEVER scanned as incoming scam threats.
                        // Only incoming messages (!contextInfo.isSent) are evaluated.
                        if (!contextInfo.isInsideQuote && !contextInfo.isSent && (!contextInfo.isInsideGroup || isApk || isUrlFraud)) {
                            if (chatTitle.isEmpty() || text.trim() != chatTitle.trim()) {
                                val trimmedContent = text.trim()
                                val contentHash = (packageName + ":" + chatTitle + ":" + trimmedContent).hashCode()
                                if (currentScanHashes.add(contentHash)) {
                                    val isNewlyAppeared = !previouslyVisibleMessageHashes.contains(contentHash)
                                    val finalSender = if (chatTitle.isNotEmpty()) chatTitle else "Screen Reader"
                                    val message = MessageEntity(
                                        sender = finalSender,
                                        content = trimmedContent,
                                        appName = packageName
                                    )
                                    ScamAnalyzer.getInstance(this).analyzeMessage(
                                        message = message,
                                        isApk = isApk,
                                        isFakeCredit = if (contextInfo.isInsideGroup) false else isFakeCredit,
                                        forceRealert = isNewlyAppeared
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        for (i in node.childCount - 1 downTo 0) {
            val child = node.getChild(i) ?: continue
            extractTextFromMessageContainer(child, packageName, chatTitle, screenWidth, effectiveRight, effectiveLeft, isWhatsApp, depth + 1, currentScanHashes)
            child.recycle()
        }
    }

    private fun scanContainerForApksOnly(container: AccessibilityNodeInfo, packageName: String, chatTitle: String, depth: Int = 0) {
        if (depth > 12) return
        val rawText = container.text?.toString() ?: container.contentDescription?.toString()
        if (!rawText.isNullOrBlank()) {
            val lowerText = rawText.lowercase(Locale.ROOT)
            if (lowerText.contains(".apk") && !container.isEditable) {
                val finalSender = if (chatTitle.isNotEmpty()) chatTitle else "Group APK"
                val message = MessageEntity(
                    sender = finalSender,
                    content = rawText.trim(),
                    appName = packageName
                )
                ScamAnalyzer.getInstance(this).analyzeMessage(message, isApk = true, isFakeCredit = false, forceRealert = true)
            }
        }
        for (i in 0 until container.childCount) {
            val child = container.getChild(i) ?: continue
            scanContainerForApksOnly(child, packageName, chatTitle, depth + 1)
            child.recycle()
        }
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo, packageName: String, chatTitle: String) {
        val container = findMessageListContainer(node, packageName) ?: node
        scanContainerForMessages(container, packageName, chatTitle)
        if (container != node) container.recycle()
    }

    private fun scanForApksOnly(node: AccessibilityNodeInfo, packageName: String, chatTitle: String) {
        val container = findMessageListContainer(node, packageName) ?: node
        scanContainerForApksOnly(container, packageName, chatTitle)
        if (container != node) container.recycle()
    }

    private fun isFileManagerOrDownloadApp(packageName: String): Boolean {
        val lower = packageName.lowercase(Locale.ROOT)
        return lower == "com.sec.android.app.myfiles" ||
               lower == "com.google.android.apps.nbu.files" ||
               lower == "com.android.documentsui" ||
               lower == "com.google.android.documentsui" ||
               lower == "com.android.providers.downloads.ui" ||
               lower.contains("myfiles") ||
               lower.contains("filemanager") ||
               lower.contains("fileexplorer") ||
               lower.contains("documentsui") ||
               lower.contains("filestore") ||
               lower.contains("download") ||
               lower.contains("solidexplorer") ||
               lower.contains("totalcommander") ||
               lower.contains("cxinventor") ||
               lower.contains("hidisk") ||
               lower.contains("estrongs")
    }

    private fun extractApkFileName(text: String): String? {
        val clean = text.trim()
        val lower = clean.lowercase(Locale.ROOT)

        // Ignore category headers, filter chips, search prompts, and storage sections
        if (lower == "apk" || lower == "apks" || lower == ".apk" ||
            lower.contains("installation file") ||
            lower.contains("install file") ||
            lower.contains("categories") ||
            lower.contains("category") ||
            lower.contains("search") ||
            lower.contains("filter") ||
            lower.contains("sort by") ||
            lower.contains("internal storage") ||
            lower.contains("sd card") ||
            lower.contains("network storage") ||
            lower.contains("recycle bin") ||
            lower.contains("trash")) {
            return null
        }

        // Exclude document types that aren't APKs
        if (isNonApkDocument(clean)) return null

        // Whitelist EdgeGuard itself so developer/user updates don't trigger false self-alerts
        if (lower.contains("edgeguard") || lower.contains("safetexts")) {
            return null
        }

        val match = Regex("""(?i)\b([a-zA-Z0-9_\-\.\+]+\.apk)\b""").find(clean)
        val candidate = match?.value ?: return null
        val candLower = candidate.lowercase(Locale.ROOT)
        if (candLower == ".apk" || candLower == "apk" || (candLower == "the.apk" && lower.contains("install the.apk"))) {
            return null
        }
        return candidate
    }

    private fun collectApkFileElements(
        node: AccessibilityNodeInfo,
        apkFiles: MutableList<String>,
        screenWidth: Int,
        screenHeight: Int,
        depth: Int = 0
    ) {
        if (depth > 25) return

        if (!node.isEditable) {
            val rawText = node.text?.toString() ?: node.contentDescription?.toString()
            if (!rawText.isNullOrBlank()) {
                val apkName = extractApkFileName(rawText)
                if (apkName != null) {
                    val bounds = android.graphics.Rect()
                    node.getBoundsInScreen(bounds)
                    // Ensure node is rendered inside the visible screen bounds
                    if (bounds.width() > 0 && bounds.height() > 0 &&
                        bounds.bottom > 0 && bounds.top < screenHeight &&
                        bounds.right > 0 && bounds.left < screenWidth) {
                        apkFiles.add(apkName)
                    }
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectApkFileElements(child, apkFiles, screenWidth, screenHeight, depth + 1)
            child.recycle()
        }
    }

    private fun scanVisibleApkFilesInFileManager(rootNode: AccessibilityNodeInfo, packageName: String) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val foundApks = mutableListOf<String>()
        collectApkFileElements(rootNode, foundApks, screenWidth, screenHeight)

        if (foundApks.isEmpty()) {
            return
        }

        val appDisplayName = when {
            packageName.contains("myfiles") -> "My Files"
            packageName.contains("nbu.files") -> "Files by Google"
            packageName.contains("documentsui") -> "Files"
            packageName.contains("download") -> "Downloads"
            else -> "Storage Shield"
        }

        val currentScanHashes = mutableSetOf<Int>()

        for (apkName in foundApks.distinct()) {
            val contentHash = (packageName + ":FileManager:" + apkName).hashCode()
            currentScanHashes.add(contentHash)
            val isNewlyAppeared = !previouslyVisibleMessageHashes.contains(contentHash)

            val alert = MessageEntity(
                sender = appDisplayName,
                content = "Dangerous unverified APK detected in storage: $apkName",
                appName = packageName,
                analysisResult = "Alert",
                scamProbability = 1.0f
            )

            ScamAnalyzer.getInstance(this).analyzeMessage(
                message = alert,
                isApk = true,
                isFakeCredit = false,
                forceRealert = isNewlyAppeared
            )
        }

        previouslyVisibleMessageHashes.clear()
        previouslyVisibleMessageHashes.addAll(currentScanHashes)
    }

    override fun onInterrupt() {
        Log.d("MessageReader", "Accessibility Service Interrupted")
    }

    private fun collectAllScreenTexts(node: AccessibilityNodeInfo, texts: MutableList<String>, depth: Int = 0) {
        if (depth > 20) return
        val t = node.text?.toString()?.lowercase(Locale.ROOT)
        if (!t.isNullOrBlank()) texts.add(t)
        val d = node.contentDescription?.toString()?.lowercase(Locale.ROOT)
        if (!d.isNullOrBlank()) texts.add(d)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectAllScreenTexts(child, texts, depth + 1)
            child.recycle()
        }
    }

    private fun isInstallScreen(node: AccessibilityNodeInfo): Boolean {
        val texts = mutableListOf<String>()
        collectAllScreenTexts(node, texts)
        if (texts.isEmpty()) return false

        val combined = texts.joinToString(" ")

        // If ANY text mentions uninstall / delete / remove, it is DEFINITELY an uninstallation screen!
        val isUninstall = combined.contains("uninstall") ||
                          combined.contains("delete") ||
                          combined.contains("remove") ||
                          combined.contains("uninstallation")
        if (isUninstall) {
            Log.d("MessageReader", "Detected uninstallation screen, ignoring PackageInstaller: $combined")
            return false
        }

        // Positively verify that this screen is an installation or app update prompt
        val hasInstallSignature = combined.contains("install") ||
                                  combined.contains("update") ||
                                  combined.contains("staging") ||
                                  combined.contains("unknown sources") ||
                                  combined.contains("allow from this source") ||
                                  combined.contains("unknown apps")

        return hasInstallSignature
    }

    private fun triggerScamFunnelOverlay(isDM: Boolean = false) {
        val alert = MessageEntity(
            sender = "System Alert",
            content = if (isDM) "Scam Funnel Interceptor triggered: User was blocked from a fraudulent DM conversation." else "Scam Funnel Interceptor triggered: User was blocked from joining a fraudulent group or illegal betting site.",
            appName = "EdgeGuard",
            analysisResult = "Alert",
            scamProbability = 1.0f
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
        }
        OverlayUtils.showScamFunnelWarningOverlay(this, isDM)
    }

    private fun checkForJoinGroupUI(node: AccessibilityNodeInfo): Boolean {
        val texts = mutableListOf<String>()
        collectAllScreenTexts(node, texts)
        if (texts.isEmpty()) return false

        // Look strictly for positive group joining action buttons
        var hasJoinAction = false
        for (t in texts) {
            val clean = t.trim()
            if (clean == "join group" || clean == "join channel" || clean == "join chat" ||
                clean == "join now" || clean == "request to join group" || clean == "apply to join group") {
                hasJoinAction = true
                break
            }
        }

        if (!hasJoinAction) return false

        val combined = texts.joinToString(" ")
        // Group preview/invitation card signature (participants, invite link, etc.)
        val isInviteCard = combined.contains("invite link") ||
                           combined.contains("invited you to join") ||
                           combined.contains("invitation to join") ||
                           combined.contains("group invite") ||
                           combined.contains("participants") ||
                           combined.contains("members")

        return isInviteCard
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
            appName = "EdgeGuard",
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

    private fun handleViewClickedForApkDownload(event: AccessibilityEvent, packageName: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastApkDownloadWarningTime < 1000L) {
            return
        }

        val sourceNode = event.source ?: return
        try {
            val clickedTexts = mutableListOf<String>()
            collectTexts(sourceNode, clickedTexts, maxDepth = 4)

            for (t in event.text) {
                if (!t.isNullOrBlank()) clickedTexts.add(t.toString())
            }
            if (event.contentDescription != null) {
                clickedTexts.add(event.contentDescription.toString())
            }

            var isApkClick = false
            var apkFileName: String? = null
            val apkRegex = Regex("""(?i)\b[\w\-\.\+]+\.apk\b""")

            // 1. Direct match on .apk in clicked view
            for (t in clickedTexts) {
                val match = apkRegex.find(t)
                if (match != null) {
                    apkFileName = match.value
                    isApkClick = true
                    break
                }
            }

            // 2. Action buttons like "Download APK", "Install APK", "Get APK", "Open APK"
            if (!isApkClick) {
                for (t in clickedTexts) {
                    val lower = t.lowercase(Locale.ROOT)
                    if (lower.contains("apk") && (lower.contains("download") || lower.contains("install") || lower.contains("get") || lower.contains("save") || lower.contains("open"))) {
                        isApkClick = true
                        apkFileName = "Unverified APK"
                        break
                    }
                }
            }

            // 3. User clicked any action button ("download", "install", "get", "open", "save", "download anyway") on an APK page/dialog
            if (!isApkClick) {
                val combined = clickedTexts.joinToString(" ").lowercase(Locale.ROOT)
                val hasActionWord = combined.contains("download") || combined.contains("install") ||
                                    combined.contains("get") || combined.contains("save") ||
                                    combined.contains("open") || combined.contains("keep")
                if (hasActionWord) {
                    val root = rootInActiveWindow
                    if (root != null) {
                        val windowTexts = mutableListOf<String>()
                        collectTexts(root, windowTexts, maxDepth = 12)
                        val windowCombined = windowTexts.joinToString(" ")
                        val match = apkRegex.find(windowCombined)
                        if (match != null) {
                            apkFileName = match.value
                            isApkClick = true
                        } else if (windowCombined.contains(".apk", ignoreCase = true)) {
                            apkFileName = "Unverified APK"
                            isApkClick = true
                        }
                    }
                }
            }

            // 4. Check if the parent or ancestor container references .apk (e.g. card with file info + download button)
            if (!isApkClick) {
                var currentParent = sourceNode.parent
                var depth = 0
                while (currentParent != null && depth < 3) {
                    val parentTexts = mutableListOf<String>()
                    collectTexts(currentParent, parentTexts, maxDepth = 4)
                    for (pt in parentTexts) {
                        val match = apkRegex.find(pt)
                        if (match != null) {
                            apkFileName = match.value
                            isApkClick = true
                            break
                        }
                    }
                    if (isApkClick) break
                    currentParent = currentParent.parent
                    depth++
                }
            }

            if (isApkClick && apkFileName != null) {
                lastApkDownloadWarningTime = currentTime
                Log.d("MessageReader", "INTERCEPTED APK CLICK: $apkFileName in $packageName! Triggering alert.")
                triggerApkDownloadAlert(packageName, apkFileName)
            }
        } catch (e: Exception) {
            Log.e("MessageReader", "Error inspecting clicked APK download node", e)
        } finally {
            sourceNode.recycle()
        }
    }

    private fun checkForBrowserApkDownloadDialog(rootNode: AccessibilityNodeInfo, packageName: String): Boolean {
        if (OverlayUtils.isOverlayShowing || ScamAlertActivity.isAlertActive) {
            return false
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastApkDownloadWarningTime < 1000L) {
            return false
        }

        val texts = mutableListOf<String>()
        collectTexts(rootNode, texts, maxDepth = 10)
        if (texts.isEmpty()) return false

        val combinedText = texts.joinToString(" ").lowercase(Locale.ROOT)

        // Strictly check for genuine browser download confirmation prompts resulting from click
        val hasBrowserPromptSignature = 
            combinedText.contains(".apk") && (
                combinedText.contains("harmful") ||
                combinedText.contains("do you want to download") ||
                combinedText.contains("download again") ||
                combinedText.contains("download anyway") ||
                combinedText.contains("file may harm") ||
                combinedText.contains("keep anyway") ||
                (combinedText.contains("download") && (combinedText.contains("cancel") || combinedText.contains("details")))
            )

        if (!hasBrowserPromptSignature) {
            return false
        }

        val apkRegex = Regex("""(?i)\b[\w\-\.\+]+\.apk\b""")
        val apkMatch = apkRegex.find(combinedText)
        val apkFileName = apkMatch?.value ?: if (combinedText.contains(".apk")) "Unverified APK" else return false

        lastApkDownloadWarningTime = currentTime
        triggerApkDownloadAlert(packageName, apkFileName)
        return true
    }

    private fun triggerApkDownloadAlert(packageName: String, apkFileName: String) {
        val appDisplayName = when {
            packageName.contains("chrome") -> "Chrome"
            packageName.contains("sbrowser") -> "Samsung Internet"
            packageName.contains("firefox") -> "Firefox"
            packageName.contains("brave") -> "Brave"
            packageName.contains("opera") -> "Opera"
            packageName.contains("emmx") -> "Edge"
            packageName.contains("packageinstaller") -> "Package Installer"
            packageName.contains("download") -> "Download Manager"
            else -> "Browser"
        }

        val alert = MessageEntity(
            sender = appDisplayName,
            content = "Dangerous APK Download: $apkFileName",
            appName = packageName,
            analysisResult = "Alert",
            scamProbability = 1.0f
        )

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
        }

        Handler(Looper.getMainLooper()).post {
            ScamAnalyzer.getInstance(this@MessageReaderAccessibilityService).showScamWarningOverlay(alert, isApk = true)
        }
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, list: MutableList<String>, maxDepth: Int) {
        if (node == null || maxDepth <= 0 || list.size > 60) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) list.add(text)
        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrEmpty() && desc != text) list.add(desc)

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            collectTexts(child, list, maxDepth - 1)
            child.recycle()
        }
    }

    private var lastBrowserScanDomain: String = ""
    private var lastBrowserScanUrl: String = ""
    private var lastBrowserScanTime: Long = 0L
    private var lastAlertSavedDomain: String = ""
    private var lastAlertSavedTime: Long = 0L

    private fun checkBrowserFraudWebsite(rootNode: AccessibilityNodeInfo) {
        val urlText = extractBrowserUrl(rootNode)
        if (urlText.isNullOrBlank()) {
            return
        }

        val domain = FraudDetectionEngine.extractDomain(urlText)
        if (domain.isBlank()) {
            return
        }

        if (ContactUtils.isTrustedSender(this, domain)) {
            Log.d("BrowserFraud", "Skipping trusted website: $domain")
            return
        }

        val now = System.currentTimeMillis()
        if (domain == lastBrowserScanDomain && urlText == lastBrowserScanUrl && (now - lastBrowserScanTime) < 3000L) {
            return
        }

        val textList = mutableListOf<String>()
        collectTexts(rootNode, textList, maxDepth = 25)
        val renderedText = textList.joinToString(" ")

        lastBrowserScanDomain = domain
        lastBrowserScanUrl = urlText
        lastBrowserScanTime = now

        Log.d("BrowserFraud", "Scanning URL: $urlText (domain: $domain)")
        val result = FraudDetectionEngine.analyze(urlText, renderedText)
        Log.d("BrowserFraud", "Scan result for $domain: score=${result.score}, reasons=${result.reasons}")

        if (result.score >= 50) {
            val alertSender = when (result.fraudCategory) {
                "GOVERNMENT" -> "Fake Govt WebShield"
                "BANK" -> "Bank Phishing WebShield"
                else -> "EdgeGuard WebShield"
            }
            val alertHeader = when (result.fraudCategory) {
                "GOVERNMENT" -> "🚨 Fake Government Portal: $domain (Risk Score: ${result.score}/100)\n"
                "BANK" -> "🚨 Fake Bank / Phishing: $domain (Risk Score: ${result.score}/100)\n"
                else -> "🚨 Fake Website: $domain (Risk Score: ${result.score}/100)\n"
            }
            val alert = MessageEntity(
                sender = alertSender,
                content = alertHeader + result.reasons.joinToString("\n") { "• $it" },
                appName = "EdgeGuard",
                analysisResult = "Alert",
                scamProbability = (result.score / 100f).coerceIn(0.5f, 1.0f)
            )

            val nowAlert = System.currentTimeMillis()
            if (domain != lastAlertSavedDomain || (nowAlert - lastAlertSavedTime) > 30_000L) {
                lastAlertSavedDomain = domain
                lastAlertSavedTime = nowAlert
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        AppDatabase.getDatabase(this@MessageReaderAccessibilityService).messageDao().insertMessage(alert)
                        Log.d("BrowserFraud", "Saved fraud alert for $domain to database ($alertSender)")
                    } catch (e: Exception) {
                        Log.e("BrowserFraud", "Failed to save alert to database: ${e.message}", e)
                    }
                }
            }

            Handler(Looper.getMainLooper()).post {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    android.widget.Toast.makeText(
                        this,
                        "🚨 DANGEROUS SCAM WEBSITE (${result.score}/100)!\n$domain\n${result.reasons.firstOrNull() ?: ""}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    OverlayUtils.showScamCardOverlay(
                        context = this,
                        message = alert,
                        isFakeWebsite = true,
                        websiteDomain = domain,
                        fraudCategory = result.fraudCategory
                    )
                }
            }
        }
    }

    /**
     * Extracts active browser URL using known resource IDs and fallback hierarchical traversal.
     */
    private fun extractBrowserUrl(rootNode: AccessibilityNodeInfo): String? {
        val knownIds = listOf(
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/location_bar",
            "com.android.chrome:id/search_box_text",
            "com.brave.browser:id/url_bar",
            "com.brave.browser:id/location_bar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "org.mozilla.firefox:id/toolbar",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.sec.android.app.sbrowser:id/location_bar"
        )
        for (vid in knownIds) {
            val list = rootNode.findAccessibilityNodeInfosByViewId(vid)
            if (!list.isNullOrEmpty()) {
                for (node in list) {
                    val urlFromText = FraudDetectionEngine.findValidUrlInText(node.text?.toString())
                    if (urlFromText != null) {
                        list.forEach { @Suppress("DEPRECATION") it.recycle() }
                        return urlFromText
                    }
                    val urlFromDesc = FraudDetectionEngine.findValidUrlInText(node.contentDescription?.toString())
                    if (urlFromDesc != null) {
                        list.forEach { @Suppress("DEPRECATION") it.recycle() }
                        return urlFromDesc
                    }
                }
                list.forEach { @Suppress("DEPRECATION") it.recycle() }
            }
        }

        // Fallback 1: search nodes with url / location / toolbar in their ID
        val urlFromBar = findUrlByScanningTopBar(rootNode, depth = 0)
        if (urlFromBar != null) return urlFromBar

        // Fallback 2: if DNS failed or browser error page displays the domain (e.g. "Check if there is a typo in ..."), extract from screen
        val errorTexts = mutableListOf<String>()
        collectTexts(rootNode, errorTexts, maxDepth = 15)
        for (t in errorTexts) {
            val found = FraudDetectionEngine.findValidUrlInText(t)
            if (found != null) {
                val d = FraudDetectionEngine.extractDomain(found)
                if (d.isNotEmpty() && !d.contains("google.") && !d.contains("chrome.") && !d.contains("android.")) {
                    return found
                }
            }
        }
        return null
    }

    private fun findUrlByScanningTopBar(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > 15) return null
        val id = node.viewIdResourceName
        if (id != null && (id.contains("url") || id.contains("location") || id.contains("toolbar") || id.contains("address"))) {
            val urlFromText = FraudDetectionEngine.findValidUrlInText(node.text?.toString())
            if (urlFromText != null) return urlFromText
            val urlFromDesc = FraudDetectionEngine.findValidUrlInText(node.contentDescription?.toString())
            if (urlFromDesc != null) return urlFromDesc
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findUrlByScanningTopBar(child, depth + 1)
            @Suppress("DEPRECATION")
            child.recycle()
            if (found != null) return found
        }
        return null
    }
}
















