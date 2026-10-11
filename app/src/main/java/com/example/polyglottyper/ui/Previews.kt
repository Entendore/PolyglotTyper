@file:OptIn(ExperimentalLayoutApi::class)

package com.example.polyglottyper.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.ui.common.Sparkline
import com.example.polyglottyper.ui.game.LayoutKeyboard
import com.example.polyglottyper.ui.game.WordsDisplay
import com.example.polyglottyper.ui.theme.PolyglotTyperTheme

@Preview(showBackground = true)
@Composable
private fun WordsDisplayPreview() {
    PolyglotTyperTheme(dynamicColor = false) {
        WordsDisplay(
            words = listOf("typing", "fast", "is", "fun", "keep", "going", "more", "words"),
            results = listOf(true, false),
            currentIndex = 2,
            typedText = "is",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LayoutKeyboardPreview() {
    PolyglotTyperTheme(dynamicColor = false) {
        LayoutKeyboard(
            layout = KeyboardLayout.AZERTY,
            language = Language.FRENCH,
            nextChar = 'e',
            onChar = {},
            onBackspace = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SparklinePreview() {
    PolyglotTyperTheme(dynamicColor = false) {
        Sparkline(
            values = listOf(38f, 42f, 41f, 47f, 45f, 52f, 55f, 54f, 61f),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().height(72.dp),
        )
    }
}
