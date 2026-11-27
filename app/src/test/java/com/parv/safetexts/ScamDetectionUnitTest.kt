package com.parv.safetexts

import org.junit.Assert.*
import org.junit.Test

class ScamDetectionUnitTest {

    @Test
    fun testElectricityDisconnectionScam() {
        val message = "Dear consumer, your electricity power will be disconnected tonight at 9:30 PM from the electricity office because your previous month bill was unpaid. Please immediately contact our officer at 9876543210. Thank you."
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("Electricity disconnection scam must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be >= 0.95", (result?.second ?: 0f) >= 0.95f)
    }

    @Test
    fun testBankKycSuspensionScam() {
        val message = "Dear SBI Customer, your bank account will be blocked today due to pending PAN card & KYC verification. Update immediately by clicking: http://bit.ly/sbi-kyc-update"
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("Bank KYC suspension scam must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be >= 0.95", (result?.second ?: 0f) >= 0.95f)
    }

    @Test
    fun testFakeLotteryKbcScam() {
        val message = "Congratulations! You have won Rs 25 Lakh in KBC Jio Lucky Draw 2026. To claim your prize money, send your details to WhatsApp +919754239203 immediately."
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("KBC lottery scam must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be >= 0.95", (result?.second ?: 0f) >= 0.95f)
    }

    @Test
    fun testWorkFromHomeTaskScam() {
        val message = "Earn Rs 3,000 to 8,000 daily per day by simple YouTube video like and subscribe tasks. Work from home part time. Contact Telegram @EarnDailyTask now."
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("Work from home task scam must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be >= 0.95", (result?.second ?: 0f) >= 0.95f)
    }

    @Test
    fun testMaliciousApkEChallanScam() {
        val message = "Traffic Police: You have an unpaid traffic challan against your vehicle fine Rs 1500. Pay immediately or court summon will be issued: http://traffic-echallan.apk"
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("Malicious APK challan scam must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be 1.0 for APK files", (result?.second ?: 0f) >= 1.0f)
    }

    @Test
    fun testDigitalArrestScam() {
        val message = "This is Officer Sharma from Delhi Police Cyber Crime & CBI Headquarters. An illegal narcotics parcel with your Aadhaar ID was intercepted. Digital arrest warrant issued. Connect immediately on Skype."
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("Digital arrest extortion scam must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be >= 0.98", (result?.second ?: 0f) >= 0.98f)
    }

    @Test
    fun testPostalDeliveryFailureScam() {
        val message = "India Post: Your package shipment delivery failed because of incorrect address details. Please update your address within 24 hours to avoid parcel return: http://indpost-update.top"
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("India Post smishing scam must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be >= 0.95", (result?.second ?: 0f) >= 0.95f)
    }

    @Test
    fun testRemoteAccessOtpHarvestingScam() {
        val message = "Bank technical desk: To unfreeze your YONO transaction, download AnyDesk from PlayStore and share the 9-digit remote access code with our support officer immediately."
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("Remote access AnyDesk fraud must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be >= 0.98", (result?.second ?: 0f) >= 0.98f)
    }

    @Test
    fun testPreApprovedLoanScam() {
        val message = "Instant approval! Your pre-approved personal loan of Rs 5,00,000 has been credited. Click link to claim now with zero interest: http://bit.ly/fast-instant-loan"
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("Pre-approved loan scam must be flagged", result)
        assertEquals("Alert", result?.first)
        assertTrue("Confidence must be >= 0.95", (result?.second ?: 0f) >= 0.95f)
    }

