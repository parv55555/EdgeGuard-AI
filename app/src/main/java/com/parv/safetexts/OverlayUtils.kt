// programmed by parv
package com.parv.safetexts

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

object OverlayUtils {

    @Volatile
    var isOverlayShowing = false

    fun launchAlertActivity(context: Context, alertType: String, configureIntent: (Intent) -> Unit = {}) {
        if (!Settings.canDrawOverlays(context) || ScamAlertActivity.isAlertActive || isOverlayShowing) {
            Log.d("OverlayUtils", "Overlay permission not granted or already active for $alertType.")
            return
        }
        isOverlayShowing = true
        try {
            val intent = Intent(context, ScamAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(ScamAlertActivity.EXTRA_ALERT_TYPE, alertType)
                configureIntent(this)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                alertType.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("SCAM_ALERTS", "Security Alerts", NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, "SCAM_ALERTS")
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ Security Alert")
                .setContentText("Important security alert from SafeConvos")
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(alertType.hashCode(), notification)

            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    val method = options.javaClass.getMethod("setPendingIntentBackgroundActivityStartMode", Int::class.javaPrimitiveType)
                    method.invoke(options, 1)
                } catch (e: Throwable) {}
            }
            context.startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Log.e("OverlayUtils", "Failed to launch ScamAlertActivity for $alertType: ${e.message}", e)
            isOverlayShowing = false
        }
    }

    fun showOtpWarningOverlay(context: Context) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_OTP)
    }

    fun showRatWarningOverlay(context: Context, isSevere: Boolean) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_RAT) {
            it.putExtra(ScamAlertActivity.EXTRA_IS_SEVERE, isSevere)
        }
    }

    fun showDigitalArrestWarningOverlay(context: Context) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_DIGITAL_ARREST)
    }

    fun showSextortionWarningOverlay(context: Context) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_SEXTORTION)
    }

    fun showPreInstallWarningOverlay(context: Context, isSevere: Boolean) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_PRE_INSTALL) {
            it.putExtra(ScamAlertActivity.EXTRA_IS_SEVERE, isSevere)
        }
    }

    fun showPostInstallWarningOverlay(context: Context, maliciousPackageName: String) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_POST_INSTALL) {
            it.putExtra(ScamAlertActivity.EXTRA_PACKAGE_NAME, maliciousPackageName)
        }
    }

    fun showScamFunnelWarningOverlay(context: Context, isDM: Boolean = false) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_SCAM_FUNNEL) {
            it.putExtra(ScamAlertActivity.EXTRA_IS_DM, isDM)
        }
    }

    fun showPreEmptiveBookingAlert(context: Context) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_BOOKING)
    }

    fun showPreEmptiveHelplineAlert(context: Context) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_HELPLINE)
    }

    fun showPreEmptiveLoanAlert(context: Context) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_LOAN)
    }

    fun showPreEmptiveJobAlert(context: Context) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_JOB)
    }

    fun showPreEmptiveTradingAlert(context: Context) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_TRADING)
    }

    fun showFakeBookingWarningOverlay(context: Context, chatTitle: String) {
        launchAlertActivity(context, ScamAlertActivity.TYPE_FAKE_BOOKING) {
            it.putExtra(ScamAlertActivity.EXTRA_CHAT_TITLE, chatTitle)
        }
    }
}
