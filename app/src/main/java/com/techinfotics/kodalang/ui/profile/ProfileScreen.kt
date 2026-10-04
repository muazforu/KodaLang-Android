package com.techinfotics.kodalang.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techinfotics.kodalang.core.WebPaths
import com.techinfotics.kodalang.core.openWebPage
import com.techinfotics.kodalang.data.AuthRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val auth: AuthRepository) : ViewModel() {

    var email by mutableStateOf(auth.email() ?: "")
        private set
    var signingOut by mutableStateOf(false)
        private set

    fun signOut(onSignedOut: () -> Unit) {
        signingOut = true
        viewModelScope.launch {
            auth.signOut()
            signingOut = false
            onSignedOut()
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSignedOut: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Email", style = MaterialTheme.typography.labelMedium)
                Text(
                    viewModel.email.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Signed in with the same account as kodalang.com. " +
                        "Your learning data stays in sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = { openWebPage(context, WebPaths.HOME) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open kodalang.com")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { viewModel.signOut(onSignedOut) },
            enabled = !viewModel.signingOut,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "To delete your account, please use the website " +
                "(Settings → Delete account on kodalang.com).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "KodaLang v1.0.0 · by Techinfotics",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