    @Test
    fun testMultilingualScamDetectionAcrossEightLanguages() {
        val multilingualScamCases = listOf(
            // 1. English
            "English Electricity" to "Dear consumer, your electricity power will be disconnected tonight at 9:30 PM from electricity office due to previous bill unpaid. Call 9876543210",
            "English Bank KYC" to "Dear SBI user, your bank account will be blocked today due to pending PAN card & KYC verification. Update immediately at http://bit.ly/sbi-kyc",
            "English Lottery" to "Congratulations! You have won Rs 25 Lakh in KBC Jio Lucky Draw 2026. Claim your prize money now.",
            "English Job" to "Earn Rs 5,000 daily per day by simple YouTube video part time tasks. Work from home.",

            // 2. Hinglish (Latin script / Romanized Hindi)
            "Hinglish Electricity" to "Aapka bijli connection aaj raat 9:30 baje cut kar diya jayega bill unpaid hone ke karan. Turant contact kare 9876543210",
            "Hinglish Bank KYC" to "Aapka bank account band ho jayega. Turant apna PAN card update kare http://bit.ly/bank-pan-update",
            "Hinglish Lottery" to "Badhai ho! Aapne KBC lottery me 25 lakh rupaye jeet liye hai. Claim karne ke liye WhatsApp kare",
            "Hinglish Job" to "Ghar baithe rojana kamaye 3000 se 5000 rs per day youtube video like karke part time work from home",
            "Hinglish Loan" to "Aapka 5 lakh ka turant loan approved hua hai. Claim now click link http://loan-instant.xyz",

            // 3. Hindi (हिन्दी)
            "Hindi Electricity" to "प्रिय उपभोक्ता, आपकी बिजली आज रात 9:30 बजे काट दी जाएगी क्योंकि पिछला बिल बकाया है। संपर्क करें 9876543210",
            "Hindi Bank KYC" to "प्रिय ग्राहक, आपका बैंक खाता आज ब्लॉक कर दिया जाएगा। पैन कार्ड तुरंत अपडेट करें: http://sbi-pan-kyc.in",
            "Hindi Lottery" to "बधाई हो! आपने केबीसी लॉटरी में 25 लाख रुपये जीते हैं। अपना इनाम पाने के लिए तुरंत संपर्क करें",
            "Hindi Job" to "घर बैठे रोजाना 3000 से 5000 रुपये कमाएं। यूट्यूब वीडियो लाइक करने का पार्ट टाइम काम।",
            "Hindi Digital Arrest" to "सीबीआई पुलिस: आपके आधार पर अवैध पार्सल मिलने के कारण डिजिटल अरेस्ट वारंट जारी किया गया है।",

            // 4. Marathi (मराठी)
            "Marathi Electricity" to "प्रिय ग्राहक, तुमचे वीज बिल न भरल्यामुळे आज रात्री 9:30 वाजता वीज पुरवठा खंडित केला जाईल. महावितरण अधिकारी 9876543210",
            "Marathi Bank KYC" to "प्रिय ग्राहक, तुमचे बँक खाते केवायसी अपडेट न केल्यामुळे ब्लॉक केले जाईल. त्वरित अद्यतनित करा: http://bank-update.top",
            "Marathi Lottery" to "अभिनंदन! तुम्ही लॉटरीमध्ये 25 लाख रुपये जिंकले आहेत. बक्षीस मिळवण्यासाठी संपर्क करा",
            "Marathi Job" to "घरबसल्या पार्ट टाईम काम करून दररोज 3000 रुपये कमवा. त्वरित संपर्क करा.",
            "Marathi Courier" to "भारतीय टपाल: पत्ता अपूर्ण असल्यामुळे तुमचे पार्सल रोखण्यात आले आहे. त्वरित पत्ता अपडेट करा: http://post.xyz",

            // 5. Tamil (தமிழ்)
            "Tamil Electricity" to "மின் நுகர்வோர் கவனத்திற்கு: உங்கள் மின் கட்டணம் செலுத்தாததால் இன்று இரவு மின்சாரம் துண்டிக்கப்படும். தொடர்பு கொள்ளவும் 9876543210",
            "Tamil Bank KYC" to "உங்கள் வங்கி கணக்கு முடக்கப்படும். உடனே பான் கார்டு மற்றும் ஆதார் புதுப்பிக்கவும்: http://bank-kyc.top",
            "Tamil Lottery" to "வாழ்த்துகள்! நீங்கள் லாட்டரி பரிசாக 25 லட்சம் ரூபாய் வென்றீர்கள். பெற தொடர்பு கொள்ளவும்",
            "Tamil Job" to "பகுதி நேரம் வீட்டிலிருந்தே வேலை செய்து ஒரு நாளைக்கு 3000 ரூபாய் சம்பாதிக்கலாம். தொடர்பு கொள்ளவும்",

            // 6. Telugu (తెలుగు)
            "Telugu Electricity" to "విద్యుత్ బిల్లు చెల్లించనందున ఈ రాత్రి మీ కరెంట్ నిలిపివేయబడుతుంది. వెంటనే సంప్రదించండి 9876543210",
            "Telugu Bank KYC" to "మీ బ్యాంక్ ఖాతా బ్లాక్ చేయబడుతుంది. వెంటనే పాన్ కార్డ్ మరియు ఆధార్ అప్‌డేట్ చేయండి: http://telugu-sbi.top",
            "Telugu Lottery" to "అభినందనలు! మీరు లక్కీ డ్రాలో 25 లక్షలు గెలుచుకున్నారు. క్లెయిమ్ చేయడానికి సంప్రదించండి",
            "Telugu Job" to "ఇంటి వద్ద నుండే పార్ట్ టైమ్ పని చేసి రోజుకు 3000 రూపాయలు సంపాదించండి. టెలిగ్రామ్ సంప్రదించండి",

            // 7. Kannada (ಕನ್ನಡ)
            "Kannada Electricity" to "ವಿದ್ಯುತ್ ಬಿಲ್ ಪಾವತಿಸದ ಕಾರಣ ಇಂದು ರಾತ್ರಿ ನಿಮ್ಮ ಕರೆಂಟ್ ಸಂಪರ್ಕ ಕಡಿತಗೊಳಿಸಲಾಗುವುದು. ಸಂಪರ್ಕಿಸಿ 9876543210",
            "Kannada Bank KYC" to "ನಿಮ್ಮ ಬ್ಯಾಂಕ್ ಖಾತೆ ನಿರ್ಬಂಧಿಸಲಾಗುತ್ತದೆ. ತಕ್ಷಣ ಪ್ಯಾನ್ ಕಾರ್ಡ್ ಮತ್ತು ಆಧಾರ್ ನವೀಕರಿಸಿ: http://kannada-bank.top",
            "Kannada Lottery" to "ಅಭಿನಂದನೆಗಳು! ನೀವು ಲಾಟರಿಯಲ್ಲಿ 25 ಲಕ್ಷ ರೂಪಾಯಿ ಗೆದ್ದಿದ್ದೀರಿ. ಬಹುಮಾನ ಪಡೆಯಲು ಸಂಪರ್ಕಿಸಿ",
            "Kannada Job" to "ಮನೆಯಿಂದಲೇ ಅರೆಕಾಲಿಕ ಕೆಲಸ ಮಾಡಿ ದಿನಕ್ಕೆ 3000 ಗಳಿಸಿ. ಸಂಪರ್ಕಿಸಿ",

            // 8. Malayalam (മലയാളം)
            "Malayalam Electricity" to "കറന്റ് ബിൽ അടക്കാത്തതിനാൽ ഇന്ന് രാത്രി വൈദ്യുതി വിച്ഛേദിക്കും. ഉടൻ ബന്ധപ്പെടുക 9876543210",
            "Malayalam Bank KYC" to "നിങ്ങളുടെ ബാങ്ക് അക്കൗണ്ട് ബ്ലോക്ക് ചെയ്യും. പാൻ കാർഡ് ഉടൻ പുതുക്കുക: http://kerala-bank.top",
            "Malayalam Lottery" to "അഭിനന്ദനങ്ങൾ! നിങ്ങൾ ലോട്ടറി സമ്മാനമായി 25 ലക്ഷം രൂപ നേടി. സമ്മാനം ലഭിക്കാൻ ബന്ധപ്പെടുക",
            "Malayalam Job" to "വീട്ടിലിരുന്ന് പാർട്ട് ടൈം ജോലി ചെയ്ത് പ്രതിദിനം 3000 സമ്പാദിക്കാം. ബന്ധപ്പെടുക"
        )

        for ((testName, scamText) in multilingualScamCases) {
            val result = ScamAnalyzer.checkFastDeterministicScam(scamText)
            assertNotNull("[$testName] Scam message must be flagged by deterministic keywords: '$scamText'", result)
            assertEquals("[$testName] Result type must be Alert", "Alert", result?.first)
            assertTrue("[$testName] Confidence must be >= 0.90", (result?.second ?: 0f) >= 0.90f)
        }
    }

