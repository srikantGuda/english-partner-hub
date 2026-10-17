package com.englishpartner.hub.ui.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.englishpartner.hub.data.db.Bookmark

/** Lists a material's bookmarks. Tap to jump, bin icon to delete, button to add one at the current spot. */
@Composable
fun BookmarksDialog(
    bookmarks: List<Bookmark>,
    positionLabel: (Long) -> String,
    addLabel: String,
    onAdd: () -> Unit,
    onJump: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bookmarks") },
        text = {
            if (bookmarks.isEmpty()) {
                Text("No bookmarks yet. Save the current spot to come back to it later.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(bookmarks, key = { it.id }) { b ->
                        ListItem(
                            headlineContent = { Text(b.label.ifBlank { positionLabel(b.position) }) },
                            supportingContent = { Text(positionLabel(b.position)) },
                            trailingContent = {
                                IconButton(onClick = { onDelete(b) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete bookmark")
                                }
                            },
                            modifier = Modifier.clickable { onJump(b) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAdd) { Text(addLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
