// programmed by parv
package com.parv.safetexts

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AlertCardBuilder {

    fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    fun resolveScamCopy(
        content: String,
        isApk: Boolean = false,
        isFakeCredit: Boolean = false,
        isBrowserSpam: Boolean = false,
        isFakeWebsite: Boolean = false,
        fraudCategory: String? = null,
        context: Context? = null,
        language: AlertLanguage? = null
    ): Pair<String, String> {
        val lang = language ?: (context?.let { AlertTranslationManager.getSelectedLanguage(it) } ?: AlertLanguage.ENGLISH)
        val lower = content.lowercase()
        val scamType = when {
            // #11 Counterfeit Govt Portal WebShield
            isFakeWebsite && fraudCategory == "GOVERNMENT" -> AlertTranslationManager.ScamType.GOVT_WEBSITE
            // #12 Counterfeit Bank WebShield
            isFakeWebsite && fraudCategory == "BANK" -> AlertTranslationManager.ScamType.BANK_PAGE
            // Case 1: Unknown website with payment page
            isFakeWebsite && (fraudCategory == "PAYMENT" || lower.contains("payment page") || lower.contains("originality of the website")) -> AlertTranslationManager.ScamType.UNVERIFIED_PAYMENT
            // Case 2: Big company name / lookalike domain (Fake Website)
            isFakeWebsite && (fraudCategory == "BRAND_IMPERSONATION" || fraudCategory == "ECOMMERCE" || lower.contains("brand impersonation") || lower.contains("brand hijacking") || lower.contains("typosquatting") || lower.contains("fake website")) -> AlertTranslationManager.ScamType.FAKE_WEBSITE
            // #13 Brand Impersonation & Spoofed URLs / Browser spam fallback
            isFakeWebsite || isBrowserSpam -> AlertTranslationManager.ScamType.SUSPICIOUS_WEBSITE
            // #10 Fake Transaction / Reverse-Credit
            isFakeCredit -> AlertTranslationManager.ScamType.FAKE_CREDIT
            // #5 Fake Traffic Challan APK or other APK files
            isApk -> AlertTranslationManager.ScamType.APK_CHALLAN
            // #2 Electricity / Utility Disconnection
            ((lower.contains("electricity") || lower.contains("bijli") || lower.contains("bijlee") || lower.contains("power") ||
              lower.contains("बिजली") || lower.contains("विद्युत") || lower.contains("वीज") || lower.contains("महावितरण") ||
              lower.contains("மின்சாரம்") || lower.contains("மின் கட்டணம்") || lower.contains("மின்கட்டணம்") ||
              lower.contains("విద్యుత్") || lower.contains("కరెంట్") ||
              lower.contains("ವಿದ್ಯುತ್") || lower.contains("ಕರೆಂಟ್") || lower.contains("ಬೆಸ್ಕಾಂ") ||
              lower.contains("വൈദ്യുതി") || lower.contains("കറന്റ്") || lower.contains("കെഎസ്ഇബി")) &&
             (lower.contains("disconnect") || lower.contains("cut") || lower.contains("bill") || lower.contains("tonight") || lower.contains("9:30") || lower.contains("unpaid") ||
              lower.contains("kat") || lower.contains("kaat") || lower.contains("bhejo") ||
              lower.contains("काट") || lower.contains("कटेगी") || lower.contains("बिल") || lower.contains("कापला") ||
              lower.contains("துண்டிக்கப்படும்") || lower.contains("பில்") ||
              lower.contains("నిలిపివేయబడుతుంది") || lower.contains("బిల్లు") ||
              lower.contains("ಕಡಿತಗೊಳಿಸಲಾಗುವುದು") || lower.contains("ಬಿಲ್") ||
              lower.contains("വിച്ഛേദിക്കും") || lower.contains("ബിൽ"))) -> AlertTranslationManager.ScamType.UTILITY_SCAM
            // #3 Fake Lottery / KBC / Prize Winner
            ((lower.contains("kbc") || lower.contains("lottery") || lower.contains("congratulations") || lower.contains("prize") || lower.contains("winner") ||
              lower.contains("badhai") || lower.contains("inaam") ||
              lower.contains("लॉटरी") || lower.contains("बधाई") || lower.contains("इनाम") ||
              lower.contains("अभिनंदन") || lower.contains("पारितोषिक") ||
              lower.contains("லாட்டரி") || lower.contains("பரிசு") || lower.contains("வாழ்த்துகள்") ||
              lower.contains("లాటరీ") || lower.contains("బహుమతి") || lower.contains("అభినందనలు") ||
              lower.contains("ಲಾಟರಿ") || lower.contains("ಬಹುಮಾನ") || lower.contains("ಅಭಿನಂದನೆಗಳು") ||
              lower.contains("ലോട്ടറി") || lower.contains("സമ്മാനം") || lower.contains("അഭിനന്ദനങ്ങൾ")) &&
             (lower.contains("lakh") || lower.contains("crore") || lower.contains("won") || lower.contains("claim") || lower.contains("lucky") ||
              lower.contains("jeet") || lower.contains("jite") ||
              lower.contains("लाख") || lower.contains("करोड़") || lower.contains("जीते") || lower.contains("दावा") ||
              lower.contains("जिंकले") ||
              lower.contains("லட்சம்") || lower.contains("கோடி") || lower.contains("வென்றீர்கள்") ||
              lower.contains("లక్షలు") || lower.contains("కోట్లు") || lower.contains("గెలుచుకున్నారు") ||
              lower.contains("ಲಕ್ಷ") || lower.contains("ಕೋಟಿ") || lower.contains("ಗೆದ್ದಿದ್ದೀರಿ") ||
              lower.contains("ലക്ഷം") || lower.contains("കോടി") || lower.contains("നേടി"))) -> AlertTranslationManager.ScamType.LOTTERY_PRIZE
            // #4 Fake Job / YouTube Task
            ((lower.contains("part time") || lower.contains("daily income") || lower.contains("youtube") || lower.contains("like and subscribe") || lower.contains("telegram") ||
              lower.contains("ghar baithe") || lower.contains("rojana kamaye") ||
              lower.contains("घर बैठे") || lower.contains("पार्ट टाइम") || lower.contains("कमाएं") || lower.contains("रोजाना कमाई") ||
              lower.contains("घरबसल्या") || lower.contains("पार्ट टाईम") || lower.contains("कमवा") ||
              lower.contains("பகுதி நேரம்") || lower.contains("வீட்டிலிருந்தே") || lower.contains("சம்பாதிக்க") ||
              lower.contains("పార్ట్ టైమ్") || lower.contains("ఇంటి వద్ద నుండే") || lower.contains("సంపాదించండి") ||
              lower.contains("ಅರೆಕಾಲಿಕ") || lower.contains("ಮನೆಯಿಂದಲೇ") || lower.contains("ಗಳಿಸಿ") ||
              lower.contains("പാർട്ട് ടൈം") || lower.contains("വീട്ടിലിരുന്ന്") || lower.contains("സമ്പാദിക്കാം")) &&
             (lower.contains("per day") || lower.contains("earn") || lower.contains("salary") || lower.contains("daily") || lower.contains("rs.") || lower.contains("inr") || lower.contains("work from home") ||
              lower.contains("din") || lower.contains("rojana") ||
              lower.contains("प्रतिदिन") || lower.contains("वेतन") || lower.contains("रुपये") ||
              lower.contains("दररोज") ||
              lower.contains("ஒரு நாளைக்கு") || lower.contains("வருமானம்") ||
              lower.contains("రోజుకు") || lower.contains("జీతం") ||
              lower.contains("ದಿನಕ್ಕೆ") || lower.contains("ಸಂಬಳ") ||
              lower.contains("പ്രതിദിനം") || lower.contains("ശമ്പളം"))) -> AlertTranslationManager.ScamType.JOB_OFFER
            // #6 Sextortion / Digital Arrest Text Threats
            ((lower.contains("cbi") || lower.contains("narcotics") || lower.contains("cyber crime") || lower.contains("police") || lower.contains("digital arrest") ||
              lower.contains("customs") || lower.contains("parcel") ||
              lower.contains("सीबीआई") || lower.contains("पुलिस") || lower.contains("डिजिटल अरेस्ट") || lower.contains("नारकोटिक्स") ||
              lower.contains("पोलीस") || lower.contains("सीबीआय") ||
              lower.contains("காவல்துறை") || lower.contains("டிஜிட்டல் கைது") ||
              lower.contains("పోలీస్") || lower.contains("డిజిటల్ అరెస్ట్") ||
              lower.contains("ಪೊಲೀಸ್") || lower.contains("ಡಿಜಿಟಲ್ ಬಂಧನ") ||
              lower.contains("പോലീസ്") || lower.contains("ഡിജിറ്റൽ അറസ്റ്റ്")) &&
             (lower.contains("warrant") || lower.contains("arrest") || lower.contains("illegal") || lower.contains("court") || lower.contains("summon") || lower.contains("case file") ||
              lower.contains("girftar") || lower.contains("gerftar") ||
              lower.contains("वारंट") || lower.contains("गिरफ्तार") || lower.contains("अवैध") || lower.contains("अदालत") ||
              lower.contains("वॉरंट") || lower.contains("अटक") ||
              lower.contains("வாரண்ட்") || lower.contains("கைது") || lower.contains("சட்டவிரோத") ||
              lower.contains("వారెంట్") || lower.contains("అరెస్ట్") || lower.contains("చట్టవిరుద్ధం") ||
              lower.contains("ವಾರಂಟ್") || lower.contains("ಬಂಧನ") || lower.contains("ಅಕ್ರಮ") ||
              lower.contains("വാറണ്ട്") || lower.contains("അറസ്റ്റ്") || lower.contains("നിയമവിരുദ്ധം"))) -> AlertTranslationManager.ScamType.LEGAL_THREAT
            // #7 India Post / Courier Address Update
            ((lower.contains("india post") || lower.contains("post office") || lower.contains("courier") || lower.contains("parcel") || lower.contains("shipment") || lower.contains("package") || lower.contains("delivery") ||
              lower.contains("भारतीय डाक") || lower.contains("डाकघर") || lower.contains("पार्सल") || lower.contains("डिलिवरी") ||
              lower.contains("टपाल") || lower.contains("कुरिअर") ||
              lower.contains("இந்திய அஞ்சல்") || lower.contains("தபால்") || lower.contains("பார்சல்") || lower.contains("டெலிவரி") ||
              lower.contains("ఇండియా పోస్ట్") || lower.contains("తపాలా") || lower.contains("పార్శిల్") || lower.contains("డెలివరీ") ||
              lower.contains("ಇಂಡಿಯಾ ಪೋಸ್ಟ್") || lower.contains("ಅಂಚೆ") || lower.contains("ಪಾರ್ಸೆಲ್") || lower.contains("ವಿತರಣೆ") ||
              lower.contains("ഇന്ത്യാ പോസ്റ്റ്") || lower.contains("പാഴ്സൽ") || lower.contains("ഡെലിവറി")) &&
             (lower.contains("address") || lower.contains("failed") || lower.contains("suspend") || lower.contains("update") || lower.contains("reschedule") || lower.contains("undelivered") ||
              lower.contains("pata") || lower.contains("roka") ||
              lower.contains("पता") || lower.contains("अपडेट") || lower.contains("विफल") || lower.contains("अधूरा") ||
              lower.contains("पत्ता") || lower.contains("अपूर्ण") ||
              lower.contains("முகவரி") || lower.contains("தோல்வி") || lower.contains("புதுப்பிக்கவும்") ||
              lower.contains("చిరునామా") || lower.contains("విఫలమైంది") || lower.contains("నవీకరించండి") ||
              lower.contains("ವಿಳಾಸ") || lower.contains("ವಿಫಲವಾಗಿದೆ") || lower.contains("ನವೀಕರಿಸಿ") ||
              lower.contains("വിലാസം") || lower.contains("പരാജയപ്പെട്ടു") || lower.contains("പുതുക്കുക"))) -> AlertTranslationManager.ScamType.COURIER_DELIVERY
            // #8 Remote Access / OTP Coercion
            (lower.contains("anydesk") || lower.contains("teamviewer") || lower.contains("quicksupport") || lower.contains("rustdesk") ||
             lower.contains("एनीडेस्क") || lower.contains("टीमव्यूअर") ||
             lower.contains("எனிடெஸ்க்") || lower.contains("ടീംവ്യൂവർ")) ||
            (((lower.contains("otp") || lower.contains("one time password") || lower.contains("verification code") ||
               lower.contains("ओटीपी") || lower.contains("ओटिपी") ||
               lower.contains("ஓடிபி") || lower.contains("ఓటీపీ") || lower.contains("ಒಟಿಪಿ") || lower.contains("ഒടിപി")) &&
              (lower.contains("share") || lower.contains("forward") || lower.contains("send to") || lower.contains("tell me") ||
               lower.contains("bhejo") || lower.contains("batao") ||
               lower.contains("शेयर करें") || lower.contains("भेजें") || lower.contains("बताएं") ||
               lower.contains("शेअर करा") || lower.contains("पाठवा") ||
               lower.contains("பகிரவும்") || lower.contains("அனுப்பவும்") ||
               lower.contains("షేర్ చేయండి") || lower.contains("పంపండి") ||
               lower.contains("ಹಂಚಿಕೊಳ್ಳಿ") || lower.contains("ಕಳುಹಿಸಿ") ||
               lower.contains("പങ്കിടുക") || lower.contains("അയക്കുക")) &&
              (!lower.contains("do not share") && !lower.contains("never share") && !lower.contains("don't share") && !lower.contains("dont share") &&
               !lower.contains("कभी शेयर न करें") && !lower.contains("शेयर न करें") && !lower.contains("सांगू नका") && !lower.contains("பகிர வேண்டாம்")))) -> AlertTranslationManager.ScamType.OTP_REMOTE_ACCESS
            // #9 Pre-Approved Loan / Cashback
            ((lower.contains("loan") || lower.contains("refund") || lower.contains("credit card limit") || lower.contains("cashback") ||
              lower.contains("turant loan") ||
              lower.contains("लोन") || lower.contains("ऋण") || lower.contains("रिफंड") || lower.contains("कैशबैक") ||
              lower.contains("कर्ज") || lower.contains("परतावा") || lower.contains("कॅशबॅक") ||
              lower.contains("கடன்") || lower.contains("பணத்தைத் திரும்பப்பெறுதல்") || lower.contains("கேஷ்பேக்") ||
              lower.contains("రుణం") || lower.contains("రీఫండ్") || lower.contains("క్యాష్‌బ్యాక్") ||
              lower.contains("ಸಾಲ") || lower.contains("ಮರುಪಾವತಿ") || lower.contains("ಕ್ಯಾಶ್‌ಬ್ಯಾಕ್") ||
              lower.contains("വായ്പ") || lower.contains("ലോൺ") || lower.contains("റീഫണ്ട്") || lower.contains("ക്യാഷ്ബാക്ക്")) &&
             (lower.contains("pre-approved") || lower.contains("approved") || lower.contains("credited") || lower.contains("claim now") || lower.contains("apply now") ||
              lower.contains("स्वीकृत") || lower.contains("मंजूर") || lower.contains("दावा करें") ||
              lower.contains("दावा करा") ||
              lower.contains("ஒப்புதல்") || lower.contains("விண்ணப்பிக்கவும்") ||
              lower.contains("ఆమోదించబడింది") || lower.contains("క్లెయిమ్") ||
              lower.contains("ಅನುಮೋದಿಸಲಾಗಿದೆ") || lower.contains("ಕ್ಲೈಮ್") ||
              lower.contains("അനുവദിച്ചു") || lower.contains("ക്ലെയിം")) &&
             (lower.contains("link") || lower.contains("http") || lower.contains("bit.ly") || lower.contains("click") || lower.contains("url") ||
              lower.contains("लिंक") || lower.contains("லிங்க்") || lower.contains("లింక్") || lower.contains("ಲಿಂಕ್"))) -> AlertTranslationManager.ScamType.FINANCIAL_OFFER
            // #1 Bank / KYC / PAN / SIM Block
            ((lower.contains("kyc") || lower.contains("pan card") || lower.contains("pancard") || lower.contains("aadhaar") || lower.contains("account") || lower.contains("debit card") || lower.contains("sim card") || lower.contains("sim block") || lower.contains("yono") || lower.contains("sbi") ||
              lower.contains("केवाईसी") || lower.contains("पैन कार्ड") || lower.contains("आधार") || lower.contains("खाता") || lower.contains("सिम कार्ड") ||
              lower.contains("खाते") || lower.contains("आधार कार्ड") ||
              lower.contains("பான் கார்டு") || lower.contains("ஆதார்") || lower.contains("வங்கி கணக்கு") ||
              lower.contains("పాన్ కార్డ్") || lower.contains("ఆధార్") || lower.contains("బ్యాంక్ ఖాతా") ||
              lower.contains("ಪ್ಯಾನ್ ಕಾರ್ಡ್") || lower.contains("ಆಧಾರ್") || lower.contains("ಬ್ಯಾಂಕ್ ಖಾತೆ") ||
              lower.contains("പാൻ കാർഡ്") || lower.contains("ആധാർ") || lower.contains("ബാങ്ക് അക്കൗണ്ട്")) &&
             (lower.contains("block") || lower.contains("suspend") || lower.contains("update immediately") || lower.contains("deactivate") || lower.contains("expire") || lower.contains("click here") || lower.contains("update your") ||
              lower.contains("band") || lower.contains("rok") ||
              lower.contains("ब्लॉक") || lower.contains("बंद") || lower.contains("निलंबित") || lower.contains("तुरंत अपडेट") ||
              lower.contains("बंद होईल") || lower.contains("अद्यतनित करा") ||
              lower.contains("முடக்கப்படும்") || lower.contains("உடனே புதுப்பிக்கவும்") ||
              lower.contains("బ్లాక్ చేయబడుతుంది") || lower.contains("వెంటనే అప్‌డేట్") ||
              lower.contains("ನಿರ್ಬಂಧಿಸಲಾಗುತ್ತದೆ") || lower.contains("ತಕ್ಷಣ ನವೀಕರಿಸಿ") ||
              lower.contains("ബ്ലോക്ക് ചെയ്യും") || lower.contains("ഉടൻ പുതുക്കുക"))) -> AlertTranslationManager.ScamType.BANK_KYC_BLOCK
            // #14 General AI / TFLite Message Classifier
            else -> AlertTranslationManager.ScamType.GENERAL_SCAM
        }
        return AlertTranslationManager.getScamCopy(scamType, lang)
    }

    fun buildScamCard(
        context: Context,
        sender: String,
        content: String,
        appName: String,
        isApk: Boolean = false,
        isFakeCredit: Boolean = false,
        isBrowserSpam: Boolean = false,
        isFakeWebsite: Boolean = false,
        websiteDomain: String? = null,
        fraudCategory: String? = null,
        onDismiss: () -> Unit
    ): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (cardTitle, cardBody) = resolveScamCopy(
            content = content,
            isApk = isApk,
            isFakeCredit = isFakeCredit,
            isBrowserSpam = isBrowserSpam,
            isFakeWebsite = isFakeWebsite,
            fraudCategory = fraudCategory,
            context = context,
            language = lang
        )

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#FF5252"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = cardTitle
            textSize = 17f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 4))
        }
        cardLayout.addView(titleView)

        val senderView = TextView(context).apply {
            text = AlertTranslationManager.getSourcePrefix(lang, isApk, isBrowserSpam, sender, appName)
            textSize = 13f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }
        cardLayout.addView(senderView)

        val warningDesc = TextView(context).apply {
            text = cardBody
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 10))
        }
        cardLayout.addView(warningDesc)

        val btnLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 4), 0, 0)
        }

        val dismissBtn = Button(context).apply {
            text = AlertTranslationManager.getDismissButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF5252"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(0, dp(context, 40), 1f)
            setOnClickListener { onDismiss() }
        }

        val notScamBtn = Button(context).apply {
            text = AlertTranslationManager.getLooksSafeButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#4CAF50"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8))
            val p = LinearLayout.LayoutParams(0, dp(context, 40), 1f)
            p.marginStart = dp(context, 12)
            layoutParams = p
            setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context.applicationContext)
                        val msg = db.messageDao().getMessage(sender, content)
                        if (msg != null) {
                            db.messageDao().updateMessage(msg.copy(analysisResult = "FALSE_POSITIVE"))
                        }
                        ScamAnalyzer.getInstance(context).markAsFalsePositive(sender, content)
                    } catch (e: Exception) {
                        Log.e("AlertCardBuilder", "Failed updating false positive", e)
                    }
                }
                try {
                    val broadcastIntent = Intent("com.parv.safetexts.NOT_A_SCAM").apply {
                        putExtra("message", content)
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(broadcastIntent)
                } catch (e: Exception) {
                    Log.e("AlertCardBuilder", "Failed sending false positive broadcast", e)
                }
                onDismiss()
            }
        }

        btnLayout.addView(dismissBtn)
        btnLayout.addView(notScamBtn)
        cardLayout.addView(btnLayout)

        val isEligibleForTrust = isFakeWebsite || (!isApk && 
                                 !isBrowserSpam &&
                                 sender.isNotBlank() && 
                                 sender != "System Alert" && 
                                 sender != "EdgeGuard" && 
                                 sender != "Browser" && 
                                 sender != "Download Manager" && 
                                 sender != "Screen Reader" &&
                                 sender != "Group APK" &&
                                 !sender.contains("APK", ignoreCase = true))

        if (isEligibleForTrust) {
            val trustBtn = Button(context).apply {
                text = if (isFakeWebsite) AlertTranslationManager.getTrustSiteButton(lang) else AlertTranslationManager.getTrustSenderButton(lang)
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1976D2")) // Trust blue
                    cornerRadius = dp(context, 10).toFloat()
                }
                setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    dp(context, 36)
                ).apply {
                    topMargin = dp(context, 8)
                }
                setOnClickListener {
                    val targetToTrust = if (isFakeWebsite && !websiteDomain.isNullOrBlank()) websiteDomain else sender
                    ContactUtils.addTrustedSender(context.applicationContext, targetToTrust)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context.applicationContext)
                            val msg = db.messageDao().getMessage(sender, content)
                            if (msg != null) {
                                db.messageDao().updateMessage(msg.copy(analysisResult = "TRUSTED_SENDER"))
                            }
                            ScamAnalyzer.getInstance(context).markAsFalsePositive(sender, content)
                        } catch (e: Exception) {
                            Log.e("AlertCardBuilder", "Failed marking trusted site/sender", e)
                        }
                    }
                    try {
                        val broadcastIntent = Intent("com.parv.safetexts.NOT_A_SCAM").apply {
                            putExtra("message", content)
                            putExtra("trusted_sender", targetToTrust)
                            setPackage(context.packageName)
                        }
                        context.sendBroadcast(broadcastIntent)
                    } catch (e: Exception) {
                        Log.e("AlertCardBuilder", "Failed broadcasting trust sender", e)
                    }
                    onDismiss()
                }
            }
            cardLayout.addView(trustBtn)
        }

        return cardLayout
    }

    fun buildOtpCard(context: Context, onDismiss: () -> Unit): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (cardTitle, cardBody) = AlertTranslationManager.getOtpCardCopy(lang)

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF8E1"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#FF8F00"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = cardTitle
            textSize = 17f
            setTextColor(Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = cardBody
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = AlertTranslationManager.getUnderstandButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF8F00"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 40))
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildRatCard(context: Context, isSevere: Boolean, onDismiss: () -> Unit): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (cardTitle, cardBody) = AlertTranslationManager.getRatCardCopy(lang, isSevere)

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#FFEBEE") else Color.parseColor("#FFF3E0"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = cardTitle
            textSize = 17f
            setTextColor(if (isSevere) Color.parseColor("#B71C1C") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = cardBody
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = AlertTranslationManager.getUnderstandButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 40))
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildDigitalArrestCard(context: Context, onDismiss: () -> Unit): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (cardTitle, cardBody) = AlertTranslationManager.getDigitalArrestCopy(lang)

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFEBEE"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#D50000"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = cardTitle
            textSize = 17f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = cardBody
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = AlertTranslationManager.getHangUpButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D50000"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 40))
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildSextortionCard(context: Context, onDismiss: () -> Unit): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (cardTitle, cardBody) = AlertTranslationManager.getSextortionCopy(lang)

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFEBEE"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#D50000"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = cardTitle
            textSize = 17f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = cardBody
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = AlertTranslationManager.getHangUpButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D50000"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 40))
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildPreInstallCard(context: Context, isSevere: Boolean, onDismiss: () -> Unit): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (cardTitle, cardBody) = AlertTranslationManager.getPreInstallCopy(lang, isSevere)

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#FFEBEE") else Color.parseColor("#FFF3E0"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = cardTitle
            textSize = 17f
            setTextColor(if (isSevere) Color.parseColor("#B71C1C") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = cardBody
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val btnContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 4), 0, 0)
        }

        val cancelBtn = Button(context).apply {
            text = AlertTranslationManager.getCancelInstallButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
            val p = LinearLayout.LayoutParams(0, dp(context, 40), 1f)
            p.marginEnd = dp(context, 10)
            layoutParams = p
            setOnClickListener { onDismiss() }
        }

        val bypassBtn = Button(context).apply {
            text = AlertTranslationManager.getInstallAnywayButton(lang)
            textSize = 13f
            setTextColor(if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(context, 1), if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(0, dp(context, 40), 1f)
            setOnClickListener {
                MessageReaderAccessibilityService.allowNextInstallUntilTime = System.currentTimeMillis() + 60_000L
                onDismiss()
            }
        }

        btnContainer.addView(cancelBtn)
        btnContainer.addView(bypassBtn)
        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(btnContainer)
        return cardLayout
    }

    fun buildPostInstallCard(context: Context, maliciousPkg: String, onDismiss: () -> Unit): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (cardTitle, cardBody) = AlertTranslationManager.getPostInstallCopy(lang, maliciousPkg)

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#B71C1C"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = cardTitle
            textSize = 17f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = cardBody
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = AlertTranslationManager.getUninstallNowButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B71C1C"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 40))
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildScamFunnelCard(context: Context, isDM: Boolean, onDismiss: () -> Unit): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (cardTitle, cardBody) = AlertTranslationManager.getScamFunnelCopy(lang, isDM)

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#D32F2F"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = cardTitle
            textSize = 17f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = cardBody
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = AlertTranslationManager.getUnderstandButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D32F2F"))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 40))
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildSimpleAlertCard(
        context: Context,
        title: String,
        titleColor: String,
        strokeColor: String,
        bgColor: String,
        message: String,
        typeKey: String? = null,
        onDismiss: () -> Unit
    ): View {
        val lang = AlertTranslationManager.getSelectedLanguage(context)
        val (resolvedTitle, resolvedMessage) = if (typeKey != null) {
            AlertTranslationManager.getSearchAlertCopy(typeKey, lang)
        } else {
            Pair(title, message)
        }

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 2), Color.parseColor(strokeColor))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = dp(context, 8).toFloat()
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = resolvedTitle
            textSize = 17f
            setTextColor(Color.parseColor(titleColor))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = resolvedMessage
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = AlertTranslationManager.getUnderstandButton(lang)
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(strokeColor))
                cornerRadius = dp(context, 10).toFloat()
            }
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 40))
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }
}
