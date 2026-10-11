package com.example.polyglottyper.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.polyglottyper.R
import com.example.polyglottyper.domain.model.KeyboardLayout

/** UI-layer concern: human-readable descriptions live in string resources,
 *  keeping the domain enum pure. Shared by the picker and the stats screen. */
@Composable
fun layoutDescription(layout: KeyboardLayout): String = when (layout) {
    KeyboardLayout.QWERTY -> stringResource(R.string.layout_desc_qwerty)
    KeyboardLayout.AZERTY -> stringResource(R.string.layout_desc_azerty)
    KeyboardLayout.QWERTZ -> stringResource(R.string.layout_desc_qwertz)
    KeyboardLayout.DVORAK -> stringResource(R.string.layout_desc_dvorak)
    KeyboardLayout.COLEMAK -> stringResource(R.string.layout_desc_colemak)
    KeyboardLayout.BEPO -> stringResource(R.string.layout_desc_bepo)
    KeyboardLayout.NEO -> stringResource(R.string.layout_desc_neo)
    KeyboardLayout.TURKISH_F -> stringResource(R.string.layout_desc_turkish_f)
    KeyboardLayout.ABC -> stringResource(R.string.layout_desc_abc)
}
