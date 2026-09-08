package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.roundToInt

private fun AnnotatedString.imagePlaceholders() =
    getStringAnnotations(0, length).filter { it.item.startsWith("richtext-img-") }

/** Hide the replacement glyph without changing the image's atomic text range. */
internal class ImagePlaceholderVisualTransformation(
    private val delegate: VisualTransformation,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = delegate.filter(text)
        val placeholders = transformed.text.imagePlaceholders()
        if (placeholders.isEmpty()) return transformed
        val display = AnnotatedString.Builder(transformed.text).apply {
            placeholders.forEach { addStyle(SpanStyle(color = Color.Transparent), it.start, it.end) }
        }.toAnnotatedString()
        return TransformedText(display, transformed.offsetMapping)
    }
}

/** Position caller-provided content in each image's character box. */
@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun ImagePlaceholders(
    state: RichTextState,
    textOffsetY: () -> Float,
    placeholder: @Composable (contentDescription: String?) -> Unit,
) {
    val annotations = state.annotatedString.imagePlaceholders()
    Layout(content = {
        annotations.forEach { annotation ->
            key(annotation.item) {
                val image = state.getRichSpanByTextIndex(annotation.start, ignoreCustomFiltering = true)
                    ?.richSpanStyle as? RichSpanStyle.Image
                Box(Modifier.clipToBounds(), contentAlignment = Alignment.Center) {
                    placeholder(image?.contentDescription)
                }
            }
        }
    }) { measurables, constraints ->
        val textLayout = state.textLayoutResult?.takeIf {
            it.layoutInput.text.length == state.annotatedString.length
        }
        val bounds = annotations.map { textLayout?.getBoundingBox(it.start) ?: Rect.Zero }
        val placeables = measurables.mapIndexed { index, measurable ->
            val box = bounds[index]
            measurable.measure(Constraints.fixed(
                width = box.width.roundToInt().coerceAtLeast(0),
                height = box.height.roundToInt().coerceAtLeast(0),
            ))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val box = bounds[index]
                placeable.place(box.left.roundToInt(), (box.top + textOffsetY()).roundToInt())
            }
        }
    }
}
