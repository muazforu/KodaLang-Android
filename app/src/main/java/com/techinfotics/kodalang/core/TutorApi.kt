package com.techinfotics.kodalang.core

import android.content.Context
import android.media.MediaPlayer
import com.techinfotics.kodalang.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Client for the KodaLang web app's server AI routes:
 * - POST {API_BASE}/api/tutor-chat  (streamed AI tutor replies)
 * - POST {API_BASE}/api/tutor-tts    (assistant speech, audio/mpeg)
 *
 * Auth: the user's Supabase JWT as `Authorization: Bearer <token>`,
 * exactly like the web client. 401 = session expired, 402 = the plan's
 * conversation-minute allowance is used up (the JSON body carries a
 * human-readable upgrade message from the server).
 */
class TutorApi(appContext: Context) {

    private val context = appContext.applicationContext
    private val apiBase: String = BuildConfig.API_BASE.trimEnd('/')

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // AI streams can take a while
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    sealed interface ChatResult {
        data class Text(val content: String) : ChatResult
        data class LimitReached(val message: String) : ChatResult
        data object SessionExpired : ChatResult
        data class Error(val message: String) : ChatResult
    }

    suspend fun chat(
        token: String,
        conversationId: String,
        history: List<ChatMessage>,
    ): ChatResult = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(
                ChatRequest.serializer(),
                ChatRequest(messages = history, conversationId = conversationId),
            )
            val request = Request.Builder()
                .url("$apiBase/api/tutor-chat")
                .header("Authorization", "Bearer $token")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                when (response.code) {
                    401 -> ChatResult.SessionExpired
                    402 -> ChatResult.LimitReached(extractLimitMessage(raw))
                    else -> {
                        if (!response.isSuccessful) {
                            ChatResult.Error("Server error (${response.code}). Please try again.")
                        } else {
                            val text = extractStreamedText(raw)
                            if (text.isBlank()) ChatResult.Error("Empty reply from Koda. Please try again.")
                            else ChatResult.Text(text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            ChatResult.Error("Network error: ${e.message ?: "please check your connection."}")
        }
    }

    /**
     * Synthesises speech for an assistant message.
     * @return MP3 bytes, or null on any failure (callers fall back silently).
     */
    suspend fun tts(token: String, text: String): ByteArray? = withContext(Dispatchers.IO) {
        val clean = text.trim().take(800)
        if (clean.isEmpty()) return@withContext null
        try {
            val payload = buildJsonObject {
                put("text", clean)
                put("voiceId", DEFAULT_VOICE_ID) // "Sarah" — same default as the web app
            }
            val request = Request.Builder()
                .url("$apiBase/api/tutor-tts")
                .header("Authorization", "Bearer $token")
                .post(
                    json.encodeToString(JsonObject.serializer(), payload)
                        .toRequestBody("application/json".toMediaType())
                )
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.bytes()?.takeIf { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The chat route answers with `toUIMessageStreamResponse()` — an SSE-style
     * stream of `data: {...}` JSON chunks. We collect `text-delta` chunks
     * defensively; if the body is not SSE we fall back to the raw text so the
     * user never sees framing garbage.
     */
    private fun extractStreamedText(raw: String): String {
        if (!raw.contains("data:")) return raw.trim()
        val sb = StringBuilder()
        for (line in raw.lineSequence()) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("data:")) continue
            val payload = trimmed.removePrefix("data:").trim()
            if (payload.isEmpty() || payload == "[DONE]") continue
            try {
                val obj = json.parseToJsonElement(payload).jsonObject
                if (obj["type"]?.jsonPrimitive?.contentOrNull == "text-delta") {
                    val delta = obj["delta"]?.jsonPrimitive?.contentOrNull
                        ?: obj["textDelta"]?.jsonPrimitive?.contentOrNull
                    if (delta != null) sb.append(delta)
                }
            } catch (_: Exception) {
                // Skip malformed chunks — never crash on stream framing.
            }
        }
        return if (sb.isNotEmpty()) sb.toString().trim() else raw.trim()
    }

    private fun extractLimitMessage(raw: String): String {
        return try {
            json.parseToJsonElement(raw).jsonObject["message"]
                ?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } ?: "You've used all the conversation minutes in your plan for this cycle. Upgrade to keep talking with Koda."
    }

    companion object {
        /** Default TTS voice ("Sarah"), identical to the web app default. */
        const val DEFAULT_VOICE_ID = "EXAVITQu4vr4xnSDxMaL"
    }
}

/** Tiny MediaPlayer wrapper for TTS playback from downloaded MP3 bytes. */
class TtsPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    @Volatile
    var isPlaying: Boolean = false
        private set

    fun play(bytes: ByteArray, onDone: () -> Unit = {}) {
        stop()
        try {
            val file = File(appContext.cacheDir, "koda_tts_${System.currentTimeMillis()}.mp3")
            file.writeBytes(bytes)
            isPlaying = true
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { stop(); onDone() }
                setOnErrorListener { _, _, _ -> stop(); onDone(); true }
                prepare()
                start()
            }
        } catch (_: Exception) {
            stop()
            onDone()
        }
    }

    fun stop() {
        isPlaying = false
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
