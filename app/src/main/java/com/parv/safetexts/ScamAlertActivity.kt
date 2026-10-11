package com.parv.safetexts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScamAlertActivity : Activity() {

    companion object {
        const val EXTRA_ALERT_TYPE = "extra_alert_type"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_IS_APK = "extra_is_apk"
        const val EXTRA_IS_FAKE_CREDIT = "extra_is_fake_credit"
        const val EXTRA_IS_BROWSER_SPAM = "extra_is_browser_spam"
        const val EXTRA_IS_SEVERE = "extra_is_severe"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_CHAT_TITLE = "extra_chat_title"
        const val EXTRA_IS_DM = "extra_is_dm"
        const val EXTRA_MESSAGE_ID = "extra_message_id"

        const val TYPE_SCAM = "TYPE_SCAM"
        const val TYPE_OTP = "TYPE_OTP"
        const val TYPE_RAT = "TYPE_RAT"
        const val TYPE_DIGITAL_ARREST = "TYPE_DIGITAL_ARREST"
        const val TYPE_SEXTORTION = "TYPE_SEXTORTION"
        const val TYPE_PRE_INSTALL = "TYPE_PRE_INSTALL"
        const val TYPE_POST_INSTALL = "TYPE_POST_INSTALL"
        const val TYPE_SCAM_FUNNEL = "TYPE_SCAM_FUNNEL"
        const val TYPE_BOOKING = "TYPE_BOOKING"
        const val TYPE_HELPLINE = "TYPE_HELPLINE"
        const val TYPE_LOAN = "TYPE_LOAN"
        const val TYPE_JOB = "TYPE_JOB"
        const val TYPE_TRADING = "TYPE_TRADING"
        const val TYPE_FAKE_BOOKING = "TYPE_FAKE_BOOKING"

        @Volatile
        var isAlertActive = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAlertActive = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setDimAmount(0.65f)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val alertType = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: TYPE_SCAM

        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(40, 60, 40, 60)
            setOnClickListener { finish() }
        }

        val cardView = buildCardForType(alertType)
        cardView.setOnClickListener { /* prevent dismissal on card click */ }

        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        rootLayout.addView(cardView, cardParams)
        setContentView(rootLayout)
    }

    override fun onDestroy() {
        super.onDestroy()
        isAlertActive = false
        OverlayUtils.isOverlayShowing = false
    }

    private fun buildCardForType(alertType: String): View {
        return when (alertType) {
            TYPE_SCAM -> buildScamCard()
            TYPE_OTP -> buildOtpCard()
            TYPE_RAT -> buildRatCard()
            TYPE_DIGITAL_ARREST -> buildDigitalArrestCard()
            TYPE_SEXTORTION -> buildSextortionCard()
            TYPE_PRE_INSTALL -> buildPreInstallCard()
            TYPE_POST_INSTALL -> buildPostInstallCard()
            TYPE_SCAM_FUNNEL -> buildScamFunnelCard()
            TYPE_BOOKING -> buildBookingCard()
            TYPE_HELPLINE -> buildHelplineCard()
            TYPE_LOAN -> buildLoanCard()
            TYPE_JOB -> buildJobCard()
            TYPE_TRADING -> buildTradingCard()
            TYPE_FAKE_BOOKING -> buildFakeBookingCard()
            else -> buildScamCard()
        }
    }

    private fun buildScamCard(): View {
        val sender = intent.getStringExtra(EXTRA_SENDER) ?: "Unknown Sender"
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "SMS"
        val isApk = intent.getBooleanExtra(EXTRA_IS_APK, false)
        val isFakeCredit = intent.getBooleanExtra(EXTRA_IS_FAKE_CREDIT, false)
        val isBrowserSpam = intent.getBooleanExtra(EXTRA_IS_BROWSER_SPAM, false)

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = 48f
                setStroke(6, Color.parseColor("#FF5252"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(56, 56, 56, 56)
        }

        val titleView = TextView(this).apply {
            text = if (isBrowserSpam) "⚠️ PHISHING ALERT" else "🚨 SCAM ALERT 🚨"
            textSize = 24f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        cardLayout.addView(titleView)

        val senderView = TextView(this).apply {
            text = if (isBrowserSpam) {
                "Phishing notification from $sender. Do not tap that notification."
            } else {
                "From: $sender ($appName)"
            }
            textSize = 17f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        cardLayout.addView(senderView)

        val warningDesc = TextView(this).apply {
            text = "A potential scam was intercepted. Do not transfer money, share OTPs, or click any unverified links."
            textSize = 15f
            setTextColor(Color.parseColor("#424242"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        cardLayout.addView(warningDesc)

        if (isApk) {
            val apkWarning = TextView(this).apply {
                text = "⚠️ DO NOT download & open this file! It might hack your phone & bank account."
                textSize = 16f
                setTextColor(Color.parseColor("#D32F2F"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            cardLayout.addView(apkWarning)
        }

        if (isFakeCredit) {
            val fakeCredit = TextView(this).apply {
                text = "⚠️ Fake transaction alert: AMOUNT HAS NOT BEEN CREDITED TO YOUR ACCOUNT!"
                textSize = 16f
                setTextColor(Color.parseColor("#D32F2F"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            cardLayout.addView(fakeCredit)
        }

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val dismissBtn = Button(this).apply {
            text = "DISMISS"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF5252"))
                cornerRadius = 24f
            }
            setPadding(32, 20, 32, 20)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { finish() }
        }

        val notScamBtn = Button(this).apply {
            text = "NOT A SCAM"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#4CAF50"))
                cornerRadius = 24f
            }
            setPadding(32, 20, 32, 20)
            val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p.marginStart = 20
            layoutParams = p
            setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val msg = db.messageDao().getMessage(sender, content)
                        if (msg != null) {
                            db.messageDao().updateMessage(msg.copy(analysisResult = "FALSE_POSITIVE"))
                        }
                    } catch (e: Exception) {
                        Log.e("ScamAlertActivity", "Failed updating false positive", e)
                    }
                }
                finish()
            }
        }

        btnLayout.addView(dismissBtn)
        btnLayout.addView(notScamBtn)
        cardLayout.addView(btnLayout)

        return cardLayout
    }

    private fun buildOtpCard(): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF8E1"))
                cornerRadius = 48f
                setStroke(4, Color.parseColor("#FF8F00"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(this).apply {
            text = "⚠️ OTP SECURITY ALERT"
            textSize = 22f
            setTextColor(Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val messageView = TextView(this).apply {
            text = "You recently received an OTP.\n\nDo NOT share it with anyone on this call or message. Bank officials will NEVER ask for your OTP."
            textSize = 18f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(this).apply {
            text = "I UNDERSTAND"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF8F00"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { finish() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    private fun buildRatCard(): View {
        val isSevere = intent.getBooleanExtra(EXTRA_IS_SEVERE, false)
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#FFEBEE") else Color.parseColor("#FFF3E0"))
                cornerRadius = 48f
                setStroke(6, if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 40f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(this).apply {
            text = if (isSevere) "🚨 SECURITY ALERT 🚨" else "⚠️ REMOTE ACCESS WARNING"
            textSize = 24f
            setTextColor(if (isSevere) Color.parseColor("#B71C1C") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val messageView = TextView(this).apply {
            text = if (isSevere)
                "If the caller has asked you to download this app, it is better not to install it as it might hack your device & bank account.\n\nEven if you are on a call with someone claiming to be a government officer, a bank person, or a loan giver, DO NOT install this app! If they ask you to install it, they are just scammers."
            else
                "You just installed a screen-sharing app.\n\nBEWARE: If someone claiming to be a customer service/care agent, a government officer, or a banking/loan person asks you to download this over a call or video call, they might hack your phone & bank account. If they ask you to install it, they are just scammers."
            textSize = 18f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(this).apply {
            text = if (isSevere) "I UNDERSTAND THE RISK" else "GOT IT"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { finish() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    private fun buildDigitalArrestCard(): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFEBEE"))
                cornerRadius = 48f
                setStroke(8, Color.parseColor("#D50000"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 48f
            setPadding(64, 80, 64, 80)
        }

        val titleView = TextView(this).apply {
            text = "🚨 DIGITAL ARREST\nSCAM WARNING 🚨"
            textSize = 26f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val messageView = TextView(this).apply {
            text = "You are on a long video call following an unknown call, and just opened a banking app.\n\nPolice/CBI will NEVER arrest you over a video call. This is a scam.\n\nDO NOT TRANSFER MONEY."
            textSize = 20f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(this).apply {
            text = "I UNDERSTAND"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D50000"))
                cornerRadius = 24f
            }
            setPadding(32, 32, 32, 32)
            setOnClickListener { finish() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    private fun buildSextortionCard(): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFEBEE"))
                cornerRadius = 48f
                setStroke(6, Color.parseColor("#D50000"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 40f
            setPadding(64, 80, 64, 80)
        }

        val titleView = TextView(this).apply {
            text = "⚠️ VIDEO CALL ALERT"
            textSize = 24f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val messageView = TextView(this).apply {
            text = "Scammers use video calls to record your face for blackmail and extortion.\n\nIf you see a naked girl or a police officer on call, IMMEDIATELY cut the call & don't pay anything.\n\nDO NOT SHOW YOUR FACE."
            textSize = 18f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(this).apply {
            text = "DISMISS & HANG UP"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D50000"))
                cornerRadius = 24f
            }
            setPadding(32, 32, 32, 32)
            setOnClickListener { finish() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    private fun buildPreInstallCard(): View {
        val isSevere = intent.getBooleanExtra(EXTRA_IS_SEVERE, false)
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#FFEBEE") else Color.parseColor("#FFF3E0"))
                cornerRadius = 48f
                setStroke(6, if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 40f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(this).apply {
            text = if (isSevere) "🚨 SECURITY ALERT 🚨" else "⚠️ UNVERIFIED APP WARNING"
            textSize = 24f
            setTextColor(if (isSevere) Color.parseColor("#B71C1C") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val messageView = TextView(this).apply {
            text = if (isSevere)
                "If the caller has asked you to download this app, it is better not to install it as it might hack your device & bank account.\n\nEven if you are on a call with someone claiming to be a government officer, a bank person, or a loan giver, DO NOT install this app! If they ask you to install it, they are just scammers."
            else
                "You are trying to install an unverified app.\n\nBEWARE: If someone claiming to be a customer service/care agent, a government officer, or a banking/loan person asks you to download this over a call or video call, they might hack your phone & bank account. If they ask you to install it, they are just scammers."
            textSize = 18f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val btnContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val cancelBtn = Button(this).apply {
            text = "CANCEL INSTALL"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p.marginEnd = 16
            layoutParams = p
            setOnClickListener { finish() }
        }

        val bypassBtn = Button(this).apply {
            text = "INSTALL ANYWAY"
            setTextColor(if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(4, if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                MessageReaderAccessibilityService.allowNextInstallUntilTime = System.currentTimeMillis() + 60_000L
                finish()
            }
        }

        btnContainer.addView(cancelBtn)
        btnContainer.addView(bypassBtn)
        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(btnContainer)
        return cardLayout
    }

    private fun buildPostInstallCard(): View {
        val maliciousPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "Unknown App"
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = 48f
                setStroke(8, Color.parseColor("#B71C1C"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(this).apply {
            text = "🚨 CRITICAL THREAT INSTALLED"
            textSize = 24f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val messageView = TextView(this).apply {
            text = "A highly dangerous app was just installed on your phone from an untrusted source ($maliciousPkg).\n\nThis app could be secretly reading your SMS and stealing money from your bank accounts. UNINSTALL IT IMMEDIATELY!"
            textSize = 18f
            setTextColor(Color.parseColor("#424242"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val dismissBtn = Button(this).apply {
            text = "I WILL UNINSTALL IT NOW"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B71C1C"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { finish() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    private fun buildScamFunnelCard(): View {
        val isDM = intent.getBooleanExtra(EXTRA_IS_DM, false)
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = 48f
                setStroke(8, Color.parseColor("#D32F2F"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(this).apply {
            text = "⚠️ SCAM FUNNEL DETECTED"
            textSize = 22f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val messageView = TextView(this).apply {
            text = if (isDM)
                "Do not interact with this person for trading, forex, money tasks, or betting. It is possibly a scammer designed to steal your funds."
            else
                "Do not join groups for trading, forex, money tasks, betting. It is possibly a scam group designed to steal your funds."
            textSize = 16f
            setTextColor(Color.parseColor("#424242"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val dismissBtn = Button(this).apply {
            text = "I UNDERSTAND"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D32F2F"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { finish() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    private fun buildBookingCard(): View {
        return buildSimpleAlertCard(
            title = "✈️ TRAVEL SAFETY ALERT",
            titleColor = "#1565C0",
            strokeColor = "#1976D2",
            bgColor = "#E3F2FD",
            message = "You are searching for travel bookings (hotels, flights, dhams, buses).\n\nNever pay travel agents or booking services an advance over WhatsApp using QR codes or UPI.\n\nDo bookings only from official & genuine websites.\n\nScammers create fake listings to steal your money."
        )
    }

    private fun buildHelplineCard(): View {
        return buildSimpleAlertCard(
            title = "📞 CUSTOMER CARE ALERT",
            titleColor = "#E65100",
            strokeColor = "#F57C00",
            bgColor = "#FFF3E0",
            message = "You are searching for customer care or helplines.\n\nBe very careful of numbers found on Google search results or 3rd party websites.\n\nScammers post fake customer care numbers online to steal your money by asking you to download screen sharing apps or share OTPs.\n\nOnly use contact details found inside the official app."
        )
    }

    private fun buildLoanCard(): View {
        return buildSimpleAlertCard(
            title = "💰 LOAN SAFETY ALERT",
            titleColor = "#E65100",
            strokeColor = "#F57C00",
            bgColor = "#FFF3E0",
            message = "You are searching for loans online.\n\n⚠️ NEVER download unauthorized loan apps sent via WhatsApp or SMS.\n\n⚠️ NEVER pay an 'advance fee', 'processing fee', or 'GST' to get a loan approved.\n\nScammers use these tricks to steal money and hijack your phone contacts."
        )
    }

    private fun buildJobCard(): View {
        return buildSimpleAlertCard(
            title = "💼 JOB SAFETY ALERT",
            titleColor = "#1976D2",
            strokeColor = "#1976D2",
            bgColor = "#E3F2FD",
            message = "You are searching for online jobs or tasks.\n\n⚠️ NEVER pay a 'registration fee', 'security deposit', or any sort of fee for a job. If asked for registration money etc, then it's a scam."
        )
    }

    private fun buildTradingCard(): View {
        return buildSimpleAlertCard(
            title = "📈 TRADING SAFETY ALERT",
            titleColor = "#2E7D32",
            strokeColor = "#2E7D32",
            bgColor = "#E8F5E9",
            message = "You are searching for trading or cryptocurrency.\n\n⚠️ NEVER join 'VIP Trading Groups' on Telegram or WhatsApp for stock tips.\n\n⚠️ Beware of platforms promising guaranteed high returns or risk-free profits. If it sounds too good to be true, it's a scam designed to steal your investments."
        )
    }

    private fun buildFakeBookingCard(): View {
        val chatTitle = intent.getStringExtra(EXTRA_CHAT_TITLE) ?: "Unknown"
        return buildSimpleAlertCard(
            title = "⚠️ BOOKING SCAM WARNING",
            titleColor = "#E65100",
            strokeColor = "#F57C00",
            bgColor = "#FFF3E0",
            message = "You are discussing a booking or service with '$chatTitle' and they have requested payment.\n\nScammers frequently create fake listings and websites to steal advance payments via QR codes.\n\nNEVER scan a QR code for an unverified booking or service."
        )
    }

    private fun buildSimpleAlertCard(title: String, titleColor: String, strokeColor: String, bgColor: String, message: String): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 48f
                setStroke(4, Color.parseColor(strokeColor))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 22f
            setTextColor(Color.parseColor(titleColor))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val messageView = TextView(this).apply {
            text = message
            textSize = 18f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(this).apply {
            text = "I UNDERSTAND"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(strokeColor))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { finish() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }
}
