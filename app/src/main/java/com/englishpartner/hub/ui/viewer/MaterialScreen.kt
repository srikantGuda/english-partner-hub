@file:OptIn(ExperimentalMaterial3Api::class)

package com.englishpartner.hub.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.materialType
import com.englishpartner.hub.data.model.MaterialType
import com.englishpartner.hub.ui.common.EmptyState
import com.englishpartner.hub.ui.common.appContainer

/** Loads a material once and shows the right viewer for its type. */
@Composable
fun MaterialScreen(materialId: Long, onBack: () -> Unit) {
    val repo = appContainer().repository
    val loaded by produceState<Material?>(initialValue = null, materialId) {
        value = repo.getMaterial(materialId)
    }
    val material = loaded
    if (material == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    when (material.materialType) {
        MaterialType.AUDIO, MaterialType.VIDEO -> PlayerScreen(material, onBack)
        MaterialType.PDF -> PdfScreen(material, onBack)
        MaterialType.TEXT -> TextReaderScreen(material, onBack)
        MaterialType.IMAGE -> ImageScreen(material, onBack)
        MaterialType.OTHER -> UnsupportedScreen(material, onBack)
    }
}

@Composable
fun ViewerTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        actions = { actions() }
    )
}

@Composable
private fun UnsupportedScreen(material: Material, onBack: () -> Unit) {
    Scaffold(topBar = { ViewerTopBar(material.displayName, onBack) }) { padding ->
        EmptyState(
            icon = Icons.Filled.AttachFile,
            title = "Preview not available",
            message = "This file type (${material.mimeType.ifBlank { "unknown" }}) is stored safely in the app, " +
                "but it cannot be opened here yet.",
            modifier = Modifier.padding(padding)
        )
    }
}
