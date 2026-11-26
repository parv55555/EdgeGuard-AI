package com.parv.safetexts

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher

object SafeBrowserLauncher {

    private val PREFERRED_BROWSERS = listOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.duckduckgo.mobile.android"
    )

    fun openInRealBrowser(context: Context, rawUrl: String): Boolean {
        return try {
            val uri = Uri.parse(rawUrl)
            val baseIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    baseIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                ).ifEmpty {
                    context.packageManager.queryIntentActivities(
                        baseIntent,
                        PackageManager.ResolveInfoFlags.of(0)
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(
                    baseIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                ).ifEmpty {
                    @Suppress("DEPRECATION")
                    context.packageManager.queryIntentActivities(baseIntent, 0)
                }
            }

            val nonSelfBrowsers = resolveList.filter {
                it.activityInfo != null && it.activityInfo.packageName != context.packageName
            }

            if (nonSelfBrowsers.isEmpty()) {
                return false
            }

            val selectedPackage = PREFERRED_BROWSERS.firstOrNull { browserPkg ->
                nonSelfBrowsers.any { it.activityInfo.packageName == browserPkg }
            } ?: nonSelfBrowsers.first().activityInfo.packageName

            baseIntent.setPackage(selectedPackage)
            context.startActivity(baseIntent)
            true
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
