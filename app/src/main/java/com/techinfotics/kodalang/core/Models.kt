package com.techinfotics.kodalang.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data models mirroring the kodalang.com Supabase schema.
 *
 * Column names/types were taken from the web repo's generated
 * `src/integrations/supabase/types.ts`. Only columns the app actually
 * selects are modelled; unknown columns are never requested.
 */

@Serializable
data class Profile(
    val id: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("cefr_level") val cefrLevel: String? = null,
    @SerialName("native_language") val nativeLanguage: String? = null,
    @SerialName("daily_goal_minutes") val dailyGoalMinutes: Int = 15,
    @SerialName("total_xp") val totalXp: Int? = null,
    val plan: String? = null,
)

@Serializable
data class DailyProgress(
    val day: String = "",
    @SerialName("minutes_practiced") val minutesPracticed: Double = 0.0,
    @SerialName("xp_earned") val xpEarned: Int = 0,
    @SerialName("lessons_completed") val lessonsCompleted: Int = 0,
)

@Serializable
data class StreakRow(
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
)

@Serializable
data class SubscriptionStatus(
    val status: String? = null,
)

/** AI SDK v5 UIMessage shape, as expected by POST /api/tutor-chat. */
@Serializable
data class ChatPart(
    val type: String = "text",
    val text: String,
)

@Serializable
data class ChatMessage(
    val id: String,
    val role: String, // "user" | "assistant"
    val parts: List<ChatPart>,
)

@Serializable
data class ChatRequest(
    val messages: List<ChatMessage>,
    val conversationId: String,
)

/** UI-level chat message used by the Practice screen. */
data class UiChatMessage(
    val id: String,
    val role: String,
    val text: String,
)
