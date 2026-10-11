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

    companion object {
        var allowNextInstallUntilTime: Long = 0L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            // LAYER 1: Pre-Install Shield for Malicious APKs
            val isPackageInstaller = packageName == "com.google.android.packageinstaller" ||
                                     packageName == "com.samsung.android.packageinstaller" ||
                                     packageName == "com.miui.packageinstaller" ||
                                     packageName == "com.android.packageinstaller" ||
                                     packageName == "com.coloros.safecenter" ||
                                     packageName == "com.oplus.appmanager"

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

            // 2. The Link Interceptor & Betting Scanner (Browser scanning)
            if (isBrowser) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
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
            
            // Best-effort extraction of the chat title (sender name)
            var chatTitle = extractBestEffortChatTitle(rootNode, packageName)


            if (chatTitle.isNotEmpty() && isWhatsApp) {
                // Temporal Linking: Check if there was a Chrome Booking Intent in the last 30 minutes
                val prefs = getSharedPreferences("safetexts_prefs", android.content.Context.MODE_PRIVATE)
                val intentTime = prefs.getLong("booking_intent_timestamp", 0L)
                val currentTime = System.currentTimeMillis()
                
                if (intentTime > 0 && currentTime - intentTime < 30 * 60 * 1000) {
                    // Link this chat to the booking context!
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val dao = AppDatabase.getDatabase(this@MessageReaderAccessibilityService).chatContextDao()
                        dao.insertOrUpdate(ChatContextEntity(chatTitle, "BOOKING_SCAM_RISK", currentTime))
                    }
                    // Clear intent so we don't tag every chat for the next 30 mins
                    prefs.edit().remove("booking_intent_timestamp").apply()
                    Log.d("FunnelInterceptor", "Temporal Link: Chat '$chatTitle' tagged as BOOKING_SCAM_RISK")
                }
                
                // OR if the chat explicitly contains booking keywords, log it to RoomDB
                if (checkForBookingIntent(rootNode)) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val dao = AppDatabase.getDatabase(this@MessageReaderAccessibilityService).chatContextDao()
                        dao.insertOrUpdate(ChatContextEntity(chatTitle, "BOOKING_SCAM_RISK", currentTime))
                    }
                }
                

            }

            // ONLY scan the text if we are actually on a chat screen.
            // For WhatsApp and Telegram, we strictly require the chat title (contact name header) to be present 
            // to avoid mistakenly scanning the main chat list (which has a hidden search bar that tricks isChatScreen).
            val isStrictApp = packageName.contains("whatsapp") || packageName.contains("telegram")
            val shouldScanText = if (isStrictApp) {
                chatTitle.isNotEmpty() && !isGroupChat(rootNode, packageName, chatTitle)
            } else {
                (chatTitle.isNotEmpty() || isChatScreen(rootNode)) && !isGroupChat(rootNode, packageName, chatTitle)
            }
            
            // Skip official WhatsApp service account, alphanumeric SMS senders, and saved contacts as requested.
            if (chatTitle.isNotEmpty()) {
                val isWhatsApp = packageName.contains("whatsapp")
                val isSms = packageName.contains("messaging") || packageName.contains("sms") || packageName.contains("mms")
                
                val isOfficialWhatsApp = isWhatsApp && chatTitle.lowercase() == "whatsapp"
                val isVerifiedCompany = isWhatsApp && ContactUtils.isWhatsAppCompanyAccount(chatTitle)
                val isAlphanumericSms = isSms && ContactUtils.isAlphanumericSmsSender(chatTitle)
                val isSavedContact = ContactUtils.isContactExists(this, chatTitle)
                
                if (isOfficialWhatsApp || isVerifiedCompany || isAlphanumericSms || isSavedContact) {
                    return
                }
            }
            // Check if we are actually on a Profile/Info screen (Contact Info, Group Info)
            val isProfileScreen = isProfileScreen(rootNode)
            
            if (shouldScanText && !isProfileScreen) {
                extractTextFromNode(rootNode, packageName, chatTitle)
            }
        }
    }

    private fun isGroupChat(rootNode: AccessibilityNodeInfo, packageName: String, chatTitle: String): Boolean {
        if (packageName.contains("whatsapp")) {
            val subtitles = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_status")
            if (!subtitles.isNullOrEmpty()) {
                val text = subtitles[0].text?.toString()?.lowercase() ?: ""
                if (text.contains("group") || text.contains("you") || text.contains(",") || text.contains("announcement") || text.contains("community") || text.contains("channel") || text.contains("members") || text.contains("subscribers") || text.contains("only admin")) {
                    return true
                }
                        }
            // Check content description of the profile photo
            val photoNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_photo")
            if (!photoNodes.isNullOrEmpty()) {
                val contentDesc = photoNodes[0].contentDescription?.toString()?.lowercase() ?: ""
                if (contentDesc.contains("group")) {
                    return true
                }
            }
            // Another strong indicator: if we see participant names above messages in the chat
                        // Check for admin-only group indicator
            val adminNodes = rootNode.findAccessibilityNodeInfosByText("only admins can send")
            if (!adminNodes.isNullOrEmpty()) {
                return true
            }
            val nameNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/name_in_message_color")
            if (!nameNodes.isNullOrEmpty()) {
                return true
            }
        } else if (packageName.contains("telegram")) {
            val subtitles = rootNode.findAccessibilityNodeInfosByViewId("org.telegram.messenger:id/action_bar_subtitle")
            if (!subtitles.isNullOrEmpty()) {
                val text = subtitles[0].text?.toString()?.lowercase() ?: ""
                if (text.contains("members") || text.contains("subscribers")) {
                    return true
                }
            }
        } else if (packageName.contains("messaging") || packageName.contains("sms") || packageName.contains("mms")) {
            // In SMS apps, group chats typically list multiple names or numbers separated by commas in the title
            if (chatTitle.contains(",")) {
                return true
            }
        }
        return false
    }

    private fun isChatScreen(node: AccessibilityNodeInfo): Boolean {
        if (node.isEditable) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (isChatScreen(child)) {
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

    private fun extractTextFromNode(node: AccessibilityNodeInfo, packageName: String, chatTitle: String) {
        if (node.text != null && node.text.isNotBlank()) {
            val text = node.text.toString()
            
            // Basic filtering to avoid processing short UI labels
            val lowerText = text.lowercase()
            
            // WhatsApp often adds invisible bidirectional markers to dates and times. Strip them out.
            val cleanText = text.trim().replace(Regex("[\\u200E\\u200F\\u202A-\\u202E]"), "")
            
            // Check if the text is just a timestamp or date (e.g. "12:45 PM", "Yesterday", "Monday", "24 August 2023")
            val isTimeOrDate = cleanText.matches(Regex("^\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?$")) ||
                               cleanText.matches(Regex("^(Yesterday|Today|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)$", RegexOption.IGNORE_CASE)) ||
                               cleanText.matches(Regex("^\\d{1,2}/\\d{1,2}/\\d{2,4}$")) ||
                               cleanText.matches(Regex("^\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4}$")) || // e.g., "24 August 2023"
                               cleanText.matches(Regex("^[A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4}$")) // e.g., "August 24, 2023"
            
            // Skip editable text fields (this is where the user is currently typing a message)
            val isEditable = node.isEditable
            
            val ignoredExactWords = setOf(
                "message", "forwarded", "forwarded many times",
                "document", "camera", "gallery", "audio", "location", "contact", "poll",
                "recent", "images", "image", "photo", "photos", "video", "videos", "files",
                "gif", "stickers", "avatar", "payment", "catalogue", "reply", "copy", "delete",
                "share", "info", "report", "block", "cancel", "ok", "yes", "no", "done", "send",
                "attach", "attachment", "recent files", "all files", "downloads", "documents",
                "large files", "oldest", "newest", "sort by", "view", "edit", "select", "albums", "add"
            )
            
            if (!isEditable && !isTimeOrDate && cleanText.isNotEmpty() && 
                !ignoredExactWords.contains(lowerText.trim()) &&
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
                !lowerText.contains("groups including")) {
                
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                val screenWidth = resources.displayMetrics.widthPixels
                
                // --- ROBUST FILTERING ---
                val viewId = node.viewIdResourceName ?: ""
                
                // 1. Explicitly ignore known bad UI elements (Sender names in groups, timestamps, list previews)
                val isKnownBadId = viewId.endsWith("name_in_message_color") || 
                                   viewId.endsWith("date") ||
                                   viewId.endsWith("conversations_row_message") ||
                                   viewId.endsWith("conversation_contact_name")
                                   
                // 2. Center-Alignment Rejection for System Messages
                val nodeCenter = bounds.centerX()
                val screenCenter = screenWidth / 2
                // If the text is strictly centered (within 5% of the center) and spans less than 50% of screen, it's likely a system message (e.g. date header)
                val isCenterAligned = Math.abs(nodeCenter - screenCenter) < (screenWidth * 0.05f) && bounds.width() < (screenWidth * 0.5f)

                // 3. Known Message IDs
                val isKnownMessageId = viewId.endsWith("message_text") || 
                                       viewId.endsWith("chat_message_text") || 
                                       viewId.endsWith("text_message")
                
                val isValidMessage = !isKnownBadId && !isCenterAligned && !isNodeInsideQuote(node) && (isKnownMessageId || isNodeInsideList(node))

                if (isValidMessage) {
                    // In WhatsApp, if a user replies with a short message to a very wide quoted message,
                    // the short text node itself will be left-aligned inside the wide right-aligned chat bubble.
                    // This means the text node's distanceToLeftEdge < distanceToRightEdge, falsely marking it as received!
                    // To fix this, we must check if the node OR ANY OF ITS PARENTS is a right-aligned chat bubble.
                    val isSent = isNodeInsideSentMessage(node, screenWidth, packageName.contains("whatsapp"))
                    val isReceivedMessage = !isSent
                
                    val isApk = lowerText.contains(".apk")
    
                    if (isReceivedMessage) {
                        if (chatTitle.isNotEmpty() && text.trim() == chatTitle.trim()) {
                            // Skip if the extracted text is exactly the sender's name (the top header bar)
                        } else {
                            val finalSender = if (chatTitle.isNotEmpty()) chatTitle else "Screen Reader"
                            val message = MessageEntity(
                                sender = finalSender,
                                content = text.trim(),
                                appName = packageName
                            )
                            
                            // The analyzeMessage method handles deduplication automatically
                            ScamAnalyzer.getInstance(this).analyzeMessage(message, isApk = isApk, isFakeCredit = false)
                        }
                    }
                }
            }
        }

        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                extractTextFromNode(childNode, packageName, chatTitle)
                childNode.recycle() // Important: recycle to avoid memory leaks
            }
        }
    }

    private fun isNodeInsideQuote(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var count = 0
        while (current != null && count < 30) {
            val viewId = current.viewIdResourceName?.lowercase() ?: ""
            if (viewId.contains("quote") || viewId.contains("reply")) {
                return true
            }
            current = current.parent
            count++
        }
        return false
    }

    private fun isNodeInsideList(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var count = 0
        while (current != null && count < 30) {
            val className = current.className?.toString() ?: ""
            if (className.contains("RecyclerView") || className.contains("ListView") || className.contains("ScrollView")) {
                return true
            }
            current = current.parent
            count++
        }
        return false
    }

                            private fun isNodeInsideSentMessage(node: AccessibilityNodeInfo, screenWidth: Int, isWhatsApp: Boolean): Boolean {
        var current: AccessibilityNodeInfo? = node
        var count = 0

        while (current != null && count < 20) {
            val contentDesc = current.contentDescription?.toString()?.lowercase() ?: ""
            if (contentDesc.startsWith("you:") || contentDesc.contains("you: ")) {
                return true
            }

            val className = current.className?.toString() ?: ""
            if (className.contains("RecyclerView") || className.contains("ListView") || className.contains("ScrollView")) {
                // Reached the list of all messages. Stop here so we don't accidentally find status icons of OTHER messages on the screen.
                break
            }
            
            if (isWhatsApp) {
                // For WhatsApp, we rely EXCLUSIVELY on the deterministic status icon
                var foundStatus = false
                val statusNodes1 = current.findAccessibilityNodeInfosByViewId("com.whatsapp:id/status")
                if (!statusNodes1.isNullOrEmpty()) {
                    foundStatus = true
                    statusNodes1.forEach { it.recycle() }
                }
                
                val statusNodes2 = current.findAccessibilityNodeInfosByViewId("com.whatsapp:id/message_status_indicator")
                if (!statusNodes2.isNullOrEmpty()) {
                    foundStatus = true
                    statusNodes2.forEach { it.recycle() }
                }
                
                if (foundStatus) return true
            } else {
                // For SMS and other apps, we use the geometry fallback check
                val bounds = android.graphics.Rect()
                current.getBoundsInScreen(bounds)
                
                if (bounds.width() > 0 && bounds.height() > 0) {
                    val distanceToLeftEdge = bounds.left
                    val distanceToRightEdge = screenWidth - bounds.right
                    
                    if (bounds.width() < screenWidth * 0.99f) {
                        if (distanceToRightEdge < distanceToLeftEdge - (screenWidth * 0.02f) && distanceToRightEdge <= (screenWidth * 0.25f)) {
                            return true
                        }
                    }
                }
            }
            current = current.parent
            count++
        }
        return false
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
        
        if (text.contains("install") || desc.contains("install") || 
            text.contains("update") || desc.contains("update")) {
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
            val nodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")
            if (!nodes.isNullOrEmpty()) {
                return nodes[0].text?.toString() ?: ""
            }
        }
        
        // Try common action bar title IDs for Telegram and SMS apps
        var titleNodes = rootNode.findAccessibilityNodeInfosByViewId("$packageName:id/action_bar_title")
        if (!titleNodes.isNullOrEmpty()) {
            return titleNodes[0].text?.toString() ?: ""
        }
        titleNodes = rootNode.findAccessibilityNodeInfosByViewId("$packageName:id/conversation_title")
        if (!titleNodes.isNullOrEmpty()) {
            return titleNodes[0].text?.toString() ?: ""
        }
        titleNodes = rootNode.findAccessibilityNodeInfosByViewId("$packageName:id/title")
        if (!titleNodes.isNullOrEmpty()) {
            return titleNodes[0].text?.toString() ?: ""
        }

        if (packageName.contains("telegram")) {
            return findFirstTextNode(rootNode)
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
}
















