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
            TYPE_SCAM -> {
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: "Unknown Sender"
                val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "SMS"
                val isApk = intent.getBooleanExtra(EXTRA_IS_APK, false)
                val isFakeCredit = intent.getBooleanExtra(EXTRA_IS_FAKE_CREDIT, false)
                val isBrowserSpam = intent.getBooleanExtra(EXTRA_IS_BROWSER_SPAM, false)
                AlertCardBuilder.buildScamCard(this, sender, content, appName, isApk, isFakeCredit, isBrowserSpam) { finish() }
            }
            TYPE_OTP -> AlertCardBuilder.buildOtpCard(this) { finish() }
            TYPE_RAT -> {
                val isSevere = intent.getBooleanExtra(EXTRA_IS_SEVERE, false)
                AlertCardBuilder.buildRatCard(this, isSevere) { finish() }
            }
            TYPE_DIGITAL_ARREST -> AlertCardBuilder.buildDigitalArrestCard(this) { finish() }
            TYPE_SEXTORTION -> AlertCardBuilder.buildSextortionCard(this) { finish() }
            TYPE_PRE_INSTALL -> {
                val isSevere = intent.getBooleanExtra(EXTRA_IS_SEVERE, false)
                AlertCardBuilder.buildPreInstallCard(this, isSevere) { finish() }
            }
            TYPE_POST_INSTALL -> {
                val maliciousPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "Unknown App"
                AlertCardBuilder.buildPostInstallCard(this, maliciousPkg) { finish() }
            }
            TYPE_SCAM_FUNNEL -> {
                val isDM = intent.getBooleanExtra(EXTRA_IS_DM, false)
                AlertCardBuilder.buildScamFunnelCard(this, isDM) { finish() }
            }
            TYPE_BOOKING -> AlertCardBuilder.buildSimpleAlertCard(
                this,
                title = "✈️ TRAVEL SAFETY ALERT",
                titleColor = "#1565C0",
                strokeColor = "#1976D2",
                bgColor = "#E3F2FD",
                message = "You are searching for travel bookings (hotels, flights, dhams, buses).\n\nNever pay travel agents or booking services an advance over WhatsApp using QR codes or UPI.\n\nDo bookings only from official & genuine websites.\n\nScammers create fake listings to steal your money.",
                onDismiss = { finish() }
            )
            TYPE_HELPLINE -> AlertCardBuilder.buildSimpleAlertCard(
                this,
                title = "📞 CUSTOMER CARE ALERT",
                titleColor = "#E65100",
                strokeColor = "#F57C00",
                bgColor = "#FFF3E0",
                message = "You are searching for customer care or helplines.\n\nBe very careful of numbers found on Google search results or 3rd party websites.\n\nScammers post fake customer care numbers online to steal your money by asking you to download screen sharing apps or share OTPs.\n\nOnly use contact details found inside the official app.",
                onDismiss = { finish() }
            )
            TYPE_LOAN -> AlertCardBuilder.buildSimpleAlertCard(
                this,
                title = "💰 LOAN SAFETY ALERT",
                titleColor = "#E65100",
                strokeColor = "#F57C00",
                bgColor = "#FFF3E0",
                message = "You are searching for loans online.\n\n⚠️ NEVER download unauthorized loan apps sent via WhatsApp or SMS.\n\n⚠️ NEVER pay an 'advance fee', 'processing fee', or 'GST' to get a loan approved.\n\nScammers use these tricks to steal money and hijack your phone contacts.",
                onDismiss = { finish() }
            )
            TYPE_JOB -> AlertCardBuilder.buildSimpleAlertCard(
                this,
                title = "💼 JOB SAFETY ALERT",
                titleColor = "#1976D2",
                strokeColor = "#1976D2",
                bgColor = "#E3F2FD",
                message = "You are searching for online jobs or tasks.\n\n⚠️ NEVER pay a 'registration fee', 'security deposit', or any sort of fee for a job. If asked for registration money etc, then it's a scam.",
                onDismiss = { finish() }
            )
            TYPE_TRADING -> AlertCardBuilder.buildSimpleAlertCard(
                this,
                title = "📈 TRADING SAFETY ALERT",
                titleColor = "#2E7D32",
                strokeColor = "#2E7D32",
                bgColor = "#E8F5E9",
                message = "You are searching for trading or cryptocurrency.\n\n⚠️ NEVER join 'VIP Trading Groups' on Telegram or WhatsApp for stock tips.\n\n⚠️ Beware of platforms promising guaranteed high returns or risk-free profits. If it sounds too good to be true, it's a scam designed to steal your investments.",
                onDismiss = { finish() }
            )
            TYPE_FAKE_BOOKING -> {
                val chatTitle = intent.getStringExtra(EXTRA_CHAT_TITLE) ?: "Unknown"
                AlertCardBuilder.buildSimpleAlertCard(
                    this,
                    title = "⚠️ BOOKING SCAM WARNING",
                    titleColor = "#E65100",
                    strokeColor = "#F57C00",
                    bgColor = "#FFF3E0",
                    message = "You are discussing a booking or service with '$chatTitle' and they have requested payment.\n\nScammers frequently create fake listings and websites to steal advance payments via QR codes.\n\nNEVER scan a QR code for an unverified booking or service.",
                    onDismiss = { finish() }
                )
            }
            else -> {
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: "Unknown Sender"
                val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "SMS"
                val isApk = intent.getBooleanExtra(EXTRA_IS_APK, false)
                val isFakeCredit = intent.getBooleanExtra(EXTRA_IS_FAKE_CREDIT, false)
                val isBrowserSpam = intent.getBooleanExtra(EXTRA_IS_BROWSER_SPAM, false)
                AlertCardBuilder.buildScamCard(this, sender, content, appName, isApk, isFakeCredit, isBrowserSpam) { finish() }
            }
        }
    }
}