    @Test
    fun testMultilingualLegitimateMessagesNotFlagged() {
        val legitimateMultilingualCases = listOf(
            "English Bank Debit" to "INR 450.00 debited from A/C **1234 on 14-OCT-26 at ZOMATO UPI. Avl Bal: INR 12,450.00. Not you? SMS BLOCK to 5676712.",
            "English Google OTP" to "G-492104 is your Google verification code. Never share this code with anyone.",
            "English Casual Chat" to "Hey beta, remember to bring groceries on your way back from office. Call me when you leave.",
            "English Work Calendar" to "The project sync has been rescheduled to 4 PM on Google Meet. See you there.",
            "Hindi Casual Chat" to "अरे भाई, शाम को घर आते समय सब्जी ले आना। भूलना मत।",
            "Marathi Casual Chat" to "आई, मी संध्याकाळी 7 वाजता घरी येतो, काळजी करू नकोस.",
            "Tamil Casual Chat" to "தம்பி, வீட்டிற்கு வரும்போது பால் வாங்கி வா.",
            "Telugu Casual Chat" to "అన్నయ్య, సాయంత్రం ఇంటికి వచ్చేటప్పుడు పండ్లు తీసుకురా.",
            "Kannada Casual Chat" to "ಅಮ್ಮಾ, ಸಂಜೆ ಮನೆಗೆ ಬರುವಾಗ ಹಾಲು ತರುತ್ತೇನೆ.",
            "Malayalam Casual Chat" to "ചേട്ടാ, വൈകുന്നേരം വരുമ്പോൾ കടയിൽ നിന്ന് സാധനങ്ങൾ വാങ്ങണം.",
            "Hinglish Casual Chat" to "Bhai kal subah college kitne baje aana hai bata dena."
        )

        for ((testName, safeText) in legitimateMultilingualCases) {
            val result = ScamAnalyzer.checkFastDeterministicScam(safeText)
            assertNull("[$testName] Legitimate message must NOT trigger deterministic scam alert: '$safeText' -> $result", result)
        }
    }

