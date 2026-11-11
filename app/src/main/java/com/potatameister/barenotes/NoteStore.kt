package com.potatameister.barenotes

import android.content.Context
import org.json.JSONArray
import java.io.File

object NoteStore {

    private const val NOTES_DIR = "notes"
    private const val LEGACY_FILE = "notes.json"
    private const val LEGACY_BACKUP = "notes.json.migrated"

    fun load(context: Context): List<Note> {
        val dir = File(context.filesDir, NOTES_DIR).apply { mkdirs() }
        val files = markdownFiles(dir)

        if (files.isEmpty()) {
            val migrated = migrateLegacy(context, dir)
            if (migrated.isNotEmpty()) return migrated
        }

        return files.mapNotNull { file ->
            val id = file.name.removeSuffix(".md")
            if (id.isBlank()) return@mapNotNull null
            try {
                MarkdownCodec.decode(id, file.readText())
            } catch (_: Exception) {
                null
            }
        }
    }

    fun save(context: Context, notes: List<Note>) {
        val dir = File(context.filesDir, NOTES_DIR).apply { mkdirs() }
        val ids = notes.mapTo(HashSet()) { it.id }

        notes.forEach { note ->
            if (!MarkdownCodec.isSafeId(note.id)) return@forEach
            writeNote(dir, note)
        }

        dir.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            if (file.name.endsWith(".tmp")) {
                file.delete()
            } else if (file.name.endsWith(".md") &&
                file.name.removeSuffix(".md") !in ids
            ) {
                file.delete()
            }
        }
    }

    private fun writeNote(dir: File, note: Note) {
        val target = File(dir, "${note.id}.md")
        val temp = File(dir, "${note.id}.md.tmp")
        val content = MarkdownCodec.encode(note)
        temp.writeText(content)
        if (!temp.renameTo(target)) {
            target.writeText(content)
            temp.delete()
        }
    }

    private fun markdownFiles(dir: File): List<File> =
        dir.listFiles { file -> file.isFile && file.name.endsWith(".md") }
            ?.sortedBy { it.name }
            .orEmpty()

    private fun migrateLegacy(context: Context, dir: File): List<Note> {
        val legacy = File(context.filesDir, LEGACY_FILE)
        if (!legacy.exists()) return emptyList()

        val notes = try {
            decodeLegacy(JSONArray(legacy.readText()))
        } catch (_: Exception) {
            emptyList()
        }
        if (notes.isEmpty()) return emptyList()

        notes.forEach { note ->
            if (MarkdownCodec.isSafeId(note.id)) writeNote(dir, note)
        }

        val backup = File(context.filesDir, LEGACY_BACKUP)
        try {
            legacy.copyTo(backup, overwrite = true)
            legacy.delete()
        } catch (_: Exception) {
            // Keep the legacy file if the backup cannot be written.
        }
        return notes
    }

    fun decodeLegacy(array: JSONArray): List<Note> = buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val body = item.optString("body")
            val id = item.optString("id")
            if (id.isBlank()) continue
            add(
                Note(
                    id = id,
                    title = item.optString("title"),
                    body = body,
                    createdAt = item.optLong("createdAt", item.optLong("updatedAt")),
                    updatedAt = item.optLong("updatedAt"),
                    spans = decodeLegacySpans(item.optJSONArray("spans"), body.length),
                    folderId = item.optString("folderId")
                )
            )
        }
    }

    private fun decodeLegacySpans(array: JSONArray?, length: Int): List<NoteSpan> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val type = item.optString("type")
                if (type !in NoteSpan.TYPES) continue
                val start = item.optInt("start").coerceIn(0, length)
                val end = item.optInt("end").coerceIn(0, length)
                if (start < end) add(NoteSpan(type, start, end))
            }
        }.distinct().sortedWith(compareBy({ it.start }, { it.end }, { it.type }))
    }
}
