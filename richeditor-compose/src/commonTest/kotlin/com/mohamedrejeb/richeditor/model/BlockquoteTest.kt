package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockquoteTest {
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


}
