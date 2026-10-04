package com.techinfotics.kodalang.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techinfotics.kodalang.core.WebPaths
import com.techinfotics.kodalang.core.openWebPage
import com.techinfotics.kodalang.data.AuthRepository
import com.techinfotics.kodalang.data.DashboardRepository
import kotlinx.coroutines.launch
import kotlin.math.min

class HomeViewModel(
    private val dashboard: DashboardRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Loading)
        private set

    fun load() {
        val userId = auth.userId() ?: run {
            uiState = HomeUiState.Error("Not signed in.")
            return
        }
        uiState = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val data = dashboard.load(userId)
                uiState = HomeUiState.Ready(data)
            } catch (e: Exception) {
                uiState = HomeUiState.Error("Couldn't load your dashboard. Pull to retry.")
            }
        }
    }

    sealed interface HomeUiState {
        data object Loading : HomeUiState
        data class Ready(val dashboard: DashboardRepository.Dashboard) : HomeUiState
        data class Error(val message: String) : HomeUiState
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenPractice: () -> Unit,
) {
    val context = LocalContext.current
    val state = viewModel.uiState

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        when (state) {
            HomeViewModel.HomeUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeViewModel.HomeUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message)
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.material3.Button(onClick = { viewModel.load() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is HomeViewModel.HomeUiState.Ready -> {
                DashboardContent(
                    dashboard = state.dashboard,
                    onOpenPractice = onOpenPractice,
                    onOpenWeb = { path -> openWebPage(context, path) },
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    dashboard: DashboardRepository.Dashboard,
    onOpenPractice: () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    val profile = dashboard.profile
    val name = profile?.displayName?.takeIf { it.isNotBlank() } ?: "there"
    val today = dashboard.today
    val goalMinutes = profile?.dailyGoalMinutes?.takeIf { it > 0 } ?: 15
    val practiced = today?.minutesPracticed ?: 0.0
    val progress = min(1f, (practiced / goalMinutes).toFloat())

    Text(
        text = "Bonjour, $name 👋",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        profile?.cefrLevel?.let {
            AssistChip(onClick = {}, label = { Text("Level $it") })
        }
        AssistChip(onClick = {}, label = { Text("Plan: ${dashboard.planLabel}") })
    }

    Spacer(Modifier.height(20.dp))

    // Daily goal ring
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoalRing(progress = progress)
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Today's goal", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${practiced.toInt()} / $goalMinutes min",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (progress >= 1f) "Goal reached — amazing! 🎉"
                    else "Keep going, every minute counts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Streak + XP
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            title = "🔥 Streak",
            value = "${dashboard.currentStreak}",
            subtitle = if (dashboard.currentStreak == 1) "day" else "days",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "⭐ Total XP",
            value = "${profile?.totalXp ?: 0}",
            subtitle = "all time",
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(20.dp))
    Text("Continue learning", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(12.dp))

    FeatureCard(
        icon = Icons.Filled.ChatBubble,
        title = "Practice speaking",
        subtitle = "Chat with Koda, your AI English tutor",
        onClick = onOpenPractice,
        primary = true,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "More on the website",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
    )
    Spacer(Modifier.height(12.dp))
    FeatureCard(
        icon = Icons.Filled.Star,
        title = "My Goal",
        subtitle = "Your goal-driven learning plan",
        onClick = { onOpenWeb(WebPaths.GOAL) },
    )
    Spacer(Modifier.height(10.dp))
    FeatureCard(
        icon = Icons.Filled.Favorite,
        title = "Memory Health",
        subtitle = "What to review before you forget it",
        onClick = { onOpenWeb(WebPaths.MEMORY_HEALTH) },
    )
    Spacer(Modifier.height(10.dp))
    FeatureCard(
        icon = Icons.Filled.School,
        title = "Exam Simulator",
        subtitle = "TOEIC & IELTS practice tests",
        onClick = { onOpenWeb(WebPaths.EXAM) },
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun GoalRing(progress: Float) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 22f, cap = StrokeCap.Round),
            )
            drawArc(
                color = fill,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 22f, cap = StrokeCap.Round),
            )
        }
        Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (primary) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!primary) {
                Icon(
                    Icons.Filled.OpenInNew,
                    contentDescription = "Opens the website",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
