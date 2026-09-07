package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
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
                    val paragraphs = richTextState.richParagraphList
                    paragraphs.forEach { paragraph ->
                        if (paragraph.quoteDepth > 0) {
                            val range = paragraph.getTextRange()
                            val start = range.min.coerceIn(0, layout.layoutInput.text.length)
                            val end = (range.max - 1).coerceIn(start, layout.layoutInput.text.length)
                            val top = layout.getLineTop(layout.getLineForOffset(start)) + topPadding
                            val bottom = layout.getLineBottom(layout.getLineForOffset(end)) + topPadding
                            drawRect(richTextState.config.blockquoteBackgroundColor,
                                topLeft = Offset(startPadding, top),
                                size = Size((size.width - startPadding).coerceAtLeast(0f), bottom - top))
                        }
                    }
                    // Draw bars after every background so later paragraphs cannot cover a shared bar.
                    paragraphs.forEachIndexed { paragraphIndex, paragraph ->
                        if (paragraph.quoteDepth > 0) {
                            val start = paragraph.getTextRange().min.coerceIn(0, layout.layoutInput.text.length)
                            val top = layout.getLineTop(layout.getLineForOffset(start)) + topPadding
                            val rtl = layout.getParagraphDirection(start) == ResolvedTextDirection.Rtl
                            repeat(paragraph.quoteDepth) { depth ->
                                // One continuous rounded bar for adjacent paragraphs at this depth.
                                if ((paragraphs.getOrNull(paragraphIndex - 1)?.quoteDepth ?: 0) > depth) {
                                    return@repeat
                                }
                                var lastIndex = paragraphIndex
                                while ((paragraphs.getOrNull(lastIndex + 1)?.quoteDepth ?: 0) > depth) lastIndex++
                                val lastRange = paragraphs[lastIndex].getTextRange()
                                // An empty final paragraph has no last character: use its own
                                // start so Enter extends the bar before the user types again.
                                val lastStart = lastRange.min.coerceIn(start, layout.layoutInput.text.length)
                                val lastOffset = (lastRange.max - 1)
                                    .coerceIn(lastStart, layout.layoutInput.text.length)
                                val barBottom = layout.getLineBottom(layout.getLineForOffset(lastOffset)) + topPadding
                                val width = richTextState.config.blockquoteStrokeWidth.toPx()
                                val inset = startPadding + depth * richTextState.config.blockquoteIndent.toPx()
                                val x = if (rtl) size.width - inset - width else inset
                                drawRoundRect(
                                    color = richTextState.config.blockquoteColor,
                                    topLeft = Offset(x, top),
                                    size = Size(width, barBottom - top),
                                    cornerRadius = CornerRadius(width / 2f),
                                )
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
