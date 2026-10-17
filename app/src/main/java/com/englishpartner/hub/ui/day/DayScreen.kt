@file:OptIn(ExperimentalMaterial3Api::class)

package com.englishpartner.hub.ui.day

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.Note
import com.englishpartner.hub.data.db.VocabWord
import com.englishpartner.hub.data.db.displayTitle
import com.englishpartner.hub.data.db.materialType
import com.englishpartner.hub.data.db.playbackFraction
import com.englishpartner.hub.data.model.MaterialType
import com.englishpartner.hub.ui.common.ConfirmDialog
import com.englishpartner.hub.ui.common.EmptyState
import com.englishpartner.hub.ui.common.TextInputDialog
import com.englishpartner.hub.ui.common.appViewModel
import com.englishpartner.hub.ui.common.icon
import com.englishpartner.hub.ui.vocab.VocabDialog
import com.englishpartner.hub.util.Formatters
import java.text.DateFormat
import java.util.Date

/** Mime types offered in the system file picker. */
val PICKER_MIME_TYPES = arrayOf("audio/*", "video/*", "image/*", "application/pdf", "text/*", "application/ogg")

@Composable
fun DayScreen(
    dayId: Long,
    onBack: () -> Unit,
    onOpenMaterial: (Long) -> Unit
) {
    val vm = appViewModel(key = "day-$dayId") { DayViewModel(it.repository, dayId) }
    val day by vm.day.collectAsStateWithLifecycle()
    val materials by vm.materials.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val words by vm.words.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var newNote by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<VocabWord?>(null) }
    val snackbar = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.import(uris)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val current = day
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.displayTitle ?: "Day", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { vm.toggleCompleted() }) {
                        Icon(
                            if (current?.completed == true) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (current?.completed == true) "Mark not done" else "Mark day done",
                            tint = if (current?.completed == true) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Rename day") }, onClick = { menuOpen = false; renaming = true })
                            DropdownMenuItem(text = { Text("Delete day") }, onClick = { menuOpen = false; confirmDelete = true })
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            when (tab) {
                0 -> ExtendedFloatingActionButton(
                    onClick = { picker.launch(PICKER_MIME_TYPES) },
                    icon = { Icon(Icons.Filled.UploadFile, contentDescription = null) },
                    text = { Text("Add files") }
                )
                1 -> ExtendedFloatingActionButton(
                    onClick = { newNote = true },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    text = { Text("New note") }
                )
                else -> ExtendedFloatingActionButton(
                    onClick = { editingWord = VocabWord(word = "", dayId = dayId) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New word") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Materials (${materials.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Notes (${notes.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Words (${words.size})") })
            }
            if (importing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Copying files into the app…",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            when (tab) {
                0 -> MaterialsTab(
                    materials = materials,
                    onOpen = onOpenMaterial,
                    vm = vm
                )
                1 -> NotesTab(notes = notes, onEdit = { editingNote = it }, onDelete = { vm.deleteNote(it) })
                else -> WordsTab(words = words, onEdit = { editingWord = it }, onDelete = { vm.deleteWord(it) })
            }
        }
    }

    if (renaming && current != null) {
        TextInputDialog(
            title = "Rename Day ${current.dayNumber}",
            initial = current.title,
            label = "Topic (optional), e.g. Past tense",
            onDismiss = { renaming = false },
            onConfirm = { vm.rename(it); renaming = false }
        )
    }
    if (confirmDelete && current != null) {
        ConfirmDialog(
            title = "Delete ${current.displayTitle}?",
            message = "All materials, notes and bookmarks of this day will be removed from the app. This cannot be undone.",
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; vm.deleteDay(onBack) }
        )
    }
    if (newNote || editingNote != null) {
        TextInputDialog(
            title = if (newNote) "New note" else "Edit note",
            initial = editingNote?.text ?: "",
            singleLine = false,
            onDismiss = { newNote = false; editingNote = null },
            onConfirm = { vm.saveNote(editingNote, it); newNote = false; editingNote = null }
        )
    }
    editingWord?.let { w ->
        VocabDialog(
            initial = w,
            onDismiss = { editingWord = null },
            onSave = { vm.saveWord(it); editingWord = null }
        )
    }
}

