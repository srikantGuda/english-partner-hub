package com.englishpartner.hub.ui.viewer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.materialType
import com.englishpartner.hub.data.model.MaterialType
import com.englishpartner.hub.ui.common.appViewModel
import com.englishpartner.hub.ui.common.findActivity
import com.englishpartner.hub.util.Formatters

@Composable
fun PlayerScreen(material: Material, onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel(key = "player-${material.id}") {
        PlayerViewModel(context.applicationContext, it.repository, it.settings, it.appScope, material)
    }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    var showBookmarks by remember { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val isVideo = material.materialType == MaterialType.VIDEO

    if (isVideo && fullscreen) {
        FullscreenVideo(vm = vm, onExit = { fullscreen = false })
        return
    }

    Scaffold(
        topBar = {
            ViewerTopBar(material.displayName, onBack) {
                IconButton(onClick = { showBookmarks = true }) { Icon(Icons.Filled.Bookmark, contentDescription = "Bookmarks") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isVideo) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
                ) {
                    VideoSurface(vm, Modifier.fillMaxSize())
                    IconButton(
                        onClick = { fullscreen = true },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) { Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen", tint = Color.White) }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(72.dp))
                    }
                }
            }

            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            SeekBar(ui = ui, onSeek = vm::seekTo)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.seekBy(-10_000) }) {
                    Icon(Icons.Filled.Replay10, contentDescription = "Back 10 seconds", modifier = Modifier.size(36.dp))
                }
                FilledIconButton(onClick = vm::togglePlay, modifier = Modifier.size(72.dp)) {
                    Icon(
                        if (ui.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (ui.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { vm.seekBy(10_000) }) {
                    Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", modifier = Modifier.size(36.dp))
                }
            }

            Text("Speed", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Formatters.playbackSpeeds.forEach { s ->
                    FilterChip(
                        selected = ui.speed == s,
                        onClick = { vm.setSpeed(s) },
                        label = { Text(Formatters.speedLabel(s)) }
                    )
                }
            }

            OutlinedButton(onClick = vm::markDone, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Mark as done")
            }
            if (!isVideo) {
                Text(
                    "Audio keeps playing with the screen off while this page is open.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showBookmarks) {
        BookmarksDialog(
            bookmarks = bookmarks,
            positionLabel = { Formatters.duration(it) },
            addLabel = "Bookmark ${Formatters.duration(ui.positionMs)}",
            onAdd = { vm.addBookmark() },
            onJump = { vm.seekTo(it.position); showBookmarks = false },
            onDelete = { vm.deleteBookmark(it) },
            onDismiss = { showBookmarks = false }
        )
    }
}

@Composable
private fun SeekBar(ui: PlaybackUi, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = ui.durationMs.coerceAtLeast(1)
    val value = if (dragging) dragValue else (ui.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    Column {
        Slider(
            value = value,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                onSeek((dragValue * duration).toLong())
                dragging = false
            },
            enabled = ui.durationMs > 0
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = if (dragging) (dragValue * duration).toLong() else ui.positionMs
            Text(Formatters.duration(shown), style = MaterialTheme.typography.bodySmall)
            Text(Formatters.duration(ui.durationMs), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Media3's PlayerView, used only as a video surface with its own built-in controls. */
@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(vm: PlayerViewModel, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
                keepScreenOn = true
                player = vm.player
            }
        },
        update = { view -> if (view.player !== vm.player) view.player = vm.player },
        onRelease = { view -> view.player = null },
        modifier = modifier
    )
}

/** Landscape, edge-to-edge video. Back or the exit button returns to the normal screen. */
@Composable
private fun FullscreenVideo(vm: PlayerViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    BackHandler(onBack = onExit)
    DisposableEffect(Unit) {
        val activity: Activity? = context.findActivity()
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previousOrientation
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VideoSurface(vm, Modifier.fillMaxSize())
        IconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit fullscreen", tint = Color.White)
        }
    }
}
