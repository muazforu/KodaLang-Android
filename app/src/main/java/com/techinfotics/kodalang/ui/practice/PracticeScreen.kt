package com.techinfotics.kodalang.ui.practice

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techinfotics.kodalang.core.ChatMessage
import com.techinfotics.kodalang.core.ChatPart
import com.techinfotics.kodalang.core.Supa
import com.techinfotics.kodalang.core.TtsPlayer
import com.techinfotics.kodalang.core.TutorApi
import com.techinfotics.kodalang.core.UiChatMessage
import com.techinfotics.kodalang.core.WebPaths
import com.techinfotics.kodalang.core.openWebPage
import com.techinfotics.kodalang.data.AuthRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.UUID

class PracticeViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val auth = AuthRepository(appContext)
    private val api = TutorApi(appContext)
    private val ttsPlayer = TtsPlayer(appContext)

    val messages = mutableStateListOf<UiChatMessage>()
    var input by mutableStateOf("")
        private set
    var sending by mutableStateOf(false)
        private set
    var ttsLoadingId by mutableStateOf<String?>(null)
        private set
    var limitMessage by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var conversationId: String? = null

    var onSessionExpired: () -> Unit = {}

    fun onInputChange(value: String) {
        input = value
    }

    fun dismissLimit() {
        limitMessage = null
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || sending) return
        input = ""
        error = null
        val userMsg = UiChatMessage(id = UUID.randomUUID().toString(), role = "user", text = text)
        messages.add(userMsg)
        sending = true

        viewModelScope.launch {
            try {
                val token = auth.accessToken()
                val userId = auth.userId()
                if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                    onSessionExpired()
                    return@launch
                }
                val convId = conversationId ?: createConversation(userId, text).also {
                    conversationId = it
                }
                val history = messages.map { m ->
                    ChatMessage(
                        id = m.id,
                        role = m.role,
                        parts = listOf(ChatPart(text = m.text)),
                    )
                }
                when (val result = api.chat(token, convId, history)) {
                    is TutorApi.ChatResult.Text -> {
                        messages.add(
                            UiChatMessage(
                                id = UUID.randomUUID().toString(),
                                role = "assistant",
                                text = result.content,
                            )
                        )
                    }
                    is TutorApi.ChatResult.LimitReached -> limitMessage = result.message
                    TutorApi.ChatResult.SessionExpired -> onSessionExpired()
                    is TutorApi.ChatResult.Error -> error = result.message
                }
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong. Please try again."
            } finally {
                sending = false
            }
        }
    }

    /** Creates the conversation row the server expects (id must be a UUID). */
    private suspend fun createConversation(userId: String, firstMessage: String): String {
        val title = firstMessage.take(48).ifBlank { "Mobile chat" }
        val row = Supa.client(appContext).from("conversations")
            .insert(
                buildJsonObject {
                    put("user_id", userId)
                    put("title", title)
                    // Same scenario format as the web client: "tutor|<level>|text|<topic>"
                    put("scenario", "tutor||text|")
                }
            ) {
                select()
            }
            .decodeSingle<JsonObject>()
        return row["id"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("Could not start a conversation.")
    }

    fun speak(message: UiChatMessage) {
        if (ttsLoadingId != null || ttsPlayer.isPlaying) return
        ttsLoadingId = message.id
        viewModelScope.launch {
            try {
                val token = auth.accessToken()
                if (token.isNullOrBlank()) {
                    onSessionExpired()
                    return@launch
                }
                val bytes = api.tts(token, message.text)
                if (bytes != null) {
                    ttsPlayer.play(bytes) { ttsLoadingId = null }
                } else {
                    ttsLoadingId = null
                    error = "Couldn't play audio right now."
                }
            } catch (_: Exception) {
                ttsLoadingId = null
                error = "Couldn't play audio right now."
            }
        }
    }

    override fun onCleared() {
        ttsPlayer.stop()
        super.onCleared()
    }
}

@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    viewModel.onSessionExpired = onSessionExpired
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = "Practice with Koda",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        viewModel.limitMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Conversation limit reached", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Button(onClick = { openWebPage(context, WebPaths.PRICING) }) {
                            Text("View plans")
                        }
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.dismissLimit() }
                        ) { Text("Dismiss") }
                    }
                }
            }
        }

        if (viewModel.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Say hello to Koda!",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Type a message to start practising your English.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(viewModel.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        isSpeaking = viewModel.ttsLoadingId == message.id,
                        onSpeak = { viewModel.speak(message) },
                    )
                }
                if (viewModel.sending) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Card {
                                Box(Modifier.padding(14.dp)) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = viewModel.input,
                onValueChange = viewModel::onInputChange,
                placeholder = { Text("Type in English…") },
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.send() },
                enabled = !viewModel.sending && viewModel.input.isNotBlank(),
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: UiChatMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            colors = if (isUser) CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) else CardDefaults.cardColors(),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                if (!isUser) {
                    Spacer(Modifier.height(4.dp))
                    IconButton(
                        onClick = onSpeak,
                        enabled = !isSpeaking,
                        modifier = Modifier.size(32.dp),
                    ) {
                        if (isSpeaking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Listen",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
