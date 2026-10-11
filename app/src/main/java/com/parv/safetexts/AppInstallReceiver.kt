// programmed by parv
package com.parv.safetexts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.launch

class AppInstallReceiver : BroadcastReceiver() {

    // Comprehensive list of Remote Access / Screen Sharing apps commonly used in scams
    private val blacklistedApps = setOf(
        "com.anydesk.anydeskandroid",
        "com.teamviewer.quicksupport.market",
        "com.teamviewer.host.market",
        "com.carriez.rustdesk",
        "com.apowersoft.mirror",
        "com.microsoft.rdc.androidx",
        "com.google.chromeremotedesktop",
        "com.sand.airdroid",
        "com.realvnc.viewer.android"
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart
            Log.d("RatWatchdog", "App installed: $packageName")

            if (packageName != null) {
                // LAYER 2: Watchdog for ALL sideloaded apps
                val pm = context.packageManager
                try {
                    val installerInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        pm.getInstallSourceInfo(packageName).installingPackageName
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getInstallerPackageName(packageName)
                    }

                    val safeInstallers = listOf(
                        "com.android.vending", // Google Play
                        "com.sec.android.app.samsungapps", // Samsung
                        "com.xiaomi.mipicks", // Xiaomi
                        "com.heytap.market", "com.oppo.market", // Oppo/OnePlus
                        "com.bbk.appstore", // Vivo
                        "com.huawei.appmarket" // Huawei
                    )

                    val isSafeInstaller = safeInstallers.contains(installerInfo)
                    
                    // If the installer is null (ADB/manual) or a packageinstaller/browser/messenger, it's sideloaded
                    val isSideloaded = !isSafeInstaller && (installerInfo == null || installerInfo.contains("packageinstaller") || installerInfo.contains("browser") || installerInfo.contains("whatsapp") || installerInfo.contains("telegram") || installerInfo.contains("chrome"))
                    
                    if (isSideloaded && !blacklistedApps.contains(packageName)) {
                        Log.d("RatWatchdog", "SIDELOAD DETECTED: $packageName was installed via $installerInfo")
                        
                        // Log to database
                        val alert = MessageEntity(
                            sender = "System Alert",
                            content = "Post-Install Watchdog triggered: Sideloaded app ($packageName) was installed from an untrusted source.",
                            appName = "EdgeGuard AI",
                            analysisResult = "Alert",
                            scamProbability = 1.0f
                        )
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AppDatabase.getDatabase(context).messageDao().insertMessage(alert)
                        }
                        
                        OverlayUtils.showPostInstallWarningOverlay(context, packageName)
                    }

                } catch (e: Exception) {
                    Log.e("RatWatchdog", "Failed to verify installer for $packageName", e)
                }

                if (blacklistedApps.contains(packageName)) {
                Log.d("RatWatchdog", "DANGER: Blacklisted remote access app installed!")

                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val isTelecomCallActive = tm.callState == TelephonyManager.CALL_STATE_OFFHOOK
                val activeIncomingNumber = OtpManager.getActiveIncomingNumber(context)
                
                val activeVoipNumber = OtpManager.getActiveVoipCallNumber(context)
                val isVoipCallActive = activeVoipNumber != null

                val isCallActive = isTelecomCallActive || isVoipCallActive

                val isActiveTelecomUnknown = activeIncomingNumber != null && !ContactUtils.isContactExists(context, activeIncomingNumber)
                val isActiveVoipUnknown = activeVoipNumber != null && !ContactUtils.isContactExists(context, activeVoipNumber)
                val isActiveCallUnknown = isActiveTelecomUnknown || isActiveVoipUnknown

                val lastUnknownTelecomCall = OtpManager.getLastUnknownCallTime(context)
                val lastUnknownVoipCall = OtpManager.getLastUnknownVoipCallTime(context)
                val lastUnknownCall = maxOf(lastUnknownTelecomCall, lastUnknownVoipCall)

                val timeSinceUnknownCall = System.currentTimeMillis() - lastUnknownCall

                // If currently on a call AND the active call is unknown (regardless of duration)
                val isActiveScamCall = isCallActive && isActiveCallUnknown
                
                // If not currently on a call, but an unknown call was received within 1 hour
                val isRecentScamCall = !isCallActive && (timeSinceUnknownCall <= 1L * 60 * 60 * 1000)

                // Scenario: Active unknown call recently, OR unknown caller just ended within 1 hour
                val isSevere = isActiveScamCall || isRecentScamCall

                // Log to database
                val alert = MessageEntity(
                    sender = "System Alert",
                    content = "RAT Watchdog triggered: User installed a blacklisted Remote Access app ($packageName) during/after an unknown call.",
                    appName = "EdgeGuard AI",
                    analysisResult = "Alert",
                    scamProbability = 1.0f
                )
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    AppDatabase.getDatabase(context).messageDao().insertMessage(alert)
                }

                // Show the warning overlay
                OverlayUtils.showRatWarningOverlay(context, isSevere)
            }
        } // closes if (packageName != null)
        } // closes if (intent.action == Intent.ACTION_PACKAGE_ADDED)
    }
}


