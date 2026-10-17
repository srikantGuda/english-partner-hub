package com.englishpartner.hub.ui.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.ui.common.EmptyState
import com.englishpartner.hub.ui.common.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Decode a large photo at a reduced size so it never runs the phone out of memory. */
private fun decodeSampled(file: File, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
    return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
}

@Composable
fun ImageScreen(material: Material, onBack: () -> Unit) {
    val container = appContainer()
    val repo = container.repository
    val scope = rememberCoroutineScope()
    val result by produceState<Pair<Boolean, Bitmap?>>(initialValue = false to null, material.id) {
        val bmp = withContext(Dispatchers.IO) {
            runCatching { decodeSampled(repo.fileFor(material), 2048) }.getOrNull()
        }
        value = true to bmp
    }
    LaunchedEffect(material.id) { repo.markOpened(material.id) }

    Scaffold(
        topBar = {
            ViewerTopBar(material.displayName, onBack) {
                IconButton(onClick = { scope.launch { repo.setMaterialCompleted(material.id, true) } }) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Mark as done")
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
            val (done, bmp) = result
            when {
                !done -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                bmp == null -> EmptyState(Icons.Filled.Image, "Cannot show image", "This image format is not supported on this phone.")
                else -> ZoomableImage(bmp.asImageBitmap(), contentDescription = material.displayName)
            }
        }
    }
}
