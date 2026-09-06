package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
internal fun RichTextEditorInput(
    state: RichTextState,
    editable: Boolean,
    content: @Composable () -> Unit,
) {
    val interceptor = remember(state, editable) {
        PlatformTextInputInterceptor { request, next ->
            next.startInputMethod(if (editable) request.withEmptyQuoteBackspace(state) else request)
        }
    }
    InterceptPlatformTextInput(interceptor, content)
}

internal expect fun PlatformTextInputMethodRequest.withEmptyQuoteBackspace(
    state: RichTextState,
): PlatformTextInputMethodRequest
