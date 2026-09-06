package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalRichTextApi::class)
internal fun Modifier.drawRichSpanStyle(
    richTextState: RichTextState,
    topPadding: Float = 0f,
    startPadding: Float = 0f,
    textOffsetY: () -> Float = { 0f },
): Modifier {
    return this
        .drawBehind {
            val topPadding = topPadding + textOffsetY()
            richTextState.textLayoutResult?.let { layout ->
                if (layout.layoutInput.text.length == richTextState.annotatedString.length) {
                    richTextState.richParagraphList.forEach { paragraph ->
                        if (paragraph.quoteDepth > 0) {
                            val range = paragraph.getTextRange()
                            val start = range.min.coerceIn(0, layout.layoutInput.text.length)
                            val end = (range.max - 1).coerceIn(start, layout.layoutInput.text.length)
                            val top = layout.getLineTop(layout.getLineForOffset(start)) + topPadding
                            val bottom = layout.getLineBottom(layout.getLineForOffset(end)) + topPadding
                            drawRect(richTextState.config.blockquoteBackgroundColor,
                                topLeft = Offset(startPadding, top),
                                size = Size((size.width - startPadding).coerceAtLeast(0f), bottom - top))
                            val rtl = layout.getParagraphDirection(start) == ResolvedTextDirection.Rtl
                            repeat(paragraph.quoteDepth) { depth ->
                                val inset = startPadding + depth * 16.sp.toPx() + 2.dp.toPx()
                                val x = if (rtl) size.width - inset else inset
                                drawLine(richTextState.config.blockquoteColor,
                                    Offset(x, top), Offset(x, bottom), strokeWidth = 2.dp.toPx())
                            }
                        }
                    }
                }
            }

            val styledRichSpanList = mutableListOf<Pair<RichSpanStyle, TextRange>>()

            richTextState.styledRichSpanList.fastForEach { richSpan ->
                val lastAddedItem = styledRichSpanList.lastOrNull()

                val end = richSpan.getLastNonEmptyChild()?.textRange?.end ?: richSpan.textRange.end

                if (
                    lastAddedItem != null &&
                    lastAddedItem.first::class == richSpan.richSpanStyle::class &&
                    lastAddedItem.second.end == richSpan.textRange.start
                )
                    styledRichSpanList[styledRichSpanList.lastIndex] =
                        lastAddedItem.first to TextRange(lastAddedItem.second.start, end)
                else
                    styledRichSpanList.add(richSpan.richSpanStyle to TextRange(richSpan.textRange.start, end))
            }

            styledRichSpanList.fastForEach { (style, textRange) ->
                richTextState.textLayoutResult?.let { textLayoutResult ->
                    with(style) {
                        val textLength = richTextState.annotatedString.length
                        val measuredTextLength = textLayoutResult.multiParagraph.intrinsics.annotatedString.length
                        if (textLength == measuredTextLength) {
                            drawCustomStyle(
                                layoutResult = textLayoutResult,
                                textRange = textRange,
                                config = richTextState.config,
                                topPadding = topPadding,
                                startPadding = startPadding
                            )
                        }
                    }
                }
            }
        }
}
