@file:OptIn(ExperimentalMaterial3Api::class)

package com.englishpartner.hub.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.englishpartner.hub.AppContainer
import com.englishpartner.hub.PendingShare
import com.englishpartner.hub.data.model.MaterialType
import com.englishpartner.hub.ui.common.appViewModel
import com.englishpartner.hub.ui.common.icon
import com.englishpartner.hub.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SharedItem(val name: String, val type: MaterialType, val size: Long)

data class ShareUi(
    val items: List<SharedItem> = emptyList(),
    val text: String? = null,
    val suggestedDay: Int = 1,
    val working: Boolean = false,
    val error: String? = null
)

class ShareImportViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(ShareUi())
    val ui: StateFlow<ShareUi> = _ui.asStateFlow()

    private var share: PendingShare? = null

    init {
        viewModelScope.launch {
            val pending = c.shareInbox.pending.value
            if (pending == null) {
                _ui.value = ShareUi(error = "Nothing to import. Share the files to the app again.")
                return@launch
            }
            share = pending
            val items = withContext(Dispatchers.IO) {
                pending.uris.map { uri ->
                    val info = runCatching { c.storage.describe(uri) }.getOrNull()
                    SharedItem(
                        name = info?.displayName ?: (uri.lastPathSegment ?: "file"),
                        type = MaterialType.detect(info?.mimeType, info?.displayName),
                        size = info?.sizeBytes ?: -1
                    )
                }
            }
            val guessed = items.firstNotNullOfOrNull { Formatters.guessDayNumber(it.name) }
                ?: Formatters.guessDayNumber(pending.subject)
                ?: Formatters.guessDayNumber(pending.text?.take(200))
            val fallback = c.repository.observeDaysWithStats().first()
                .firstOrNull { !it.day.completed }?.day?.dayNumber ?: 1
            _ui.value = ShareUi(items = items, text = pending.text, suggestedDay = guessed ?: fallback)
        }
    }

    fun import(dayNumber: Int, onDone: (Long) -> Unit) {
        val pending = share ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(working = true, error = null)
            try {
                val dayId = c.repository.getOrCreateDay(dayNumber)
                val result = if (pending.uris.isNotEmpty()) c.repository.importUris(dayId, pending.uris) else null
                val text = pending.text
                if (!text.isNullOrBlank()) {
                    val title = pending.subject?.takeIf { it.isNotBlank() } ?: "Day $dayNumber message"
                    c.repository.importText(dayId, title, text)
                }
                if (result != null && result.imported == 0 && text.isNullOrBlank()) {
                    _ui.value = _ui.value.copy(working = false, error = "The shared files could not be copied.")
                    return@launch
                }
                c.shareInbox.clear()
                onDone(dayId)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(working = false, error = t.message ?: "Import failed")
            }
        }
    }

    fun cancel() = c.shareInbox.clear()
}

@Composable
fun ShareImportScreen(onImported: (Long) -> Unit, onCancel: () -> Unit) {
    val vm = appViewModel(key = "share-import") { ShareImportViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    var dayText by rememberSaveable { mutableStateOf("") }
    var edited by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(ui.suggestedDay) {
        if (!edited) dayText = ui.suggestedDay.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to course") },
                navigationIcon = {
                    IconButton(onClick = { vm.cancel(); onCancel() }) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Received", style = MaterialTheme.typography.titleMedium)
                    ui.items.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.type.icon(), contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                "  ${item.name}" + if (item.size >= 0) " (${Formatters.fileSize(item.size)})" else "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    ui.text?.let {
                        Text("Text message:", style = MaterialTheme.typography.labelMedium)
                        Text(it, maxLines = 6, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                    if (ui.items.isEmpty() && ui.text == null && ui.error == null) Text("Loading…")
                }
            }

            OutlinedTextField(
                value = dayText,
                onValueChange = { v -> dayText = v.filter { it.isDigit() }.take(3); edited = true },
                label = { Text("Save to day number") },
                supportingText = { Text("If this day does not exist yet, it will be created.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (ui.working) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            val dayNumber = dayText.toIntOrNull()
            Button(
                onClick = { dayNumber?.let { vm.import(it, onImported) } },
                enabled = !ui.working && dayNumber != null && dayNumber in 1..999,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (dayNumber != null) "Save to Day $dayNumber" else "Enter a day number") }
            OutlinedButton(
                onClick = { vm.cancel(); onCancel() },
                enabled = !ui.working,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancel") }
        }
    }
}
