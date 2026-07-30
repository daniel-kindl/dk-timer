package com.emomtimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable preset row: header with a "Save" button, chips for each preset.
 *
 * Generic so it works with any preset type — callers supply [getKey] and
 * [getLabel] to extract display information without coupling to a specific model.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PresetsSection(
    presets: List<T>,
    getKey: (T) -> String,
    getLabel: (T) -> String,
    onPresetClick: (T) -> Unit,
    onDeleteClick: (T) -> Unit,
    onSavePreset: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Presets", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onSavePreset, enabled = saveEnabled) {
                Icon(
                    Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Save")
            }
        }
        if (presets.isEmpty()) {
            Text(
                "Save a configuration above to see it here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets, key = { getKey(it) }) { preset ->
                    InputChip(
                        selected = false,
                        onClick = { onPresetClick(preset) },
                        label = { Text(getLabel(preset)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onDeleteClick(preset) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete ${getLabel(preset)}",
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