    @Test
    fun testLegitimateMessagesDoNotTriggerAlert() {
        val legitimateBank = "INR 450.00 debited from A/C **1234 on 14-OCT-26 at ZOMATO UPI. Avl Bal: INR 12,450.00. Not you? SMS BLOCK to 5676712."
        val resultBank = ScamAnalyzer.checkFastDeterministicScam(legitimateBank)
        assertNull("Legitimate bank debit SMS must NOT trigger scam alert", resultBank)

        val legitimateOtp = "G-492104 is your Google verification code. Never share this code with anyone."
        val resultOtp = ScamAnalyzer.checkFastDeterministicScam(legitimateOtp)
        assertNull("Legitimate OTP receipt must NOT trigger scam alert", resultOtp)

        val casualChat = "Hey beta, remember to bring groceries on your way back from office. Call me when you leave."
        val resultChat = ScamAnalyzer.checkFastDeterministicScam(casualChat)
        assertNull("Casual personal chat must NOT trigger scam alert", resultChat)

        val workCalendar = "The project sync has been rescheduled to 4 PM on Google Meet. See you there."
        val resultMeeting = ScamAnalyzer.checkFastDeterministicScam(workCalendar)
        assertNull("Work meeting notification must NOT trigger scam alert", resultMeeting)
    }

    @Test
    fun testSpoofedBrandUrlObfuscationsAreFlagged() {
        val spoofedUrls = listOf(
            "flipkart:priyashopping.xyz",
            "https://flipkart:priyashopping.xyz",
            "flipkart:https://priyashopping.xyz",
            "https://flipkart.com@priyashopping.xyz",
            "flipkart.com@priyashopping.xyz",
            "flipkart:offer@priyashopping.xyz",
            "flipkart.priyashopping.xyz",
            "shop.flipkart.priyashopping.xyz",
            "flipkartpriya.xyz",
            "flipkartsale.xyz",
            "priyaflipkart.in",
            "amazonsale.shop",
            "priyashopping.xyz/flipkart",
            "https://google-verify.xyz/search?q=login"
        )

        for (url in spoofedUrls) {
            val scan = FraudDetectionEngine.analyze(url, url)
            assertTrue("Expected score >= 50 for spoofed URL '$url', got ${scan.score} (${scan.reasons})", scan.score >= 50)

            val msgAlert = ScamAnalyzer.checkFastDeterministicScam("Claim your deal at $url")
            assertNotNull("Message containing spoofed URL '$url' must be flagged", msgAlert)
        }
    }

