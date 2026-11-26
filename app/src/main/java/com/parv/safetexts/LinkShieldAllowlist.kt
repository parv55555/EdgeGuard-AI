package com.parv.safetexts

import android.net.Uri
import java.util.Locale

object LinkShieldAllowlist {
    private val VERIFIED_DOMAINS = setOf(
        "google.com",
        "google.co.in",
        "google.in",
        "youtube.com",
        "youtu.be",
        "wikipedia.org",
        "wikimedia.org",
        "github.com",
        "microsoft.com",
        "apple.com",
        "android.com",
        "play.google.com",
        "support.google.com",
        "maps.google.com",
        "accounts.google.com",
        "docs.google.com",
        "drive.google.com",
        "amazon.in",
        "amazon.com",
        "flipkart.com",
        "irctc.co.in",
        "rbi.org.in",
        "uidai.gov.in",
        "incometax.gov.in",
        "epfindia.gov.in",
        "linkedin.com",
        "twitter.com",
        "x.com",
        "instagram.com",
        "facebook.com",
        "whatsapp.com",
        "reddit.com",
        "bing.com",
        "duckduckgo.com",
        "yahoo.com"
    )

    fun isAllowlisted(rawUrl: String): Boolean {
        if (rawUrl.isBlank()) return false
        val host = try {
            val fromEngine = FraudDetectionEngine.extractDomain(rawUrl)
            if (fromEngine.isNotBlank()) fromEngine else (Uri.parse(rawUrl).host ?: "")
        } catch (_: Exception) {
            FraudDetectionEngine.extractDomain(rawUrl)
        }.lowercase(Locale.ROOT).trim()

        val cleanHost = if (host.startsWith("www.")) host.substring(4) else host
        if (cleanHost.isBlank()) return false

        if (VERIFIED_DOMAINS.any { cleanHost == it || cleanHost.endsWith(".$it") }) {
            return true
        }

        if (FraudDetectionEngine.isAuthenticGovernmentDomain(cleanHost)) {
            return true
        }

        if (FraudDetectionEngine.PROTECTED_BRANDS.any { (officialDomain, _) ->
                cleanHost == officialDomain || cleanHost.endsWith(".$officialDomain")
            }) {
            return true
        }

        if (FraudDetectionEngine.VERIFIED_SEARCH_ENGINES.any { cleanHost == it || cleanHost.endsWith(".$it") }) {
            return true
        }

        if (FraudDetectionEngine.VERIFIED_TRUSTED_PLATFORMS.any { cleanHost == it || cleanHost.endsWith(".$it") }) {
            return true
        }

        if (FraudDetectionEngine.VERIFIED_PAYMENT_GATEWAYS.any { cleanHost == it || cleanHost.endsWith(".$it") }) {
            return true
        }

        return false
    }
}
