package com.potatameister.barenotes

data class Note(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val spans: List<NoteSpan> = emptyList(),
    val folderId: String = ""
)

data class NoteSpan(
    val type: String,
    val start: Int,
    val end: Int
) {
    companion object {
        const val BOLD = "bold"
        const val ITALIC = "italic"
        const val UNDERLINE = "underline"
        const val STRIKE = "strike"
        const val H1 = "h1"
        const val H2 = "h2"
    }
}