    @Test
    fun testSpoofedDomainAfterSafeUrlInMessageIsCaught() {
        val message = "Compare prices on https://www.flipkart.com or claim offer at flipkart:priyashopping.xyz"
        val result = ScamAnalyzer.checkFastDeterministicScam(message)
        assertNotNull("Message hiding spoofed URL after legitimate URL must be flagged", result)
        assertEquals("EdgeGuard WebShield", result?.first)
    }

    @Test
    fun testBrandImpersonationWithSpaceColonOrPaytmIsFlagged() {
        val flipkartColonText = ScamAnalyzer.checkFastDeterministicScam("Flipkart: priyashopping.xyz")
        assertNotNull("Flipkart colon prefix with burner domain must be flagged", flipkartColonText)

        val paytmColonText = ScamAnalyzer.checkFastDeterministicScam("Paytm: getbonus.xyz")
        assertNotNull("Paytm colon prefix with burner domain must be flagged", paytmColonText)
    }

    @Test
    fun testLegitimateWebUrlsAndEmailsDoNotFalsePositive() {
        val safeCases = listOf(
            "https://www.flipkart.com/big-billion-days" to "",
            "https://fkrt.it/abc123" to "",
            "https://www.google.com/search?q=flipkart:priyashopping.xyz" to "",
            "https://gmail.com" to "",
            "https://pineapple.com" to "",
            "https://amazonia.org" to "",
            "https://app.netlify.com" to "",
            "https://pay.acmecorp.com" to "",
            "https://art.gallery.org" to "",
            "https://apply.indeed.com" to "",
            "https://green-apple-recipes.com" to "",
            "https://myblog.com/auth/google" to "",
            "https://myblog.xyz" to "I bought this microphone on Amazon last week"
        )

        for ((url, text) in safeCases) {
            val scan = FraudDetectionEngine.analyze(url, text)
            assertTrue("Legitimate URL '$url' must score < 50, got ${scan.score} (${scan.reasons})", scan.score < 50)
        }

        val safeMessages = listOf(
            "Email your resume to parv335@gmail.com or john.doe@acmecorp.com by tomorrow.",
            "Please review invoice.pdf before our 10:30 standup.",
            "Check https://app.netlify.com for the latest preview deployment."
        )
        for (msg in safeMessages) {
            val result = ScamAnalyzer.checkFastDeterministicScam(msg)
            assertNull("Safe message must not trigger alert: '$msg' -> $result", result)
        }
    }

