@file:OptIn(ExperimentalMaterial3Api::class)

package com.englishpartner.hub.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.englishpartner.hub.data.LearningRepository
import com.englishpartner.hub.data.db.DayWithStats
import com.englishpartner.hub.data.db.displayTitle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.englishpartner.hub.ui.common.appViewModel

class LibraryViewModel(private val repo: LearningRepository) : ViewModel() {
    val days: StateFlow<List<DayWithStats>> = repo.observeDaysWithStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun addDay(): Long = repo.addNextDay()
}

private enum class DayFilter(val label: String) { ALL("All"), PENDING("To do"), DONE("Done"), WITH_FILES("Has files") }

@Composable
fun LibraryScreen(onOpenDay: (Long) -> Unit) {
    val vm = appViewModel { LibraryViewModel(it.repository) }
    val days by vm.days.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(DayFilter.ALL) }
    val scope = rememberCoroutineScope()

    val visible = days.filter { d ->
        val matchesFilter = when (filter) {
            DayFilter.ALL -> true
            DayFilter.PENDING -> !d.day.completed
            DayFilter.DONE -> d.day.completed
            DayFilter.WITH_FILES -> d.materialCount > 0
        }
        val q = query.trim()
        val matchesQuery = q.isEmpty() ||
            d.day.dayNumber.toString() == q ||
            d.day.displayTitle.contains(q, ignoreCase = true)
        matchesFilter && matchesQuery
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Course library") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { scope.launch { val id = vm.addDay(); if (id > 0) onOpenDay(id) } },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add day") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search day number or title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)
            ) {
                DayFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(visible, key = { it.day.id }) { d ->
                    ListItem(
                        headlineContent = { Text(d.day.displayTitle) },
                        supportingContent = {
                            Text(
                                when {
                                    d.materialCount == 0 -> "No materials yet"
                                    else -> "${d.completedCount}/${d.materialCount} materials done"
                                }
                            )
                        },
                        leadingContent = {
                            Icon(
                                if (d.day.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = if (d.day.completed) "Completed" else "Not completed",
                                tint = if (d.day.completed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                        },
                        modifier = Modifier.clickable { onOpenDay(d.day.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
