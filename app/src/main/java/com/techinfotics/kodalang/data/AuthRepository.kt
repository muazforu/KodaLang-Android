package com.techinfotics.kodalang.data

import android.content.Context
import com.techinfotics.kodalang.core.SessionStore
import com.techinfotics.kodalang.core.Supa
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email

/**
 * Email/password authentication against the shared KodaLang Supabase project.
 *
 * Mirrors the website behaviour: new sign-ups need email confirmation, so a
 * sign-up that yields no session is reported as [AuthResult.NeedsEmailConfirmation]
 * (friendly notice, never raw JSON).
 */
class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val store = SessionStore(appContext)
    private val client get() = Supa.client(appContext)

    sealed interface AuthResult {
        data object SignedIn : AuthResult
        data object NeedsEmailConfirmation : AuthResult
        data class Failed(val message: String) : AuthResult
    }

    /** Cold-start: try to turn a stored refresh token into a live session. */
    suspend fun restoreSession(): Boolean {
        val refresh = store.getRefreshToken() ?: return false
        return try {
            val session = client.auth.refreshSession(refresh)
            store.saveRefreshToken(session.refreshToken)
            true
        } catch (_: Exception) {
            store.clear()
            false
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            saveRefreshToken()
            AuthResult.SignedIn
        } catch (e: AuthRestException) {
            AuthResult.Failed(
                when (e.error) {
                    "email_not_confirmed" ->
                        "Please confirm your email first — check your inbox, then sign in."
                    "invalid_credentials", "invalid_grant" ->
                        "Wrong email or password. Please try again."
                    else -> e.errorDescription.ifBlank { "Sign-in failed. Please try again." }
                }
            )
        } catch (e: Exception) {
            AuthResult.Failed(e.message ?: "Sign-in failed. Please try again.")
        }
    }

    suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            // With email confirmation ON (like kodalang.com), sign-up returns
            // a user but no session — ask the user to confirm, then sign in.
            val hasSession = !client.auth.currentSessionOrNull()?.accessToken.isNullOrBlank()
            if (hasSession) {
                saveRefreshToken()
                AuthResult.SignedIn
            } else {
                AuthResult.NeedsEmailConfirmation
            }
        } catch (e: AuthRestException) {
            AuthResult.Failed(
                when (e.error) {
                    "user_already_exists", "email_exists" ->
                        "This email is already registered — try signing in instead."
                    "weak_password", "password_too_short" ->
                        "Password is too weak — use at least 8 characters."
                    else -> e.errorDescription.ifBlank { "Sign-up failed. Please try again." }
                }
            )
        } catch (e: Exception) {
            AuthResult.Failed(e.message ?: "Sign-up failed. Please try again.")
        }
    }

    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Local sign-out must succeed even if the network call fails.
        }
        store.clear()
        Supa.reset()
    }

    fun accessToken(): String? = client.auth.currentSessionOrNull()?.accessToken

    fun userId(): String? = client.auth.currentUserOrNull()?.id

    fun email(): String? = client.auth.currentUserOrNull()?.email

    private fun saveRefreshToken() {
        client.auth.currentSessionOrNull()?.refreshToken?.let(store::saveRefreshToken)
    }
}
