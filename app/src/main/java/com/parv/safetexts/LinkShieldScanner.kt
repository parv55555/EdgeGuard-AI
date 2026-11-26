package com.parv.safetexts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class LinkShieldScanResult(
    val isScam: Boolean,
    val riskScore: Int,
    val reason: String,
    val isDangerousDownload: Boolean = false,
    val targetUrl: String = ""
)

object LinkShieldScanner {

    private const val MAX_BODY_BYTES = 150 * 1024
    private const val NETWORK_TIMEOUT_MS = 2500L
    private const val WEBVIEW_TIMEOUT_MS = 2000L

    private val DANGEROUS_MIME_TYPES = setOf(
        "application/vnd.android.package-archive",
        "application/octet-stream",
        "application/zip",
        "application/x-zip-compressed",
        "application/x-msdownload",
        "application/x-dex"
    )

    private val DANGEROUS_EXTENSIONS = listOf(
        ".apk",
        ".zip",
        ".exe",
        ".dex",
        ".iso"
    )

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(3000L, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun scan(context: Context, rawUrl: String): LinkShieldScanResult {
        if (LinkShieldAllowlist.isAllowlisted(rawUrl)) {
            return LinkShieldScanResult(
                isScam = false,
                riskScore = 0,
                reason = "Verified safe domain",
                targetUrl = rawUrl
            )
        }

        val urlHeuristic = FraudDetectionEngine.analyze(rawUrl, "")
        if (urlHeuristic.score >= 50) {
            return LinkShieldScanResult(
                isScam = true,
                riskScore = urlHeuristic.score,
                reason = urlHeuristic.reasons.firstOrNull() ?: "Looks like an impersonated or high-risk link",
                targetUrl = rawUrl
            )
        }

        var finalUrl = rawUrl
        var htmlContent = ""
        var isNetworkSuccess = false

        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(rawUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    finalUrl = response.request.url.toString()
                    val contentType = response.header("Content-Type")?.lowercase(Locale.ROOT) ?: ""
                    val lowerFinalUrl = finalUrl.lowercase(Locale.ROOT)

                    if (DANGEROUS_MIME_TYPES.any { contentType.contains(it) } ||
                        DANGEROUS_EXTENSIONS.any { lowerFinalUrl.endsWith(it) }
                    ) {
                        return@withContext
                    }

                    if (finalUrl != rawUrl) {
                        val redirectHeuristic = FraudDetectionEngine.analyze(finalUrl, "")
                        if (redirectHeuristic.score >= 50) {
                            htmlContent = ""
                            return@withContext
                        }
                    }

                    val responseBody = response.body
                    if (responseBody != null) {
                        val buffer = ByteArray(4096)
                        val out = ByteArrayOutputStream(MAX_BODY_BYTES)
                        var bytesReadTotal = 0
                        responseBody.byteStream().use { input ->
                            while (bytesReadTotal < MAX_BODY_BYTES) {
                                val toRead = minOf(buffer.size, MAX_BODY_BYTES - bytesReadTotal)
                                val read = input.read(buffer, 0, toRead)
                                if (read <= 0) break
                                out.write(buffer, 0, read)
                                bytesReadTotal += read
                            }
                        }
                        htmlContent = out.toString("UTF-8")
                        isNetworkSuccess = true
                    }
                }
            } catch (_: Exception) {
            }
        }

        val lowerFinalUrl = finalUrl.lowercase(Locale.ROOT)
        if (DANGEROUS_EXTENSIONS.any { lowerFinalUrl.endsWith(it) }) {
            return LinkShieldScanResult(
                isScam = true,
                riskScore = 100,
                reason = "Executable or dangerous file download detected",
                isDangerousDownload = true,
                targetUrl = finalUrl
            )
        }

        if (finalUrl != rawUrl) {
            val redirectHeuristic = FraudDetectionEngine.analyze(finalUrl, "")
            if (redirectHeuristic.score >= 50) {
                return LinkShieldScanResult(
                    isScam = true,
                    riskScore = redirectHeuristic.score,
                    reason = redirectHeuristic.reasons.firstOrNull() ?: "Redirects to a suspicious destination",
                    targetUrl = finalUrl
                )
            }
        }

