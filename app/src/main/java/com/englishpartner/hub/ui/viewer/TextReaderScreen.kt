package com.englishpartner.hub.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.englishpartner.hub.data.LearningRepository
import com.englishpartner.hub.data.db.Bookmark
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.VocabWord
import com.englishpartner.hub.data.settings.AppSettings
import com.englishpartner.hub.data.settings.SettingsRepository
import com.englishpartner.hub.ui.common.EmptyState
import com.englishpartner.hub.ui.common.appViewModel
import com.englishpartner.hub.ui.vocab.VocabDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Splits an article into paragraphs. Pure function so it can be unit tested. */
object ArticleParser {
    fun paragraphs(text: String): List<String> {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val byBlank = normalized.split(Regex("""\n\s*\n""")).map { it.trim() }.filter { it.isNotEmpty() }
        // Chat exports often use single newlines only; split those too when there is just one block.
        return if (byBlank.size <= 1) normalized.lines().map { it.trim() }.filter { it.isNotEmpty() } else byBlank
    }
}

class TextReaderViewModel(
    private val repo: LearningRepository,
    private val settingsRepo: SettingsRepository,
    private val appScope: CoroutineScope,
    val material: Material
) : ViewModel() {

    private val _paragraphs = MutableStateFlow<List<String>?>(null)
    val paragraphs: StateFlow<List<String>?> = _paragraphs.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val bookmarks: StateFlow<List<Bookmark>> = repo.observeBookmarks(material.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _paragraphs.value = withContext(Dispatchers.IO) {
                runCatching { ArticleParser.paragraphs(repo.fileFor(material).readText()) }.getOrDefault(emptyList())
            }
            repo.markOpened(material.id)
        }
    }

    fun changeFont(delta: Float) {
        viewModelScope.launch { settingsRepo.setReaderFontSize(settings.value.readerFontSize + delta) }
    }

    fun savePosition(paragraph: Int) {
        appScope.launch { repo.saveReadingPosition(material.id, paragraph) }
    }

    fun addBookmark(paragraph: Int, preview: String) {
        viewModelScope.launch { repo.addBookmark(material.id, paragraph.toLong(), preview.take(60)) }
    }

    fun deleteBookmark(b: Bookmark) {
        viewModelScope.launch { repo.deleteBookmark(b) }
    }

    fun markDone() {
        viewModelScope.launch { repo.setMaterialCompleted(material.id, true) }
    }

    fun saveWord(w: VocabWord) {
        viewModelScope.launch { repo.saveWord(w) }
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun TextReaderScreen(material: Material, onBack: () -> Unit) {
    val vm = appViewModel(key = "text-${material.id}") {
        TextReaderViewModel(it.repository, it.settings, it.appScope, material)
    }
    val paragraphs by vm.paragraphs.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    var showBookmarks by remember { mutableStateOf(false) }
    var newWord by remember { mutableStateOf(false) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = material.lastPage.coerceAtLeast(0))
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(600)
            .collect { vm.savePosition(it) }
    }

    val current = listState.firstVisibleItemIndex
    val list = paragraphs

    Scaffold(
        topBar = {
            ViewerTopBar(material.displayName, onBack) {
                TextButton(onClick = { vm.changeFont(-2f) }) { Text("A−") }
                TextButton(onClick = { vm.changeFont(2f) }) { Text("A+") }
                IconButton(onClick = { showBookmarks = true }) { Icon(Icons.Filled.Bookmark, contentDescription = "Bookmarks") }
                IconButton(onClick = { vm.markDone() }) { Icon(Icons.Filled.CheckCircle, contentDescription = "Mark as done") }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { newWord = true },
                icon = { Icon(Icons.Filled.Translate, contentDescription = null) },
                text = { Text("Save word") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                list.isEmpty() -> EmptyState(Icons.Filled.Description, "Empty article", "This text file has no readable content.")
                else -> SelectionContainer {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(list) { _, p ->
                            Text(
                                p,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = settings.readerFontSize.sp,
                                    lineHeight = (settings.readerFontSize * 1.5f).sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBookmarks) {
        val count = list?.size ?: 0
        BookmarksDialog(
            bookmarks = bookmarks,
            positionLabel = { "Paragraph ${it + 1}" },
            addLabel = "Bookmark here",
            onAdd = { vm.addBookmark(current, list?.getOrNull(current) ?: "") },
            onJump = { b ->
                showBookmarks = false
                scope.launch { listState.scrollToItem(b.position.toInt().coerceIn(0, (count - 1).coerceAtLeast(0))) }
            },
            onDelete = { vm.deleteBookmark(it) },
            onDismiss = { showBookmarks = false }
        )
    }
    if (newWord) {
        VocabDialog(
            initial = VocabWord(word = "", dayId = material.dayId),
            onDismiss = { newWord = false },
            onSave = { vm.saveWord(it); newWord = false }
        )
    }
}
