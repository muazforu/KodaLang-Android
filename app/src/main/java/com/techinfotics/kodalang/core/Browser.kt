package com.techinfotics.kodalang.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.techinfotics.kodalang.BuildConfig

/**
 * Opens a kodalang.com page in a Custom Tab (falls back to the browser).
 *
 * MVP honesty rule: Goal / Memory Health / Exam Simulator are NOT re-implemented
 * natively yet — they open the real website instead of fake native UI.
 */
fun openWebPage(context: Context, path: String) {
    val url = BuildConfig.API_BASE.trimEnd('/') + path
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            // Nothing we can do — no browser available.
        }
    }
}

object WebPaths {
    const val GOAL = "/goal"
    const val MEMORY_HEALTH = "/memory-health"
    const val EXAM = "/exam"
    const val PRICING = "/pricing"
    const val HOME = "/"
}
