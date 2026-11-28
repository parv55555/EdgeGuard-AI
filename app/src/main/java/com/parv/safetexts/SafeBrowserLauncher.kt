package com.parv.safetexts

import android.app.Activity
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

object SafeBrowserLauncher {

    private const val ACTION_CUSTOM_TABS_CONNECTION = "android.support.customtabs.action.CustomTabsService"
    private const val CHROME_PACKAGE = "com.android.chrome"

    private val PREFERRED_BROWSERS = listOf(
        CHROME_PACKAGE,
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.duckduckgo.mobile.android"
    )

    fun findCustomTabBrowserPackage(context: Context): String? {
        val pm = context.packageManager
        val serviceIntent = Intent(ACTION_CUSTOM_TABS_CONNECTION)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(serviceIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentServices(serviceIntent, 0)
        }
        val customTabPackages = resolveInfos
            .mapNotNull { it.serviceInfo?.packageName }
            .filter { it != context.packageName }

        if (customTabPackages.contains(CHROME_PACKAGE)) {
            return CHROME_PACKAGE
        }
        return customTabPackages.firstOrNull()
    }

    fun findFallbackBrowserPackage(context: Context): String? {
        val pm = context.packageManager
        val baseIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(baseIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
                .ifEmpty { pm.queryIntentActivities(baseIntent, PackageManager.ResolveInfoFlags.of(0)) }
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(baseIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .ifEmpty {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(baseIntent, 0)
                }
        }
        val nonSelfBrowsers = resolveList.mapNotNull { it.activityInfo?.packageName }.filter { it != context.packageName }
        return PREFERRED_BROWSERS.firstOrNull { it in nonSelfBrowsers } ?: nonSelfBrowsers.firstOrNull()
    }

    private fun getShieldBitmap(context: Context): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_shield_24dp)
        val sizePx = (24 * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, sizePx, sizePx)
        drawable?.draw(canvas)
        return bitmap
    }

    fun openInRealBrowser(context: Context, rawUrl: String): Boolean {
        return try {
            val formattedUrl = if (!rawUrl.startsWith("http://", ignoreCase = true) && !rawUrl.startsWith("https://", ignoreCase = true)) {
                "https://$rawUrl"
            } else {
                rawUrl
            }
            val uri = Uri.parse(formattedUrl)

            val customTabPackage = findCustomTabBrowserPackage(context)
            if (customTabPackage != null) {
                try {
                    val scanIntent = Intent(context, ScanOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        1001,
                        scanIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                    val shieldBitmap = getShieldBitmap(context)
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                        .setActionButton(shieldBitmap, "Scan with EdgeGuard", pendingIntent, true)
                        .build()

                    customTabsIntent.intent.setPackage(customTabPackage)
                    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    customTabsIntent.launchUrl(context, uri)
                    return true
                } catch (_: Exception) {
                }
            }

            val fallbackPackage = findFallbackBrowserPackage(context)
            val baseIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (fallbackPackage != null) {
                    setPackage(fallbackPackage)
                }
            }
            try {
                context.startActivity(baseIntent)
                return true
            } catch (_: Exception) {
                val chooserIntent = Intent.createChooser(Intent(Intent.ACTION_VIEW, uri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }, "Open with").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                return true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isDefaultBrowser(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
        } else {
            val testIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
            @Suppress("DEPRECATION")
            val resolved = context.packageManager.resolveActivity(
                testIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolved?.activityInfo?.packageName == context.packageName
        }
    }

    fun requestDefaultBrowser(activity: Activity, launcher: ActivityResultLauncher<Intent>? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                if (launcher != null) {
                    launcher.launch(intent)
                } else {
                    activity.startActivity(intent)
                }
                return
            }
        }
        val fallbackIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        try {
            activity.startActivity(fallbackIntent)
        } catch (_: Exception) {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(appDetailsIntent)
        }
    }
}
