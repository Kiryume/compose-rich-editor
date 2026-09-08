package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImagePlaceholderVisualTransformationTest {
    @Test fun customPlaceholderKeepsImageOffsetsFormattingAndHtml() {
        val state = RichTextState().setHtml(
            """<p><b>Before</b><img src="https://example.com/a.png" alt="A"><img src="https://example.com/b.png" alt="B"><a href="https://example.com">After</a></p>""",
        )
        val html = state.toHtml()
        val original = state.annotatedString
        val result = ImagePlaceholderVisualTransformation(state.visualTransformation).filter(original)

        assertEquals(original.text, result.text.text)
        assertEquals(original.getStringAnnotations(0, original.length), result.text.getStringAnnotations(0, result.text.length))
        assertTrue(result.text.spanStyles.containsAll(original.spanStyles))
        val hidden = result.text.spanStyles.filter { it.item.color == Color.Transparent }
        assertEquals(listOf(6 to 7, 7 to 8), hidden.map { it.start to it.end })
        for (offset in 0..original.length) {
            assertEquals(offset, result.offsetMapping.originalToTransformed(offset))
            assertEquals(offset, result.offsetMapping.transformedToOriginal(offset))
        }
        assertEquals(html, state.toHtml())
    }

    @Test fun literalReplacementCharactersAreNotHidden() {
        val original = AnnotatedString("A literal \uFFFD symbol")
        val result = ImagePlaceholderVisualTransformation(VisualTransformation.None).filter(original)
        assertEquals(original, result.text)
    }
}
