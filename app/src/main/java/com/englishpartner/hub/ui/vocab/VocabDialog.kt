package com.englishpartner.hub.ui.vocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.englishpartner.hub.data.db.VocabWord

/** Add or edit a vocabulary word. */
@Composable
fun VocabDialog(
    initial: VocabWord,
    onDismiss: () -> Unit,
    onSave: (VocabWord) -> Unit
) {
    var word by rememberSaveable { mutableStateOf(initial.word) }
    var meaning by rememberSaveable { mutableStateOf(initial.meaning) }
    var example by rememberSaveable { mutableStateOf(initial.example) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "New word" else "Edit word") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = word, onValueChange = { word = it },
                    label = { Text("Word or phrase") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = meaning, onValueChange = { meaning = it },
                    label = { Text("Meaning (any language)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = example, onValueChange = { example = it },
                    label = { Text("Example sentence") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = word.isNotBlank(),
                onClick = { onSave(initial.copy(word = word, meaning = meaning, example = example)) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
