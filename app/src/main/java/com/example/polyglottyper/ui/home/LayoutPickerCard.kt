package com.example.polyglottyper.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.polyglottyper.R
import com.example.polyglottyper.domain.model.KeyboardCategory
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.ui.common.layoutDescription

/**
 * Summary card + grouped bottom-sheet picker (Standard / Ergonomic / Learning).
 * Descriptions come from the shared [layoutDescription] helper (Part 8) —
 * safe because all parts are concatenated before compiling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutPickerCard(
    selected: KeyboardLayout?,
    resolved: KeyboardLayout,
    onSelected: (KeyboardLayout?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetVisible by remember { mutableStateOf(false) }

    Card(onClick = { sheetVisible = true }, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Keyboard, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = selected?.displayName
                        ?: stringResource(R.string.layout_auto_label, resolved.displayName),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = layoutDescription(selected ?: resolved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ExpandMore, contentDescription = stringResource(R.string.select_layout))
        }
    }

    if (sheetVisible) {
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                Text(
                    stringResource(R.string.select_layout),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                LayoutOptionRow(
                    selected = selected == null,
                    title = stringResource(R.string.layout_auto_label, resolved.displayName),
                    subtitle = stringResource(R.string.layout_auto_desc),
                    onClick = {
                        onSelected(null)
                        sheetVisible = false
                    },
                )

                OptionGroup(stringResource(R.string.layout_group_standard)) {
                    entriesIn(KeyboardCategory.STANDARD, selected, onSelected) { sheetVisible = false }
                }
                OptionGroup(stringResource(R.string.layout_group_ergonomic)) {
                    entriesIn(KeyboardCategory.ERGONOMIC, selected, onSelected) { sheetVisible = false }
                }
                OptionGroup(stringResource(R.string.layout_group_learning)) {
                    entriesIn(KeyboardCategory.LEARNING, selected, onSelected) { sheetVisible = false }
                }
            }
        }
    }
}

@Composable
private fun entriesIn(
    category: KeyboardCategory,
    selected: KeyboardLayout?,
    onSelected: (KeyboardLayout?) -> Unit,
    dismiss: () -> Unit,
) {
    KeyboardLayout.entries
        .filter { it.category == category }
        .forEach { layout ->
            LayoutOptionRow(
                selected = layout == selected,
                title = layout.displayName,
                subtitle = layoutDescription(layout),
                onClick = {
                    onSelected(layout)
                    dismiss()
                },
            )
        }
}

@Composable
private fun OptionGroup(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
    )
    content()
}

@Composable
private fun LayoutOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null) // the row itself is the target
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
