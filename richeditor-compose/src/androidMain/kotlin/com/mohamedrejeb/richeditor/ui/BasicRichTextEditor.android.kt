package com.mohamedrejeb.richeditor.ui

import android.view.inputmethod.InputConnectionWrapper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.CoroutineScope

internal actual fun Modifier.adjustTextIndicatorOffset(
    state: RichTextState,
    contentPadding: PaddingValues,
    density: Density,
    layoutDirection: LayoutDirection,
    scope: CoroutineScope,
): Modifier = this

// Empty paragraphs do not produce a TextFieldValue change for IME deletion commands.
internal actual fun PlatformTextInputMethodRequest.withEmptyQuoteBackspace(
    state: RichTextState,
): PlatformTextInputMethodRequest = PlatformTextInputMethodRequest { outAttributes ->
    object : InputConnectionWrapper(createInputConnection(outAttributes), false) {
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            if (beforeLength == 1 && afterLength == 0 && state.removeEmptyQuoteOnBackspace()) return true
            return super.deleteSurroundingText(beforeLength, afterLength)
        }

        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
            if (beforeLength == 1 && afterLength == 0 && state.removeEmptyQuoteOnBackspace()) return true
            return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
        }
    }
}