@Composable
private fun MaterialsTab(
    materials: List<Material>,
    onOpen: (Long) -> Unit,
    vm: DayViewModel
) {
    var renameTarget by remember { mutableStateOf<Material?>(null) }
    var moveTarget by remember { mutableStateOf<Material?>(null) }
    var deleteTarget by remember { mutableStateOf<Material?>(null) }

    if (materials.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.UploadFile,
            title = "No materials yet",
            message = "Tap \"Add files\" to pick audio, video, PDF, images or text from your phone. " +
                "You can also share files from WhatsApp straight into the app."
        )
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(materials, key = { it.id }) { m ->
                MaterialRow(
                    m = m,
                    onClick = { onOpen(m.id) },
                    onToggleDone = { vm.toggleMaterialDone(m) },
                    onRename = { renameTarget = m },
                    onMove = { moveTarget = m },
                    onDelete = { deleteTarget = m }
                )
                HorizontalDivider()
            }
        }
    }

    renameTarget?.let { m ->
        TextInputDialog(
            title = "Rename",
            initial = m.displayName,
            onDismiss = { renameTarget = null },
            onConfirm = { vm.renameMaterial(m, it); renameTarget = null }
        )
    }
    moveTarget?.let { m ->
        TextInputDialog(
            title = "Move to day",
            label = "Day number",
            numeric = true,
            confirmText = "Move",
            onDismiss = { moveTarget = null },
            onConfirm = { text ->
                val n = text.trim().toIntOrNull()
                if (n != null && n in 1..999) vm.moveMaterial(m, n)
                moveTarget = null
            }
        )
    }
    deleteTarget?.let { m ->
        ConfirmDialog(
            title = "Delete \"${m.displayName}\"?",
            message = "The file will be removed from the app's storage.",
            onDismiss = { deleteTarget = null },
            onConfirm = { vm.deleteMaterial(m); deleteTarget = null }
        )
    }
}

@Composable
private fun MaterialRow(
    m: Material,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val type = m.materialType
    val details = buildString {
        append(type.label)
        append(" · ")
        append(Formatters.fileSize(m.sizeBytes))
        if ((type == MaterialType.AUDIO || type == MaterialType.VIDEO) && m.durationMs > 0) {
            append(" · ")
            append(Formatters.duration(m.positionMs))
            append(" / ")
            append(Formatters.duration(m.durationMs))
        }
        if (type == MaterialType.PDF && m.lastPage > 0) append(" · page ${m.lastPage + 1}")
    }
    ListItem(
        headlineContent = { Text(m.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(details)
                if ((type == MaterialType.AUDIO || type == MaterialType.VIDEO) && m.durationMs > 0 && m.positionMs > 0) {
                    LinearProgressIndicator(
                        progress = { m.playbackFraction },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                type.icon(),
                contentDescription = type.label,
                tint = if (m.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (m.completed) "Mark not done" else "Mark done") },
                        onClick = { menu = false; onToggleDone() }
                    )
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Move to another day") }, onClick = { menu = false; onMove() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun NotesTab(notes: List<Note>, onEdit: (Note) -> Unit, onDelete: (Note) -> Unit) {
    if (notes.isEmpty()) {
        EmptyState(Icons.Filled.Edit, "No notes yet", "Write down what you learned, questions for the teacher, or homework.")
        return
    }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(notes, key = { it.id }) { n ->
            var menu by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(n.text) },
                supportingContent = { Text(fmt.format(Date(n.updatedAt))) },
                trailingContent = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit(n) })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete(n) })
                        }
                    }
                },
                modifier = Modifier.clickable { onEdit(n) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun WordsTab(words: List<VocabWord>, onEdit: (VocabWord) -> Unit, onDelete: (VocabWord) -> Unit) {
    if (words.isEmpty()) {
        EmptyState(Icons.Filled.Translate, "No words for this day", "Save new words and phrases you learned in this class.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(words, key = { it.id }) { w ->
            var menu by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(w.word) },
                supportingContent = {
                    Column {
                        if (w.meaning.isNotBlank()) Text(w.meaning)
                        if (w.example.isNotBlank()) Text("“${w.example}”", style = MaterialTheme.typography.bodySmall)
                    }
                },
                trailingContent = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit(w) })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete(w) })
                        }
                    }
                },
                modifier = Modifier.clickable { onEdit(w) }
            )
            HorizontalDivider()
        }
    }
}