    @Test
    fun testRevisedAlertCopyCatalogue() {
        // #1 Bank / KYC
        val (kycTitle, kycBody) = AlertCardBuilder.resolveScamCopy("Dear customer, your bank account is suspended due to PAN KYC. Update immediately.")
        assertEquals("POTENTIAL SCAM ALERT", kycTitle)
        assertTrue(kycBody.contains("KYC or account-block scam"))

        // #2 Electricity
        val (elecTitle, elecBody) = AlertCardBuilder.resolveScamCopy("Your electricity power will be disconnected tonight at 9:30 due to unpaid bill.")
        assertEquals("POSSIBLE UTILITY SCAM", elecTitle)
        assertTrue(elecBody.contains("fake power-disconnection threat"))

        // #3 Prize / Lottery
        val (lotteryTitle, lotteryBody) = AlertCardBuilder.resolveScamCopy("Congratulations! You won 25 Lakh in KBC lottery prize lucky draw.")
        assertEquals("SUSPICIOUS PRIZE MESSAGE", lotteryTitle)
        assertTrue(lotteryBody.contains("lottery or prize scam"))

        // #4 Job Offer
        val (jobTitle, jobBody) = AlertCardBuilder.resolveScamCopy("Earn 5000 daily per day by simple YouTube part time tasks.")
        assertEquals("SUSPICIOUS JOB OFFER", jobTitle)
        assertTrue(jobBody.contains("part-time task or job scam"))

        // #5 Challan APK
        val (challanTitle, challanBody) = AlertCardBuilder.resolveScamCopy("Pay traffic echallan fine immediately", isApk = true)
        assertEquals("SUSPICIOUS APK FILE", challanTitle)
        assertTrue(challanBody.contains("unverified challan APK file"))

        // #6 Sextortion / Digital Arrest
        val (arrestTitle, arrestBody) = AlertCardBuilder.resolveScamCopy("CBI police digital arrest warrant issued against you.")
        assertEquals("POSSIBLE IMPERSONATION SCAM", arrestTitle)
        assertTrue(arrestBody.contains("fake legal or “digital arrest” threat"))

        // #7 India Post / Courier
        val (postTitle, postBody) = AlertCardBuilder.resolveScamCopy("India Post courier package delivery failed. Update address.")
        assertEquals("SUSPICIOUS DELIVERY LINK", postTitle)
        assertTrue(postBody.contains("courier or address-update scam"))

        // #8 Remote Access
        val (ratTitle, ratBody) = AlertCardBuilder.resolveScamCopy("Download AnyDesk to fix bank issue.")
        assertEquals("OTP & REMOTE ACCESS WARNING", ratTitle)
        assertTrue(ratBody.contains("ask for your OTP or remote access"))

        // #9 Pre-Approved Loan
        val (loanTitle, loanBody) = AlertCardBuilder.resolveScamCopy("Your pre-approved loan of 500000 is credited. Click link to claim.")
        assertEquals("SUSPICIOUS FINANCIAL OFFER", loanTitle)
        assertTrue(loanBody.contains("phishing link disguised as a loan"))

        // #10 Fake Credit
        val (creditTitle, creditBody) = AlertCardBuilder.resolveScamCopy("A/C 123 credited with 50,000", isFakeCredit = true)
        assertEquals("UNVERIFIED CREDIT MESSAGE", creditTitle)
        assertTrue(creditBody.contains("fake credit alert"))

        // #11 Govt WebShield
        val (govtTitle, govtBody) = AlertCardBuilder.resolveScamCopy("fake portal", isFakeWebsite = true, fraudCategory = "GOVERNMENT")
        assertEquals("UNVERIFIED GOVT WEBSITE", govtTitle)
        assertTrue(govtBody.contains("unofficial government portal"))

        // #12 Bank WebShield
        val (bankTitle, bankBody) = AlertCardBuilder.resolveScamCopy("fake bank login", isFakeWebsite = true, fraudCategory = "BANK")
        assertEquals("UNVERIFIED BANKING PAGE", bankTitle)
        assertTrue(bankBody.contains("phishing page"))

        // #13 Brand Impersonation / Website warning
        val (webTitle, webBody) = AlertCardBuilder.resolveScamCopy("phishing site", isFakeWebsite = true)
        assertEquals("SUSPICIOUS WEBSITE WARNING", webTitle)
        assertTrue(webBody.contains("Double-check the web address"))

        // Case 1: Unknown website with payment page
        val (paymentTitle, paymentBody) = AlertCardBuilder.resolveScamCopy("Unknown merchant checkout", isFakeWebsite = true, fraudCategory = "PAYMENT")
        assertEquals("UNVERIFIED PAYMENT PAGE", paymentTitle)
        assertTrue(paymentBody.contains("Check for the originality of the website before making any payment"))

        // Case 2: Similar domain to big company (Fake Website)
        val (fakeTitle, fakeBody) = AlertCardBuilder.resolveScamCopy("Impersonating big company", isFakeWebsite = true, fraudCategory = "BRAND_IMPERSONATION")
        assertEquals("FAKE WEBSITE WARNING", fakeTitle)
        assertTrue(fakeBody.contains("similar to a known company and looks like a fake website"))

        // #14 General AI
        val (generalTitle, generalBody) = AlertCardBuilder.resolveScamCopy("Random suspicious message")
        assertEquals("POTENTIAL SCAM ALERT", generalTitle)
        assertTrue(generalBody.contains("matches patterns commonly seen in scams"))
    }

