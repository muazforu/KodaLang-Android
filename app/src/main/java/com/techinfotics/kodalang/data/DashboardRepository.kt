package com.techinfotics.kodalang.data

import android.content.Context
import com.techinfotics.kodalang.core.DailyProgress
import com.techinfotics.kodalang.core.Profile
import com.techinfotics.kodalang.core.StreakRow
import com.techinfotics.kodalang.core.SubscriptionStatus
import com.techinfotics.kodalang.core.Supa
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

/**
 * Reads the learner's dashboard data from the shared Supabase tables,
 * using the signed-in user's JWT (same RLS as the website).
 *
 * Every query is defensive: missing rows / unreadable tables degrade to
 * sensible defaults instead of crashing.
 */
class DashboardRepository(context: Context) {

    private val client = Supa.client(context.applicationContext)

    data class Dashboard(
        val profile: Profile?,
        val today: DailyProgress?,
        val currentStreak: Int,
        val planLabel: String,
    )

    suspend fun load(userId: String): Dashboard = coroutineScope {
        val profileDeferred = async {
            runCatching {
                client.from("profiles")
                    .select(
                        Columns.list(
                            "id",
                            "display_name",
                            "cefr_level",
                            "native_language",
                            "daily_goal_minutes",
                            "total_xp",
                            "plan",
                        )
                    ) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<Profile>()
            }.getOrNull()
        }

        val todayStr = LocalDate.now().toString() // "YYYY-MM-DD", matches the `day` column
        val todayDeferred = async {
            runCatching {
                client.from("daily_progress")
                    .select(
                        Columns.list(
                            "day",
                            "minutes_practiced",
                            "xp_earned",
                            "lessons_completed",
                        )
                    ) {
                        filter { eq("user_id", userId); eq("day", todayStr) }
                    }
                    .decodeSingleOrNull<DailyProgress>()
            }.getOrNull()
        }

        val streakDeferred = async {
            runCatching {
                client.from("streaks")
                    .select(Columns.list("current_streak", "longest_streak")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeSingleOrNull<StreakRow>()
            }.getOrNull()?.currentStreak ?: 0
        }

        // A subscription row with an active/trialing status means a paid plan.
        val hasPaidSubscriptionDeferred = async {
            runCatching {
                client.from("subscriptions")
                    .select(Columns.list("status")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<SubscriptionStatus>()
                    .any { it.status == "active" || it.status == "trialing" }
            }.getOrDefault(false)
        }

        val profile = profileDeferred.await()
        val hasPaid = hasPaidSubscriptionDeferred.await()
        val planLabel = when {
            hasPaid -> (profile?.plan?.takeIf { it.isNotBlank() } ?: "Premium").prettyPlan()
            else -> "Free"
        }

        Dashboard(
            profile = profile,
            today = todayDeferred.await(),
            currentStreak = streakDeferred.await(),
            planLabel = planLabel,
        )
    }

    private fun String.prettyPlan(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
