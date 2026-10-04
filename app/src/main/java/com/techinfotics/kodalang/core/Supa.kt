package com.techinfotics.kodalang.core

import android.content.Context
import com.techinfotics.kodalang.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Single shared Supabase client for the whole app.
 *
 * Points at the SAME Supabase project as kodalang.com, so the mobile app and
 * the website share accounts, progress, streaks and goals.
 *
 * NOTE: supabase-kt does not persist sessions across process restarts by
 * default, so [SessionStore] keeps the refresh token in SharedPreferences and
 * [com.techinfotics.kodalang.data.AuthRepository] refreshes it on cold start.
 */
object Supa {

    @Volatile
    private var client: SupabaseClient? = null

    /** True when local.properties was filled in with a real anon key. */
    fun isConfigured(): Boolean = BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    fun client(appContext: Context): SupabaseClient =
        client ?: synchronized(this) {
            client ?: createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth)
                install(Postgrest)
            }.also { client = it }
        }

    /** Drops the in-memory client (used on sign-out). */
    fun reset() {
        client = null
    }
}

/** Minimal refresh-token persistence (private app storage, never logged). */
class SessionStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("koda_session", Context.MODE_PRIVATE)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH, token).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_REFRESH).apply()
    }

    companion object {
        private const val KEY_REFRESH = "refresh_token"
    }
}
