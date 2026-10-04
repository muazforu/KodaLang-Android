package com.techinfotics.kodalang.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techinfotics.kodalang.data.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isSignUpMode by mutableStateOf(false)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    fun submit(onSignedIn: () -> Unit) {
        error = null
        notice = null
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            error = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            error = "Password must be at least 6 characters."
            return
        }
        loading = true
        viewModelScope.launch {
            val result = if (isSignUpMode) auth.signUp(email, password)
            else auth.signIn(email, password)
            loading = false
            when (result) {
                AuthRepository.AuthResult.SignedIn -> onSignedIn()
                AuthRepository.AuthResult.NeedsEmailConfirmation -> {
                    isSignUpMode = false
                    notice = "Account created! Please check your email to confirm it, then sign in."
                }
                is AuthRepository.AuthResult.Failed -> error = result.message
            }
        }
    }

    fun toggleMode() {
        isSignUpMode = !isSignUpMode
        error = null
        notice = null
    }
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onSignedIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "KodaLang",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (viewModel.isSignUpMode) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        viewModel.error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        viewModel.notice?.let { message ->
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.submit(onSignedIn) },
            enabled = !viewModel.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(if (viewModel.isSignUpMode) "Create account" else "Sign in")
        }
        TextButton(onClick = { viewModel.toggleMode() }) {
            Text(
                if (viewModel.isSignUpMode) "Already have an account? Sign in"
                else "New to KodaLang? Create an account"
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Uses the same account as kodalang.com — your progress syncs automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
