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

    private val BIDI_REGEX = Regex("[\\u200E\\u200F\\u202A-\\u202E]")

    fun addTrustedSender(context: Context, sender: String) {
        val cleanSender = sender.trim().replace(BIDI_REGEX, "")
        if (cleanSender.isBlank()) return
        val prefs = context.getSharedPreferences("safetexts_prefs", Context.MODE_PRIVATE)

        // Store human-readable display string
        val displaySet = prefs.getStringSet("trusted_senders_display", emptySet())?.toMutableSet() ?: mutableSetOf()
        displaySet.add(cleanSender)

        // Store fast lookup representations
        val currentSet = prefs.getStringSet("trusted_senders", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(cleanSender.lowercase())
        
        val normalized = cleanSender.replace(Regex("[^0-9+]"), "")
        if (normalized.isNotEmpty()) {
            currentSet.add(normalized)
        }
        val digitsOnly = cleanSender.filter { it.isDigit() }
        if (digitsOnly.length >= 7) {
            currentSet.add(digitsOnly)
            if (digitsOnly.length >= 10) {
                currentSet.add(digitsOnly.takeLast(10))
            }
        }
        prefs.edit()
            .putStringSet("trusted_senders_display", displaySet)
            .putStringSet("trusted_senders", currentSet)
            .apply()
        android.util.Log.d("ContactUtils", "Added trusted sender: '$cleanSender'")
    }

    fun isTrustedSender(context: Context, sender: String): Boolean {
        val cleanSender = sender.trim().replace(BIDI_REGEX, "")
        if (cleanSender.isBlank()) return false
        val prefs = context.getSharedPreferences("safetexts_prefs", Context.MODE_PRIVATE)
        val trusted = prefs.getStringSet("trusted_senders", emptySet()) ?: emptySet()
        if (trusted.contains(cleanSender.lowercase())) return true

        val normalized = cleanSender.replace(Regex("[^0-9+]"), "")
        if (normalized.isNotEmpty() && trusted.contains(normalized)) return true

        val digitsOnly = cleanSender.filter { it.isDigit() }
        if (digitsOnly.length >= 7) {
            if (trusted.contains(digitsOnly)) return true
            if (digitsOnly.length >= 10 && trusted.contains(digitsOnly.takeLast(10))) return true
        }
        return false
    }

    fun removeTrustedSender(context: Context, sender: String) {
        val cleanSender = sender.trim().replace(BIDI_REGEX, "")
        if (cleanSender.isBlank()) return
        val prefs = context.getSharedPreferences("safetexts_prefs", Context.MODE_PRIVATE)

        val displaySet = prefs.getStringSet("trusted_senders_display", emptySet())?.toMutableSet() ?: mutableSetOf()
        displaySet.remove(cleanSender)

        val currentSet = prefs.getStringSet("trusted_senders", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(cleanSender.lowercase())
        val normalized = cleanSender.replace(Regex("[^0-9+]"), "")
        if (normalized.isNotEmpty()) currentSet.remove(normalized)
        val digitsOnly = cleanSender.filter { it.isDigit() }
        if (digitsOnly.length >= 7) {
            currentSet.remove(digitsOnly)
            if (digitsOnly.length >= 10) currentSet.remove(digitsOnly.takeLast(10))
        }
        prefs.edit()
            .putStringSet("trusted_senders_display", displaySet)
            .putStringSet("trusted_senders", currentSet)
            .apply()
        android.util.Log.d("ContactUtils", "Removed trusted sender: '$cleanSender'")
    }

    fun clearTrustedSenders(context: Context) {
        val prefs = context.getSharedPreferences("safetexts_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("trusted_senders_display")
            .remove("trusted_senders")
            .apply()
    }

    fun getTrustedDisplayList(context: Context): List<String> {
        val prefs = context.getSharedPreferences("safetexts_prefs", Context.MODE_PRIVATE)
        val displaySet = prefs.getStringSet("trusted_senders_display", null)
        if (!displaySet.isNullOrEmpty()) {
            return displaySet.toList().sorted()
        }
        val legacy = prefs.getStringSet("trusted_senders", emptySet()) ?: emptySet()
        return legacy.filter { it.length >= 7 || !it.all { c -> c.isDigit() } }.toList().sorted()
    }

    fun getTrustedSenders(context: Context): Set<String> {
        val prefs = context.getSharedPreferences("safetexts_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("trusted_senders_display", null) 
            ?: prefs.getStringSet("trusted_senders", emptySet()) 
            ?: emptySet()
    }

    fun isContactExists(context: Context, senderName: String): Boolean {
        if (senderName.isEmpty()) return false
        if (isTrustedSender(context, senderName)) return true
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

    fun isAlphabetOrAlphanumeric(name: String): Boolean {
        if (name.isBlank()) return false
        return name.any { it.isLetter() }
    }

    fun isRawPhoneNumber(title: String): Boolean {
        if (title.isBlank()) return false
        val digits = title.count { it.isDigit() }
        return digits >= 7 && title.matches(Regex("^[\\d\\s\\+\\-\\(\\)]+$"))
    }
}
