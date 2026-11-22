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
            (lower.contains("electricity") || lower.contains("bijli") || lower.contains("power")) &&
            (lower.contains("disconnect") || lower.contains("cut") || lower.contains("bill") || lower.contains("tonight") || lower.contains("9:30") || lower.contains("unpaid")) -> AlertTranslationManager.ScamType.UTILITY_SCAM
            // #3 Fake Lottery / KBC / Prize Winner
            (lower.contains("kbc") || lower.contains("lottery") || lower.contains("congratulations") || lower.contains("prize") || lower.contains("winner")) &&
            (lower.contains("lakh") || lower.contains("crore") || lower.contains("won") || lower.contains("claim") || lower.contains("lucky")) -> AlertTranslationManager.ScamType.LOTTERY_PRIZE
            // #4 Fake Job / YouTube Task
            (lower.contains("part time") || lower.contains("daily income") || lower.contains("youtube") || lower.contains("like and subscribe") || lower.contains("telegram")) &&
            (lower.contains("per day") || lower.contains("earn") || lower.contains("salary") || lower.contains("daily") || lower.contains("rs.") || lower.contains("inr") || lower.contains("work from home")) -> AlertTranslationManager.ScamType.JOB_OFFER
            // #6 Sextortion / Digital Arrest Text Threats
            (lower.contains("cbi") || lower.contains("narcotics") || lower.contains("cyber crime") || lower.contains("police") || lower.contains("digital arrest")) &&
            (lower.contains("warrant") || lower.contains("arrest") || lower.contains("illegal") || lower.contains("court") || lower.contains("summon") || lower.contains("case file")) -> AlertTranslationManager.ScamType.LEGAL_THREAT
            // #7 India Post / Courier Address Update
            (lower.contains("india post") || lower.contains("post office") || lower.contains("courier") || lower.contains("parcel") || lower.contains("shipment") || lower.contains("package") || lower.contains("delivery")) &&
            (lower.contains("address") || lower.contains("failed") || lower.contains("suspend") || lower.contains("update") || lower.contains("reschedule") || lower.contains("undelivered")) -> AlertTranslationManager.ScamType.COURIER_DELIVERY
            // #8 Remote Access / OTP Coercion
            (lower.contains("anydesk") || lower.contains("teamviewer") || lower.contains("quicksupport") || lower.contains("rustdesk")) ||
            ((lower.contains("otp") || lower.contains("one time password") || lower.contains("verification code")) &&
             (lower.contains("share") || lower.contains("forward") || lower.contains("send to") || lower.contains("tell me")) &&
             (!lower.contains("do not share") && !lower.contains("never share") && !lower.contains("don't share") && !lower.contains("dont share"))) -> AlertTranslationManager.ScamType.OTP_REMOTE_ACCESS
            // #9 Pre-Approved Loan / Cashback
            (lower.contains("loan") || lower.contains("refund") || lower.contains("credit card limit") || lower.contains("cashback")) &&
            (lower.contains("pre-approved") || lower.contains("approved") || lower.contains("credited") || lower.contains("claim now") || lower.contains("apply now")) &&
            (lower.contains("link") || lower.contains("http") || lower.contains("bit.ly") || lower.contains("click")) -> AlertTranslationManager.ScamType.FINANCIAL_OFFER
            // #1 Bank / KYC / PAN / SIM Block
            (lower.contains("kyc") || lower.contains("pan card") || lower.contains("pancard") || lower.contains("aadhaar") || lower.contains("account") || lower.contains("debit card") || lower.contains("sim card") || lower.contains("sim block") || lower.contains("yono") || lower.contains("sbi")) &&
            (lower.contains("block") || lower.contains("suspend") || lower.contains("update immediately") || lower.contains("deactivate") || lower.contains("expire") || lower.contains("click here") || lower.contains("update your")) -> AlertTranslationManager.ScamType.BANK_KYC_BLOCK
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
