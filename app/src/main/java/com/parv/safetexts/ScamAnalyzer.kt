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

    fun classifyText(text: String): Pair<String, Float> {
        return tfliteClassifier.classify(text)
    }

    companion object {
        // Flag to toggle between Tier-1 Deterministic Keywords + Tier-2 Model vs Pure Model-Only Mode
        const val ENABLE_MESSAGE_KEYWORDS = true

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

            // 1. Missed call alerts
            val isMissedCall = lower.contains("missed call") || 
                               lower.contains("missed a call") || 
                               lower.contains("u missed a call") || 
                               lower.contains("you missed a call") ||
                               lower.contains("call alert") ||
                               lower.contains("tried calling you") ||
                               lower.contains("called you while") ||
                               lower.contains("मिस्ड कॉल") ||
                               lower.contains("தவறிய அழைப்பு") ||
                               lower.contains("మిస్డ్ కాల్") ||
                               lower.contains("ಮಿಸ್ಡ್ ಕಾಲ್") ||
                               lower.contains("മിസ്ഡ് കോൾ")

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
                                  lower.contains("daily data limit") ||
                                  lower.contains("रिचार्ज") || lower.contains("वैधता") ||
                                  lower.contains("ரீசார்ஜ்") ||
                                  lower.contains("రీఛార్జ్") ||
                                  lower.contains("ರೀಚಾರ್ಜ್") ||
                                  lower.contains("റീചാർജ്")

            return isMissedCall || isCallAvailable || isCarrierUpdate
        }

        fun checkFastDeterministicScam(text: String): Pair<String, Float>? {
            val textWithoutEmojis = TFLiteScamClassifier.removeEmojis(text)
            val lower = textWithoutEmojis.lowercase()
            // 0. Whitelist legitimate telecom operator service notifications
            if (isTelecomServiceMessage(textWithoutEmojis)) {
                return Pair("Safe", 0.0f)
            }
            // 1. APK files
            if (lower.contains(".apk")) {
                return Pair("Alert", 1.0f)
            }

            // 2. Electricity bill disconnection scam
            // Multilingual: English, Hinglish, Hindi, Marathi, Tamil, Telugu, Kannada, Malayalam
            val isElecEntity = lower.contains("electricity") || lower.contains("bijli") || lower.contains("bijlee") || lower.contains("power") ||
                    lower.contains("बिजली") || lower.contains("विद्युत") ||
                    lower.contains("वीज") || lower.contains("महावितरण") ||
                    lower.contains("மின்சாரம்") || lower.contains("மின் கட்டணம்") || lower.contains("மின்கட்டணம்") ||
                    lower.contains("విద్యుత్") || lower.contains("కరెంట్") ||
                    lower.contains("ವಿದ್ಯುತ್") || lower.contains("ಕರೆಂಟ್") || lower.contains("ಬೆಸ್ಕಾಂ") ||
                    lower.contains("വൈദ്യുതി") || lower.contains("കറന്റ്") || lower.contains("കെഎസ്ഇബി")

            val isElecAction = lower.contains("disconnect") || lower.contains("cut") || lower.contains("unpaid") || lower.contains("tonight") || lower.contains("9:30") ||
                    lower.contains("kat di jayegi") || lower.contains("kaat di") || lower.contains("aaj raat") || lower.contains("turant bhare") ||
                    lower.contains("काट दी जाएगी") || lower.contains("बकाया") || lower.contains("डिस्कनेक्ट") || lower.contains("आज रात") ||
                    lower.contains("खंडित") || lower.contains("थकीत") || lower.contains("आज रात्री") || lower.contains("तोडला जाईल") ||
                    lower.contains("துண்டிக்கப்படும்") || lower.contains("நிலுவை") || lower.contains("இன்றிரவு") ||
                    lower.contains("నిలిపివేయబడుతుంది") || lower.contains("కట్ చేయబడుతుంది") || lower.contains("బకాయి") || lower.contains("ఈ రాత్రి") ||
                    lower.contains("ಕಡಿತಗೊಳಿಸಲಾಗುತ್ತದೆ") || lower.contains("ಸಂಪರ್ಕ ಕಡಿತ") || lower.contains("ಬಾಕಿ") || lower.contains("ಇಂದು ರಾತ್ರಿ") ||
                    lower.contains("വിച്ഛേദിക്കും") || lower.contains("കുടിശ്ശിക") || lower.contains("ഇന്ന് രാത്രി")

            if (isElecEntity && isElecAction) {
                return Pair("Alert", 0.98f)
            }

            // 3. Bank / PAN / KYC / Account Suspension Scam
            val isBankEntity = lower.contains("kyc") || lower.contains("pan card") || lower.contains("pancard") || lower.contains("aadhaar") || lower.contains("adhar") ||
                    lower.contains("account") || lower.contains("debit card") || lower.contains("sim card") || lower.contains("sim block") || lower.contains("yono") || lower.contains("sbi") ||
                    lower.contains("khata") || lower.contains("bank khata") ||
                    lower.contains("केवाईसी") || lower.contains("पैन कार्ड") || lower.contains("आधार कार्ड") || lower.contains("बैंक खाता") || lower.contains("खाता") || lower.contains("सिम कार्ड") ||
                    lower.contains("केवायसी") || lower.contains("पॅन कार्ड") || lower.contains("आधार") || lower.contains("बँक खाते") || lower.contains("खाते") ||
                    lower.contains("கேஒய்சி") || lower.contains("பான் கார்டு") || lower.contains("ஆதார்") || lower.contains("வங்கி கணக்கு") || lower.contains("கணக்கு") ||
                    lower.contains("కేవైసీ") || lower.contains("పాన్ కార్డ్") || lower.contains("ఆధార్") || lower.contains("బ్యాంకు ఖాతా") || lower.contains("ఖాతా") ||
                    lower.contains("ಕೆವೈಸಿ") || lower.contains("ಪ್ಯಾನ್ ಕಾರ್ಡ್") || lower.contains("ಆಧಾರ್") || lower.contains("ಬ್ಯಾಂಕ್ ಖಾತೆ") || lower.contains("ಖಾತೆ") ||
                    lower.contains("കെവൈസി") || lower.contains("പാൻ കാർഡ്") || lower.contains("ആധാർ") || lower.contains("ബാങ്ക് അക്കൗണ്ട്") || lower.contains("അക്കൗണ്ട്")

            val isBankAction = lower.contains("block") || lower.contains("suspend") || lower.contains("update immediately") || lower.contains("deactivate") || lower.contains("expire") || lower.contains("click here") || lower.contains("update your") ||
                    lower.contains("band ho jayega") || lower.contains("turant update") || lower.contains("rok diya gaya") ||
                    lower.contains("ब्लॉक") || lower.contains("बंद") || lower.contains("निलंबित") || lower.contains("तुरंत अपडेट") || lower.contains("निष्क्रिय") || lower.contains("अपडेट") ||
                    lower.contains("बंद होईल") || lower.contains("त्वरित अपडेट") || lower.contains("अद्यतनित") ||
                    lower.contains("முடக்க") || lower.contains("இடைநிறுத்த") || lower.contains("புதுப்பிக்க") || lower.contains("செயலிழக்க") ||
                    lower.contains("బ్లాక్") || lower.contains("నిలిపివేయ") || lower.contains("అప్‌డేట్") || lower.contains("నవీకరించ") ||
                    lower.contains("ನಿರ್ಬಂಧಿಸ") || lower.contains("ಅಮಾನತು") || lower.contains("ತಕ್ಷಣ ನವೀಕರಿಸಿ") || lower.contains("ನವೀಕರಿಸ") || lower.contains("ಬ್ಲಾಕ್") ||
                    lower.contains("ബ്ലോക്ക്") || lower.contains("നിർത്തിവ") || lower.contains("ഉടൻ അപ്ഡേറ്റ്") || lower.contains("പുതുക്കുക")

            if (isBankEntity && isBankAction) {
                return Pair("Alert", 0.97f)
            }

            // 4. Fake Lottery / KBC / Prize
            val isLotteryEntity = lower.contains("kbc") || lower.contains("lottery") || lower.contains("congratulations") || lower.contains("prize") || lower.contains("winner") || lower.contains("lucky draw") ||
                    lower.contains("inam") || lower.contains("inaam") || lower.contains("badhai ho") ||
                    lower.contains("लॉटरी") || lower.contains("बधाई") || lower.contains("इनाम") || lower.contains("विजेता") || lower.contains("लकी ड्रॉ") ||
                    lower.contains("अभिनंदन") || lower.contains("बक्षीस") || lower.contains("जिंकले") ||
                    lower.contains("லாட்டரி") || lower.contains("வாழ்த்துகள்") || lower.contains("பரிசு") || lower.contains("வெற்றியாளர்") ||
                    lower.contains("లాటరీ") || lower.contains("అభినందనలు") || lower.contains("బహుమతి") || lower.contains("విజేత") ||
                    lower.contains("ಲಾಟರಿ") || lower.contains("ಅಭಿನಂದನೆಗಳು") || lower.contains("ಬಹುಮಾನ") || lower.contains("ವಿಜೇತರು") ||
                    lower.contains("ലോട്ടറി") || lower.contains("അഭിനന്ദനങ്ങൾ") || lower.contains("സമ്മാനം") || lower.contains("വിജയി")

            val isLotteryAction = lower.contains("lakh") || lower.contains("crore") || lower.contains("won") || lower.contains("claim") || lower.contains("lucky") ||
                    lower.contains("jeeta") || lower.contains("jeet gaye") || lower.contains("claim kare") || lower.contains("jeet") ||
                    lower.contains("लाख") || lower.contains("करोड़") || lower.contains("जीता") || lower.contains("जीते") || lower.contains("दावा") ||
                    lower.contains("कोटी") || lower.contains("दावा करा") || lower.contains("जिंकले") || lower.contains("बक्षीस") ||
                    lower.contains("லட்சம்") || lower.contains("கோடி") || lower.contains("வென்றுள்ளீர்கள்") || lower.contains("வென்றீர்கள்") ||
                    lower.contains("లక్ష") || lower.contains("కోట్లు") || lower.contains("గెలుచుకున్నారు") ||
                    lower.contains("ಲಕ್ಷ") || lower.contains("ಕೋಟಿ") || lower.contains("ಗೆದ್ದಿದ್ದೀರಿ") ||
                    lower.contains("ലക്ഷം") || lower.contains("കോടി") || lower.contains("വിജയിച്ചു") || lower.contains("നേടി")

            if (isLotteryEntity && isLotteryAction) {
                return Pair("Alert", 0.96f)
            }

            // 5. Fake Job / YouTube Tasks
            val isJobEntity = lower.contains("part time") || lower.contains("daily income") || lower.contains("youtube") || lower.contains("like and subscribe") || lower.contains("telegram") || lower.contains("work from home") ||
                    lower.contains("ghar baithe") || lower.contains("daily kamai") || lower.contains("paise kamaye") ||
                    lower.contains("घर बैठे") || lower.contains("दैनिक आय") || lower.contains("पार्ट टाइम") ||
                    lower.contains("घरी बसून") || lower.contains("घरबसल्या") || lower.contains("पार्ट टाईम") || lower.contains("दैनिक उत्पन्न") ||
                    lower.contains("வீட்டிலிருந்தே") || lower.contains("பகுதி நேர") || lower.contains("பகுதி நேரம்") || lower.contains("தினசரி வருமானம்") ||
                    lower.contains("ఇంటి నుంచే") || lower.contains("ఇంటి వద్ద") || lower.contains("పార్ట్ టైమ్") || lower.contains("రోజువారీ ఆదాయం") ||
                    lower.contains("ಮನೆಯಿಂದಲೇ") || lower.contains("ಪಾರ್ಟ್ ಟೈಮ್") || lower.contains("ಅರೆಕಾಲಿಕ") || lower.contains("ದಿನದ ಆದಾಯ") ||
                    lower.contains("വീട്ടിലിരുന്ന്") || lower.contains("പാർട്ട് ടൈം") || lower.contains("ദിവസേന വരുമാനം")

            val isJobAction = lower.contains("per day") || lower.contains("earn") || lower.contains("salary") || lower.contains("daily") || lower.contains("rs.") || lower.contains("inr") ||
                    lower.contains("roj kamao") || lower.contains("har din") || lower.contains("rojana") ||
                    lower.contains("प्रति दिन") || lower.contains("कमाएं") || lower.contains("वेतन") || lower.contains("रोजाना") ||
                    lower.contains("दररोज") || lower.contains("कमवा") || lower.contains("पगार") ||
                    lower.contains("தினமும்") || lower.contains("சம்பாதிக்க") || lower.contains("சம்பளம்") || lower.contains("ஒரு நாளைக்கு") ||
                    lower.contains("ప్రతి రోజు") || lower.contains("సంపాదించండి") || lower.contains("జీతం") || lower.contains("రోజుకు") ||
                    lower.contains("ಪ್ರತಿದಿನ") || lower.contains("ಸಂಪಾದಿಸಿ") || lower.contains("ವೇತನ") || lower.contains("ಗಳಿಸಿ") || lower.contains("ದಿನಕ್ಕೆ") ||
                    lower.contains("പ്രതിദിനം") || lower.contains("സമ്പാദിക്കുക") || lower.contains("ശമ്പളം") || lower.contains("സമ്പാദിക്കാം")

            if (isJobEntity && isJobAction) {
                return Pair("Alert", 0.95f)
            }

            // 6. Fake Traffic Challan
            val isChallanEntity = lower.contains("challan") || lower.contains("echallan") || lower.contains("traffic") || lower.contains("parivahan") ||
                    lower.contains("चालान") || lower.contains("ई-चालान") || lower.contains("ट्रैफिक") ||
                    lower.contains("चलन") || lower.contains("ई-चलन") || lower.contains("वाहतूक") ||
                    lower.contains("சலான்") || lower.contains("போக்குவரத்து") ||
                    lower.contains("చలాన్") || lower.contains("ట్రాఫిక్") ||
                    lower.contains("ಚಲನ್") || lower.contains("ಟ್ರಾಫಿಕ್") ||
                    lower.contains("ചെല്ലാൻ") || lower.contains("ട്രാഫിക്")

            val isChallanAction = lower.contains("pay") || lower.contains("court") || lower.contains(".apk") || lower.contains("fine") || lower.contains("vehicle") ||
                    lower.contains("jurmana") || lower.contains("bhare") ||
                    lower.contains("जुर्माना") || lower.contains("भुगतान") || lower.contains("कोर्ट") || lower.contains("वाहन") ||
                    lower.contains("दंड") || lower.contains("भरा") || lower.contains("न्यायालय") ||
                    lower.contains("அபராதம்") || lower.contains("செலுத்தவும்") || lower.contains("நீதிமன்றம்") ||
                    lower.contains("జరిమానా") || lower.contains("చెల్లించండి") || lower.contains("కోర్టు") ||
                    lower.contains("ದಂಡ") || lower.contains("ಪಾವತಿಸಿ") || lower.contains("ನ್ಯಾಯಾಲಯ") ||
                    lower.contains("പിഴ") || lower.contains("അടയ്ക്കുക") || lower.contains("കോടതി")

            if (isChallanEntity && isChallanAction) {
                return Pair("Alert", 0.95f)
            }

            // 7. Sextortion / Digital Arrest
            val isArrestEntity = lower.contains("cbi") || lower.contains("narcotics") || lower.contains("cyber crime") || lower.contains("police") || lower.contains("digital arrest") || lower.contains("customs") || lower.contains("ed") ||
                    lower.contains("giraftari") || lower.contains("cyber thana") ||
                    lower.contains("डिजिटल अरेस्ट") || lower.contains("सीबीआई") || lower.contains("पुलिस") || lower.contains("नारकोटिक्स") || lower.contains("साइबर अपराध") ||
                    lower.contains("डिजिटल अटक") || lower.contains("पोलीस") || lower.contains("सायबर गुन्हे") ||
                    lower.contains("டிஜிட்டல் கைது") || lower.contains("காவல்துறை") || lower.contains("போதைப்பொருள்") || lower.contains("சைபர் கிரைம்") ||
                    lower.contains("డిజిటల్ అరెస్ట్") || lower.contains("పోలీస్") || lower.contains("నార్కోటిక్స్") || lower.contains("సైబర్ క్రైమ్") ||
                    lower.contains("ಡಿಜಿಟಲ್ ಬಂಧನ") || lower.contains("ಪೊಲೀಸ್") || lower.contains("ಮಾದಕ ದ್ರವ್ಯ") || lower.contains("ಸೈಬರ್ ಕ್ರೈಮ್") ||
                    lower.contains("ഡിജിറ്റൽ അറസ്റ്റ്") || lower.contains("പോലീസ്") || lower.contains("നാർക്കോട്ടിക്സ്") || lower.contains("സൈബർ ക്രൈം")

            val isArrestAction = lower.contains("warrant") || lower.contains("arrest") || lower.contains("illegal") || lower.contains("court") || lower.contains("summon") || lower.contains("case file") ||
                    lower.contains("वारंट") || lower.contains("गिरफ्तारी") || lower.contains("समन") || lower.contains("अवैध") || lower.contains("मुकदमा") ||
                    lower.contains("वॉरंट") || lower.contains("अटक") || lower.contains("समन्स") || lower.contains("बेकायदेशीर") ||
                    lower.contains("வாரண்ட்") || lower.contains("கைது") || lower.contains("அழைப்பாணை") || lower.contains("சட்டவிரோத") ||
                    lower.contains("వారెంట్") || lower.contains("అరెస్ట్") || lower.contains("సమన్లు") || lower.contains("చట్టవిరుద్ధ") ||
                    lower.contains("ವಾರೆಂಟ್") || lower.contains("ಬಂಧನ") || lower.contains("ಸಮನ್ಸ್") || lower.contains("ಕಾನೂನುಬಾಹಿರ") ||
                    lower.contains("വാറണ്ട്") || lower.contains("അറസ്റ്റ്") || lower.contains("സമ്മൻസ്") || lower.contains("നിയമവിരുദ്ധ")

            if (isArrestEntity && isArrestAction) {
                return Pair("Alert", 0.99f)
            }

            // 8. India Post / Courier delivery address update scam
            val isCourierEntity = lower.contains("india post") || lower.contains("post office") || lower.contains("courier") || lower.contains("parcel") || lower.contains("shipment") || lower.contains("package") || lower.contains("delivery") ||
                    lower.contains("dak vibhag") ||
                    lower.contains("इंडिया पोस्ट") || lower.contains("डाकघर") || lower.contains("पार्सल") || lower.contains("कूरियर") ||
                    lower.contains("टपाल") || lower.contains("कुरिअर") ||
                    lower.contains("இந்தியா போஸ்ட்") || lower.contains("தபால்") || lower.contains("பார்சல்") || lower.contains("கூரியர்") ||
                    lower.contains("ఇండియా పోస్ట్") || lower.contains("పోస్ట్ ఆఫీస్") || lower.contains("పార్శిల్") || lower.contains("కొరియర్") ||
                    lower.contains("ಇಂಡಿಯಾ ಪೋಸ್ಟ್") || lower.contains("ಅಂಚೆ ಕಚೇರಿ") || lower.contains("ಪಾರ್ಸೆಲ್") || lower.contains("ಕೊರಿಯರ್") ||
                    lower.contains("ഇന്ത്യ പോസ്റ്റ്") || lower.contains("പോസ്റ്റ് ഓഫീസ്") || lower.contains("പാർസൽ") || lower.contains("കൊറിയർ")

            val isCourierAction = lower.contains("address") || lower.contains("failed") || lower.contains("suspend") || lower.contains("update") || lower.contains("reschedule") || lower.contains("undelivered") ||
                    lower.contains("pata update") || lower.contains("address galat") ||
                    lower.contains("पता अपडेट") || lower.contains("विफल") || lower.contains("अधूरा पता") || lower.contains("अवितरित") ||
                    lower.contains("पत्ता") || lower.contains("अयशस्वी") || lower.contains("अपूर्ण") ||
                    lower.contains("முகவரி") || lower.contains("தோல்வியடைந்தது") || lower.contains("புதுப்பிக்கவும்") ||
                    lower.contains("చిరునామా") || lower.contains("విఫలమైంది") || lower.contains("అప్‌డేట్") ||
                    lower.contains("ವಿಳಾಸ") || lower.contains("ವಿಫಲವಾಗಿದೆ") || lower.contains("ನವೀಕರಿಸಿ") ||
                    lower.contains("വിലാസം") || lower.contains("പരാജയപ്പെട്ടു") || lower.contains("അപ്ഡേറ്റ്")

            if (isCourierEntity && isCourierAction) {
                return Pair("Alert", 0.96f)
            }

            // 9. Remote access tools / OTP fraud
            val isRemoteTool = lower.contains("anydesk") || lower.contains("teamviewer") || lower.contains("quicksupport") || lower.contains("rustdesk") || lower.contains("screen share")
            val isOtpEntity = lower.contains("otp") || lower.contains("one time password") || lower.contains("verification code") ||
                    lower.contains("ओटीपी") || lower.contains("सत्यापन कोड") ||
                    lower.contains("पड पडताळणी") ||
                    lower.contains("ஓடிபி") || lower.contains("சரிபார்ப்பு") ||
                    lower.contains("ఓటీపీ") || lower.contains("ధృవీకరణ") ||
                    lower.contains("ಒಟಿಪಿ") || lower.contains("ದೃಢೀಕರಣ") ||
                    lower.contains("ഒടിപി") || lower.contains("സ്ഥിരീകരണ")

            val isOtpTheftAction = (lower.contains("share") || lower.contains("forward") || lower.contains("send to") || lower.contains("tell me") ||
                    lower.contains("bhejo") || lower.contains("batao") || lower.contains("share karo") ||
                    lower.contains("शेयर करें") || lower.contains("भेजें") || lower.contains("बताएं") || lower.contains("फॉरवर्ड करें") ||
                    lower.contains("शेअर करा") || lower.contains("पाठवा") || lower.contains("सांगा") ||
                    lower.contains("பகிரவும்") || lower.contains("அனுப்பவும்") || lower.contains("சொல்லுங்கள்") ||
                    lower.contains("షేర్ చేయండి") || lower.contains("పంపండి") || lower.contains("చెప్పండి") ||
                    lower.contains("ಹಂಚಿಕೊಳ್ಳಿ") || lower.contains("ಕಳುಹಿಸಿ") || lower.contains("ತಿಳಿಸಿ") ||
                    lower.contains("പങ്കിടുക") || lower.contains("അയക്കുക") || lower.contains("പറയുക")) &&
                    (!lower.contains("do not share") && !lower.contains("never share") && !lower.contains("don't share") && !lower.contains("dont share") &&
                     !lower.contains("कभी शेयर न करें") && !lower.contains("शेयर न करें") && !lower.contains("सांगू नका") && !lower.contains("பகிர வேண்டாம்"))

            if (isRemoteTool || (isOtpEntity && isOtpTheftAction)) {
                return Pair("Alert", 0.98f)
            }

            // 10. Pre-approved loan / refund scam
            val isLoanEntity = lower.contains("loan") || lower.contains("refund") || lower.contains("credit card limit") || lower.contains("cashback") ||
                    lower.contains("turant loan") ||
                    lower.contains("लोन") || lower.contains("ऋण") || lower.contains("रिफंड") || lower.contains("कैशबैक") ||
                    lower.contains("कर्ज") || lower.contains("परतावा") || lower.contains("कॅशबॅक") ||
                    lower.contains("கடன்") || lower.contains("பணத்தைத் திரும்பப்பெறுதல்") || lower.contains("கேஷ்பேக்") ||
                    lower.contains("రుణం") || lower.contains("రీఫండ్") || lower.contains("క్యాష్‌బ్యాక్") ||
                    lower.contains("ಸಾಲ") || lower.contains("ಮರುಪಾವತಿ") || lower.contains("ಕ್ಯಾಶ್‌ಬ್ಯಾಕ್") ||
                    lower.contains("വായ്പ") || lower.contains("ലോൺ") || lower.contains("റീഫണ്ട്") || lower.contains("ക്യാഷ്ബാക്ക്")

            val isLoanAction = (lower.contains("pre-approved") || lower.contains("approved") || lower.contains("credited") || lower.contains("claim now") || lower.contains("apply now") ||
                    lower.contains("manjoor") || lower.contains("pass hua") ||
                    lower.contains("स्वीकृत") || lower.contains("मंजूर") || lower.contains("दावा करें") ||
                    lower.contains("मंजूर") || lower.contains("दावा करा") ||
                    lower.contains("ஒப்புதல்") || lower.contains("விண்ணப்பிக்கவும்") ||
                    lower.contains("ఆమోదించబడింది") || lower.contains("క్లెయిమ్") ||
                    lower.contains("ಅನುಮೋದಿಸಲಾಗಿದೆ") || lower.contains("ಕ್ಲೈಮ್") ||
                    lower.contains("അനുവദിച്ചു") || lower.contains("ക്ലെയിം")) &&
                    (lower.contains("link") || lower.contains("http") || lower.contains("bit.ly") || lower.contains("click") || lower.contains("url") || lower.contains("லிங்க்") || lower.contains("లింక్") || lower.contains("ಲಿಂಕ್"))

            if (isLoanEntity && isLoanAction) {
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
                        "PAYMENT" -> "Payment WebShield"
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
        val contentWithoutEmojis = TFLiteScamClassifier.removeEmojis(message.content)
        val contentLower = contentWithoutEmojis.lowercase().trim()

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

        val isSms = message.appName.equals("SMS", ignoreCase = true)
        if (isSms && ContactUtils.isPromotionalSmsSender(message.sender)) {
            Log.d("ScamAnalyzer", "Skipping analysis for promotional SMS sender: ${message.sender}")
            return
        }

        val cleanText = contentWithoutEmojis.replace("\n", " ").replace(WHITESPACE_REGEX, " ").trim()
        if (cleanText.isEmpty()) {
            return
        }
        if (ENABLE_MESSAGE_KEYWORDS && !isApk && isTelecomServiceMessage(cleanText)) {
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
        val fastScam = if (ENABLE_MESSAGE_KEYWORDS && !isApk && !isFakeCredit && cached == null) checkFastDeterministicScam(cleanText) else null

        val immediateClassification = when {
            isApk || (ENABLE_MESSAGE_KEYWORDS && isFakeCredit) -> Pair("Alert", 1.0f)
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

        // Transformer inference path for direct neural model evaluation
        scope.launch(Dispatchers.Default) {
            try {
                Log.d("ScamAnalyzer", "Directly evaluating message with A0908i neural model: '${cleanText.take(30)}...'")
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
        val highestUrlScan = if (isWebFraud) FraudDetectionEngine.findHighestRiskUrlInText(message.content) else null
        val category = when {
            highestUrlScan != null && highestUrlScan.second.fraudCategory.isNotEmpty() && highestUrlScan.second.fraudCategory != "GENERIC" ->
                highestUrlScan.second.fraudCategory
            message.analysisResult.contains("Govt") || message.sender.contains("Govt") -> "GOVERNMENT"
            message.analysisResult.contains("Bank") || message.sender.contains("Bank") -> "BANK"
            message.analysisResult.contains("Payment") || message.sender.contains("Payment") -> "PAYMENT"
            message.analysisResult.contains("Fake Website") || message.sender.contains("Fake Website") -> "BRAND_IMPERSONATION"
            else -> "GENERIC"
        }
        val domainFromContent = if (isWebFraud) {
            (highestUrlScan?.first ?: FraudDetectionEngine.findValidUrlInText(message.content))
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
            .setContentTitle("POTENTIAL SCAM ALERT: ${message.sender}")
            .setContentText("Potential scam intercepted from ${message.sender}")
            .setStyle(Notification.BigTextStyle().bigText("EdgeGuard intercepted a potential scam from ${message.sender} (${message.appName}). Open EdgeGuard to view details."))
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(message.id, notification)
    }
}
