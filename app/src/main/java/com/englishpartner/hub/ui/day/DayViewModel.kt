package com.englishpartner.hub.ui.day

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishpartner.hub.data.LearningRepository
import com.englishpartner.hub.data.db.CourseDay
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.Note
import com.englishpartner.hub.data.db.VocabWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayViewModel(
    private val repo: LearningRepository,
    val dayId: Long
) : ViewModel() {

    val day: StateFlow<CourseDay?> = repo.observeDay(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val materials: StateFlow<List<Material>> = repo.observeMaterials(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<Note>> = repo.observeNotes(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val words: StateFlow<List<VocabWord>> = repo.observeVocabForDay(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importing.value = true
            val result = repo.importUris(dayId, uris)
            _importing.value = false
            _message.value = buildString {
                append("Added ${result.imported} file")
                if (result.imported != 1) append("s")
                if (result.failed.isNotEmpty()) append(". ${result.failed.size} could not be imported.")
            }
        }
    }

    fun rename(title: String) = viewModelScope.launch { day.value?.let { repo.renameDay(it, title) } }

    fun toggleCompleted() = viewModelScope.launch {
        day.value?.let { repo.setDayCompleted(it, !it.completed) }
    }

    fun deleteDay(onDone: () -> Unit) = viewModelScope.launch {
        day.value?.let { repo.deleteDay(it) }
        onDone()
    }

    fun renameMaterial(m: Material, name: String) = viewModelScope.launch { repo.renameMaterial(m, name) }

    fun toggleMaterialDone(m: Material) = viewModelScope.launch { repo.setMaterialCompleted(m.id, !m.completed) }

    fun deleteMaterial(m: Material) = viewModelScope.launch { repo.deleteMaterial(m) }

    fun moveMaterial(m: Material, dayNumber: Int) = viewModelScope.launch {
        val target = repo.getOrCreateDay(dayNumber)
        repo.moveMaterial(m, target)
        _message.value = "Moved to Day $dayNumber"
    }

    fun saveNote(existing: Note?, text: String) = viewModelScope.launch { repo.saveNote(dayId, existing, text) }

    fun deleteNote(note: Note) = viewModelScope.launch { repo.deleteNote(note) }

    fun saveWord(word: VocabWord) = viewModelScope.launch { repo.saveWord(word) }

    fun deleteWord(word: VocabWord) = viewModelScope.launch { repo.deleteWord(word) }
}
