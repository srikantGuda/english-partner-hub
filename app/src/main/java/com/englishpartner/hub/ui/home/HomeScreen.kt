@file:OptIn(ExperimentalMaterial3Api::class)

package com.englishpartner.hub.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.englishpartner.hub.data.LearningRepository
import com.englishpartner.hub.data.db.CourseDay
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.displayTitle
import com.englishpartner.hub.data.db.materialType
import com.englishpartner.hub.data.db.playbackFraction
import com.englishpartner.hub.data.model.MaterialType
import com.englishpartner.hub.ui.common.appViewModel
import com.englishpartner.hub.ui.common.icon
import com.englishpartner.hub.util.Formatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RecentItem(val material: Material, val day: CourseDay?)

data class HomeState(
    val totalDays: Int = 0,
    val completedDays: Int = 0,
    val totalMaterials: Int = 0,
    val completedMaterials: Int = 0,
    val vocabCount: Int = 0,
    val nextDay: CourseDay? = null,
    val recent: List<RecentItem> = emptyList()
)

class HomeViewModel(repo: LearningRepository) : ViewModel() {
    val state: StateFlow<HomeState> = combine(
        repo.observeDaysWithStats(),
        repo.observeRecentMaterials(5),
        repo.observeVocabCount()
    ) { days, recent, vocab ->
        val byId = days.associate { it.day.id to it.day }
        HomeState(
            totalDays = days.size,
            completedDays = days.count { it.day.completed },
            totalMaterials = days.sumOf { it.materialCount },
            completedMaterials = days.sumOf { it.completedCount },
            vocabCount = vocab,
            nextDay = days.firstOrNull { !it.day.completed }?.day,
            recent = recent.map { RecentItem(it, byId[it.dayId]) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}

@Composable
fun HomeScreen(
    onOpenMaterial: (Long) -> Unit,
    onOpenDay: (Long) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val vm = appViewModel { HomeViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("English Partner") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ProgressCard(state) }

            val continueItem = state.recent.firstOrNull()
            item {
                Text("Continue learning", style = MaterialTheme.typography.titleMedium)
            }
            if (continueItem != null) {
                item { ContinueCard(continueItem, onClick = { onOpenMaterial(continueItem.material.id) }) }
            } else {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Nothing opened yet", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Open a day, add your WhatsApp audio, videos or PDFs, and start learning. " +
                                    "Tip: in WhatsApp, long-press a file → Share → English Partner.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            state.nextDay?.let { next ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenDay(next.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.School, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Next class to finish", style = MaterialTheme.typography.labelMedium)
                                Text(next.displayTitle, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            if (state.recent.size > 1) {
                item { Text("Recently opened", style = MaterialTheme.typography.titleMedium) }
                items(state.recent.drop(1), key = { it.material.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.material.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(item.day?.displayTitle ?: "") },
                        leadingContent = { Icon(item.material.materialType.icon(), contentDescription = null) },
                        modifier = Modifier.clickable { onOpenMaterial(item.material.id) }
                    )
                }
            }

            item {
                FilledTonalButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
                    Text("Open course library")
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(state: HomeState) {
    val dayFraction = if (state.totalDays == 0) 0f else state.completedDays.toFloat() / state.totalDays
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Your progress", style = MaterialTheme.typography.labelLarge)
            Text(
                "${Formatters.percent(state.completedDays, state.totalDays)}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text("${state.completedDays} of ${state.totalDays} days completed")
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { dayFraction }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Stat("Materials", "${state.completedMaterials}/${state.totalMaterials}")
                Stat("Words saved", "${state.vocabCount}")
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ContinueCard(item: RecentItem, onClick: () -> Unit) {
    val m = item.material
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(m.materialType.icon(), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.day?.displayTitle ?: "", style = MaterialTheme.typography.labelMedium)
                Text(m.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (m.materialType == MaterialType.AUDIO || m.materialType == MaterialType.VIDEO) {
                    if (m.durationMs > 0) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { m.playbackFraction }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "${Formatters.duration(m.positionMs)} / ${Formatters.duration(m.durationMs)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (m.materialType == MaterialType.PDF) {
                    Text("Page ${m.lastPage + 1}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Continue")
        }
    }
}
