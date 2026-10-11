package com.example.polyglottyper.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.polyglottyper.R
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language

/**
 * Tappable rendering of the selected keyboard layout. The key matching
 * [nextChar] is highlighted, so players can learn a layout (AZERTY, Dvorak, …)
 * without installing a system keyboard for it. Accent keys missing from the
 * layout itself are appended in a scrollable row (no duplicates).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutKeyboard(
    layout: KeyboardLayout,
    language: Language,
    nextChar: Char?,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ergonomic layouts like BÉPO/Neo carry é, ß, ü… on their keys — only
    // append the language's accents the layout does NOT already cover.
    val layoutChars = remember(layout) { layout.rows.flatten().toSet() }
    val extraChars = remember(layout, language) {
        language.specialChars.filterNot { it in layoutChars }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            layout.rows.forEach { row ->
                KeyRow(chars = row, nextChar = nextChar, onChar = onChar)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(extraChars) { char ->
                            Key(
                                char = char,
                                isNext = char == nextChar,
                                modifier = Modifier.width(42.dp),
                                onChar = onChar,
                            )
                        }
                    }
                }
                SpaceKey(
                    isNext = nextChar == ' ',
                    onPress = { onChar(' ') },
                    modifier = Modifier.width(96.dp),
                )
                BackspaceKey(onBackspace = onBackspace)
            }
        }
    }
}

@Composable
private fun KeyRow(chars: List<Char>, nextChar: Char?, onChar: (Char) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chars.forEach { char ->
            Key(
                char = char,
                isNext = char == nextChar,
                modifier = Modifier.weight(1f),
                onChar = onChar,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Key(
    char: Char,
    isNext: Boolean,
    modifier: Modifier = Modifier,
    onChar: (Char) -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (isNext) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        label = "keyColor",
    )
    Surface(
        onClick = { onChar(char) },
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (isNext) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceKey(isNext: Boolean, onPress: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onPress,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.space_key),
                style = MaterialTheme.typography.labelLarge,
                color = if (isNext) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackspaceKey(onBackspace: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onBackspace,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Box(
            Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = stringResource(R.string.backspace_key),
            )
        }
    }
}
