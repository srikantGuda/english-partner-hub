@file:OptIn(ExperimentalMaterial3Api::class)

package com.englishpartner.hub.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.englishpartner.hub.AppContainer
import com.englishpartner.hub.data.settings.AppSettings
import com.englishpartner.hub.data.settings.ThemeMode
import com.englishpartner.hub.ui.common.ConfirmDialog
import com.englishpartner.hub.ui.common.appViewModel
import com.englishpartner.hub.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _busy = MutableStateFlow<String?>(null)
    val busy: StateFlow<String?> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _storage = MutableStateFlow(0L)
    val storage: StateFlow<Long> = _storage.asStateFlow()

    init { refreshStorage() }

    fun refreshStorage() {
        viewModelScope.launch { _storage.value = withContext(Dispatchers.IO) { c.repository.storageBytes() } }
    }

    fun consumeMessage() { _message.value = null }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { c.settings.setThemeMode(mode) }
    fun setSpeed(speed: Float) = viewModelScope.launch { c.settings.setPlaybackSpeed(speed) }

    fun export(uri: Uri) = viewModelScope.launch {
        _busy.value = "Creating backup… keep the app open."
        _message.value = try {
            val s = c.backup.export(uri)
            "Backup saved: ${s.days} days, ${s.materials} materials, ${s.files} files."
        } catch (t: Throwable) {
            "Backup failed: ${t.message ?: "unknown error"}"
        }
        _busy.value = null
    }

    fun restore(uri: Uri) = viewModelScope.launch {
        _busy.value = "Restoring backup… keep the app open."
        _message.value = try {
            val s = c.backup.restore(uri)
            "Restored ${s.days} days and ${s.materials} materials."
        } catch (t: Throwable) {
            "Restore failed: ${t.message ?: "unknown error"}"
        }
        _busy.value = null
        refreshStorage()
    }
}

@Composable
fun SettingsScreen() {
    val vm = appViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.export(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings & backup") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Section("Appearance") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setTheme(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            Section("Default audio/video speed") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Formatters.playbackSpeeds.forEach { s ->
                        FilterChip(
                            selected = settings.playbackSpeed == s,
                            onClick = { vm.setSpeed(s) },
                            label = { Text(Formatters.speedLabel(s)) }
                        )
                    }
                }
            }

            Section("Backup and restore") {
                Text(
                    "A backup is one .zip file with all your days, notes, words, progress and imported files. " +
                        "Save it to your phone storage, SD card or a USB drive. Nothing is uploaded to the internet.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Course files in the app: ${Formatters.fileSize(storage)}", style = MaterialTheme.typography.bodySmall)
                busy?.let {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("EnglishPartner-backup-$date.zip")
                    },
                    enabled = busy == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Backup, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Create backup")
                }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")) },
                    enabled = busy == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Restore from backup")
                }
            }

            Section("About") {
                Text("English Partner Learning Hub", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Works fully offline. No login, no ads, no analytics. Your course files stay on this phone.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = "Replace current data?",
            message = "Restoring replaces all days, materials, notes, words and progress in the app with the backup. " +
                "Create a backup first if you want to keep the current data.",
            confirmText = "Restore",
            onDismiss = { pendingRestore = null },
            onConfirm = { pendingRestore = null; vm.restore(uri) }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
