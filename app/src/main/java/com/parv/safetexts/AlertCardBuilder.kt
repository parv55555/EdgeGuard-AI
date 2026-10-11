// programmed by parv
package com.parv.safetexts

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AlertCardBuilder {

    fun buildScamCard(
        context: Context,
        sender: String,
        content: String,
        appName: String,
        isApk: Boolean = false,
        isFakeCredit: Boolean = false,
        isBrowserSpam: Boolean = false,
        onDismiss: () -> Unit
    ): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = 48f
                setStroke(6, Color.parseColor("#FF5252"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(56, 56, 56, 56)
        }

        val titleView = TextView(context).apply {
            text = if (isBrowserSpam) "⚠️ PHISHING ALERT" else "🚨 SCAM ALERT 🚨"
            textSize = 24f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        cardLayout.addView(titleView)

        val senderView = TextView(context).apply {
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

        val warningDesc = TextView(context).apply {
            text = "A potential scam was intercepted. Do not transfer money, share OTPs, or click any unverified links."
            textSize = 15f
            setTextColor(Color.parseColor("#424242"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        cardLayout.addView(warningDesc)

        if (isApk) {
            val apkWarning = TextView(context).apply {
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
            val fakeCredit = TextView(context).apply {
                text = "⚠️ Fake transaction alert: AMOUNT HAS NOT BEEN CREDITED TO YOUR ACCOUNT!"
                textSize = 16f
                setTextColor(Color.parseColor("#D32F2F"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            cardLayout.addView(fakeCredit)
        }

        val btnLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val dismissBtn = Button(context).apply {
            text = "DISMISS"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF5252"))
                cornerRadius = 24f
            }
            setPadding(32, 20, 32, 20)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onDismiss() }
        }

        val notScamBtn = Button(context).apply {
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
                        val db = AppDatabase.getDatabase(context.applicationContext)
                        val msg = db.messageDao().getMessage(sender, content)
                        if (msg != null) {
                            db.messageDao().updateMessage(msg.copy(analysisResult = "FALSE_POSITIVE"))
                        }
                    } catch (e: Exception) {
                        Log.e("AlertCardBuilder", "Failed updating false positive", e)
                    }
                }
                try {
                    val broadcastIntent = Intent("com.parv.safetexts.NOT_A_SCAM").apply {
                        putExtra("message", content)
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(broadcastIntent)
                } catch (e: Exception) {
                    Log.e("AlertCardBuilder", "Failed sending false positive broadcast", e)
                }
                onDismiss()
            }
        }

        btnLayout.addView(dismissBtn)
        btnLayout.addView(notScamBtn)
        cardLayout.addView(btnLayout)

        return cardLayout
    }

    fun buildOtpCard(context: Context, onDismiss: () -> Unit): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF8E1"))
                cornerRadius = 48f
                setStroke(4, Color.parseColor("#FF8F00"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(context).apply {
            text = "⚠️ OTP SECURITY ALERT"
            textSize = 22f
            setTextColor(Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val messageView = TextView(context).apply {
            text = "You recently received an OTP.\n\nDo NOT share it with anyone on this call or message. Bank officials will NEVER ask for your OTP."
            textSize = 18f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(context).apply {
            text = "I UNDERSTAND"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF8F00"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildRatCard(context: Context, isSevere: Boolean, onDismiss: () -> Unit): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#FFEBEE") else Color.parseColor("#FFF3E0"))
                cornerRadius = 48f
                setStroke(6, if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 40f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(context).apply {
            text = if (isSevere) "🚨 SECURITY ALERT 🚨" else "⚠️ REMOTE ACCESS WARNING"
            textSize = 24f
            setTextColor(if (isSevere) Color.parseColor("#B71C1C") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val messageView = TextView(context).apply {
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

        val dismissBtn = Button(context).apply {
            text = if (isSevere) "I UNDERSTAND THE RISK" else "GOT IT"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildDigitalArrestCard(context: Context, onDismiss: () -> Unit): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFEBEE"))
                cornerRadius = 48f
                setStroke(8, Color.parseColor("#D50000"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 48f
            setPadding(64, 80, 64, 80)
        }

        val titleView = TextView(context).apply {
            text = "🚨 DIGITAL ARREST\nSCAM WARNING 🚨"
            textSize = 26f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val messageView = TextView(context).apply {
            text = "You are on a long video call following an unknown call, and just opened a banking app.\n\nPolice/CBI will NEVER arrest you over a video call. This is a scam.\n\nDO NOT TRANSFER MONEY."
            textSize = 20f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(context).apply {
            text = "I UNDERSTAND"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D50000"))
                cornerRadius = 24f
            }
            setPadding(32, 32, 32, 32)
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildSextortionCard(context: Context, onDismiss: () -> Unit): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFEBEE"))
                cornerRadius = 48f
                setStroke(6, Color.parseColor("#D50000"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 40f
            setPadding(64, 80, 64, 80)
        }

        val titleView = TextView(context).apply {
            text = "⚠️ VIDEO CALL ALERT"
            textSize = 24f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val messageView = TextView(context).apply {
            text = "Scammers use video calls to record your face for blackmail and extortion.\n\nIf you see a naked girl or a police officer on call, IMMEDIATELY cut the call & don't pay anything.\n\nDO NOT SHOW YOUR FACE."
            textSize = 18f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(context).apply {
            text = "DISMISS & HANG UP"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D50000"))
                cornerRadius = 24f
            }
            setPadding(32, 32, 32, 32)
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildPreInstallCard(context: Context, isSevere: Boolean, onDismiss: () -> Unit): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(if (isSevere) Color.parseColor("#FFEBEE") else Color.parseColor("#FFF3E0"))
                cornerRadius = 48f
                setStroke(6, if (isSevere) Color.parseColor("#D50000") else Color.parseColor("#E65100"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 40f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(context).apply {
            text = if (isSevere) "🚨 SECURITY ALERT 🚨" else "⚠️ UNVERIFIED APP WARNING"
            textSize = 24f
            setTextColor(if (isSevere) Color.parseColor("#B71C1C") else Color.parseColor("#E65100"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val messageView = TextView(context).apply {
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

        val btnContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val cancelBtn = Button(context).apply {
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
            setOnClickListener { onDismiss() }
        }

        val bypassBtn = Button(context).apply {
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
                onDismiss()
            }
        }

        btnContainer.addView(cancelBtn)
        btnContainer.addView(bypassBtn)
        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(btnContainer)
        return cardLayout
    }

    fun buildPostInstallCard(context: Context, maliciousPkg: String, onDismiss: () -> Unit): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = 48f
                setStroke(8, Color.parseColor("#B71C1C"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(context).apply {
            text = "🚨 CRITICAL THREAT INSTALLED"
            textSize = 24f
            setTextColor(Color.parseColor("#B71C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val messageView = TextView(context).apply {
            text = "A highly dangerous app was just installed on your phone from an untrusted source ($maliciousPkg).\n\nThis app could be secretly reading your SMS and stealing money from your bank accounts. UNINSTALL IT IMMEDIATELY!"
            textSize = 18f
            setTextColor(Color.parseColor("#424242"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val dismissBtn = Button(context).apply {
            text = "I WILL UNINSTALL IT NOW"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B71C1C"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildScamFunnelCard(context: Context, isDM: Boolean, onDismiss: () -> Unit): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF0F0"))
                cornerRadius = 48f
                setStroke(8, Color.parseColor("#D32F2F"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(context).apply {
            text = "⚠️ SCAM FUNNEL DETECTED"
            textSize = 22f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val messageView = TextView(context).apply {
            text = if (isDM)
                "Do not interact with this person for trading, forex, money tasks, or betting. It is possibly a scammer designed to steal your funds."
            else
                "Do not join groups for trading, forex, money tasks, betting. It is possibly a scam group designed to steal your funds."
            textSize = 16f
            setTextColor(Color.parseColor("#424242"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val dismissBtn = Button(context).apply {
            text = "I UNDERSTAND"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D32F2F"))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }

    fun buildSimpleAlertCard(
        context: Context,
        title: String,
        titleColor: String,
        strokeColor: String,
        bgColor: String,
        message: String,
        onDismiss: () -> Unit
    ): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 48f
                setStroke(4, Color.parseColor(strokeColor))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) elevation = 32f
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 22f
            setTextColor(Color.parseColor(titleColor))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val messageView = TextView(context).apply {
            text = message
            textSize = 18f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissBtn = Button(context).apply {
            text = "I UNDERSTAND"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(strokeColor))
                cornerRadius = 24f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener { onDismiss() }
        }

        cardLayout.addView(titleView)
        cardLayout.addView(messageView)
        cardLayout.addView(dismissBtn)
        return cardLayout
    }
}
