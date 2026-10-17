@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)

package com.englishpartner.hub.ui.vocab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.englishpartner.hub.data.LearningRepository
import com.englishpartner.hub.data.db.VocabWord
import com.englishpartner.hub.ui.common.ConfirmDialog
import com.englishpartner.hub.ui.common.EmptyState
import com.englishpartner.hub.ui.common.appViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VocabularyViewModel(private val repo: LearningRepository) : ViewModel() {
    val query = MutableStateFlow("")

    val words: StateFlow<List<VocabWord>> = query
        .flatMapLatest { repo.searchVocabulary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(w: VocabWord) = viewModelScope.launch { repo.saveWord(w) }
    fun delete(w: VocabWord) = viewModelScope.launch { repo.deleteWord(w) }
    fun toggleMastered(w: VocabWord) = viewModelScope.launch { repo.saveWord(w.copy(mastered = !w.mastered)) }
}

@Composable
fun VocabularyScreen() {
    val vm = appViewModel { VocabularyViewModel(it.repository) }
    val query by vm.query.collectAsStateWithLifecycle()
    val words by vm.words.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<VocabWord?>(null) }
    var deleting by remember { mutableStateOf<VocabWord?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My vocabulary") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = VocabWord(word = "") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add word")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search words or meanings") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (words.isEmpty()) {
                EmptyState(
                    Icons.Filled.Translate,
                    if (query.isBlank()) "No words saved yet" else "No matches",
                    "Save new words from your classes. Tap ★ when you have learned one."
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(words, key = { it.id }) { w ->
                        ListItem(
                            headlineContent = { Text(w.word, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = {
                                Column {
                                    if (w.meaning.isNotBlank()) Text(w.meaning)
                                    if (w.example.isNotBlank()) {
                                        Text("“${w.example}”", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                            leadingContent = {
                                IconButton(onClick = { vm.toggleMastered(w) }) {
                                    Icon(
                                        if (w.mastered) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = if (w.mastered) "Learned" else "Mark as learned",
                                        tint = if (w.mastered) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { deleting = w }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            },
                            modifier = Modifier.clickable { editing = w }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    editing?.let { w ->
        VocabDialog(initial = w, onDismiss = { editing = null }, onSave = { vm.save(it); editing = null })
    }
    deleting?.let { w ->
        ConfirmDialog(
            title = "Delete \"${w.word}\"?",
            message = "This word will be removed from your vocabulary.",
            onDismiss = { deleting = null },
            onConfirm = { vm.delete(w); deleting = null }
        )
    }
}
