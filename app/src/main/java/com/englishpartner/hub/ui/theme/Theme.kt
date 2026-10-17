package com.englishpartner.hub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.englishpartner.hub.data.settings.ThemeMode

// Deep green + warm amber: calm for long study sessions, with a friendly accent.
private val Green10 = Color(0xFF00201A)
private val Green30 = Color(0xFF0E4E40)
private val Green40 = Color(0xFF1F5E4E)
private val Green80 = Color(0xFF8DD5BE)
private val Green90 = Color(0xFFC8F0E2)
private val Amber40 = Color(0xFF8A5A00)
private val Amber80 = Color(0xFFF2B84B)
private val Amber90 = Color(0xFFFFDDA6)

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Amber90,
    onSecondaryContainer = Color(0xFF2B1700),
    background = Color(0xFFFBFAF6),
    surface = Color(0xFFFBFAF6)
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Color(0xFF00382D),
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Amber80,
    onSecondary = Color(0xFF462B00),
    secondaryContainer = Color(0xFF643F00),
    onSecondaryContainer = Amber90,
    background = Color(0xFF121412),
    surface = Color(0xFF121412)
)

@Composable
fun EnglishPartnerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // A fixed brand palette (not wallpaper-based dynamic color) keeps the app recognisable.
    val colors = if (dark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
