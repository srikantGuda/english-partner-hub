package com.englishpartner.hub.ui.viewer

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.ui.common.EmptyState
import com.englishpartner.hub.ui.common.appViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun PdfScreen(material: Material, onBack: () -> Unit) {
    val vm = appViewModel(key = "pdf-${material.id}") { PdfViewModel(it.repository, it.appScope, material) }
    val state by vm.state.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    var showBookmarks by remember { mutableStateOf(false) }
    var zoomPage by rememberSaveable { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = material.lastPage.coerceAtLeast(0))
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(600)
            .collect { vm.savePage(it) }
    }

    val pageNow = listState.firstVisibleItemIndex
    val pageCount = (state as? PdfState.Ready)?.pageCount ?: 0

    val zoomed = zoomPage
    if (zoomed != null) {
        BackHandler { zoomPage = null }
        ZoomedPage(vm, zoomed, onClose = { zoomPage = null })
        return
    }

    Scaffold(
        topBar = {
            ViewerTopBar(material.displayName, onBack) {
                IconButton(onClick = { vm.markDone() }) { Icon(Icons.Filled.CheckCircle, contentDescription = "Mark as done") }
                IconButton(onClick = { showBookmarks = true }) { Icon(Icons.Filled.Bookmark, contentDescription = "Bookmarks") }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                PdfState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is PdfState.Failed -> EmptyState(Icons.Filled.PictureAsPdf, "Cannot open PDF", s.message)
                is PdfState.Ready -> BoxWithConstraints(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val widthPx = with(LocalDensity.current) { maxWidth.toPx().toInt() }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(count = s.pageCount, key = { it }) { index ->
                            PdfPage(vm, index, widthPx, onClick = { zoomPage = index })
                        }
                    }
                    if (pageCount > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.inverseSurface,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                        ) {
                            Text(
                                "${(pageNow + 1).coerceAtMost(pageCount)} / $pageCount",
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBookmarks) {
        BookmarksDialog(
            bookmarks = bookmarks,
            positionLabel = { "Page ${it + 1}" },
            addLabel = "Bookmark page ${pageNow + 1}",
            onAdd = { vm.addBookmark(pageNow) },
            onJump = { b ->
                showBookmarks = false
                scope.launch { listState.scrollToItem(b.position.toInt().coerceIn(0, (pageCount - 1).coerceAtLeast(0))) }
            },
            onDelete = { vm.deleteBookmark(it) },
            onDismiss = { showBookmarks = false }
        )
    }
}

@Composable
private fun PdfPage(vm: PdfViewModel, index: Int, widthPx: Int, onClick: () -> Unit) {
    val bitmap by produceState<Bitmap?>(initialValue = null, index, widthPx) {
        value = vm.renderPage(index, widthPx)
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.707f).background(Color.White),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
    } else {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(bmp.width.toFloat() / bmp.height)
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun ZoomedPage(vm: PdfViewModel, index: Int, onClose: () -> Unit) {
    Scaffold(topBar = { ViewerTopBar("Page ${index + 1} · pinch to zoom", onClose) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color.White)) {
            val bitmap by produceState<Bitmap?>(initialValue = null, index) {
                value = vm.renderPage(index, 2000)
            }
            val bmp = bitmap
            if (bmp == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else ZoomableImage(bmp.asImageBitmap(), contentDescription = "Page ${index + 1}")
        }
    }
}
