package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockquoteTest {
    @Test fun quoteAppearanceRemainsVisualThroughTypingCopyAndToggle() {
        val state = RichTextState().setHtml("<blockquote><p>Quoted</p></blockquote>")
        val quoteStyle = SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic)
        state.config.blockquoteSpanStyle = quoteStyle
        state.config.blockquoteIndent = 13.sp
        state.config.blockquoteStrokeWidth = 3.dp
        assertTrue(state.annotatedString.spanStyles.any { it.item == quoteStyle && it.start == 0 && it.end == 6 })
        assertEquals(13.sp, state.annotatedString.paragraphStyles.first().item.textIndent?.restLine)
        assertEquals("<blockquote><p>Quoted</p></blockquote>", state.toHtml())

        state.selection = TextRange(state.annotatedString.length)
        state.commit(state.selection.start, value = " text")
        assertTrue(state.annotatedString.spanStyles.any { it.item == quoteStyle && it.end == 11 })
        assertEquals("<blockquote><p>Quoted text</p></blockquote>", state.toHtml())
        assertFalse(state.currentSpanStyle.fontStyle == FontStyle.Italic)

        val copy = state.copy()
        assertEquals(quoteStyle, copy.config.blockquoteSpanStyle)
        assertEquals(13.sp, copy.config.blockquoteIndent)
        assertEquals(3.dp, copy.config.blockquoteStrokeWidth)
        assertEquals(state.toHtml(), copy.toHtml())

        state.toggleBlockquote()
        assertFalse(state.annotatedString.spanStyles.any { it.item == quoteStyle })
        assertEquals("<p>Quoted text</p>", state.toHtml())
        state.history.undo()
        assertTrue(state.annotatedString.spanStyles.any { it.item == quoteStyle })
    }

    @Test fun backspaceExitsNewEmptyQuoteAndCanBeUndone() {
        val state = RichTextState()
        state.toggleBlockquote()
        assertTrue(state.removeEmptyQuoteOnBackspace())
        assertFalse(state.isBlockquote)
        assertEquals("", state.toText())
        state.history.undo()
        assertTrue(state.isBlockquote)
        state.history.redo()
        assertFalse(state.isBlockquote)
        assertFalse(state.removeEmptyQuoteOnBackspace())
    }

    @Test fun backspaceRemovesOneQuoteLevelWithoutChangingHeadingOrAdjacentText() {
        val state = RichTextState().setHtml("<p>Before</p><blockquote><blockquote><h1></h1></blockquote></blockquote><p>After</p>")
        val originalText = state.toText()
        state.selection = TextRange(state.richParagraphList[1].getTextRange().min)
        assertTrue(state.removeEmptyQuoteOnBackspace())
        assertEquals(listOf(0, 1, 0), state.richParagraphList.map { it.quoteDepth })
        assertEquals(HeadingStyle.H1, state.currentHeadingStyle)
        assertEquals(originalText, state.toText())
        assertTrue(state.removeEmptyQuoteOnBackspace())
        assertEquals(listOf(0, 0, 0), state.richParagraphList.map { it.quoteDepth })
        assertEquals(originalText, state.toText())
    }

    @Test fun backspaceLeavesNonEmptyQuotesAndSelectionsToNormalTextDeletion() {
        val state = RichTextState().setHtml("<blockquote><p>Text</p></blockquote>")
        state.selection = TextRange(0)
        assertFalse(state.removeEmptyQuoteOnBackspace())
        state.selection = TextRange(4)
        assertFalse(state.removeEmptyQuoteOnBackspace())
        state.selection = TextRange(0, 4)
        assertFalse(state.removeEmptyQuoteOnBackspace())
        assertTrue(state.isBlockquote)
        assertEquals("Text", state.toText())
    }

    private fun RichTextState.commit(start: Int, end: Int = start, value: String) {
        val text = textFieldValue.text.replaceRange(start, end, value)
        onTextFieldValueChange(TextFieldValue(text, TextRange(start + value.length)))
    }

    @Test fun quotesAreIndependentOfHeadingsAndLists() {
        val state = RichTextState().setText("Text")
        state.setHeadingStyle(HeadingStyle.H1)
        state.toggleBlockquote()
        assertEquals("<blockquote><h1>Text</h1></blockquote>", state.toHtml())
        state.toggleUnorderedList()
        assertTrue(state.isBlockquote)
        assertTrue(state.isUnorderedList)
        state.toggleBlockquote()
        assertFalse(state.isBlockquote)
        assertTrue(state.isUnorderedList)
        assertEquals(HeadingStyle.H1, state.currentHeadingStyle)
    }

    @Test fun quoteEnterContinuesAndEmptyEnterExits() {
        val state = RichTextState().setHtml("<blockquote><p>Text</p></blockquote>")
        state.selection = TextRange(state.annotatedString.length)
        state.commit(state.selection.start, value = "\n")
        assertEquals(listOf(1, 1), state.richParagraphList.map { it.quoteDepth })
        state.commit(state.selection.start, value = "\n")
        assertEquals(listOf(1, 0), state.richParagraphList.map { it.quoteDepth })
        state.history.undo()
        assertEquals(listOf(1, 1), state.richParagraphList.map { it.quoteDepth })
        state.history.redo()
        assertEquals(listOf(1, 0), state.richParagraphList.map { it.quoteDepth })
    }

    @Test fun autocorrectEnterPreservesCorrectedWordAndQuote() {
        val state = RichTextState().setHtml("<blockquote><p>teh</p></blockquote>")
        state.selection = TextRange(3)
        state.commit(1, 3, "he\n")
        assertEquals("the\n", state.toText())
        assertEquals(listOf(1, 1), state.richParagraphList.map { it.quoteDepth })
    }

    @Test fun plainMultilinePasteInheritsQuoteIncludingBlankLinesAndSuffix() {
        val state = RichTextState().setHtml("<blockquote><p>Before &#32;after</p></blockquote>")
        state.selection = TextRange(7)
        state.commit(7, value = "first\nsecond\n\nfourth")
        assertEquals("Before first\nsecond\n\nfourth after", state.toText())
        assertEquals(listOf(1, 1, 1, 1), state.richParagraphList.map { it.quoteDepth })
        state.history.undo()
        assertEquals("Before  after", state.toText())
    }

    @Test fun richPasteRetainsFormattingAndQuote() {
        val state = RichTextState().setHtml("<blockquote><p>Before &#32;after</p></blockquote>")
        state.selection = TextRange(7)
        state.pendingClipboardHtml = "<p><b>bold</b></p><p><a href=\"https://example.com\">link</a></p>"
        state.pendingClipboardPlainText = "bold\nlink"
        state.commit(7, value = "bold\nlink")
        assertEquals(listOf(1, 1), state.richParagraphList.map { it.quoteDepth })
        assertTrue(state.toHtml().contains("<b>bold</b>"), state.toHtml())
        assertTrue(state.toHtml().contains("href=\"https://example.com\""))
        assertEquals("Before bold\nlink after", state.toText())
        state.history.undo()
        assertEquals("Before  after", state.toText())
        state.history.redo()
        assertTrue(state.toHtml().contains("<b>bold</b>"))
    }

    @Test fun nestedQuotesAndListsRoundTripThroughHtml() {
        val state = RichTextState().setHtml("<p>Before</p><blockquote><p>One</p><blockquote><p>Two</p></blockquote><ul><li>Item</li></ul><p>Three</p></blockquote><p>After</p>")
        val html = state.toHtml()
        val restored = RichTextState().setHtml(html)
        assertEquals(listOf(0, 1, 2, 1, 1, 0), state.richParagraphList.map { it.quoteDepth })
        assertEquals(state.richParagraphList.map { it.quoteDepth }, restored.richParagraphList.map { it.quoteDepth })
        assertEquals(state.toText(), restored.toText())
        assertEquals(html, restored.toHtml())
    }

    @Test fun emptyQuotedDraftRoundTrips() {
        val state = RichTextState()
        state.toggleBlockquote()
        val restored = RichTextState().setHtml(state.toHtml())
        assertEquals(1, restored.richParagraphList.single().quoteDepth)
        assertTrue(restored.isBlockquote)
    }

    @Test fun quoteOnlyFormattingIsUndoable() {
        val state = RichTextState().setText("Text")
        state.toggleBlockquote()
        assertTrue(state.isBlockquote)
        state.history.undo()
        assertFalse(state.isBlockquote)
        state.history.redo()
        assertTrue(state.isBlockquote)
    }

    @Test fun markdownQuotesAndBlankLinesRetainTheirDepth() {
        val state = RichTextState().setMarkdown("> First\n>\n> Third\n\nAfter")
        assertEquals(listOf(1, 1, 1, 0, 0), state.richParagraphList.map { it.quoteDepth })
        val restored = RichTextState().setMarkdown(state.toMarkdown())
        assertEquals(state.richParagraphList.map { it.quoteDepth }, restored.richParagraphList.map { it.quoteDepth })
    }

    @OptIn(com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class)
    @Test fun documentAndClipboardCopiesRetainQuoteDepth() {
        val state = RichTextState().setHtml("<blockquote><p>Outer</p><blockquote><p>Inner</p></blockquote></blockquote>")
        val document = state.toRichTextDocument()
        assertEquals(listOf(1, 2), document.blocks.map { it.quoteDepth })
        val restored = RichTextState().setRichTextDocument(document)
        assertEquals(state.toHtml(), restored.toHtml())
        assertEquals("<blockquote><p>Outer</p></blockquote>", state.toHtml(TextRange(0, 5)))
    }

    @Test fun windowsNewlinesInQuotedPasteAreNormalized() {
        val state = RichTextState().setHtml("<blockquote><p>Before </p></blockquote>")
        state.selection = TextRange(state.annotatedString.length)
        state.commit(state.selection.start, value = "first\r\nsecond")
        assertEquals("Before first\nsecond", state.toText())
        assertEquals(listOf(1, 1), state.richParagraphList.map { it.quoteDepth })
    }

}
