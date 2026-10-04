package com.techinfotics.kodalang.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// KodaLang brand palette (matches kodalang.com's violet identity).
private val KodaViolet = Color(0xFF6C4CF1)
private val KodaVioletDark = Color(0xFF5740C9)
private val KodaTeal = Color(0xFF00BFA6)
private val KodaAmber = Color(0xFFFFB020)

private val LightColors = lightColorScheme(
    primary = KodaViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E1FF),
    onPrimaryContainer = Color(0xFF241358),
    secondary = KodaTeal,
    onSecondary = Color.White,
    tertiary = KodaAmber,
    background = Color(0xFFFAFAFF),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA893FF),
    onPrimary = Color(0xFF1E1040),
    primaryContainer = KodaVioletDark,
    onPrimaryContainer = Color(0xFFE7E1FF),
    secondary = KodaTeal,
    onSecondary = Color(0xFF00332C),
    tertiary = KodaAmber,
    background = Color(0xFF12101D),
    surface = Color(0xFF1B1830),
)

@Composable
fun KodaLangTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
