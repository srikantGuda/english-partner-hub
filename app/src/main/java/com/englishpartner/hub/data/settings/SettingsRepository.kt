package com.englishpartner.hub.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val playbackSpeed: Float = 1.0f,
    val readerFontSize: Float = 18f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

/** Small user preferences stored with DataStore (not lesson data, which lives in Room). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val speed = floatPreferencesKey("playback_speed")
        val fontSize = floatPreferencesKey("reader_font_size")
        val theme = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            playbackSpeed = p[Keys.speed] ?: 1.0f,
            readerFontSize = p[Keys.fontSize] ?: 18f,
            themeMode = ThemeMode.entries.firstOrNull { it.name == p[Keys.theme] } ?: ThemeMode.SYSTEM
        )
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.settingsStore.edit { it[Keys.speed] = speed }
    }

    suspend fun setReaderFontSize(size: Float) {
        context.settingsStore.edit { it[Keys.fontSize] = size.coerceIn(12f, 32f) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { it[Keys.theme] = mode.name }
    }
}
