package com.englishpartner.hub.ui.viewer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.englishpartner.hub.data.LearningRepository
import com.englishpartner.hub.data.db.Bookmark
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.settings.SettingsRepository
import com.englishpartner.hub.util.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUi(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val ended: Boolean = false,
    val error: String? = null
)

/**
 * Owns one ExoPlayer for one audio/video material.
 * Living in the ViewModel means playback survives rotation and fullscreen changes.
 * Audio also keeps playing while the screen is off, as long as this screen stays open.
 */
class PlayerViewModel(
    context: Context,
    private val repo: LearningRepository,
    private val settings: SettingsRepository,
    private val appScope: CoroutineScope,
    private val material: Material
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _ui = MutableStateFlow(PlaybackUi())
    val ui: StateFlow<PlaybackUi> = _ui.asStateFlow()

    val bookmarks: StateFlow<List<Bookmark>> = repo.observeBookmarks(material.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var lastSavedAt = 0L
    private var markedDone = material.completed

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _ui.value = _ui.value.copy(isPlaying = isPlaying)
            if (!isPlaying) savePosition()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshPosition()
            if (playbackState == Player.STATE_ENDED) {
                _ui.value = _ui.value.copy(ended = true)
                if (!markedDone) {
                    markedDone = true
                    viewModelScope.launch { repo.setMaterialCompleted(material.id, true) }
                }
                savePosition()
            } else if (playbackState == Player.STATE_READY) {
                _ui.value = _ui.value.copy(ended = false)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _ui.value = _ui.value.copy(error = "Cannot play this file: ${error.errorCodeName}")
        }
    }

    init {
        player.addListener(listener)
        val file = repo.fileFor(material)
        if (!file.exists()) {
            _ui.value = _ui.value.copy(error = "The file is missing from app storage. Try importing it again.")
        } else {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            // Resume where the learner stopped, unless they had reached the very end.
            val resumeAt = material.positionMs
            val nearEnd = material.durationMs > 0 && resumeAt >= material.durationMs - 3_000
            if (resumeAt > 0 && !nearEnd) player.seekTo(resumeAt)
            player.prepare()
        }
        viewModelScope.launch {
            val speed = settings.settings.first().playbackSpeed
            setSpeed(speed, persist = false)
            repo.markOpened(material.id)
        }
        viewModelScope.launch {
            while (isActive) {
                refreshPosition()
                if (player.isPlaying && System.currentTimeMillis() - lastSavedAt > 10_000) savePosition()
                delay(500)
            }
        }
    }

    private fun refreshPosition() {
        val d = player.duration
        _ui.value = _ui.value.copy(
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = if (d == C.TIME_UNSET || d < 0) 0 else d
        )
    }

    private fun savePosition() {
        lastSavedAt = System.currentTimeMillis()
        val pos = player.currentPosition.coerceAtLeast(0)
        val d = player.duration.let { if (it == C.TIME_UNSET || it < 0) 0L else it }
        // appScope so the save finishes even if this screen is closing.
        appScope.launch { repo.savePlayback(material.id, pos, d) }
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceAtLeast(0))
        refreshPosition()
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0)
        val d = _ui.value.durationMs
        seekTo(if (d > 0) target.coerceAtMost(d) else target)
    }

    fun setSpeed(speed: Float, persist: Boolean = true) {
        player.playbackParameters = PlaybackParameters(speed)
        _ui.value = _ui.value.copy(speed = speed)
        if (persist) viewModelScope.launch { settings.setPlaybackSpeed(speed) }
    }

    fun addBookmark() {
        val pos = player.currentPosition.coerceAtLeast(0)
        viewModelScope.launch { repo.addBookmark(material.id, pos, "At ${Formatters.duration(pos)}") }
    }

    fun deleteBookmark(b: Bookmark) {
        viewModelScope.launch { repo.deleteBookmark(b) }
    }

    fun markDone() {
        markedDone = true
        viewModelScope.launch { repo.setMaterialCompleted(material.id, true) }
    }

    override fun onCleared() {
        savePosition()
        player.removeListener(listener)
        player.release()
        super.onCleared()
    }
}