    @Test
    fun testWebsiteDetectionTwoCases() {
        // Case 1: Unknown website with payment page
        val unknownPaymentUrl = "https://freshstore99.in/checkout"
        val paymentScreenText = "Order Summary: 1 Item. Total: ₹ 899. Proceed to pay with card or enter UPI ID."
        val paymentScan = FraudDetectionEngine.analyze(unknownPaymentUrl, paymentScreenText)
        assertEquals("PAYMENT", paymentScan.fraudCategory)
        assertTrue("Payment page must score >= 50", paymentScan.score >= 50)
        assertTrue(paymentScan.reasons.any { it.contains("Check for the originality of the website before making any payment") })

        val (c1Title, c1Body) = AlertCardBuilder.resolveScamCopy(
            paymentScan.reasons.joinToString("\n"),
            isFakeWebsite = true,
            fraudCategory = paymentScan.fraudCategory
        )
        assertEquals("UNVERIFIED PAYMENT PAGE", c1Title)
        assertTrue(c1Body.contains("Check for the originality of the website before making any payment"))

        // Case 2: Similar domain name as a big company (Fake Website)
        val fakeBrandUrls = listOf(
            "https://flipkartsale.xyz/deals",
            "https://amazonsale.shop/products",
            "https://priyaflipkart.in/offer"
        )
        for (fakeUrl in fakeBrandUrls) {
            val brandScan = FraudDetectionEngine.analyze(fakeUrl, "Mega discounts on top brands")
            assertEquals("BRAND_IMPERSONATION", brandScan.fraudCategory)
            assertTrue("Fake brand website must score >= 50", brandScan.score >= 50)

            val (c2Title, c2Body) = AlertCardBuilder.resolveScamCopy(
                brandScan.reasons.joinToString("\n"),
                isFakeWebsite = true,
                fraudCategory = brandScan.fraudCategory
            )
            assertEquals("FAKE WEBSITE WARNING", c2Title)
            assertTrue(c2Body.contains("similar to a known company and looks like a fake website"))
        }

        // Verified authentic payment gateway must NOT be flagged
        val razorpayScan = FraudDetectionEngine.analyze("https://api.razorpay.com/v1/checkout.js", "Payment processing gateway")
        assertEquals(0, razorpayScan.score)
        val cashfreeScan = FraudDetectionEngine.analyze("https://cashfree.com/pay", "Secure payment checkout")
        assertEquals(0, cashfreeScan.score)
    }

