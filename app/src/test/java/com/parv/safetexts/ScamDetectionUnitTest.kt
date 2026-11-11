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
}
