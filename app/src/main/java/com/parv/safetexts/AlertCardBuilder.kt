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
        fraudCategory: String? = null
    ): Pair<String, String> {
        val lower = content.lowercase()
        return when {
            // #11 Counterfeit Govt Portal WebShield
            isFakeWebsite && fraudCategory == "GOVERNMENT" -> Pair(
                "UNVERIFIED GOVT WEBSITE",
                "This page looks like an unofficial government portal. Official Indian government sites typically end in .gov.in or .nic.in. Avoid entering personal details or paying here."
            )
            // #12 Counterfeit Bank WebShield
            isFakeWebsite && fraudCategory == "BANK" -> Pair(
                "UNVERIFIED BANKING PAGE",
                "This website does not match an official bank domain and looks like a phishing page. Do NOT enter your NetBanking ID, password, MPIN, or OTP here."
            )
            // #13 Brand Impersonation & Spoofed URLs / Browser spam
            isFakeWebsite || isBrowserSpam -> Pair(
                "SUSPICIOUS WEBSITE WARNING",
                "This link looks suspicious and may be impersonating a known brand. Double-check the web address before entering passwords, phone numbers, or card details."
            )
            // #10 Fake Transaction / Reverse-Credit
            isFakeCredit -> Pair(
                "UNVERIFIED CREDIT MESSAGE",
                "This looks like a fake credit alert. Do NOT transfer money back based on an SMS alone—open your official bank app to check your real balance first."
            )
            // #5 Fake Traffic Challan APK or other APK files
            isApk -> {
                if (lower.contains("challan") || lower.contains("echallan") || lower.contains("traffic") ||
                    lower.contains("parivahan") || lower.contains("fine") || lower.contains("court")) {
                    Pair(
                        "SUSPICIOUS APK FILE",
                        "This looks like an unverified challan APK file. Installing unknown APKs can put your OTPs and bank accounts at risk. Check challans only on the official Parivahan portal."
                    )
                } else {
                    Pair(
                        "SUSPICIOUS APK FILE",
                        "This looks like an unverified challan APK file. Installing unknown APKs can put your OTPs and bank accounts at risk. Check challans only on the official Parivahan portal."
                    )
                }
            }
            // #2 Electricity / Utility Disconnection
            (lower.contains("electricity") || lower.contains("bijli") || lower.contains("power")) &&
            (lower.contains("disconnect") || lower.contains("cut") || lower.contains("bill") || lower.contains("tonight") || lower.contains("9:30") || lower.contains("unpaid")) -> Pair(
                "POSSIBLE UTILITY SCAM",
                "This looks like a fake power-disconnection threat. Utility providers rarely demand urgent payment to personal numbers. Check your official electricity app before paying."
            )
            // #3 Fake Lottery / KBC / Prize Winner
            (lower.contains("kbc") || lower.contains("lottery") || lower.contains("congratulations") || lower.contains("prize") || lower.contains("winner")) &&
            (lower.contains("lakh") || lower.contains("crore") || lower.contains("won") || lower.contains("claim") || lower.contains("lucky")) -> Pair(
                "SUSPICIOUS PRIZE MESSAGE",
                "This message looks like a lottery or prize scam. Be cautious of any prize claim asking for processing fees, GST, or personal details."
            )
            // #4 Fake Job / YouTube Task
            (lower.contains("part time") || lower.contains("daily income") || lower.contains("youtube") || lower.contains("like and subscribe") || lower.contains("telegram")) &&
            (lower.contains("per day") || lower.contains("earn") || lower.contains("salary") || lower.contains("daily") || lower.contains("rs.") || lower.contains("inr") || lower.contains("work from home")) -> Pair(
                "SUSPICIOUS JOB OFFER",
                "This looks like a part-time task or job scam. Be wary of easy daily-income offers—especially any that ask for upfront deposits to unlock earnings."
            )
            // #6 Sextortion / Digital Arrest Text Threats
            (lower.contains("cbi") || lower.contains("narcotics") || lower.contains("cyber crime") || lower.contains("police") || lower.contains("digital arrest")) &&
            (lower.contains("warrant") || lower.contains("arrest") || lower.contains("illegal") || lower.contains("court") || lower.contains("summon") || lower.contains("case file")) -> Pair(
                "POSSIBLE IMPERSONATION SCAM",
                "This message looks like a fake legal or “digital arrest” threat. Police and CBI do not serve arrest warrants over SMS or WhatsApp. Do not panic or transfer money."
            )
            // #7 India Post / Courier Address Update
            (lower.contains("india post") || lower.contains("post office") || lower.contains("courier") || lower.contains("parcel") || lower.contains("shipment") || lower.contains("package") || lower.contains("delivery")) &&
            (lower.contains("address") || lower.contains("failed") || lower.contains("suspend") || lower.contains("update") || lower.contains("reschedule") || lower.contains("undelivered")) -> Pair(
                "SUSPICIOUS DELIVERY LINK",
                "This looks like a courier or address-update scam. Avoid clicking unknown links or paying small redelivery fees by card. Track parcels only on the official courier website."
            )
            // #8 Remote Access / OTP Coercion
            (lower.contains("anydesk") || lower.contains("teamviewer") || lower.contains("quicksupport") || lower.contains("rustdesk")) ||
            ((lower.contains("otp") || lower.contains("one time password") || lower.contains("verification code")) &&
             (lower.contains("share") || lower.contains("forward") || lower.contains("send to") || lower.contains("tell me")) &&
             (!lower.contains("do not share") && !lower.contains("never share") && !lower.contains("don't share") && !lower.contains("dont share"))) -> Pair(
                "OTP & REMOTE ACCESS WARNING",
                "This message looks like an attempt to ask for your OTP or remote access. Bank and police officials never ask for OTPs or screen-sharing apps—do not share them."
            )
            // #9 Pre-Approved Loan / Cashback
            (lower.contains("loan") || lower.contains("refund") || lower.contains("credit card limit") || lower.contains("cashback")) &&
            (lower.contains("pre-approved") || lower.contains("approved") || lower.contains("credited") || lower.contains("claim now") || lower.contains("apply now")) &&
            (lower.contains("link") || lower.contains("http") || lower.contains("bit.ly") || lower.contains("click")) -> Pair(
                "SUSPICIOUS FINANCIAL OFFER",
                "This looks like a phishing link disguised as a loan, refund, or cashback offer. Avoid clicking shortened links and verify offers directly in your official app."
            )
            // #1 Bank / KYC / PAN / SIM Block
            (lower.contains("kyc") || lower.contains("pan card") || lower.contains("pancard") || lower.contains("aadhaar") || lower.contains("account") || lower.contains("debit card") || lower.contains("sim card") || lower.contains("sim block") || lower.contains("yono") || lower.contains("sbi")) &&
            (lower.contains("block") || lower.contains("suspend") || lower.contains("update immediately") || lower.contains("deactivate") || lower.contains("expire") || lower.contains("click here") || lower.contains("update your")) -> Pair(
                "POTENTIAL SCAM ALERT",
                "This message looks like a KYC or account-block scam. Do NOT transfer money, share OTPs, or click links. Verify directly in your official bank or telecom app."
            )
            // #14 General AI / TFLite Message Classifier
            else -> Pair(
                "POTENTIAL SCAM ALERT",
                "This message matches patterns commonly seen in scams. Pause before acting—do not share OTPs, transfer money, or click links without verifying the sender."
            )
        }
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
        val (cardTitle, cardBody) = resolveScamCopy(
            content = content,
            isApk = isApk,
            isFakeCredit = isFakeCredit,
            isBrowserSpam = isBrowserSpam,
            isFakeWebsite = isFakeWebsite,
            fraudCategory = fraudCategory
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
            text = when {
                isApk -> "Source: $sender"
                isBrowserSpam -> "Phishing notification from $sender"
                else -> "From: $sender ($appName)"
            }
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
            text = "DISMISS"
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
            text = "LOOKS SAFE"
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
                text = if (isFakeWebsite) "TRUST THIS SITE" else "TRUST THIS SENDER"
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
                                db.messageDao().updateMessage(msg.copy(analysisResult = "Trusted"))
                            }
                            ScamAnalyzer.getInstance(context).markAsFalsePositive(sender, content)
                            if (isFakeWebsite && !websiteDomain.isNullOrBlank()) {
                                ScamAnalyzer.getInstance(context).markAsFalsePositive(websiteDomain, content)
                            }
                        } catch (e: Exception) {
                            Log.e("AlertCardBuilder", "Failed marking trusted site/sender", e)
                        }
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val toastMsg = if (isFakeWebsite) {
                            "Trusted '$targetToTrust'. This website will no longer trigger scam warnings."
                        } else {
                            "Trusted '$sender'. Messages from this number will no longer be scanned."
                        }
                        android.widget.Toast.makeText(
                            context.applicationContext,
                            toastMsg,
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    onDismiss()
                }
            }
            cardLayout.addView(trustBtn)
        }

        return cardLayout
    }

    fun buildOtpCard(context: Context, onDismiss: () -> Unit): View {
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
            text = "OTP SAFETY REMINDER"
            textSize = 17f
            setTextColor(Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = "A sensitive OTP arrived near an unknown call. Never share OTPs on a call or chat—bank and police officials will never ask for them."
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = "I UNDERSTAND"
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
            text = if (isSevere) "HIGH-RISK SCREEN-SHARE ALERT" else "REMOTE ACCESS CAUTION"
            textSize = 17f
            setTextColor(if (isSevere) Color.parseColor("#B71C1C") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = if (isSevere)
                "A screen-sharing app was detected during or right after an unknown call. If a caller asked you to install this, do NOT grant permissions—it may expose your OTPs and bank balance."
            else
                "Screen-sharing apps let others view or control your phone. Only allow access if you personally know and trust the person."
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = if (isSevere) "I UNDERSTAND THE RISK" else "GOT IT"
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
            text = "POSSIBLE DIGITAL ARREST SCAM"
            textSize = 17f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = "Opening a banking app during a long unknown video call matches a common “Digital Arrest” scam pattern. Police and CBI never arrest people or ask for money over video call. Hang up and verify."
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = "I UNDERSTAND (HANG UP)"
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
            text = "UNKNOWN VIDEO CALL CAUTION"
            textSize = 17f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = "Incoming video call from an unsaved number. Be cautious before showing your face—unknown video calls are sometimes used for blackmail or extortion."
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = "DISMISS & HANG UP"
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
            text = if (isSevere) "HIGH-RISK INSTALL WARNING" else "UNVERIFIED APP WARNING"
            textSize = 17f
            setTextColor(if (isSevere) Color.parseColor("#B71C1C") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = if (isSevere)
                "You are on an unknown call. Installing outside apps during a call is a common scam tactic that can put your device and bank balance at risk."
            else
                "This app is from outside Google Play. If an unknown caller asked you to install it, it could be an attempt to access your phone or banking apps."
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
            text = "CANCEL INSTALL"
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
            text = "INSTALL ANYWAY"
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
            text = "UNVERIFIED APP INSTALLED"
            textSize = 17f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = "An app from an untrusted source ($maliciousPkg) was just installed. Unverified apps can put your OTPs and bank accounts at risk—uninstall it if you didn’t mean to install it."
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = "I WILL UNINSTALL NOW"
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
            text = if (isDM) "UNVERIFIED CHAT CAUTION" else "SUSPICIOUS GROUP INVITE"
            textSize = 17f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = if (isDM)
                "Moving from an online ad to a private chat with an unsaved number is a common pattern in trading, task, or betting scams. Verify carefully before sending any money."
            else
                "Joining a group straight from an online ad is a common pattern in trading, task, and betting scams. Verify the source carefully before sending money or personal details."
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = "I UNDERSTAND"
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
        onDismiss: () -> Unit
    ): View {
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
            text = title
            textSize = 17f
            setTextColor(Color.parseColor(titleColor))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val messageView = TextView(context).apply {
            text = message
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }

        val dismissBtn = Button(context).apply {
            text = "I UNDERSTAND"
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
