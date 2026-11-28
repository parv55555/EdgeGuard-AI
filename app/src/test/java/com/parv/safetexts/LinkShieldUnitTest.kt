package com.parv.safetexts

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkShieldUnitTest {

    @Test
    fun testAllowlistVerifiedDomains() {
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://google.com"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://en.wikipedia.org/wiki/Phishing"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://amazon.in/dp/B012345"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://amazon.com/product/123"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://flipkart.com/item/456"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://sbi.co.in/portal"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://parivahan.gov.in/parivahan"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://accounts.google.com/signin"))
        assertTrue(LinkShieldAllowlist.isAllowlisted("https://github.com/parv55555/webcheck"))
    }

    @Test
    fun testAllowlistRejectsSuspiciousAndScamDomains() {
        assertFalse(LinkShieldAllowlist.isAllowlisted("https://amazon-offer-90percent.xyz"))
        assertFalse(LinkShieldAllowlist.isAllowlisted("https://echallan-parivahan-gov-in.click"))
        assertFalse(LinkShieldAllowlist.isAllowlisted("https://yono-sbi-kyc-update.com"))
        assertFalse(LinkShieldAllowlist.isAllowlisted("https://free-recharge-loot.top"))
        assertFalse(LinkShieldAllowlist.isAllowlisted("https://webcheck-theta.vercel.app"))
        assertFalse(LinkShieldAllowlist.isAllowlisted("https://evil-spoof.org"))
        assertFalse(LinkShieldAllowlist.isAllowlisted(""))
    }

    @Test
    fun testUrlHeuristicsCatchesLookalikesAndScams() {
        val amazonPhish = FraudDetectionEngine.analyze("https://amazon-offer-90percent.xyz", "")
        assertTrue(amazonPhish.score >= 50)

        val sbiPhish = FraudDetectionEngine.analyze("https://sbi-netbanking-verify.xyz", "")
        assertTrue(sbiPhish.score >= 50)

        val govtSpoof = FraudDetectionEngine.analyze("https://parivahan.gov.in.echallan-pay.top", "")
        assertTrue(govtSpoof.score >= 50)
    }

    @Test
    fun testJsoupExtractionAndScamHeuristicsOnWebpage() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Amazon.in: Smartphone 16 Pro Max</title>
                <meta name="description" content="Flash clearance 90% off iPhone" />
                <meta property="og:title" content="Exclusive Deal" />
                <meta property="og:description" content="Unbelievable discount" />
            </head>
            <body>
                <h1>Smartphone 16 Pro Max</h1>
                <div>Limited time deal 90% off ₹13,990</div>
                <div>Only 2 left in stock! Hurry up</div>
                <form>
                    <label>Enter UPI ID</label>
                    <input type="text" value="user@upi" />
                    <button type="submit">Proceed to pay ₹13,990 (Pay with UPI)</button>
                </form>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(sampleHtml, "https://webcheck-theta.vercel.app")
        val title = doc.title()
        val metaDesc = doc.select("meta[name=description]").attr("content")
        val headings = doc.select("h1, h2, h3").eachText().joinToString(" ")
        val buttons = doc.select("button").eachText().joinToString(" ")
        val bodyText = doc.body()?.text() ?: ""

        val combinedText = "$title $metaDesc $headings $buttons $bodyText"
        val analysis = FraudDetectionEngine.analyze("https://webcheck-theta.vercel.app", combinedText)
        assertTrue(analysis.score >= 50)
    }

    @Test
    fun testDangerousDownloadExtensions() {
        val dangerousUrls = listOf(
            "https://update-service.com/patch.apk",
            "https://secure-bank.xyz/portal.zip",
            "https://traffic-challan.site/pay.exe"
        )
        val exts = listOf(".apk", ".zip", ".exe", ".dex", ".iso")
        for (url in dangerousUrls) {
            val isDangerous = exts.any { url.lowercase().endsWith(it) }
            assertTrue(isDangerous)
        }
    }

    @Test
    fun testEmojiIgnoringInScamAnalysis() {
        val scamWithEmojis = "⚡ FLASH CLEARANCE SALE ⚡ 90% OFF on iPhone! Only 2 left 🚨 Claim now 🎁 Pay with UPI 💳"
        val analysis = FraudDetectionEngine.analyze("https://webcheck-theta.vercel.app", scamWithEmojis)
        assertTrue(analysis.score >= 50)
    }

    @Test
    fun testUrlExtractionFromSharedText() {
        val urlRegex = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
        val textWithUrl = "Check out this crazy offer: https://offer-zone.xyz/win?ref=123 before it expires!"
        val match = urlRegex.find(textWithUrl)?.value
        assertEquals("https://offer-zone.xyz/win?ref=123", match)

        val plainTextMessage = "Your electricity power will be disconnected tonight. Call officer at 9876543210 immediately."
        val noMatch = urlRegex.find(plainTextMessage)?.value
        assertEquals(null, noMatch)
    }

    @Test
    fun testNormalWebsitesNotFlagged() {
        val sites = listOf(
            "https://kotlinlang.org" to "Kotlin is a programming language. Learn Kotlin for Android, Multiplatform and Server-Side.",
            "https://stackoverflow.com/questions/12345" to "How to implement Custom Tabs in Android? Sign in to your account with GitHub. Click here to answer.",
            "https://bbc.com/news/technology" to "BBC News: Global tech firms report quarterly earnings. More updates soon.",
            "https://medium.com/@dev/guide" to "Comprehensive guide on modern software packages and IP address management.",
            "https://cloudflare.com/pricing" to "Protect your website traffic from attacks. Pay monthly or annually with transparent pricing."
        )
        for ((url, text) in sites) {
            val res = FraudDetectionEngine.analyze(url, text)
            assertTrue("Expected safe for $url but got score ${res.score} reasons ${res.reasons}", res.score < 50)
        }
    }

    @Test
    fun testUnknownWebsitePaymentAdvisory() {
        val unknownStorePayUrl = "https://custombakerytreats.in/checkout"
        val analysis = FraudDetectionEngine.analyze(unknownStorePayUrl, "Order summary: Strawberry Cake. Total: Rs 550. Proceed to pay.")
        assertEquals("PAYMENT", analysis.fraudCategory)
        assertTrue(analysis.reasons.any { it.contains("verify the originality of website before paying") })
    }
}