    @Test
    fun testAlertLanguageTranslations() {
        val testContent = "Dear customer, your SBI account is blocked due to KYC. Update now."
        
        // English
        val (enTitle, enBody) = AlertCardBuilder.resolveScamCopy(testContent, language = AlertLanguage.ENGLISH)
        assertEquals("POTENTIAL SCAM ALERT", enTitle)
        assertTrue(enBody.contains("KYC or account-block scam"))
        assertEquals("DISMISS", AlertTranslationManager.getDismissButton(AlertLanguage.ENGLISH))
        assertEquals("LOOKS SAFE", AlertTranslationManager.getLooksSafeButton(AlertLanguage.ENGLISH))

        // Hindi
        val (hiTitle, hiBody) = AlertCardBuilder.resolveScamCopy(testContent, language = AlertLanguage.HINDI)
        assertEquals("संदिग्ध बैंक केवाईसी / ब्लॉक स्कैम", hiTitle)
        assertTrue(hiBody.contains("बैंक केवाईसी या खाता बंद करने का फर्जी झांसा"))
        assertEquals("खारिज करें", AlertTranslationManager.getDismissButton(AlertLanguage.HINDI))
        assertEquals("सुरक्षित दिखता है", AlertTranslationManager.getLooksSafeButton(AlertLanguage.HINDI))

        // Marathi
        val (mrTitle, mrBody) = AlertCardBuilder.resolveScamCopy(testContent, language = AlertLanguage.MARATHI)
        assertEquals("बँक केवायसी / खाते ब्लॉक इशारा", mrTitle)
        assertTrue(mrBody.contains("बँक केवायसी किंवा खाते बंद करण्याचा बनावट इशारा"))
        assertEquals("रद्द करा", AlertTranslationManager.getDismissButton(AlertLanguage.MARATHI))
        assertEquals("सुरक्षित दिसते", AlertTranslationManager.getLooksSafeButton(AlertLanguage.MARATHI))

        // Kannada
        val (knTitle, knBody) = AlertCardBuilder.resolveScamCopy(testContent, language = AlertLanguage.KANNADA)
        assertEquals("ಬ್ಯಾಂಕ್ ಕೆವೈಸಿ / ಖಾತೆ ಬ್ಲಾಕ್ ಎಚ್ಚರಿಕೆ", knTitle)
        assertTrue(knBody.contains("ಕೆವೈಸಿ ಅಥವಾ ಬ್ಯಾಂಕ್ ಖಾತೆ ನಿರ್ಬಂಧಿಸುವ ನಕಲಿ ಸಂದೇಶ"))
        assertEquals("ತಿರಸ್ಕರಿಸಿ", AlertTranslationManager.getDismissButton(AlertLanguage.KANNADA))
        assertEquals("ಸುರಕ್ಷಿತವಾಗಿದೆ", AlertTranslationManager.getLooksSafeButton(AlertLanguage.KANNADA))

        // Tamil
        val (taTitle, taBody) = AlertCardBuilder.resolveScamCopy(testContent, language = AlertLanguage.TAMIL)
        assertEquals("வங்கி KYC / கணக்கு முடக்கம் எச்சரிக்கை", taTitle)
        assertTrue(taBody.contains("வங்கி கேஒய்சி (KYC) அல்லது கணக்கு முடக்கப்படும்"))
        assertEquals("விலக்கு", AlertTranslationManager.getDismissButton(AlertLanguage.TAMIL))
        assertEquals("பாதுகாப்பானது", AlertTranslationManager.getLooksSafeButton(AlertLanguage.TAMIL))

        // Telugu
        val (teTitle, teBody) = AlertCardBuilder.resolveScamCopy(testContent, language = AlertLanguage.TELUGU)
        assertEquals("బ్యాంక్ కేవైసీ / ఖాతా బ్లాక్ హెచ్చరిక", teTitle)
        assertTrue(teBody.contains("బ్యాంక్ కేవైసీ లేదా ఖాతా బ్లాక్ అయ్యే నకిలీ బెదిరింపు"))
        assertEquals("రద్దు చేయి", AlertTranslationManager.getDismissButton(AlertLanguage.TELUGU))
        assertEquals("సురక్షితంగా ఉంది", AlertTranslationManager.getLooksSafeButton(AlertLanguage.TELUGU))
    }
@Test
    fun testOfficialDltSmsSenderFiltering() {
        val serviceAndPromotionalSenders = listOf(
            "ABS-XYSB-P",
            "VM-FLPKRT-P",
            "AD-SWIGGY-P",
            "BZ-MYNTRA-P",
            "JX-AIRTEL-P",
            "123456-P",
            "XY-ABC-P",
            "abs-xysb-p",
            "AD-DEALS-P ",
            "A-HXCSDS-S",
            "VM-AIRTEL-S",
            "AX-ZOMATO-S",
            "HXCSDS-S",
            "BW-JIOFIB-S",
            "A-HXCSDS-S ",
            "AD-GOVIND-G",
            "VK-COWIN-G"
        )
        for (sender in serviceAndPromotionalSenders) {
            assertTrue("Sender should be identified as service/promo: $sender", ContactUtils.isOfficialServiceOrPromoSmsSender(sender))
            assertFalse("Sender should NOT be identified as transactional bank: $sender", ContactUtils.isTransactionalBankSmsSender(sender))
        }

        val transactionalBankSenders = listOf(
            "xyz-xyz-T",
            "AD-HDFCBK-T",
            "VK-SBIINB-T",
            "AX-ICICIB-T",
            "HDFCBK-T",
            "VM-SBIBNK-T",
            "ad-hdfcbk-t"
        )
        for (sender in transactionalBankSenders) {
            assertTrue("Sender should be identified as transactional bank: $sender", ContactUtils.isTransactionalBankSmsSender(sender))
            assertFalse("Sender should NOT be identified as service/promo: $sender", ContactUtils.isOfficialServiceOrPromoSmsSender(sender))
        }

        val nonDltSenders = listOf(
            "+919876543210",
            "9876543210",
            "Google",
            "Unknown Sender",
            "HDFCBK",
            "SBIINB"
        )
        for (sender in nonDltSenders) {
            assertFalse("Sender should NOT be identified as service/promo: $sender", ContactUtils.isOfficialServiceOrPromoSmsSender(sender))
            assertFalse("Sender should NOT be identified as transactional bank: $sender", ContactUtils.isTransactionalBankSmsSender(sender))
        }
    }
}
