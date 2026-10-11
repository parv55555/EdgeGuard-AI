// programmed by parv
package com.parv.safetexts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactUtils {
    fun isUserOnCall(context: Context): Boolean {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            val isTelecomCallActive = tm.callState == android.telephony.TelephonyManager.CALL_STATE_OFFHOOK
            val isVoipCallActive = OtpManager.getActiveVoipCallNumber(context) != null
            return isTelecomCallActive || isVoipCallActive
        } catch (e: Exception) {
            return false
        }
    }

    fun isWhatsAppCompanyAccount(title: String): Boolean {
        if (title.isBlank()) return false
        
        // On WhatsApp, unverified accounts (including scammers) display their phone number as the title (e.g., +91 98765 43210) which has 10+ digits.
        // Verified company accounts (like Swiggy, MakeMyTrip) display their actual name (e.g., "Swiggy"), which rarely contains many digits.
        // By checking if the title has fewer than 7 digits, we can accurately identify verified company names.
        // Note: Saved personal contacts (like "Mom") also have 0 digits, but they are filtered out before this check.
        val digitCount = title.count { it.isDigit() }
        return digitCount < 7
    }

    fun isContactExists(context: Context, senderName: String): Boolean {
        if (senderName.isEmpty()) return false
        try {
            // 1. Try PhoneLookup (best for phone numbers)
            val phoneUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
                Uri.encode(senderName)
            )
            context.contentResolver.query(phoneUri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                if (cursor.count > 0) return true
            }

            // 2. Try exact match on Display Name (best for names like in WhatsApp notifications)
            val projection = arrayOf(ContactsContract.Contacts._ID)
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(senderName)
            context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.count > 0) return true
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    fun isAlphanumericSmsSender(title: String): Boolean {
        if (title.isBlank()) return false
        
        // For SMS, normal unsaved phone numbers consist only of digits (and +, -, etc.).
        // If an SMS sender ID contains ANY letter, it is an alphanumeric or alphabetic sender (e.g. AD-HDFCBK, JioNet, VK-SBIINB).
        // Note: Saved contacts also contain letters, but they are filtered out BEFORE this using isContactExists.
        return title.any { it.isLetter() }
    }
}
