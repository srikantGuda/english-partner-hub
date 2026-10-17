package com.englishpartner.hub.ui.viewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishpartner.hub.data.LearningRepository
import com.englishpartner.hub.data.db.Bookmark
import com.englishpartner.hub.data.db.Material
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface PdfState {
    data object Loading : PdfState
    data class Ready(val pageCount: Int) : PdfState
    data class Failed(val message: String) : PdfState
}

/** Renders PDF pages with Android's built-in PdfRenderer (no extra library, works offline). */
class PdfViewModel(
    private val repo: LearningRepository,
    private val appScope: CoroutineScope,
    val material: Material
) : ViewModel() {

    private var fd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val mutex = Mutex() // PdfRenderer allows only one open page at a time.

    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val _state = MutableStateFlow<PdfState>(PdfState.Loading)
    val state: StateFlow<PdfState> = _state.asStateFlow()

    val bookmarks: StateFlow<List<Bookmark>> = repo.observeBookmarks(material.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) {
                try {
                    val file = repo.fileFor(material)
                    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val r = PdfRenderer(descriptor)
                    fd = descriptor
                    renderer = r
                    PdfState.Ready(r.pageCount)
                } catch (t: Throwable) {
                    PdfState.Failed("Cannot open this PDF. It may be damaged or password-protected.")
                }
            }
            repo.markOpened(material.id)
        }
    }

    /** Render a page to a bitmap [widthPx] wide. Cached, thread-safe. */
    suspend fun renderPage(index: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$index@$widthPx"
        cache.get(key)?.let { return@withContext it }
        mutex.withLock {
            val r = renderer ?: return@withLock null
            if (index !in 0 until r.pageCount) return@withLock null
            try {
                r.openPage(index).use { page ->
                    val w = widthPx.coerceIn(200, 2000)
                    val h = (w.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(key, bmp)
                    bmp
                }
            } catch (t: Throwable) {
                null
            }
        }
    }

    fun savePage(page: Int) {
        appScope.launch { repo.saveReadingPosition(material.id, page) }
    }

    fun addBookmark(page: Int) {
        viewModelScope.launch { repo.addBookmark(material.id, page.toLong(), "Page ${page + 1}") }
    }

    fun deleteBookmark(b: Bookmark) {
        viewModelScope.launch { repo.deleteBookmark(b) }
    }

    fun markDone() {
        viewModelScope.launch { repo.setMaterialCompleted(material.id, true) }
    }

    override fun onCleared() {
        val r = renderer
        val d = fd
        renderer = null
        fd = null
        // Close after any in-flight render finishes.
        appScope.launch {
            mutex.withLock {
                runCatching { r?.close() }
                runCatching { d?.close() }
            }
        }
        cache.evictAll()
        super.onCleared()
    }
}
