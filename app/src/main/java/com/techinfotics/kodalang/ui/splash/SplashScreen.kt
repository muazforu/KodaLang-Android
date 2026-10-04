package com.techinfotics.kodalang.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techinfotics.kodalang.core.Supa
import com.techinfotics.kodalang.data.AuthRepository
import kotlinx.coroutines.launch

class SplashViewModel(private val auth: AuthRepository) : ViewModel() {

    var state by mutableStateOf<SplashState>(SplashState.Loading)
        private set

    fun check() {
        if (!Supa.isConfigured()) {
            state = SplashState.MissingKeys
            return
        }
        viewModelScope.launch {
            val ok = auth.restoreSession()
            state = if (ok) SplashState.SignedIn else SplashState.SignedOut
        }
    }

    fun retry() = check()

    sealed interface SplashState {
        data object Loading : SplashState
        data object SignedIn : SplashState
        data object SignedOut : SplashState
        data object MissingKeys : SplashState
    }
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val state = viewModel.state

    LaunchedEffect(Unit) { viewModel.check() }
    LaunchedEffect(state) {
        when (state) {
            SplashViewModel.SplashState.SignedIn -> onSignedIn()
            SplashViewModel.SplashState.SignedOut -> onSignedOut()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "KodaLang",
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Speak English with confidence",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "by Techinfotics",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state == SplashViewModel.SplashState.MissingKeys) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = "The app is missing its Supabase API key.\n" +
                    "Add SUPABASE_ANON_KEY to local.properties (see README), then rebuild.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.retry() }) { Text("Retry") }
        }
    }
}
