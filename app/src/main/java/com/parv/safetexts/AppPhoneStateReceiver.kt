package com.parv.safetexts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.launch

class AppPhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                
                if (phoneNumber == null) return
                
                // Save the incoming number as the currently active call
                OtpManager.saveActiveIncomingNumber(context, phoneNumber)

                if (!ContactUtils.isContactExists(context, phoneNumber)) {
                    Log.d("OtpSecurity", "Incoming call from unknown number: $phoneNumber")
                    OtpManager.saveLastUnknownCallTime(context)

                    val lastOtpTime = OtpManager.getLastOtpTime(context)
                    val sevenMinutesInMillis = 7L * 60 * 1000

                    if (System.currentTimeMillis() - lastOtpTime <= sevenMinutesInMillis) {
                        Log.d("OtpSecurity", "OTP received recently. Triggering overlay.")
                        
                        // Log alert to database
                        val database = AppDatabase.getDatabase(context)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val alert = MessageEntity(
                                sender = "System Alert",
                                content = "Warning triggered for potential OTP Theft. Unknown telecom call received shortly after an OTP.",
                                appName = "EdgeGuard AI",
                                analysisResult = "Alert",
                                scamProbability = 1.0f
                            )
                            database.messageDao().insertMessage(alert)
                        }
                        
                        Handler(Looper.getMainLooper()).post {
                            OverlayUtils.showOtpWarningOverlay(context)
                        }
                    }
                }
            }
        }
    }
}


