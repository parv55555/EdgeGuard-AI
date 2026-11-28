package com.parv.safetexts

import android.content.Context
import android.content.SharedPreferences

enum class AlertLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिंदी"),
    MARATHI("mr", "Marathi", "मराठी"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ"),
    TAMIL("ta", "Tamil", "தமிழ்"),
    TELUGU("te", "Telugu", "తెలుగు");

    companion object {
        fun fromCode(code: String?): AlertLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

object AlertTranslationManager {
    private const val PREFS_NAME = "safetexts_prefs"
    private const val KEY_ALERT_LANGUAGE = "alert_language"

    fun getSelectedLanguage(context: Context): AlertLanguage {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_ALERT_LANGUAGE, "en")
        return AlertLanguage.fromCode(code)
    }

    fun setSelectedLanguage(context: Context, language: AlertLanguage) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ALERT_LANGUAGE, language.code).apply()
    }

    // =========================================================================
    // BUTTON TRANSLATIONS
    // =========================================================================
    fun getDismissButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "DISMISS"
        AlertLanguage.HINDI -> "खारिज करें"
        AlertLanguage.MARATHI -> "रद्द करा"
        AlertLanguage.KANNADA -> "ತಿರಸ್ಕರಿಸಿ"
        AlertLanguage.TAMIL -> "விலக்கு"
        AlertLanguage.TELUGU -> "రద్దు చేయి"
    }

    fun getLooksSafeButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "LOOKS SAFE"
        AlertLanguage.HINDI -> "सुरक्षित दिखता है"
        AlertLanguage.MARATHI -> "सुरक्षित दिसते"
        AlertLanguage.KANNADA -> "ಸುರಕ್ಷಿತವಾಗಿದೆ"
        AlertLanguage.TAMIL -> "பாதுகாப்பானது"
        AlertLanguage.TELUGU -> "సురక్షితంగా ఉంది"
    }

    fun getTrustSenderButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "TRUST THIS SENDER"
        AlertLanguage.HINDI -> "प्रेषक पर विश्वास करें"
        AlertLanguage.MARATHI -> "प्रेषकावर विश्वास ठेवा"
        AlertLanguage.KANNADA -> "ಕಳುಹಿಸುವವರನ್ನು ನಂಬಿ"
        AlertLanguage.TAMIL -> "அனுப்புநரை நம்பு"
        AlertLanguage.TELUGU -> "పంపినవారిని నమ్మండి"
    }

    fun getTrustSiteButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "TRUST THIS SITE"
        AlertLanguage.HINDI -> "इस वेबसाइट पर विश्वास करें"
        AlertLanguage.MARATHI -> "या वेबसाइटवर विश्वास ठेवा"
        AlertLanguage.KANNADA -> "ಈ ವೆಬ್‌ಸೈಟ್ ನಂಬಿ"
        AlertLanguage.TAMIL -> "இத்தளத்தை நம்பு"
        AlertLanguage.TELUGU -> "ఈ సైట్‌ను నమ్మండి"
    }

    fun getUnderstandButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "I UNDERSTAND"
        AlertLanguage.HINDI -> "समझ गया"
        AlertLanguage.MARATHI -> "मला समजले"
        AlertLanguage.KANNADA -> "ನನಗೆ ಅರ್ಥವಾಯಿತು"
        AlertLanguage.TAMIL -> "புரிந்துகொண்டேன்"
        AlertLanguage.TELUGU -> "అర్థమైంది"
    }

    fun getCancelInstallButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "CANCEL INSTALL"
        AlertLanguage.HINDI -> "इंस्टॉल रद्द करें"
        AlertLanguage.MARATHI -> "इन्स्टॉलेशन रद्द करा"
        AlertLanguage.KANNADA -> "ರದ್ದುಮಾಡಿ"
        AlertLanguage.TAMIL -> "நிறுவலை ரத்து செய்"
        AlertLanguage.TELUGU -> "రద్దు చేయండి"
    }

    fun getInstallAnywayButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "INSTALL ANYWAY"
        AlertLanguage.HINDI -> "फिर भी इंस्टॉल करें"
        AlertLanguage.MARATHI -> "तरीही इन्स्टॉल करा"
        AlertLanguage.KANNADA -> "ಹೇಗಾದರೂ ಇನ್‌ಸ್ಟಾಲ್ ಮಾಡಿ"
        AlertLanguage.TAMIL -> "இருப்பினும் நிறுவு"
        AlertLanguage.TELUGU -> "ఏది ఏమైనా ఇన్‌స్టాల్ చేయి"
    }

    fun getUninstallNowButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "I WILL UNINSTALL NOW"
        AlertLanguage.HINDI -> "मैं अभी अनइंस्टॉल करता हूँ"
        AlertLanguage.MARATHI -> "मी आता अनइन्स्टॉल करतो"
        AlertLanguage.KANNADA -> "ನಾನು ಈಗಲೇ ಅನ್‌ಇನ್‌ಸ್ಟಾಲ್ ಮಾಡುತ್ತೇನೆ"
        AlertLanguage.TAMIL -> "இப்போதே அன்இன்ஸ்டால் செய்கிறேன்"
        AlertLanguage.TELUGU -> "నేను ఇప్పుడే అన్‌ఇన్‌స్టాల్ చేస్తాను"
    }

    fun getHangUpButton(lang: AlertLanguage): String = when (lang) {
        AlertLanguage.ENGLISH -> "I UNDERSTAND (HANG UP)"
        AlertLanguage.HINDI -> "कॉल काटें / खारिज करें"
        AlertLanguage.MARATHI -> "कॉल बंद करा / रद्द करा"
        AlertLanguage.KANNADA -> "ಕರೆ ಕಡಿತಗೊಳಿಸಿ"
        AlertLanguage.TAMIL -> "அழைப்பை துண்டி"
        AlertLanguage.TELUGU -> "కాల్ కట్ చేయండి"
    }

    fun getSourcePrefix(lang: AlertLanguage, isApk: Boolean, isBrowserSpam: Boolean, sender: String, appName: String): String = when (lang) {
        AlertLanguage.ENGLISH -> when {
            isApk -> "Source: $sender"
            isBrowserSpam -> "Phishing notification from $sender"
            else -> "From: $sender ($appName)"
        }
        AlertLanguage.HINDI -> when {
            isApk -> "स्रोत: $sender"
            isBrowserSpam -> "$sender से फर्जी नोटिफिकेशन"
            else -> "भेजने वाला: $sender ($appName)"
        }
        AlertLanguage.MARATHI -> when {
            isApk -> "स्रोत: $sender"
            isBrowserSpam -> "$sender कडून आलेला संशयित मेसेज"
            else -> "पाठवणारा: $sender ($appName)"
        }
        AlertLanguage.KANNADA -> when {
            isApk -> "ಮೂಲ: $sender"
            isBrowserSpam -> "$sender ನಿಂದ ಬಂದ ಫಿಶಿಂಗ್ ನೋಟಿಫಿಕೇಶನ್"
            else -> "ಕಳುಹಿಸಿದವರು: $sender ($appName)"
        }
        AlertLanguage.TAMIL -> when {
            isApk -> "ஆதாரம்: $sender"
            isBrowserSpam -> "$sender இலிருந்து ஃபிஷிங் அறிவிப்பு"
            else -> "அனுப்பியவர்: $sender ($appName)"
        }
        AlertLanguage.TELUGU -> when {
            isApk -> "మూలం: $sender"
            isBrowserSpam -> "$sender నుండి ఫిషింగ్ నోటిఫికేషన్"
            else -> "పంపినవారు: $sender ($appName)"
        }
    }

    // =========================================================================
    // CONTENT-BASED SCAM TRANSLATIONS (#1 - #14)
    // =========================================================================
    enum class ScamType {
        GOVT_WEBSITE,
        BANK_PAGE,
        UNVERIFIED_PAYMENT,
        FAKE_WEBSITE,
        SUSPICIOUS_WEBSITE,
        FAKE_CREDIT,
        APK_CHALLAN,
        UTILITY_SCAM,
        LOTTERY_PRIZE,
        JOB_OFFER,
        LEGAL_THREAT,
        COURIER_DELIVERY,
        OTP_REMOTE_ACCESS,
        FINANCIAL_OFFER,
        BANK_KYC_BLOCK,
        GENERAL_SCAM
    }

    fun getScamCopy(type: ScamType, lang: AlertLanguage): Pair<String, String> {
        return when (type) {
            ScamType.GOVT_WEBSITE -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "UNVERIFIED GOVT WEBSITE",
                    "This page looks like an unofficial government portal. Official Indian government sites typically end in .gov.in or .nic.in. Avoid entering personal details or paying here."
                )
                AlertLanguage.HINDI -> Pair(
                    "अनधिकृत सरकारी वेबसाइट",
                    "यह वेबसाइट गैर-सरकारी या फर्जी पोर्टल लग रही है। आधिकारिक भारतीय सरकारी वेबसाइटें आमतौर पर .gov.in या .nic.in पर समाप्त होती हैं। यहाँ व्यक्तिगत जानकारी या भुगतान न करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "अनधिकृत सरकारी वेबसाइट",
                    "हे पेज अनधिकृत सरकारी पोर्टल वाटत आहे. अधिकृत सरकारी वेबसाइट्स सहसा .gov.in किंवा .nic.in ने संपतात. येथे कोणतीही माहिती किंवा पैसे भरू नका."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಅನಧಿಕೃತ ಸರ್ಕಾರಿ ವೆಬ್‌ಸೈಟ್",
                    "ಈ ಪುಟವು ನಕಲಿ ಸರ್ಕಾರಿ ಪೋರ್ಟಲ್‌ನಂತೆ ಕಾಣುತ್ತಿದೆ. ಅಧಿಕೃತ ಸರ್ಕಾರಿ ಸೈಟ್‌ಗಳು .gov.in ಅಥವಾ .nic.in ನಲ್ಲಿ ಕೊನೆಗೊಳ್ಳುತ್ತವೆ. ಇಲ್ಲಿ ವೈಯಕ್ತಿಕ ವಿವರಗಳನ್ನು ನಮೂದಿಸಬೇಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "அங்கீகரிக்கப்படாத அரசு தளம்",
                    "இந்த பக்கம் அதிகாரப்பூர்வமற்ற அரசு போர்டல் போல் தெரிகிறது. இந்திய அரசு தளங்கள் .gov.in அல்லது .nic.in இல் முடியும். தனிப்பட்ட விவரங்களை உள்ளிடவோ பணம் செலுத்தவோ வேண்டாம்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "అనధికారిక ప్రభుత్వ వెబ్‌సైట్",
                    "ఈ పేజీ అనధికారిక ప్రభుత్వ పోర్టల్‌లా కనిపిస్తోంది. భారత ప్రభుత్వ అధికారిక సైట్‌లు సాధారణంగా .gov.in లేదా .nic.in తో ముగుస్తాయి. ఇక్కడ వివరాలు నమోదు చేయవద్దు."
                )
            }

            ScamType.BANK_PAGE -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "UNVERIFIED BANKING PAGE",
                    "This website does not match an official bank domain and looks like a phishing page. Do NOT enter your NetBanking ID, password, MPIN, or OTP here."
                )
                AlertLanguage.HINDI -> Pair(
                    "फर्जी नेटबैंकिंग पेज चेतावनी",
                    "यह वेबसाइट किसी आधिकारिक बैंक डोमेन से मेल नहीं खाती और फर्जी फिशिंग पेज लग रही है। यहाँ अपना नेटबैंकिंग आईडी, पासवर्ड, एमपीआईएन या ओटीपी कभी दर्ज न करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बनावट बँकिंग पेज इशारा",
                    "ही वेबसाइट अधिकृत बँकेच्या डोमेनशी जुळत नाही आणि फिशिंग पेज वाटते. येथे तुमचा नेटबँकिंग आयडी, पासवर्ड किंवा ओटीपी प्रविष्ट करू नका."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ನಕಲಿ ಬ್ಯಾಂಕಿಂಗ್ ಪೇಜ್ ಎಚ್ಚರಿಕೆ",
                    "ಈ ವೆಬ್‌ಸೈಟ್ ಅಧಿಕೃತ ಬ್ಯಾಂಕ್ ಡೊಮೇನ್‌ಗೆ ಹೊಂದಿಕೆಯಾಗುವುದಿಲ್ಲ. ಇಲ್ಲಿ ನಿಮ್ಮ ನೆಟ್‌ಬ್ಯಾಂಕಿಂಗ್ ಐಡಿ, ಪಾಸ್‌ವರ್ಡ್ ಅಥವಾ ಒಟಿಪಿ ನಮೂದಿಸಬೇಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "போலி வங்கி இணையப்பக்கம்",
                    "இந்த வலைத்தளம் அதிகாரப்பூர்வ வங்கி டொமைனுடன் பொருந்தவில்லை. உங்கள் நெட்பேங்கிங் ஐடி, கடவுச்சொல் அல்லது OTP-ஐ இங்கே உள்ளிட வேண்டாம்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "నకిలీ బ్యాంకింగ్ పేజీ హెచ్చరిక",
                    "ఈ వెబ్‌సైట్ అధికారిక బ్యాంక్ డొమైన్‌తో సరిపోలడం లేదు. ఇక్కడ మీ నెట్‌బ్యాంకింగ్ ఐడీ, పాస్‌వర్డ్ లేదా ఓటీపీని ఎంటర్ చేయవద్దు."
                )
            }

            ScamType.UNVERIFIED_PAYMENT -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "UNKNOWN WEBSITE",
                    "Unknown website, verify the originality of website before paying. Check for the originality of the website before making any payment."
                )
                AlertLanguage.HINDI -> Pair(
                    "अज्ञात वेबसाइट (Unknown Website)",
                    "अज्ञात वेबसाइट, भुगतान करने से पहले वेबसाइट की मौलिकता और सत्यता की जांच करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "अनोळखी वेबसाइट (Unknown Website)",
                    "अनोळखी वेबसाइट, कोणतेही पेमेंट करण्यापूर्वी वेबसाइटची सत्यता तपासा।"
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಅಪರಿಚಿತ ವೆಬ್‌ಸೈಟ್ (Unknown Website)",
                    "ಅಪರಿಚಿತ ವೆಬ್‌ಸೈಟ್, ಯಾವುದೇ ಪಾವತಿ ಮಾಡುವ ಮುನ್ನ ವೆಬ್‌ಸೈಟ್‌ನ ನೈಜತೆಯನ್ನು ಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "அறியப்படாத இணையதளம் (Unknown Website)",
                    "அறியப்படாத இணையதளம், எந்தவொரு கட்டணத்தையும் செலுத்தும் முன் இணையதளத்தின் உண்மைத்தன்மையை சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "గుర్తుతెలియని వెబ్‌సైట్ (Unknown Website)",
                    "గుర్తుతెలియని వెబ్‌సైట్, ఏదైనా చెల్లింపు చేసే ముందు వెబ్‌సైట్ యొక్క వాస్తవికతను ధృవీకరించండి."
                )
            }

            ScamType.FAKE_WEBSITE -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "FAKE WEBSITE WARNING",
                    "This website has a domain name similar to a known company and looks like a fake website. Double-check the web address before entering passwords, phone numbers, or card details."
                )
                AlertLanguage.HINDI -> Pair(
                    "फर्जी वेबसाइट चेतावनी",
                    "इस वेबसाइट का डोमेन नाम किसी जानी-मानी बड़ी कंपनी से मिलता-जुलता है और यह एक नकली वेबसाइट लग रही है। पासवर्ड, मोबाइल नंबर या कार्ड विवरण डालने से पहले वेब पता दोबारा जांचें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बनावट वेबसाइट इशारा",
                    "या वेबसाइटचे डोमेन नाव नामांकित कंपनीसारखे आहे आणि ही एक बनावट वेबसाइट वाटत आहे. पासवर्ड, फोन नंबर किंवा कार्ड तपशील प्रविष्ट करण्यापूर्वी पत्ता पुन्हा तपासा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ನಕಲಿ ವೆಬ್‌ಸೈಟ್ ಎಚ್ಚರಿಕೆ",
                    "ಈ ವೆಬ್‌ಸೈಟ್ ಪ್ರಸಿದ್ಧ ಕಂಪನಿಯ ಹೆಸರನ್ನು ಹೋಲುವ ಡೊಮೇನ್ ಹೊಂದಿದ್ದು, ನಕಲಿ ವೆಬ್‌ಸೈಟ್‌ನಂತೆ ಕಾಣುತ್ತಿದೆ. ಪಾಸ್‌ವರ್ಡ್ ಅಥವಾ ಕಾರ್ಡ್ ವಿವರಗಳನ್ನು ನಮೂದಿಸುವ ಮೊದಲು ವಿಳಾಸವನ್ನು ಮರುಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "போலி இணையதள எச்சரிக்கை",
                    "இந்த வலைத்தளம் ஒரு பிரபல நிறுவனத்தின் பெயரைப் போன்ற டொமைனைக் கொண்டுள்ளது மற்றும் போலி தளமாகத் தெரிகிறது. விவரங்களை உள்ளிடும் முன் முகவரியை மீண்டும் சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "నకిలీ వెబ్‌సైట్ హెచ్చరిక",
                    "ఈ వెబ్‌సైట్ ఒక ప్రముఖ కంపెనీకి సమానమైన డొమైన్ పేరును కలిగి ఉంది మరియు నకిలీ వెబ్‌సైట్‌లా కనిపిస్తోంది. వివరాలను నమోదు చేసే ముందు వెబ్ చిరునామాను ఒకటికి రెండుసార్లు తనిఖీ చేయండి."
                )
            }

            ScamType.SUSPICIOUS_WEBSITE -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "SUSPICIOUS WEBSITE WARNING",
                    "This link looks suspicious and may be impersonating a known brand. Double-check the web address before entering passwords, phone numbers, or card details."
                )
                AlertLanguage.HINDI -> Pair(
                    "संदिग्ध वेबसाइट चेतावनी",
                    "यह लिंक संदिग्ध है और किसी जाने-माने ब्रांड की नकल कर रहा है। पासवर्ड, फोन नंबर या कार्ड विवरण दर्ज करने से पहले वेब पता दोबारा जांचें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "संशयित वेबसाइट इशारा",
                    "ही लिंक संशयास्पद आहे आणि एखाद्या प्रसिद्ध ब्रँडची नक्कल करत असावी. पासवर्ड, फोन नंबर किंवा कार्ड तपशील प्रविष्ट करण्यापूर्वी पत्ता तपासा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಅನುಮಾನಾಸ್ಪದ ವೆಬ್‌ಸೈಟ್ ಎಚ್ಚರಿಕೆ",
                    "ಈ ಲಿಂಕ್ ಅನುಮಾನಾಸ್ಪದವಾಗಿದ್ದು, ಪ್ರಸಿದ್ಧ ಬ್ರ್ಯಾಂಡ್ ಅನ್ನು ಹೋಲುತ್ತಿದೆ. ಪಾಸ್‌ವರ್ಡ್ ಅಥವಾ ಕಾರ್ಡ್ ವಿವರಗಳನ್ನು ನಮೂದಿಸುವ ಮೊದಲು ವಿಳಾಸವನ್ನು ಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "சந்தேகத்திற்கிடமான இணையதள எச்சரிக்கை",
                    "இந்த இணைப்பு சந்தேகத்திற்குரியதாக உள்ளது மற்றும் ஒரு பிரபல பிராண்டை போல் நடிக்கலாம். விவரங்களை உள்ளிடும் முன் முகவரியை சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "అనుమానాస్పద వెబ్‌సైట్ హెచ్చరిక",
                    "ఈ లింక్ అనుమానాస్పదంగా ఉంది మరియు తెలిసిన బ్రాండ్‌ను అనుకరిస్తూ ఉండవచ్చు. వివరాలను నమోదు చేసే ముందు వెబ్ చిరునామాను ఒకటికి రెండుసార్లు తనిఖీ చేయండి."
                )
            }

            ScamType.FAKE_CREDIT -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "UNVERIFIED CREDIT MESSAGE",
                    "This looks like a fake credit alert. Do NOT transfer money back based on an SMS alone—open your official bank app to check your real balance first."
                )
                AlertLanguage.HINDI -> Pair(
                    "फर्जी बैंक क्रेडिट संदेश",
                    "यह बैंक खाते में फर्जी पैसे जमा होने का संदेश लग रहा है। केवल एसएमएस देखकर किसी को भी पैसे वापस न भेजें—पहले अपने आधिकारिक बैंक ऐप में असली बैलेंस जांचें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बनावट बँक क्रेडिट मेसेज",
                    "हा खात्यात पैसे जमा झाल्याचा बनावट मेसेज वाटतो. केवळ एसएमएस पाहून कोणालाही पैसे परत पाठवू नका—आधी अधिकृत बँक ॲप उघडून तुमची शिल्लक रक्कम तपासा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ನಕಲಿ ಬ್ಯಾಂಕ್ ಕ್ರೆಡಿಟ್ ಸಂದೇಶ",
                    "ಇದು ಹಣ ಜಮೆಯಾದ ನಕಲಿ ಸಂದೇಶದಂತೆ ಕಾಣುತ್ತಿದೆ. ಕೇವಲ ಎಸ್‌ಎಂಎಸ್ ನೋಡಿ ಯಾರಿಗೂ ಹಣ ವಾಪಸ್ ಕಳುಹಿಸಬೇಡಿ—ಮೊದಲು ಅಧಿಕೃತ ಬ್ಯಾಂಕ್ ಆ್ಯಪ್‌ನಲ್ಲಿ ಬ್ಯಾಲೆನ್ಸ್ ಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "போலி வங்கி வரவு செய்தி",
                    "இது போலி வரவு எச்சரிக்கை போல் தெரிகிறது. SMS-ஐ மட்டும் நம்பி பணத்தை திருப்பி அனுப்பாதீர்கள்—முதலில் உங்கள் அதிகாரப்பூர்வ வங்கி செயலியை திறந்து உண்மையான இருப்பை சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "నకిలీ బ్యాంక్ క్రెడిట్ మెసేజ్",
                    "ఇది నకిలీ క్రెడిట్ హెచ్చరికలా కనిపిస్తోంది. కేవలం ఎస్సెమ్మెస్ ఆధారంగా డబ్బును తిరిగి పంపవద్దు—ముందుగా మీ అధికారిక బ్యాంక్ యాప్‌ను తెరిచి అసలు బ్యాలెన్స్‌ని తనిఖీ చేయండి."
                )
            }

            ScamType.APK_CHALLAN -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "SUSPICIOUS APK FILE",
                    "This looks like an unverified challan APK file. Installing unknown APKs can put your OTPs and bank accounts at risk. Check challans only on the official Parivahan portal."
                )
                AlertLanguage.HINDI -> Pair(
                    "संदिग्ध चालान APK फाइल",
                    "यह संदिग्ध या फर्जी चालान APK फाइल लग रही है। अज्ञात APK इंस्टॉल करने से आपके बैंक खाते और OTP चोरी हो सकते हैं। चालान केवल आधिकारिक परिवहन पोर्टल पर ही जांचें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "संशयित चालान APK फाईल",
                    "ही संशयास्पद किंवा बनावट चालान APK फाईल वाटते. अज्ञात APK इन्स्टॉल केल्यास तुमचे बँक खाते आणि OTP धोक्यात येऊ शकतात. चालान फक्त अधिकृत परिवहन पोर्टलवर तपासा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಅನುಮಾನಾಸ್ಪದ ಚಲನ್ APK ಫೈಲ್",
                    "ಇದು ಅಪರಿಚಿತ ಚಲನ್ APK ಫೈಲ್‌ನಂತೆ ಕಾಣುತ್ತಿದೆ. ಇಂತಹ APK ಇನ್‌ಸ್ಟಾಲ್ ಮಾಡುವುದರಿಂದ ನಿಮ್ಮ ಬ್ಯಾಂಕ್ ಖಾತೆ ಮತ್ತು OTP ಅಪಾಯಕ್ಕೆ ಸಿಲುಕಬಹುದು. ಪರಿವಾಹನ್ ಪೋರ್ಟಲ್‌ನಲ್ಲಿ ಮಾತ್ರ ಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "சந்தேகத்திற்கிடமான சலான் APK",
                    "இது சரிபார்க்கப்படாத சலான் APK கோப்பு போல் தெரிகிறது. தெரியாத APK-களை நிறுவுவது உங்கள் வங்கி கணக்கு மற்றும் OTP-யை திருட வழிவகுக்கும். அதிகாரப்பூர்வ பரிவாஹன் தளத்தில் மட்டும் சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "అనుమానాస్పద చలాన్ APK ఫైల్",
                    "ఇది నకిలీ చలాన్ APK ఫైల్‌లా కనిపిస్తోంది. గుర్తుతెలియని APKలను ఇన్‌స్టాల్ చేయడం వల్ల మీ బ్యాంక్ ఖాతా మరియు OTP ప్రమాదంలో పడవచ్చు. అధికారిక పరివాహన్ పోర్టల్‌లో మాత్రమే తనిఖీ చేయండి."
                )
            }

            ScamType.UTILITY_SCAM -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "POSSIBLE UTILITY SCAM",
                    "This looks like a fake power-disconnection threat. Utility providers rarely demand urgent payment to personal numbers. Check your official electricity app before paying."
                )
                AlertLanguage.HINDI -> Pair(
                    "बिजली बिल फर्जीवाड़ा चेतावनी",
                    "यह बिजली काटने का फर्जी संदेश प्रतीत होता है। बिजली विभाग कभी भी किसी व्यक्तिगत नंबर पर तुरंत पैसे भेजने को नहीं कहता। भुगतान करने से पहले अपने आधिकारिक बिजली ऐप में जांचें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "वीज बिल फसवणूक इशारा",
                    "हा वीजपुरवठा खंडित करण्याचा बनावट मेसेज वाटतो. वीज कंपन्या कधीही वैयक्तिक नंबरवर तातडीने पैसे भरण्यास सांगत नाहीत. पैसे भरण्यापूर्वी अधिकृत वीज ॲप तपासा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ವಿದ್ಯುತ್ ಬಿಲ್ ವಂಚನೆ ಎಚ್ಚರಿಕೆ",
                    "ಇದು ನಕಲಿ ವಿದ್ಯುತ್ ಸಂಪರ್ಕ ಕಡಿತಗೊಳಿಸುವ ಬೆದರಿಕೆಯಂತೆ ಕಾಣುತ್ತಿದೆ. ವಿದ್ಯುತ್ ಇಲಾಖೆಯು ವೈಯಕ್ತಿಕ ಸಂಖ್ಯೆಗಳಿಗೆ ಹಣ ಪಾವತಿಸಲು ಕೇಳುವುದಿಲ್ಲ. ಪಾವತಿಸುವ ಮುನ್ನ ಅಧಿಕೃತ ಆ್ಯಪ್‌ನಲ್ಲಿ ಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "மின்சார கட்டண மோசடி எச்சரிக்கை",
                    "இது போலி மின்சாரம் துண்டிப்பு அச்சுறுத்தலாகத் தெரிகிறது. மின்சார வாரியம் தனிப்பட்ட எண்களுக்கு உடனடியாக பணம் செலுத்தக் கோராது. பணம் செலுத்தும் முன் அதிகாரப்பூர்வ செயலியில் சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "విద్యుత్ బిల్లు స్కామ్ హెచ్చరిక",
                    "ఇది విద్యుత్ సరఫరా నిలిపివేతకు సంబంధించిన నకిలీ బెదిరింపులా కనిపిస్తోంది. విద్యుత్ బోర్డు ఎప్పుడూ వ్యక్తిగత నంబర్లకు డబ్బు చెల్లించమని అడగదు. చెల్లించే ముందు అధికారిక యాప్‌లో తనిఖీ చేయండి."
                )
            }

            ScamType.LOTTERY_PRIZE -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "SUSPICIOUS PRIZE MESSAGE",
                    "This message looks like a lottery or prize scam. Be cautious of any prize claim asking for processing fees, GST, or personal details."
                )
                AlertLanguage.HINDI -> Pair(
                    "फर्जी लॉटरी / इनाम का झांसा",
                    "यह संदेश किसी फर्जी लॉटरी या इनामी योजना का लग रहा है। किसी भी ऐसे इनाम से सावधान रहें जो प्रोसेसिंग फीस, टैक्स या बैंक विवरण मांगता हो।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बनावट लॉटरी / बक्षीस इशारा",
                    "हा मेसेज लॉटरी किंवा बक्षीस फसवणुकीचा वाटतो. प्रोसेसिंग फी, जीएसटी किंवा बँक तपशील मागणार्‍या कोणत्याही बक्षीसाच्या दाव्यापासून सावध रहा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ನಕಲಿ ಲಾಟರಿ / ಬಹುಮಾನ ಎಚ್ಚರಿಕೆ",
                    "ಈ ಸಂದೇಶವು ಲಾಟರಿ ಅಥವಾ ಬಹುಮಾನದ ವಂಚನೆಯಂತೆ ಕಾಣುತ್ತಿದೆ. ಪ್ರೊಸೆಸಿಂಗ್ ಶುಲ್ಕ ಅಥವಾ ವೈಯಕ್ತಿಕ ವಿವರಗಳನ್ನು ಕೇಳುವ ಬಹುಮಾನಗಳಿಂದ ದೂರವಿರಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "போலி லாட்டரி / பரிசு மோசடி",
                    "இந்த செய்தி லாட்டரி அல்லது பரிசு மோசடி போல் தெரிகிறது. செயலாக்க கட்டணம், வரி அல்லது வங்கி விவரங்களை கோரும் எந்தவொரு பரிசையும் நம்ப வேண்டாம்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "నకిలీ లాటరీ / బహుమతి హెచ్చరిక",
                    "ఈ సందేశం లాటరీ లేదా బహుమతి స్కామ్‌లా కనిపిస్తోంది. ప్రాసెసింగ్ ఫీజు లేదా వ్యక్తిగత వివరాలను అడిగే బహుమతుల పట్ల జాగ్రత్తగా ఉండండి."
                )
            }

            ScamType.JOB_OFFER -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "SUSPICIOUS JOB OFFER",
                    "This looks like a part-time task or job scam. Be wary of easy daily-income offers—especially any that ask for upfront deposits to unlock earnings."
                )
                AlertLanguage.HINDI -> Pair(
                    "फर्जी पार्ट-टाइम नौकरी चेतावनी",
                    "यह घर बैठे आसान कमाई या यूट्यूब टास्क का फर्जी ऑफर लग रहा है। पैसे कमाने के नाम पर पहले पैसे जमा कराने वाले ऑफर्स से बिल्कुल सावधान रहें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बनावट नोकरी / टास्क इशारा",
                    "हा पार्ट-टाईम टास्क किंवा नोकरीचा बनावट मेसेज वाटतो. घरबसल्या सोप्या कमाईच्या आणि पैसे मिळवण्यासाठी आधी पैसे भरण्यास सांगणाऱ्या ऑफर्सपासून सावध रहा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ನಕಲಿ ಉದ್ಯೋಗ ಆಫರ್ ಎಚ್ಚರಿಕೆ",
                    "ಇದು ಅರೆಕಾಲಿಕ ಕೆಲಸ ಅಥವಾ ಟಾಸ್ಕ್ ವಂಚನೆಯಂತೆ ಕಾಣುತ್ತಿದೆ. ಸುಲಭವಾಗಿ ಹಣ ಗಳಿಸಲು ಮುಂಗಡ ಠೇವಣಿ ಕೇಳುವ ಉದ್ಯೋಗ ಆಫರ್‌ಗಳಿಂದ ದೂರವಿರಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "போலி வேலை வாய்ப்பு எச்சரிக்கை",
                    "இது பகுதி நேர வேலை அல்லது டாஸ்க் மோசடி போல் தெரிகிறது. வருமானம் பெற முன்பணம் கேட்கும் எளிதான தினசரி வருமான சலுகைகளை நம்பாதீர்கள்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "నకిలీ ఉద్యోగ ఆఫర్ హెచ్చరిక",
                    "ఇది పార్ట్-టైమ్ టాస్క్ లేదా ఉద్యోగ మోసంలా కనిపిస్తోంది. ఆదాయం పొందడానికి ముందుగా డిపాజిట్లు అడిగే సులభమైన రోజువారీ ఆదాయ ఆఫర్ల పట్ల జాగ్రత్త వహించండి."
                )
            }

            ScamType.LEGAL_THREAT -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "POSSIBLE IMPERSONATION SCAM",
                    "This message looks like a fake legal or “digital arrest” threat. Police and CBI do not serve arrest warrants over SMS or WhatsApp. Do not panic or transfer money."
                )
                AlertLanguage.HINDI -> Pair(
                    "फर्जी कानूनी कार्रवाई / अरेस्ट का डर",
                    "यह संदेश फर्जी कानूनी नोटिस या 'डिजिटल अरेस्ट' की धमकी लग रहा है। पुलिस या सीबीआई कभी भी व्हाट्सएप या एसएमएस पर अरेस्ट वारंट नहीं भेजते। घबराएं नहीं और कोई पैसा न भेजें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बनावट 'डिजिटल अरेस्ट' इशारा",
                    "हा मेसेज बनावट कायदेशीर नोटीस किंवा 'डिजिटल अरेस्ट'ची धमकी वाटतो. पोलीस किंवा सीबीआय कधीही व्हॉट्सॲपवर अटक वॉरंट पाठवत नाहीत. घाबरू नका आणि पैसे पाठवू नका."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ನಕಲಿ 'ಡಿಜಿಟಲ್ ಅರೆಸ್ಟ್' ಎಚ್ಚರಿಕೆ",
                    "ಇದು ನಕಲಿ ಕಾನೂನು ನೋಟಿಸ್ ಅಥವಾ 'ಡಿಜಿಟಲ್ ಬಂಧನ'ದ ಬೆದರಿಕೆಯಂತೆ ಕಾಣುತ್ತಿದೆ. ಪೊಲೀಸರು ಅಥವಾ ಸಿಬಿಐ ಎಂದಿಗೂ ವಾಟ್ಸಾಪ್ ಮೂಲಕ ವಾರಂಟ್ ಕಳುಹಿಸುವುದಿಲ್ಲ. ಹೆದರಬೇಡಿ ಮತ್ತು ಹಣ ಕಳುಹಿಸಬೇಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "போலி 'டிஜிட்டல் கைது' அச்சுறுத்தல்",
                    "இந்த செய்தி போலி சட்ட அறிவிப்பு அல்லது 'டிஜிட்டல் கைது' அச்சுறுத்தல் போல் தெரிகிறது. போலீஸ் அல்லது சிபிஐ ஒருபோதும் வாட்ஸ்அப்பில் பிடிவாரண்ட் அனுப்பாது. பயப்படவோ பணம் அனுப்பவோ வேண்டாம்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "నకిలీ 'డిజిటల్ అరెస్ట్' హెచ్చరిక",
                    "ఈ మెసేజ్ నకిలీ చట్టపరమైన లేదా 'డిజిటల్ అరెస్ట్' బెదిరింపులా కనిపిస్తోంది. పోలీసులు లేదా సీబీఐ ఎప్పుడూ వాట్సాప్‌లో అరెస్ట్ వారెంట్లు పంపరు. భయపడవద్దు లేదా డబ్బు పంపవద్దు."
                )
            }

            ScamType.COURIER_DELIVERY -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "SUSPICIOUS DELIVERY LINK",
                    "This looks like a courier or address-update scam. Avoid clicking unknown links or paying small redelivery fees by card. Track parcels only on the official courier website."
                )
                AlertLanguage.HINDI -> Pair(
                    "फर्जी कूरियर / पार्सल डिलीवरी लिंक",
                    "यह पार्सल डिलीवरी या एड्रेस अपडेट का फर्जी संदेश लग रहा है। किसी भी अज्ञात लिंक पर क्लिक न करें और न ही कोई डिलीवरी फीस दें। पार्सल केवल आधिकारिक कूरियर वेबसाइट पर ही ट्रैक करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बनावट कुरिअर / पार्सल लिंक",
                    "हा कुरिअर किंवा पत्ता अपडेट करण्याचा बनावट मेसेज वाटतो. अज्ञात लिंकवर क्लिक करणे किंवा कोणतेही शुल्क भरणे टाळा. पार्सल फक्त अधिकृत वेबसाइटवर ट्रॅक करा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಅನುಮಾನಾಸ್ಪದ ಡೆಲಿವರಿ ಲಿಂಕ್",
                    "ಇದು ಕೊರಿಯರ್ ಅಥವಾ ವಿಳಾಸ ನವೀಕರಣದ ವಂಚನೆಯಂತೆ ಕಾಣುತ್ತಿದೆ. ಅಪರಿಚಿತ ಲಿಂಕ್‌ಗಳನ್ನು ಕ್ಲಿಕ್ ಮಾಡಬೇಡಿ ಅಥವಾ ಶುಲ್ಕ ಪಾವತಿಸಬೇಡಿ. ಅಧಿಕೃತ ಸೈಟ್‌ನಲ್ಲಿ ಮಾತ್ರ ಟ್ರ್ಯಾಕ್ ಮಾಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "சந்தேகத்திற்கிடமான பார்சல் இணைப்பு",
                    "இது கூரியர் அல்லது முகவரி புதுப்பிப்பு மோசடி போல் தெரிகிறது. தெரியாத இணைப்புகளை கிளிக் செய்யாதீர்கள் அல்லது பணம் செலுத்தாதீர்கள். அதிகாரப்பூர்வ கூரியர் இணையதளத்தில் மட்டும் கண்காணிக்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "అనుమానాస్పద డెలివరీ లింక్",
                    "ఇది కొరియర్ లేదా చిరునామా అప్‌డేట్ స్కామ్‌లా కనిపిస్తోంది. గుర్తుతెలియని లింక్‌లను క్లిక్ చేయడం లేదా రుసుము చెల్లించడం మానుకోండి. అధికారిక సైట్‌లో మాత్రమే ట్రాక్ చేయండి."
                )
            }

            ScamType.OTP_REMOTE_ACCESS -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "OTP & REMOTE ACCESS WARNING",
                    "This message looks like an attempt to ask for your OTP or remote access. Bank and police officials never ask for OTPs or screen-sharing apps—do not share them."
                )
                AlertLanguage.HINDI -> Pair(
                    "ओटीपी व स्क्रीन शेयरिंग का खतरा",
                    "यह संदेश आपका ओटीपी या फोन का स्क्रीन-शेयर एक्सेस मांगने की कोशिश लग रहा है। बैंक या पुलिस अधिकारी कभी भी ओटीपी या एनीडेस्क ऐप डाउनलोड करने को नहीं कहते—इन्हें कभी शेयर न करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "ओटीपी व स्क्रीन शेअरिंग धोका",
                    "हा मेसेज तुमचा ओटीपी किंवा स्क्रीन-शेअरिंग ॲप इन्स्टॉल करून घेण्याचा प्रयत्न वाटतो. बँक किंवा पोलीस कधीही ओटीपी किंवा एनीडेस्क ॲप मागत नाहीत—कधीही शेअर करू नका."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಒಟಿಪಿ & ಸ್ಕ್ರೀನ್ ಹಂಚಿಕೆ ಎಚ್ಚರಿಕೆ",
                    "ಈ ಸಂದೇಶವು ನಿಮ್ಮ ಒಟಿಪಿ ಅಥವಾ ಸ್ಕ್ರೀನ್ ಶೇರಿಂಗ್ ಪಡೆಯುವ ಪ್ರಯತ್ನದಂತೆ ಕಾಣುತ್ತಿದೆ. ಬ್ಯಾಂಕ್ ಅಧಿಕಾರಿಗಳು ಎಂದಿಗೂ ಒಟಿಪಿ ಕೇಳುವುದಿಲ್ಲ—ಅವುಗಳನ್ನು ಹಂಚಿಕೊಳ್ಳಬೇಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "OTP & திரை பகிர்வு எச்சரிக்கை",
                    "இந்த செய்தி உங்கள் OTP அல்லது ஸ்கிரீன்-ஷேரிங் அணுகலை கேட்கும் முயற்சி போல் தெரிகிறது. வங்கி அல்லது காவல்துறை அதிகாரிகள் ஒருபோதும் OTP-ஐ கேட்க மாட்டார்கள்—பகிர வேண்டாம்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "ఓటీపీ & స్క్రీన్ షేరింగ్ హెచ్చరిక",
                    "ఈ సందేశం మీ ఓటీపీ లేదా స్క్రీన్-షేరింగ్ యాక్సెస్‌ను అడిగే ప్రయత్నంలా కనిపిస్తోంది. బ్యాంక్ అధికారులు ఎప్పుడూ ఓటీపీని అడగరు—వాటిని ఎవరితోనూ షేర్ చేయవద్దు."
                )
            }

            ScamType.FINANCIAL_OFFER -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "SUSPICIOUS FINANCIAL OFFER",
                    "This looks like a phishing link disguised as a loan, refund, or cashback offer. Avoid clicking shortened links and verify offers directly in your official app."
                )
                AlertLanguage.HINDI -> Pair(
                    "फर्जी लोन / कैशबैक लिंक",
                    "यह लोन, रिफंड या कैशबैक के नाम पर भेजा गया फर्जी फिशिंग लिंक लग रहा है। ऐसे शॉर्ट लिंक्स पर क्लिक न करें और किसी भी ऑफर की पुष्टि सीधे अपने आधिकारिक ऐप में करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बनावट कर्ज / कॅशबॅक इशारा",
                    "हा कर्ज, परतावा किंवा कॅशबॅकच्या नावाखाली आलेला फिशिंग मेसेज वाटतो. अशा लिंक्सवर क्लिक करणे टाळा आणि अधिकृत ॲपमध्ये खात्री करा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ನಕಲಿ ಸಾಲ / ಕ್ಯಾಶ್‌ಬ್ಯಾಕ್ ಎಚ್ಚರಿಕೆ",
                    "ಇದು ಸಾಲ ಅಥವಾ ಕ್ಯಾಶ್‌ಬ್ಯಾಕ್ ಹೆಸರಿನಲ್ಲಿ ಬಂದಿರುವ ಫಿಶಿಂಗ್ ಲಿಂಕ್‌ನಂತೆ ಕಾಣುತ್ತಿದೆ. ಅಂತಹ ಲಿಂಕ್‌ಗಳನ್ನು ಕ್ಲಿಕ್ ಮಾಡಬೇಡಿ ಮತ್ತು ಅಧಿಕೃತ ಆ್ಯಪ್‌ನಲ್ಲಿ ಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "போலி கடன் / கேஷ்பேக் எச்சரிக்கை",
                    "இது கடன் அல்லது கேஷ்பேக் ஆஃபர் என்ற போர்வையில் அனுப்பப்பட்ட ஃபிஷிங் இணைப்பாகத் தெரிகிறது. குறுக்கு இணைப்புகளை கிளிக் செய்யாமல் அதிகாரப்பூர்வ செயலியில் சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "నకిలీ లోన్ / క్యాష్‌బ్యాక్ హెచ్చరిక",
                    "ఇది రుణం లేదా క్యాష్‌బ్యాక్ ఆఫర్ ముసుగులో వచ్చిన ఫిషింగ్ లింక్‌లా కనిపిస్తోంది. సంక్షిప్త లింక్‌లను క్లిక్ చేయవద్దు మరియు అధికారిక యాప్‌లో నేరుగా ఆఫర్‌లను ధృవీకరించండి."
                )
            }

            ScamType.BANK_KYC_BLOCK -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "POTENTIAL SCAM ALERT",
                    "This message looks like a KYC or account-block scam. Do NOT transfer money, share OTPs, or click links. Verify directly in your official bank or telecom app."
                )
                AlertLanguage.HINDI -> Pair(
                    "संदिग्ध बैंक केवाईसी / ब्लॉक स्कैम",
                    "यह संदेश बैंक केवाईसी या खाता बंद करने का फर्जी झांसा लग रहा है। पैसे ट्रांसफर न करें, ओटीपी साझा न करें और लिंक पर क्लिक न करें। अपने आधिकारिक बैंक या टेलीकॉम ऐप से सीधे पुष्टि करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "बँक केवायसी / खाते ब्लॉक इशारा",
                    "हा मेसेज बँक केवायसी किंवा खाते बंद करण्याचा बनावट इशारा वाटत आहे. पैसे ट्रान्सफर करू नका, ओटीपी शेअर करू नका आणि कोणत्याही लिंकवर क्लिक करू नका. थेट तुमच्या बँकेच्या अधिकृत ॲपवरून खात्री करा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಬ್ಯಾಂಕ್ ಕೆವೈಸಿ / ಖಾತೆ ಬ್ಲಾಕ್ ಎಚ್ಚರಿಕೆ",
                    "ಈ ಸಂದೇಶವು ಕೆವೈಸಿ ಅಥವಾ ಬ್ಯಾಂಕ್ ಖಾತೆ ನಿರ್ಬಂಧಿಸುವ ನಕಲಿ ಸಂದೇಶದಂತೆ ಕಾಣುತ್ತಿದೆ. ಹಣ ವರ್ಗಾಯಿಸಬೇಡಿ, ಒಟಿಪಿ ಹಂಚಿಕೊಳ್ಳಬೇಡಿ ಅಥವಾ ಲಿಂಕ್‌ಗಳನ್ನು ಕ್ಲಿಕ್ ಮಾಡಬೇಡಿ. ಅಧಿಕೃತ ಬ್ಯಾಂಕ್ ಆ್ಯಪ್‌ನಲ್ಲಿ ಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "வங்கி KYC / கணக்கு முடக்கம் எச்சரிக்கை",
                    "இந்த செய்தி வங்கி கேஒய்சி (KYC) அல்லது கணக்கு முடக்கப்படும் போலியான அச்சுறுத்தலாகத் தெரிகிறது. பணம் அனுப்பாதீர்கள், OTP பகிராதீர்கள் மற்றும் இணைப்புகளை கிளிக் செய்யாதீர்கள். உங்கள் அதிகாரப்பூர்வ வங்கி செயலியில் சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "బ్యాంక్ కేవైసీ / ఖాతా బ్లాక్ హెచ్చరిక",
                    "ఈ మెసేజ్ బ్యాంక్ కేవైసీ లేదా ఖాతా బ్లాక్ అయ్యే నకిలీ బెదిరింపులా కనిపిస్తోంది. డబ్బులు పంపవద్దు, ఓటీపీ షేర్ చేయవద్దు మరియు లింక్‌లను క్లిక్ చేయవద్దు. మీ అధికారిక బ్యాంక్ యాప్‌లో నేరుగా ధృవీకరించండి."
                )
            }

            ScamType.GENERAL_SCAM -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "POTENTIAL SCAM ALERT",
                    "This message matches patterns commonly seen in scams. Pause before acting—do not share OTPs, transfer money, or click links without verifying the sender."
                )
                AlertLanguage.HINDI -> Pair(
                    "संदिग्ध स्कैम चेतावनी",
                    "यह संदेश आमतौर पर धोखाधड़ी में देखे जाने वाले पैटर्न से मेल खाता है। रुकें—सत्यापन किए बिना ओटीपी साझा न करें, पैसे ट्रांसफर न करें और लिंक पर क्लिक न करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "संशयित फसवणूक इशारा",
                    "हा मेसेज फसवणुकीच्या प्रकारांशी जुळणारा वाटत आहे. सावध रहा—खात्री केल्याशिवाय ओटीपी देऊ नका, पैसे पाठवू नका किंवा लिंकवर क्लिक करू नका."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಅನುಮಾನಾಸ್ಪದ ವಂಚನೆ ಎಚ್ಚರಿಕೆ",
                    "ಈ ಸಂದೇಶವು ವಂಚನೆಯ ವಿಧಾನಗಳಿಗೆ ಹೊಂದಿಕೆಯಾಗುತ್ತಿದೆ. ಎಚ್ಚರವಹಿಸಿ—ಪರಿಶೀಲಿಸದೆ ಒಟಿಪಿ ಹಂಚಿಕೊಳ್ಳಬೇಡಿ, ಹಣ ಕಳುಹಿಸಬೇಡಿ ಅಥವಾ ಲಿಂಕ್ ಕ್ಲಿಕ್ ಮಾಡಬೇಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "சந்தேக மோசடி எச்சரிக்கை",
                    "இந்த செய்தி பொதுவாக மோசடிகளில் காணப்படும் அமைப்போடு பொருந்துகிறது. எச்சரிக்கையாக இருங்கள்—சரிபார்க்காமல் OTP பகிரவோ, பணம் அனுப்பவோ வேண்டாம்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "అనుమానాస్పద స్కామ్ హెచ్చరిక",
                    "ఈ సందేశం సాధారణంగా మోసాలలో కనిపించే నమూనాలతో సరిపోలుతోంది. నిర్ధారించకుండా ఓటీపీని పంచుకోవద్దు, డబ్బు పంపవద్దు లేదా లింక్‌లను క్లిక్ చేయవద్దు."
                )
            }
        }
    }

    // =========================================================================
    // BEHAVIORAL & TELEPHONY ALERT TRANSLATIONS (#15 - #27)
    // =========================================================================

    fun getOtpCardCopy(lang: AlertLanguage): Pair<String, String> = when (lang) {
        AlertLanguage.ENGLISH -> Pair(
            "OTP SAFETY REMINDER",
            "A sensitive OTP arrived near an unknown call. Never share OTPs on a call or chat—bank and police officials will never ask for them."
        )
        AlertLanguage.HINDI -> Pair(
            "महत्वपूर्ण OTP सुरक्षा चेतावनी",
            "अज्ञात कॉल के दौरान या तुरंत बाद एक संवेदनशील ओटीपी आया है। कॉल या चैट पर कभी भी ओटीपी साझा न करें—बैंक या पुलिस अधिकारी कभी भी ओटीपी नहीं मांगते।"
        )
        AlertLanguage.MARATHI -> Pair(
            "महत्त्वाचा OTP सुरक्षा इशारा",
            "अनोळखी कॉल दरम्यान किंवा लगेचच एक ओटीपी आला आहे. कॉल किंवा चॅटवर ओटीपी कधीही सांगू नका—बँक किंवा पोलीस कधीही ओटीपी मागत नाहीत."
        )
        AlertLanguage.KANNADA -> Pair(
            "ಸೂಕ್ಷ್ಮ OTP ಭದ್ರತಾ ಎಚ್ಚರಿಕೆ",
            "ಅಪರಿಚಿತ ಕರೆಯ ಸಮಯದಲ್ಲಿ ಅಥವಾ ನಂತರ ಸೂಕ್ಷ್ಮ ಒಟಿಪಿ ಬಂದಿದೆ. ಕರೆ ಅಥವಾ ಚಾಟ್‌ನಲ್ಲಿ ಒಟಿಪಿಯನ್ನು ಎಂದಿಗೂ ಹಂಚಿಕೊಳ್ಳಬೇಡಿ—ಬ್ಯಾಂಕ್ ಅಧಿಕಾರಿಗಳು ಒಟಿಪಿ ಕೇಳುವುದಿಲ್ಲ."
        )
        AlertLanguage.TAMIL -> Pair(
            "முக்கியமான OTP பாதுகாப்பு எச்சரிக்கை",
            "தெரியாத அழைப்பின் போது ஒரு ரகசிய OTP வந்துள்ளது. அழைப்பிலோ அல்லது அரட்டையிலோ ஒருபோதும் OTP-ஐ பகிர வேண்டாம்—வங்கி அல்லது காவல்துறை அதிகாரிகள் ஒருபோதும் கேட்க மாட்டார்கள்."
        )
        AlertLanguage.TELUGU -> Pair(
            "ముఖ్యమైన OTP భద్రతా హెచ్చరిక",
            "తెలియని కాల్ సమయంలో లేదా వెంటనే ఒక సున్నితమైన ఓటీపీ వచ్చింది. కాల్ లేదా చాట్‌లో ఎప్పుడూ ఓటీపీలను పంచుకోవద్దు—బ్యాంక్ లేదా పోలీసు అధికారులు ఎప్పుడూ వాటిని అడగరు."
        )
    }

    fun getRatCardCopy(lang: AlertLanguage, isSevere: Boolean): Pair<String, String> = when (lang) {
        AlertLanguage.ENGLISH -> Pair(
            if (isSevere) "HIGH-RISK SCREEN-SHARE ALERT" else "REMOTE ACCESS CAUTION",
            if (isSevere)
                "A screen-sharing app was detected during or right after an unknown call. If a caller asked you to install this, do NOT grant permissions—it may expose your OTPs and bank balance."
            else
                "Screen-sharing apps let others view or control your phone. Only allow access if you personally know and trust the person."
        )
        AlertLanguage.HINDI -> Pair(
            if (isSevere) "स्क्रीन-शेयरिंग का गंभीर खतरा" else "रिमोट एक्सेस से सावधान",
            if (isSevere)
                "अज्ञात कॉल के दौरान या तुरंत बाद स्क्रीन-शेयरिंग ऐप डिटेक्ट हुई है। यदि कॉलर ने इसे इंस्टॉल करने को कहा है, तो अनुमति न दें—यह आपके ओटीपी और बैंक बैलेंस चुरा सकता है।"
            else
                "स्क्रीन-शेयरिंग ऐप्स दूसरों को आपका फोन देखने या नियंत्रित करने की अनुमति देते हैं। केवल तभी अनुमति दें जब आप व्यक्ति को व्यक्तिगत रूप से जानते हों।"
        )
        AlertLanguage.MARATHI -> Pair(
            if (isSevere) "स्क्रीन-शेअरिंगचा गंभीर धोका" else "रिमोट ॲक्सेस सावधानता",
            if (isSevere)
                "अनोळखी कॉल दरम्यान स्क्रीन-शेअरिंग ॲप आढळले आहे. कॉलरच्या सांगण्यावरून याला परवानगी देऊ नका—यामुळे तुमचे ओटीपी आणि बँक खात्यातील पैसे चोरीला जाऊ शकतात."
            else
                "स्क्रीन-शेअरिंग ॲप्स इतरांना तुमचा फोन पाहू देतात. फक्त ओळखीच्या व्यक्तींनाच याची परवानगी द्या."
        )
        AlertLanguage.KANNADA -> Pair(
            if (isSevere) "ಸ್ಕ್ರೀನ್-ಶೇರಿಂಗ್ ಗಂಭೀರ ಅಪಾಯ" else "ರಿಮೋಟ್ ಪ್ರವೇಶ ಎಚ್ಚರಿಕೆ",
            if (isSevere)
                "ಅಪರಿಚಿತ ಕರೆಯ ಸಮಯದಲ್ಲಿ ಸ್ಕ್ರೀನ್-ಶೇರಿಂಗ್ ಆ್ಯಪ್ ಪತ್ತೆಯಾಗಿದೆ. ಅಪರಿಚಿತರ ಮಾತನ್ನು ಕೇಳಿ ಅನುಮತಿ ನೀಡಬೇಡಿ—ಇದು ನಿಮ್ಮ ಒಟಿಪಿ ಮತ್ತು ಹಣವನ್ನು ಕದಿಯಬಹುದು."
            else
                "ಸ್ಕ್ರೀನ್-ಶೇರಿಂಗ್ ಆ್ಯಪ್‌ಗಳು ಇತರರಿಗೆ ನಿಮ್ಮ ಫೋನ್ ನೋಡಲು ಅವಕಾಶ ನೀಡುತ್ತವೆ. ನಿಮಗೆ ವೈಯಕ್ತಿಕವಾಗಿ ತಿಳಿದಿರುವವರಿಗೆ ಮಾತ್ರ ಅನುಮತಿ ನೀಡಿ."
        )
        AlertLanguage.TAMIL -> Pair(
            if (isSevere) "திரை பகிர்வு தீவிர ஆபத்து" else "ரிமோட் அணுகல் எச்சரிக்கை",
            if (isSevere)
                "தெரியாத அழைப்பின் போது ஸ்கிரீன்-ஷேரிங் செயலி கண்டறியப்பட்டது. அழைப்பாளர் கூறியதால் இதற்கு அனுமதி வழங்காதீர்கள்—உங்கள் OTP மற்றும் வங்கி இருப்பு திருடப்படலாம்."
            else
                "திரை-பகிர்வு செயலிகள் மற்றவர்கள் உங்கள் தொலைபேசியைப் பார்க்க அனுமதிக்கின்றன. உங்களுக்கு தெரிந்தவர்களுக்கு மட்டுமே அனுமதி வழங்கவும்."
        )
        AlertLanguage.TELUGU -> Pair(
            if (isSevere) "స్క్రీన్-షేరింగ్ తీవ్ర ప్రమాదం" else "రిమోట్ యాక్సెస్ జాగ్రత్త",
            if (isSevere)
                "తెలియని కాల్ సమయంలో స్క్రీన్-షేరింగ్ యాప్ గుర్తించబడింది. కాలర్ సూచనల మేరకు దీనికి అనుమతి ఇవ్వవద్దు—ఇది మీ ఓటీపీలు మరియు బ్యాంక్ బ్యాలెన్స్‌ను దొంగిలించవచ్చు."
            else
                "స్క్రీన్-షేరింగ్ యాప్‌లు ఇతరులకు మీ ఫోన్‌ని చూసేందుకు వీలు కల్పిస్తాయి. మీకు వ్యక్తిగతంగా తెలిసిన వారికి మాత్రమే యాక్సెస్ ఇవ్వండి."
        )
    }

    fun getDigitalArrestCopy(lang: AlertLanguage): Pair<String, String> = when (lang) {
        AlertLanguage.ENGLISH -> Pair(
            "POSSIBLE DIGITAL ARREST SCAM",
            "Opening a banking app during a long unknown video call matches a common “Digital Arrest” scam pattern. Police and CBI never arrest people or ask for money over video call. Hang up and verify."
        )
        AlertLanguage.HINDI -> Pair(
            "डिजिटल अरेस्ट का गंभीर खतरा",
            "लंबी अज्ञात वीडियो कॉल के दौरान बैंकिंग ऐप खोलना 'डिजिटल अरेस्ट' ठगी का सामान्य पैटर्न है। पुलिस या सीबीआई कभी भी वीडियो कॉल पर अरेस्ट नहीं करती और न ही पैसे मांगती है। तुरंत कॉल काटें।"
        )
        AlertLanguage.MARATHI -> Pair(
            "बनावट डिजिटल अरेस्ट इशारा",
            "अनोळखी व्हिडिओ कॉल दरम्यान बँकिंग ॲप उघडणे हे 'डिजिटल अरेस्ट' फसवणुकीचे लक्षण आहे. पोलीस किंवा सीबीआय कधीही व्हिडिओ कॉलवर अटक करत नाहीत किंवा पैसे मागत नाहीत. कॉल बंद करा."
        )
        AlertLanguage.KANNADA -> Pair(
            "ಡಿಜಿಟಲ್ ಅರೆಸ್ಟ್ ಮೋಸದ ಅಪಾಯ",
            "ದೀರ್ಘ ಅಪರಿಚಿತ ವೀಡಿಯೊ ಕರೆಯಲ್ಲಿರುವಾಗ ಬ್ಯಾಂಕಿಂಗ್ ಆ್ಯಪ್ ತೆರೆಯುವುದು 'ಡಿಜಿಟಲ್ ಬಂಧನ' ವಂಚನೆಯ ಲಕ್ಷಣವಾಗಿದೆ. ಪೊಲೀಸರು ಎಂದಿಗೂ ವೀಡಿಯೊ ಕರೆಯಲ್ಲಿ ಬಂಧಿಸುವುದಿಲ್ಲ ಅಥವಾ ಹಣ ಕೇಳುವುದಿಲ್ಲ. ಕರೆಯನ್ನು ಕಡಿತಗೊಳಿಸಿ."
        )
        AlertLanguage.TAMIL -> Pair(
            "போலி டிஜிட்டல் கைது ஆபத்து",
            "நீண்ட வீடியோ அழைப்பின் போது வங்கி செயலியை திறப்பது 'டிஜிட்டல் கைது' மோசடியின் பொதுவான வடிவமாகும். காவல்துறை ஒருபோதும் வீடியோ காலில் கைது செய்யாது அல்லது பணம் கேட்காது. அழைப்பை உடனே துண்டிக்கவும்."
        )
        AlertLanguage.TELUGU -> Pair(
            "డిజిటల్ అరెస్ట్ తీవ్ర ప్రమాదం",
            "ఎక్కువ సేపు సాగే గుర్తుతెలియని వీడియో కాల్‌లో ఉన్నప్పుడు బ్యాంకింగ్ యాప్‌ను తెరవడం 'డిజిటల్ అరెస్ట్' స్కామ్ నమూనా. పోలీసులు లేదా సీబీఐ ఎప్పుడూ వీడియో కాల్‌లో అరెస్టు చేయరు లేదా డబ్బు అడగరు. వెంటనే కాల్ కట్ చేయండి."
        )
    }

    fun getSextortionCopy(lang: AlertLanguage): Pair<String, String> = when (lang) {
        AlertLanguage.ENGLISH -> Pair(
            "UNKNOWN VIDEO CALL CAUTION",
            "Incoming video call from an unsaved number. Be cautious before showing your face—unknown video calls are sometimes used for blackmail or extortion."
        )
        AlertLanguage.HINDI -> Pair(
            "अज्ञात वीडियो कॉल से सावधान",
            "किसी अनजान नंबर से वीडियो कॉल आ रही है। अपना चेहरा दिखाने से पहले सावधान रहें—अज्ञात वीडियो कॉल का उपयोग ब्लैकमेल या जबरन वसूली के लिए किया जा सकता है।"
        )
        AlertLanguage.MARATHI -> Pair(
            "अनोळखी व्हिडिओ कॉल सावधानता",
            "अनोळखी नंबरवरून व्हिडिओ कॉल येत आहे. चेहरा दाखवण्यापूर्वी सावधगिरी बाळगा—ब्लॅकमेल किंवा खंडणीसाठी अशा अनोळखी कॉल्सचा वापर केला जातो."
        )
        AlertLanguage.KANNADA -> Pair(
            "ಅಪರಿಚಿತ ವೀಡಿಯೊ ಕರೆ ಎಚ್ಚರಿಕೆ",
            "ಅಪರಿಚಿತ ಸಂಖ್ಯೆಯಿಂದ ವೀಡಿಯೊ ಕರೆ ಬರುತ್ತಿದೆ. ನಿಮ್ಮ ಮುಖವನ್ನು ತೋರಿಸುವ ಮುನ್ನ ಎಚ್ಚರವಿರಲಿ—ಬ್ಲ್ಯಾಕ್‌ಮೇಲ್ ಅಥವಾ ಹಣ ವಸೂಲಿಗೆ ಇಂತಹ ಕರೆಗಳನ್ನು ಬಳಸಲಾಗುತ್ತದೆ."
        )
        AlertLanguage.TAMIL -> Pair(
            "தெரியாத வீடியோ அழைப்பு எச்சரிக்கை",
            "தெரியாத எண்ணிலிருந்து வீடியோ அழைப்பு வருகிறது. உங்கள் முகத்தைக் காட்டும் முன் எச்சரிக்கையாக இருங்கள்—அச்சுறுத்தி பணம் பறிக்க இத்தகைய அழைப்புகள் பயன்படுத்தப்படலாம்."
        )
        AlertLanguage.TELUGU -> Pair(
            "గుర్తుతెలియని వీడియో కాల్ హెచ్చరిక",
            "తెలియని నంబర్ నుండి వీడియో కాల్ వస్తోంది. మీ ముఖాన్ని చూపించే ముందు జాగ్రత్తగా ఉండండి—బ్లాక్‌మెయిల్ లేదా డబ్బులు వసూలు చేయడానికి ఇలాంటి కాల్స్ ఉపయోగిస్తారు."
        )
    }

    fun getPreInstallCopy(lang: AlertLanguage, isSevere: Boolean): Pair<String, String> = when (lang) {
        AlertLanguage.ENGLISH -> Pair(
            if (isSevere) "HIGH-RISK INSTALL WARNING" else "UNVERIFIED APP WARNING",
            if (isSevere)
                "You are on an unknown call. Installing outside apps during a call is a common scam tactic that can put your device and bank balance at risk."
            else
                "This app is from outside Google Play. If an unknown caller asked you to install it, it could be an attempt to access your phone or banking apps."
        )
        AlertLanguage.HINDI -> Pair(
            if (isSevere) "अत्यधिक जोखिम भरी ऐप इंस्टॉल चेतावनी" else "अपुष्ट ऐप चेतावनी",
            if (isSevere)
                "आप किसी अनजान कॉल पर हैं। कॉल के दौरान बाहर से ऐप इंस्टॉल करवाना ठगों का मुख्य तरीका है, जिससे आपके बैंक खाते खाली हो सकते हैं।"
            else
                "यह ऐप गूगल प्ले स्टोर के बाहर से है। यदि किसी कॉलर ने इसे डाउनलोड करने को कहा है, तो यह आपके फोन या बैंक खातों तक पहुँचने का प्रयास हो सकता है।"
        )
        AlertLanguage.MARATHI -> Pair(
            if (isSevere) "धोकादायक इन्स्टॉलेशन इशारा" else "असत्यापित ॲप इशारा",
            if (isSevere)
                "तुम्ही अनोळखी कॉलवर आहात. कॉल दरम्यान बाहेरून ॲप इन्स्टॉल करणे ही फसवणुकीची पद्धत आहे, ज्यामुळे तुमचे बँक खाते धोक्यात येऊ शकते."
            else
                "हे ॲप गुगल प्ले स्टोअरच्या बाहेरचे आहे. कॉलरच्या सांगण्यावरून हे इन्स्टॉल करू नका, यामुळे फोनमधील माहिती धोक्यात येऊ शकते."
        )
        AlertLanguage.KANNADA -> Pair(
            if (isSevere) "ಅತ್ಯಂತ ಅಪಾಯಕಾರಿ ಆ್ಯಪ್ ಎಚ್ಚರಿಕೆ" else "ಅನಧಿಕೃತ ಆ್ಯಪ್ ಎಚ್ಚರಿಕೆ",
            if (isSevere)
                "ನೀವು ಅಪರಿಚಿತ ಕರೆಯಲ್ಲಿದ್ದೀರಿ. ಕರೆಯ ಸಮಯದಲ್ಲಿ ಹೊರಗಿನ ಆ್ಯಪ್ ಇನ್‌ಸ್ಟಾಲ್ ಮಾಡಿಸುವುದು ವಂಚಕರ ತಂತ್ರವಾಗಿದ್ದು, ನಿಮ್ಮ ಹಣ ಅಪಾಯಕ್ಕೆ ಸಿಲುಕಬಹುದು."
            else
                "ಈ ಆ್ಯಪ್ ಗೂಗಲ್ ಪ್ಲೇ ಸ್ಟೋರ್‌ನಿಂದ ಬಂದಿದ್ದಲ್ಲ. ಅಪರಿಚಿತರ ಮಾತನ್ನು ಕೇಳಿ ಇನ್‌ಸ್ಟಾಲ್ ಮಾಡಬೇಡಿ."
        )
        AlertLanguage.TAMIL -> Pair(
            if (isSevere) "ஆபத்தான நிறுவல் எச்சரிக்கை" else "சரிபார்க்கப்படாத செயலி எச்சரிக்கை",
            if (isSevere)
                "நீங்கள் தெரியாத அழைப்பில் உள்ளீர்கள். அழைப்பின் போது செயலிகளை நிறுவுவது உங்கள் வங்கி இருப்பை ஆபத்தில் ஆழ்த்தும் ஒரு பொதுவான மோசடி தந்திரமாகும்."
            else
                "இந்த செயலி கூகுள் ப்ளே ஸ்டோருக்கு வெளியிலிருந்து வந்தது. யாராவது கூறி இதை நிறுவினால் உங்கள் வங்கி கணக்கு திருடப்படலாம்."
        )
        AlertLanguage.TELUGU -> Pair(
            if (isSevere) "తీవ్రమైన ప్రమాదకర యాప్ హెచ్చరిక" else "ధృవీకరించని యాప్ హెచ్చరిక",
            if (isSevere)
                "మీరు తెలియని కాల్‌లో ఉన్నారు. కాల్ సమయంలో బయటి యాప్‌లను ఇన్‌స్టాల్ చేయడం అనేది మోసగాళ్ళ ప్రధాన పద్ధతి, ఇది మీ బ్యాంక్ ఖాతాను ఖాళీ చేయగలదు."
            else
                "ఈ యాప్ గూగుల్ ప్లే స్టోర్ వెలుపలి నుండి వచ్చింది. కాలర్ సూచన మేరకు దీన్ని ఇన్‌స్టాల్ చేస్తే మీ ఫోన్ లేదా బ్యాంక్ యాప్‌లు హ్యాక్ కావచ్చు."
        )
    }

    fun getPostInstallCopy(lang: AlertLanguage, maliciousPkg: String): Pair<String, String> = when (lang) {
        AlertLanguage.ENGLISH -> Pair(
            "UNVERIFIED APP INSTALLED",
            "An app from an untrusted source ($maliciousPkg) was just installed. Unverified apps can put your OTPs and bank accounts at risk—uninstall it if you didn’t mean to install it."
        )
        AlertLanguage.HINDI -> Pair(
            "अविश्वसनीय ऐप इंस्टॉल हुई",
            "अविश्वसनीय स्रोत से एक ऐप ($maliciousPkg) अभी इंस्टॉल हुई है। असत्यापित ऐप आपके ओटीपी और बैंक खातों को खतरे में डाल सकती हैं—इसे तुरंत अनइंस्टॉल करें।"
        )
        AlertLanguage.MARATHI -> Pair(
            "असुरक्षित ॲप इन्स्टॉल झाले",
            "अनोळखी स्रोतातून एक ॲप ($maliciousPkg) इन्स्टॉल झाले आहे. यामुळे तुमचे ओटीपी आणि बँक खाते धोक्यात येऊ शकते—याला त्वरित अनइन्स्टॉल करा."
        )
        AlertLanguage.KANNADA -> Pair(
            "ಅನಧಿಕೃತ ಆ್ಯಪ್ ಇನ್‌ಸ್ಟಾಲ್ ಆಗಿದೆ",
            "ಅಪರಿಚಿತ ಮೂಲದಿಂದ ಆ್ಯಪ್ ($maliciousPkg) ಇನ್‌ಸ್ಟಾಲ್ ಆಗಿದೆ. ಇದು ನಿಮ್ಮ ಒಟಿಪಿ ಮತ್ತು ಬ್ಯಾಂಕ್ ಖಾತೆಗೆ ಹಾನಿ ಉಂಟುಮಾಡಬಹುದು—ಕೂಡಲೇ ಅನ್‌ಇನ್‌ಸ್ಟಾಲ್ ಮಾಡಿ."
        )
        AlertLanguage.TAMIL -> Pair(
            "சரிபார்க்கப்படாத செயலி நிறுவப்பட்டது",
            "நம்பத்தகாத மூலத்திலிருந்து ஒரு செயலி ($maliciousPkg) நிறுவப்பட்டுள்ளது. இது உங்கள் OTP மற்றும் வங்கி கணக்கை பாதிக்கலாம்—உடனடியாக அன்இன்ஸ்டால் செய்யவும்."
        )
        AlertLanguage.TELUGU -> Pair(
            "ధృవీకరించని యాప్ ఇన్‌స్టాల్ చేయబడింది",
            "గుర్తుతెలియని మూలం నుండి ఒక యాప్ ($maliciousPkg) ఇప్పుడే ఇన్‌స్టాల్ చేయబడింది. ఇది మీ ఓటీపీలు మరియు బ్యాంక్ ఖాతాలను ప్రమాదంలో పడేయవచ్చు—వెంటనే అన్‌ఇన్‌స్టాల్ చేయండి."
        )
    }

    fun getScamFunnelCopy(lang: AlertLanguage, isDM: Boolean): Pair<String, String> = when (lang) {
        AlertLanguage.ENGLISH -> Pair(
            if (isDM) "UNVERIFIED CHAT CAUTION" else "SUSPICIOUS GROUP INVITE",
            if (isDM)
                "Moving from an online ad to a private chat with an unsaved number is a common pattern in trading, task, or betting scams. Verify carefully before sending any money."
            else
                "Joining a group straight from an online ad is a common pattern in trading, task, and betting scams. Verify the source carefully before sending money or personal details."
        )
        AlertLanguage.HINDI -> Pair(
            if (isDM) "अज्ञात चैट से सावधान" else "संदिग्ध ग्रुप इनवाइट चेतावनी",
            if (isDM)
                "ऑनलाइन विज्ञापन से सीधे किसी अनजान नंबर के साथ प्राइवेट चैट पर आना ट्रेडिंग या टास्क स्कैम का सामान्य तरीका है। पैसे भेजने से पहले अच्छी तरह जांच लें।"
            else
                "ऑनलाइन विज्ञापन से सीधे किसी अज्ञात ग्रुप में शामिल होना ट्रेडिंग, टास्क और सट्टेबाजी घोटालों का सामान्य तरीका है। पैसे या विवरण देने से पहले सतर्क रहें।"
        )
        AlertLanguage.MARATHI -> Pair(
            if (isDM) "अनोळखी चॅट सावधानता" else "संशयित ग्रुप आमंत्रण",
            if (isDM)
                "ऑनलाइन जाहिरातीवरून थेट अनोळखी नंबरशी चॅट करणे हे ट्रेडिंग किंवा टास्क फसवणुकीचे लक्षण आहे. पैसे पाठवण्यापूर्वी खात्री करा."
            else
                "ऑनलाइन जाहिरातीवरून थेट ग्रुपमध्ये सामील होणे हे फसव्या योजनांचे लक्षण आहे. कोणतीही माहिती देण्यापूर्वी काळजी घ्या."
        )
        AlertLanguage.KANNADA -> Pair(
            if (isDM) "ಅಪರಿಚಿತ ಚಾಟ್ ಎಚ್ಚರಿಕೆ" else "ಅನುಮಾನಾಸ್ಪದ ಗ್ರೂಪ್ ಆಹ್ವಾನ",
            if (isDM)
                "ಆನ್‌ಲೈನ್ ಜಾಹೀರಾತಿನಿಂದ ನೇರವಾಗಿ ಅಪರಿಚಿತರೊಂದಿಗೆ ಚಾಟ್ ಮಾಡುವುದು ಟ್ರೇಡಿಂಗ್ ಅಥವಾ ಟಾಸ್ಕ್ ಸ್ಕ್ಯಾಮ್‌ಗಳ ಲಕ್ಷಣವಾಗಿದೆ. ಹಣ ಕಳುಹಿಸುವ ಮುನ್ನ ಪರಿಶೀಲಿಸಿ."
            else
                "ಜಾಹೀರಾತಿನಿಂದ ನೇರವಾಗಿ ಗ್ರೂಪ್‌ಗೆ ಸೇರುವುದು ವಂಚನೆಯ ತಂತ್ರವಾಗಿದೆ. ಹಣ ಅಥವಾ ವಿವರಗಳನ್ನು ಹಂಚಿಕೊಳ್ಳುವ ಮುನ್ನ ಎಚ್ಚರವಿರಲಿ."
        )
        AlertLanguage.TAMIL -> Pair(
            if (isDM) "தெரியாத அரட்டை எச்சரிக்கை" else "சந்தேகத்திற்கிடமான குழு அழைப்பு",
            if (isDM)
                "ஆன்லைன் விளம்பரத்திலிருந்து தெரியாத எண்ணுடன் தனிப்பட்ட அரட்டைக்கு மாறுவது வர்த்தகம் அல்லது டாஸ்க் மோசடிகளின் பொதுவான அமைப்பாகும். பணம் அனுப்பும் முன் சரிபார்க்கவும்."
            else
                "விளம்பரத்திலிருந்து நேரடியாக ஒரு குழுவில் இணைவது ஆன்லைன் மோசடிகளில் வழக்கமானது. பணத்தை அனுப்பும் முன் எச்சரிக்கையாக இருங்கள்."
        )
        AlertLanguage.TELUGU -> Pair(
            if (isDM) "గుర్తుతెలియని చాట్ హెచ్చరిక" else "అనుమానాస్పద గ్రూప్ ఆహ్వానం",
            if (isDM)
                "ఆన్‌లైన్ ప్రకటన నుండి తెలియని నంబర్‌తో ప్రైవేట్ చాట్‌కు మారడం అనేది ట్రేడింగ్ లేదా టాస్క్ స్కామ్‌లలో సాధారణం. డబ్బు పంపే ముందు ధృవీకరించుకోండి."
            else
                "ప్రకటన నుండి నేరుగా తెలియని గ్రూప్‌లో చేరడం అనేది మోసపూరిత పథకాల లక్షణం. డబ్బు లేదా వ్యక్తిగత వివరాలను పంచుకునే ముందు జాగ్రత్త వహించండి."
        )
    }

    // =========================================================================
    // PRE-EMPTIVE SEARCH ALERTS (#23 - #27)
    // =========================================================================
    fun getSearchAlertCopy(typeKey: String, lang: AlertLanguage): Pair<String, String> {
        return when (typeKey.uppercase()) {
            "BOOKING" -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "TRAVEL BOOKING CAUTION",
                    "Contact numbers found in search results or hotel maps are sometimes fake. Verify bookings only through official apps or verified hotel websites."
                )
                AlertLanguage.HINDI -> Pair(
                    "होटल व यात्रा बुकिंग सावधानी",
                    "सर्च रिज़ल्ट या मैप्स पर दिखने वाले होटल हेल्पलाइन नंबर अक्सर फर्जी होते हैं। बुकिंग केवल आधिकारिक ऐप या होटल की असली वेबसाइट से ही करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "हॉटेल व प्रवास बुकिंग सावधानता",
                    "सर्च रिझल्ट्समध्ये दिसणारे संपर्क क्रमांक अनेकदा बनावट असतात. बुकिंग फक्त अधिकृत ॲप किंवा हॉटेलच्या मूळ वेबसाइटवरूनच करा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಹೋಟೆಲ್ & ಪ್ರಯಾಣ ಬುಕಿಂಗ್ ಎಚ್ಚರಿಕೆ",
                    "ಹುಡುಕಾಟ ಫಲಿತಾಂಶಗಳಲ್ಲಿ ಕಂಡುಬರುವ ಹೋಟೆಲ್ ಸಂಪರ್ಕ ಸಂಖ್ಯೆಗಳು ನಕಲಿಯಾಗಿರಬಹುದು. ಅಧಿಕೃತ ಆ್ಯಪ್ ಮೂಲಕ ಮಾತ್ರ ಬುಕಿಂಗ್ ಪರಿಶೀಲಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "பயண முன்பதிவு எச்சரிக்கை",
                    "தேடல் முடிவுகளில் காணப்படும் தொலைபேசி எண்கள் சில நேரங்களில் போலியானவை. அதிகாரப்பூர்வ செயலி வழியாக மட்டுமே முன்பதிவை சரிபார்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "ట్రావెల్ బుకింగ్ జాగ్రత్త",
                    "శోధన ఫలితాల్లో కనిపించే హోటల్ నంబర్లు కొన్నిసార్లు నకిలీవి కావచ్చు. కేవలం అధికారిక యాప్‌లు లేదా సైట్‌ల ద్వారా మాత్రమే బుకింగ్‌లను నిర్ధారించండి."
                )
            }

            "HELPLINE" -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "HELPLINE SAFETY REMINDER",
                    "Helpline numbers found through web search are sometimes fake. Check the official app or website directly, and never share OTPs or install remote-access apps."
                )
                AlertLanguage.HINDI -> Pair(
                    "हेल्पलाइन नंबर सुरक्षा चेतावनी",
                    "गूगल सर्च पर मिलने वाले कस्टमर केयर नंबर कई बार फर्जी होते हैं। आधिकारिक ऐप में दिए गए नंबर पर ही संपर्क करें, और कभी ओटीपी न दें या एनीडेस्क ऐप डाउनलोड न करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "हेल्पलाईन सुरक्षा इशारा",
                    "सर्चमध्ये सापडणारे कस्टमर केअर नंबर अनेकदा बनावट असतात. थेट अधिकृत ॲपमधील नंबर तपासा आणि कोणालाही ओटीपी देऊ नका किंवा रिमोट ॲप इन्स्टॉल करू नका."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಹೆಲ್ಪ್‌ಲೈನ್ ಭದ್ರತಾ ಎಚ್ಚರಿಕೆ",
                    "ಸರ್ಚ್‌ನಲ್ಲಿ ಸಿಗುವ ಸಹಾಯವಾಣಿ ಸಂಖ್ಯೆಗಳು ನಕಲಿಯಾಗಿರಬಹುದು. ಅಧಿಕೃತ ಆ್ಯಪ್‌ನಲ್ಲಿರುವ ಸಂಖ್ಯೆಯನ್ನು ಮಾತ್ರ ಸಂಪರ್ಕಿಸಿ ಮತ್ತು ಒಟಿಪಿ ಹಂಚಿಕೊಳ್ಳಬೇಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "உதவி எண் பாதுகாப்பு நினைவூட்டல்",
                    "இணைய தேடலில் கிடைக்கும் உதவி எண்கள் சில நேரங்களில் போலியானவை. அதிகாரப்பூர்வ செயலியை நேரடியாக சரிபார்க்கவும், ஒருபோதும் OTP-ஐ பகிர வேண்டாம்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "హెల్ప్‌లైన్ భద్రతా హెచ్చరిక",
                    "నెట్‌లో దొరికే హెల్ప్‌లైన్ నంబర్లు కొన్నిసార్లు నకిలీవి కావచ్చు. నేరుగా అధికారిక యాప్‌ను సంప్రదించండి మరియు ఎవరితోనూ ఓటీపీలను పంచుకోవద్దు."
                )
            }

            "LOAN" -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "LOAN APP SAFETY REMINDER",
                    "Instant loan apps that demand access to contacts or photos can be used for blackmail and extortion. Use only RBI-registered banks or NBFCs."
                )
                AlertLanguage.HINDI -> Pair(
                    "इंस्टेंट लोन ऐप से सावधान",
                    "बिना सिबिल स्कोर तुरंत लोन देने वाली और कॉन्टैक्ट/फोटो की परमिशन मांगने वाली ऐप्स ब्लैकमेल करती हैं। केवल आरबीआई-पंजीकृत बैंकों से ही लोन लें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "कर्ज ॲप सुरक्षा इशारा",
                    "कॉन्टॅक्ट्स आणि फोटोंची परवानगी मागणारी झटपट कर्ज ॲप्स ब्लॅकमेल करण्यासाठी वापरली जातात. फक्त आरबीआय-नोंदणीकृत बँकांचा वापर करा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಸಾಲದ ಆ್ಯಪ್ ಭದ್ರತಾ ಎಚ್ಚರಿಕೆ",
                    "ಫೋಟೋ ಮತ್ತು ಕಾಂಟ್ಯಾಕ್ಟ್‌ಗಳ ಅನುಮತಿ ಕೇಳುವ ತ್ವರಿತ ಸಾಲದ ಆ್ಯಪ್‌ಗಳು ಬ್ಲ್ಯಾಕ್‌ಮೇಲ್‌ಗೆ ಕಾರಣವಾಗಬಹುದು. ಆರ್‌ಬಿಐ ನೋಂದಾಯಿತ ಬ್ಯಾಂಕ್‌ಗಳನ್ನು ಮಾತ್ರ ಬಳಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "கடன் செயலி பாதுகாப்பு எச்சரிக்கை",
                    "தொடர்புகள் அல்லது புகைப்படங்களுக்கான அணுகலைக் கோரும் உடனடி கடன் செயலிகள் மிரட்டி பணம் பறிக்க பயன்படுத்தப்படலாம். RBI-பதிவுசெய்த வங்கிகளை மட்டுமே பயன்படுத்தவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "లోన్ యాప్ భద్రతా హెచ్చరిక",
                    "కాంటాక్ట్‌లు లేదా ఫోటోల అనుమతి అడిగే తక్షణ రుణ యాప్‌లు బ్లాక్‌మెయిల్‌కు దారితీయవచ్చు. కేవలం ఆర్బీఐ-రిజిస్టర్డ్ బ్యాంకులను మాత్రమే ఉపయోగించండి."
                )
            }

            "JOB" -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "JOB SEARCH SAFETY REMINDER",
                    "Online part-time jobs that pay for liking videos or rating products often turn into deposit scams. Never pay money to unlock your earnings."
                )
                AlertLanguage.HINDI -> Pair(
                    "ऑनलाइन नौकरी सर्च चेतावनी",
                    "यूट्यूब वीडियो लाइक करने या रेटिंग देने के नाम पर पैसे देने वाले जॉब ऑफर बाद में ठगी बन जाते हैं। अपनी कमाई निकालने के लिए कभी पैसे जमा न करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "नोकरी शोध सुरक्षा इशारा",
                    "व्हिडिओ लाईक करून किंवा रेटिंग देऊन पैसे देण्याचे आमिष दाखवणारे पार्ट-टाईम जॉब्स फसवणूक असू शकतात. कमाई काढण्यासाठी कधीही पैसे भरू नका."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಉದ್ಯೋಗ ಹುಡುಕಾಟ ಎಚ್ಚರಿಕೆ",
                    "ವೀಡಿಯೊಗಳನ್ನು ಲೈಕ್ ಮಾಡಲು ಹಣ ಕೊಡುವ ಆನ್‌ಲೈನ್ ಪಾರ್ಟ್-ಟೈಮ್ ಉದ್ಯೋಗಗಳು ವಂಚನೆಯಾಗಿರುತ್ತವೆ. ಗಳಿಸಿದ ಹಣ ಪಡೆಯಲು ಎಂದಿಗೂ ಹಣ ಠೇವಣಿ ಮಾಡಬೇಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "வேலை தேடல் பாதுகாப்பு நினைவூட்டல்",
                    "வீடியோக்களை லைக் செய்ய பணம் தரும் பகுதி நேர வேலைகள் பெரும்பாலும் வைப்புத்தொகை மோசடிகளாக மாறும். பணத்தைப் பெற ஒருபோதும் வைப்புத்தொகை செலுத்த வேண்டாம்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "ఉద్యోగ శోధన భద్రతా హెచ్చరిక",
                    "వీడియోలను లైక్ చేసినందుకు డబ్బు చెల్లించే ఆన్‌లైన్ పార్ట్-టైమ్ ఉద్యోగాలు తరచుగా డిపాజిట్ స్కామ్‌లుగా మారతాయి. సంపాదనను తీసుకోవడానికి ఎప్పుడూ డబ్బు చెల్లించవద్దు."
                )
            }

            "TRADING" -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "INVESTMENT SEARCH ADVISORY",
                    "High-return trading apps and VIP signal groups are frequently rigged. Avoid transferring money to personal UPI IDs or unverified platforms."
                )
                AlertLanguage.HINDI -> Pair(
                    "ट्रेडिंग व निवेश सर्च चेतावनी",
                    "भारी मुनाफे का लालच देने वाले वीआईपी ग्रुप और अनधिकृत ट्रेडिंग प्लेटफॉर्म्स अक्सर फर्जी होते हैं। कभी भी किसी व्यक्तिगत यूपीआई आईडी पर पैसे न भेजें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "गुंतवणूक शोध सावधानता",
                    "मोठ्या परताव्याचे आमिष दाखवणारे ट्रेडिंग ॲप्स आणि व्हीआयपी ग्रुप्स अनेकदा बनावट असतात. वैयक्तिक यूपीआय आयडीवर कधीही पैसे पाठवू नका."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಹೂಡಿಕೆ ಹುಡುಕಾಟ ಎಚ್ಚರಿಕೆ",
                    "ಹೆಚ್ಚಿನ ಲಾಭದ ಭರವಸೆ ನೀಡುವ ಟ್ರೇಡಿಂಗ್ ಆ್ಯಪ್‌ಗಳು ಮತ್ತು ವಿಐಪಿ ಗ್ರೂಪ್‌ಗಳು ವಂಚನೆಯಾಗಿರುತ್ತವೆ. ವೈಯಕ್ತಿಕ ಯುಪಿಐ ಐಡಿಗಳಿಗೆ ಹಣ ಕಳುಹಿಸಬೇಡಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "முதலீட்டு தேடல் ஆலோசனை",
                    "அதிக லாபம் தரும் வர்த்தக செயலிகள் மற்றும் விஐபி குழுக்கள் பெரும்பாலும் போலியானவை. தனிப்பட்ட UPI ஐடிகளுக்கு பணம் அனுப்புவதை தவிர்க்கவும்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "పెట్టుబడి శోధన సలహా",
                    "అధిక రాబడిని వాగ్దానం చేసే ట్రేడింగ్ యాప్‌లు మరియు వీఐపీ సిగ్నల్ గ్రూప్‌లు నకిలీవి. వ్యక్తిగత యూపీఐ ఐడీలకు ఎప్పుడూ డబ్బులు పంపవద్దు."
                )
            }

            "CHAT_BOOKING" -> when (lang) {
                AlertLanguage.ENGLISH -> Pair(
                    "SUSPICIOUS BOOKING INQUIRY",
                    "You recently searched for travel bookings. Scammers often share unofficial QR codes or personal accounts for “advance payments”. Pay only on verified portals."
                )
                AlertLanguage.HINDI -> Pair(
                    "संदिग्ध बुकिंग पूछताछ चेतावनी",
                    "आपने हाल ही में यात्रा बुकिंग सर्च की थी। ठग अक्सर 'एडवांस पेमेंट' के लिए फर्जी क्यूआर कोड या व्यक्तिगत नंबर भेजते हैं। केवल आधिकारिक पोर्टल पर ही भुगतान करें।"
                )
                AlertLanguage.MARATHI -> Pair(
                    "संशयित बुकिंग चॅट इशारा",
                    "तुम्ही नुकतेच प्रवास बुकिंग शोधले आहे. फसवणूक करणारे अनेकदा 'ॲडव्हान्स पेमेंट'साठी बनावट क्यूआर कोड पाठवतात. फक्त अधिकृत पोर्टलवर पैसे भरा."
                )
                AlertLanguage.KANNADA -> Pair(
                    "ಅನುಮಾನಾಸ್ಪದ ಬುಕಿಂಗ್ ವಿಚಾರಣೆ",
                    "ನೀವು ಇತ್ತೀಚೆಗೆ ಪ್ರಯಾಣ ಬುಕಿಂಗ್‌ಗಾಗಿ ಹುಡುಕಿದ್ದೀರಿ. ಮುಂಗಡ ಹಣಕ್ಕಾಗಿ ನಕಲಿ ಕ್ಯೂಆರ್ ಕೋಡ್ ಕಳುಹಿಸುವ ಸಾಧ್ಯತೆಯಿದೆ. ಪರಿಶೀಲಿಸಿದ ಪೋರ್ಟಲ್‌ಗಳಲ್ಲಿ ಮಾತ್ರ ಪಾವತಿಸಿ."
                )
                AlertLanguage.TAMIL -> Pair(
                    "சந்தேகத்திற்கிடமான முன்பதிவு அரட்டை",
                    "சமீபத்தில் நீங்கள் பயண முன்பதிவுகளை தேடியுள்ளீர்கள். முன்பணத்திற்கு போலி QR குறியீடுகளை மோசடி செய்பவர்கள் அனுப்பலாம். சரிபார்க்கப்பட்ட தளத்தில் மட்டுமே பணம் செலுத்துங்கள்."
                )
                AlertLanguage.TELUGU -> Pair(
                    "అనుమానాస్పద బుకింగ్ విచారణ",
                    "మీరు ఇటీవల ట్రావెల్ బుకింగ్‌ల కోసం వెతికారు. 'అడ్వాన్స్ చెల్లింపు' కోసం మోసగాళ్ళు తరచుగా నకిలీ క్యూఆర్ కోడ్‌లను పంపుతారు. కేవలం అధికారిక పోర్టల్‌లలో మాత్రమే చెల్లించండి."
                )
            }

            else -> Pair(
                "SECURITY ALERT",
                "Be cautious before taking action or sharing sensitive details."
            )
        }
    }
}
