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
                        setPadding(40, 60, 40, 60)
                        setOnClickListener { dismissOverlay(appContext) }
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
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
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
        Handler(Looper.getMainLooper()).post {
            try {
                currentOverlayView?.let {
                    val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    windowManager.removeView(it)
                }
            } catch (e: Exception) {
                Log.e("OverlayUtils", "Error removing overlay view: ${e.message}")
            } finally {
                currentOverlayView = null
                isOverlayShowing = false
            }
        }
    }

    fun showScamCardOverlay(
        context: Context,
        message: MessageEntity,
        isApk: Boolean = false,
        isFakeCredit: Boolean = false,
        isBrowserSpam: Boolean = false
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
                title = "✈️ TRAVEL SAFETY ALERT",
                titleColor = "#1565C0",
                strokeColor = "#1976D2",
                bgColor = "#E3F2FD",
                message = "You are searching for travel bookings (hotels, flights, dhams, buses).\n\nNever pay travel agents or booking services an advance over WhatsApp using QR codes or UPI.\n\nDo bookings only from official & genuine websites.\n\nScammers create fake listings to steal your money.",
                onDismiss = onDismiss
            )
        }
    }

    fun showPreEmptiveHelplineAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "📞 CUSTOMER CARE ALERT",
                titleColor = "#E65100",
                strokeColor = "#F57C00",
                bgColor = "#FFF3E0",
                message = "You are searching for customer care or helplines.\n\nBe very careful of numbers found on Google search results or 3rd party websites.\n\nScammers post fake customer care numbers online to steal your money by asking you to download screen sharing apps or share OTPs.\n\nOnly use contact details found inside the official app.",
                onDismiss = onDismiss
            )
        }
    }

    fun showPreEmptiveLoanAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "💰 LOAN SAFETY ALERT",
                titleColor = "#E65100",
                strokeColor = "#F57C00",
                bgColor = "#FFF3E0",
                message = "You are searching for loans online.\n\n⚠️ NEVER download unauthorized loan apps sent via WhatsApp or SMS.\n\n⚠️ NEVER pay an 'advance fee', 'processing fee', or 'GST' to get a loan approved.\n\nScammers use these tricks to steal money and hijack your phone contacts.",
                onDismiss = onDismiss
            )
        }
    }

    fun showPreEmptiveJobAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "💼 JOB SAFETY ALERT",
                titleColor = "#1976D2",
                strokeColor = "#1976D2",
                bgColor = "#E3F2FD",
                message = "You are searching for online jobs or tasks.\n\n⚠️ NEVER pay a 'registration fee', 'security deposit', or any sort of fee for a job. If asked for registration money etc, then it's a scam.",
                onDismiss = onDismiss
            )
        }
    }

    fun showPreEmptiveTradingAlert(context: Context) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "📈 TRADING SAFETY ALERT",
                titleColor = "#2E7D32",
                strokeColor = "#2E7D32",
                bgColor = "#E8F5E9",
                message = "You are searching for trading or cryptocurrency.\n\n⚠️ NEVER join 'VIP Trading Groups' on Telegram or WhatsApp for stock tips.\n\n⚠️ Beware of platforms promising guaranteed high returns or risk-free profits. If it sounds too good to be true, it's a scam designed to steal your investments.",
                onDismiss = onDismiss
            )
        }
    }

    fun showFakeBookingWarningOverlay(context: Context, chatTitle: String) {
        showSystemOverlay(context) { ctx, onDismiss ->
            AlertCardBuilder.buildSimpleAlertCard(
                ctx,
                title = "⚠️ BOOKING SCAM WARNING",
                titleColor = "#E65100",
                strokeColor = "#F57C00",
                bgColor = "#FFF3E0",
                message = "You are discussing a booking or service with '$chatTitle' and they have requested payment.\n\nScammers frequently create fake listings and websites to steal advance payments via QR codes.\n\nNEVER scan a QR code for an unverified booking or service.",
                onDismiss = onDismiss
            )
        }
    }
}
