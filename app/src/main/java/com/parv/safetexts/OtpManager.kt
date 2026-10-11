// programmed by parv
package com.parv.safetexts

import android.content.Context
import android.content.SharedPreferences

object OtpManager {
    private const val PREFS_NAME = "otp_security_prefs"
    private const val KEY_LAST_OTP_TIME = "last_otp_time"
    private const val KEY_LAST_UNKNOWN_CALL_TIME = "last_unknown_call_time"
    private const val KEY_ACTIVE_INCOMING_CALL = "active_incoming_call"
    private const val KEY_ACTIVE_VOIP_CALL = "active_voip_call"
    private const val KEY_LAST_UNKNOWN_VOIP_CALL_TIME = "last_unknown_voip_call_time"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLastOtpTime(context: Context, timestamp: Long = System.currentTimeMillis()) {
        getPrefs(context).edit().putLong(KEY_LAST_OTP_TIME, timestamp).apply()
    }

    fun getLastOtpTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_OTP_TIME, 0L)
    }

    fun saveLastUnknownCallTime(context: Context, timestamp: Long = System.currentTimeMillis()) {
        getPrefs(context).edit().putLong(KEY_LAST_UNKNOWN_CALL_TIME, timestamp).apply()
    }

    fun getLastUnknownCallTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_UNKNOWN_CALL_TIME, 0L)
    }

    fun saveActiveIncomingNumber(context: Context, number: String) {
        getPrefs(context).edit().putString(KEY_ACTIVE_INCOMING_CALL, number).apply()
    }

    fun getActiveIncomingNumber(context: Context): String? {
        return getPrefs(context).getString(KEY_ACTIVE_INCOMING_CALL, null)
    }

    fun clearActiveIncomingNumber(context: Context) {
        getPrefs(context).edit().remove(KEY_ACTIVE_INCOMING_CALL).apply()
    }

    fun saveLastUnknownVoipCallTime(context: Context, timestamp: Long = System.currentTimeMillis()) {
        getPrefs(context).edit().putLong(KEY_LAST_UNKNOWN_VOIP_CALL_TIME, timestamp).apply()
    }

    fun getLastUnknownVoipCallTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_UNKNOWN_VOIP_CALL_TIME, 0L)
    }

    fun saveActiveVoipCallNumber(context: Context, number: String) {
        getPrefs(context).edit().putString(KEY_ACTIVE_VOIP_CALL, number).apply()
    }

    fun getActiveVoipCallNumber(context: Context): String? {
        return getPrefs(context).getString(KEY_ACTIVE_VOIP_CALL, null)
    }

    fun clearActiveVoipCallNumber(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_ACTIVE_VOIP_CALL)
            .remove(KEY_IS_VIDEO_CALL)
            .remove(KEY_VIDEO_CALL_START_TIME)
            .apply()
    }

    private const val KEY_IS_VIDEO_CALL = "is_video_call"
    private const val KEY_VIDEO_CALL_START_TIME = "video_call_start_time"

    fun setVoipCallVideoStatus(context: Context, isVideo: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_VIDEO_CALL, isVideo).apply()
    }

    fun isVoipCallVideo(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_VIDEO_CALL, false)
    }

    fun setVoipCallStartTime(context: Context, timestamp: Long = System.currentTimeMillis()) {
        getPrefs(context).edit().putLong(KEY_VIDEO_CALL_START_TIME, timestamp).apply()
    }

    fun getVoipCallStartTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_VIDEO_CALL_START_TIME, 0L)
    }

    private const val KEY_DIGITAL_ARREST_RISK_TIME = "digital_arrest_risk_time"

    fun setDigitalArrestRiskTime(context: Context, timestamp: Long = System.currentTimeMillis()) {
        getPrefs(context).edit().putLong(KEY_DIGITAL_ARREST_RISK_TIME, timestamp).apply()
    }

    fun getDigitalArrestRiskTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_DIGITAL_ARREST_RISK_TIME, 0L)
    }

    fun isBankingApp(packageName: String?): Boolean {
        if (packageName == null) return false
        val knownBanks = listOf(
            // Major UPI & Wallets
            "net.one97.paytm", "com.phonepe.app", "com.google.android.apps.nbu.paisa.user", 
            "in.org.npci.upiapp", "com.mobikwik_new", "com.freecharge.android",
            "com.myairtelapp", "com.reliance.jio.jiomoney", "com.enstage.wibmo.hdfc", // PayZapp
            
            // Major Banks (Many will be caught by the .contains("bank") rule, but adding explicit ones just in case)
            "com.sbi.SBIFreedomPlus", "com.sbi.lotusintouch", "com.sbi.SbiFreedom",
            "com.snapwork.hdfc", "com.csam.icici.bank.imobile", "com.icicibank.pockets",
            "com.axis.mobile", "com.msf.kbank.mobile", "com.bankofbaroda.mconnect", 
            "com.pnb.mBanking", "com.canarabank.mobility", "com.unionbank.in.mobile.banking", 
            "com.idfcfirstbank.dfa", "com.indusind.mobile.android", "org.altruist.BajajExperia",
            
            // Neo Banks & Fintech
            "com.dreamplug.androidapp", // CRED
            "money.jupiter", "com.epifi.paisa", "in.amazon.mShop.android.shopping" // Amazon Pay is inside main app
        )
        if (knownBanks.contains(packageName)) return true
        val lower = packageName.lowercase(java.util.Locale.ROOT)
        return lower.contains("bank")
    }
}
