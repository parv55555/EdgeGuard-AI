// programmed by parv
package com.parv.safetexts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

object OverlayUtils {

    @Volatile
    var isOverlayShowing = false

    private var currentOverlayView: View? = null

    fun showSystemOverlay(context: Context, cardBuilder: (Context, () -> Unit) -> View) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext) || isOverlayShowing || ScamAlertActivity.isAlertActive) {
            Log.d("OverlayUtils", "Cannot show overlay: canDraw=${Settings.canDrawOverlays(appContext)}, isShowing=$isOverlayShowing, isAlertActive=${ScamAlertActivity.isAlertActive}")
            return
        }

        val showAction = {
            try {
                if (!isOverlayShowing) {
                    val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                    val rootLayout = FrameLayout(appContext).apply {
                        setBackgroundColor(Color.parseColor("#99000000")) // Semi-transparent dim background
                        val hPad = AlertCardBuilder.dp(appContext, 24)
                        val vPad = AlertCardBuilder.dp(appContext, 32)
                        setPadding(hPad, vPad, hPad, vPad)
                        // Consume touches to prevent click-through to apps behind; do NOT dismiss on outside tap.
                        // Critical security alerts require explicit user action ([DISMISS] or [LOOKS SAFE]).
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            Log.d("OverlayUtils", "Background dim tapped: retained alert to prevent accidental dismissal")
                        }
                    }

                    val card = cardBuilder(appContext) { dismissOverlay(appContext) }
                    card.setOnClickListener { /* prevent dismissal on card click */ }

                    val cardParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    rootLayout.addView(card, cardParams)

                    val layoutParams = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        else
                            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }

                    windowManager.addView(rootLayout, layoutParams)
                    currentOverlayView = rootLayout
                    isOverlayShowing = true
                    Log.d("OverlayUtils", "System WindowManager overlay displayed successfully!")
                }
            } catch (e: Exception) {
                Log.e("OverlayUtils", "Failed to add WindowManager overlay: ${e.message}", e)
                isOverlayShowing = false
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            showAction()
        } else {
            Handler(Looper.getMainLooper()).post(showAction)
        }
    }

    fun dismissOverlay(context: Context) {
        val appContext = context.applicationContext
        val doDismiss = Runnable {
            try {
                currentOverlayView?.let {
                    val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    windowManager.removeView(it)
                    Log.d("OverlayUtils", "Overlay view dismissed cleanly by explicit user action")
                }
            } catch (e: Exception) {
                Log.e("OverlayUtils", "Error removing overlay view: ${e.message}")
            } finally {
                currentOverlayView = null
                isOverlayShowing = false
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            doDismiss.run()
        } else {
            Handler(Looper.getMainLooper()).post(doDismiss)
        }
    }

    fun showScamCardOverlay(
        context: Context,
        message: MessageEntity,
        isApk: Boolean = false,
        isFakeCredit: Boolean = false,
        isBrowserSpam: Boolean = false,
        isFakeWebsite: Boolean = false,
        websiteDomain: String? = null,
        fraudCategory: String? = null
    ) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildScamCard(
                context = ctx,
                sender = message.sender,
                content = message.content,
                appName = message.appName,
                isApk = isApk,
                isFakeCredit = isFakeCredit,
                isBrowserSpam = isBrowserSpam,
                isFakeWebsite = isFakeWebsite,
                websiteDomain = websiteDomain,
                fraudCategory = fraudCategory,
                onDismiss = onDismiss
            )
        }
    }

    fun showOtpWarningOverlay(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildOtpCard(ctx, onDismiss)
        }
    }

    fun showRatWarningOverlay(context: Context, isSevere: Boolean) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildRatCard(ctx, isSevere, onDismiss)
        }
    }

    fun showDigitalArrestWarningOverlay(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildDigitalArrestCard(ctx, onDismiss)
        }
    }

    fun showSextortionWarningOverlay(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSextortionCard(ctx, onDismiss)
        }
    }

    fun showPreInstallWarningOverlay(context: Context, isSevere: Boolean) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildPreInstallCard(ctx, isSevere, onDismiss)
        }
    }

    fun showPostInstallWarningOverlay(context: Context, maliciousPackageName: String) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildPostInstallCard(ctx, maliciousPackageName, onDismiss)
        }
    }

    fun showScamFunnelWarningOverlay(context: Context, isDM: Boolean = false) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildScamFunnelCard(ctx, isDM, onDismiss)
        }
    }

    fun showPreEmptiveBookingAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "TRAVEL BOOKING SAFETY TIP",
                titleColor = "#1565C0",
                strokeColor = "#1976D2",
                bgColor = "#E3F2FD",
                message = "Be cautious of booking agents asking for advance payments via WhatsApp QR codes or personal UPI IDs. Verify listings and book only on official apps or websites.",
                onDismiss = onDismiss
            )
        }
    }

    fun showPreEmptiveHelplineAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "HELPLINE SAFETY REMINDER",
                titleColor = "#E65100",
                strokeColor = "#F57C00",
                bgColor = "#FFF3E0",
                message = "Helpline numbers found through web search are sometimes fake. Check the official app or website directly, and never share OTPs or install remote-access apps.",
                onDismiss = onDismiss
            )
        }
    }

    fun showPreEmptiveLoanAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "LOAN SAFETY REMINDER",
                titleColor = "#E65100",
                strokeColor = "#F57C00",
                bgColor = "#FFF3E0",
                message = "Be cautious of instant-loan offers asking for upfront “processing fees” or APK downloads via WhatsApp. Check that the lender is RBI-registered before applying.",
                onDismiss = onDismiss
            )
        }
    }

    fun showPreEmptiveJobAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "JOB SAFETY REMINDER",
                titleColor = "#1976D2",
                strokeColor = "#1976D2",
                bgColor = "#E3F2FD",
                message = "Be cautious of online jobs or tasks asking for registration fees or deposits—upfront payment requests are a strong red flag for job scams.",
                onDismiss = onDismiss
            )
        }
    }

    fun showPreEmptiveTradingAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "TRADING SAFETY REMINDER",
                titleColor = "#2E7D32",
                strokeColor = "#2E7D32",
                bgColor = "#E8F5E9",
                message = "Be cautious of VIP trading groups promising guaranteed returns. Always verify brokers with SEBI before depositing money.",
                onDismiss = onDismiss
            )
        }
    }

    fun showFakeBookingWarningOverlay(context: Context, chatTitle: String) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "UNVERIFIED BOOKING WARNING",
                titleColor = "#E65100",
                strokeColor = "#F57C00",
                bgColor = "#FFF3E0",
                message = "This chat with '$chatTitle' looks like an unverified booking request. Avoid scanning QR codes or paying advance fees over chat without verifying the service first.",
                onDismiss = onDismiss
            )
        }
    }
}
