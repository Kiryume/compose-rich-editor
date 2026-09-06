package com.mohamedrejeb.richeditor.clipboard

/** Immutable selection content, including the editor text used to match a platform clipboard write. */
internal data class RichTextClipboardContent(
    val editorText: String,
    val text: String,
    val html: String?,
)