        var visibleTextLength = 0
        if (isNetworkSuccess && htmlContent.isNotBlank()) {
            val doc = Jsoup.parse(htmlContent, finalUrl)
            val title = doc.title().trim()
            val metaDesc = doc.select("meta[name=description]").attr("content").trim()
            val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
            val ogDesc = doc.select("meta[property=og:description]").attr("content").trim()
            val headings = doc.select("h1, h2, h3").eachText().joinToString(" ").trim()
            val formLabels = doc.select("button, input[type=submit], input[type=button], label").eachText().joinToString(" ").trim()
            val bodyText = (doc.body()?.text() ?: "").trim()
            visibleTextLength = bodyText.length

            val rawExtracted = listOf(title, metaDesc, ogTitle, ogDesc, headings, formLabels, bodyText)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val extractedText = TFLiteScamClassifier.removeEmojis(rawExtracted).trim()

            if (extractedText.isNotBlank()) {
                val contentHeuristic = FraudDetectionEngine.analyze(finalUrl, extractedText)
                if (contentHeuristic.score >= 50) {
                    return LinkShieldScanResult(
                        isScam = true,
                        riskScore = contentHeuristic.score,
                        reason = contentHeuristic.reasons.firstOrNull() ?: "Webpage content looks like a scam",
                        targetUrl = finalUrl
                    )
                }

                val fastScam = ScamAnalyzer.checkFastDeterministicScam(extractedText)
                if (fastScam != null) {
                    return LinkShieldScanResult(
                        isScam = true,
                        riskScore = (fastScam.second * 100).toInt(),
                        reason = "Looks like a phishing or financial scam page",
                        targetUrl = finalUrl
                    )
                }

                val (tfliteResult, tfliteProb) = ScamAnalyzer.getInstance(context).classifyText(extractedText)
                if (tfliteResult == "Alert" || tfliteProb >= 0.70f) {
                    return LinkShieldScanResult(
                        isScam = true,
                        riskScore = (tfliteProb * 100).toInt(),
                        reason = "Page text matches scam patterns detected by EdgeGuard AI",
                        targetUrl = finalUrl
                    )
                }
            }
        }

        if (visibleTextLength < 50) {
            val renderedJsText = extractJsRenderedText(context, finalUrl)
            val cleanJsText = TFLiteScamClassifier.removeEmojis(renderedJsText).trim()
            if (cleanJsText.isNotBlank()) {
                val jsContentHeuristic = FraudDetectionEngine.analyze(finalUrl, cleanJsText)
                if (jsContentHeuristic.score >= 50) {
                    return LinkShieldScanResult(
                        isScam = true,
                        riskScore = jsContentHeuristic.score,
                        reason = jsContentHeuristic.reasons.firstOrNull() ?: "Webpage content looks like a scam",
                        targetUrl = finalUrl
                    )
                }

                val jsFastScam = ScamAnalyzer.checkFastDeterministicScam(cleanJsText)
                if (jsFastScam != null) {
                    return LinkShieldScanResult(
                        isScam = true,
                        riskScore = (jsFastScam.second * 100).toInt(),
                        reason = "Looks like a phishing or financial scam page",
                        targetUrl = finalUrl
                    )
                }

                val (jsTfliteResult, jsTfliteProb) = ScamAnalyzer.getInstance(context).classifyText(cleanJsText)
                if (jsTfliteResult == "Alert" || jsTfliteProb >= 0.70f) {
                    return LinkShieldScanResult(
                        isScam = true,
                        riskScore = (jsTfliteProb * 100).toInt(),
                        reason = "Page text matches scam patterns detected by EdgeGuard AI",
                        targetUrl = finalUrl
                    )
                }
            }
        }

        return LinkShieldScanResult(
            isScam = false,
            riskScore = 0,
            reason = "Safe",
            targetUrl = finalUrl
        )
    }

    private suspend fun extractJsRenderedText(context: Context, url: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context.applicationContext)
            var isFinished = false

            val cleanup = {
                if (!isFinished) {
                    isFinished = true
                    try {
                        webView.stopLoading()
                        webView.clearHistory()
                        webView.clearCache(true)
                        webView.loadUrl("about:blank")
                        webView.destroy()
                    } catch (_: Exception) {
                    }
                }
            }

            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!isFinished) {
                    cleanup()
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
            }
            handler.postDelayed(timeoutRunnable, WEBVIEW_TIMEOUT_MS)

            webView.settings.apply {
                javaScriptEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                setGeolocationEnabled(false)
                domStorageEnabled = false
                databaseEnabled = false
            }

            try {
                CookieManager.getInstance().setAcceptCookie(false)
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
            } catch (_: Exception) {
            }

            webView.setDownloadListener { _, _, _, _, _ -> }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                    if (isFinished) return
                    webView.evaluateJavascript("(function() { return (document.body ? document.body.innerText : ''); })();") { rawResult ->
                        if (isFinished) return@evaluateJavascript
                        handler.removeCallbacks(timeoutRunnable)
                        cleanup()
                        val unquoted = if (rawResult != null && rawResult.length >= 2 && rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                            try {
                                JSONObject("{\"text\":$rawResult}").optString("text", "")
                            } catch (_: Exception) {
                                rawResult.removeSurrounding("\"")
                            }
                        } else {
                            rawResult ?: ""
                        }
                        if (continuation.isActive) {
                            continuation.resume(unquoted.trim())
                        }
                    }
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    if (isFinished) return
                    handler.removeCallbacks(timeoutRunnable)
                    cleanup()
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
            }

            continuation.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
                cleanup()
            }

            try {
                webView.loadUrl(url)
            } catch (_: Exception) {
                handler.removeCallbacks(timeoutRunnable)
                cleanup()
                if (continuation.isActive) {
                    continuation.resume("")
                }
            }
        }
    }
}
