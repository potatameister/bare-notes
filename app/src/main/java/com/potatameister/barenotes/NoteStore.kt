package com.potatameister.barenotes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object NoteStore {

    private const val FILE_NAME = "notes.json"

    fun load(context: Context): List<Note> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val body = item.optString("body")
                    add(
                        Note(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            body = body,
                            createdAt = item.optLong("createdAt", item.optLong("updatedAt")),
                            updatedAt = item.optLong("updatedAt"),
                            spans = loadSpans(item.optJSONArray("spans"), body.length),
                            folderId = item.optString("folderId")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadSpans(array: JSONArray?, length: Int): List<NoteSpan> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val type = item.optString("type")
                if (type != NoteSpan.BOLD && type != NoteSpan.ITALIC &&
                    type != NoteSpan.UNDERLINE && type != NoteSpan.STRIKE &&
                    type != NoteSpan.H1 && type != NoteSpan.H2
                ) continue
                val start = item.optInt("start").coerceIn(0, length)
                val end = item.optInt("end").coerceIn(0, length)
                if (start < end) add(NoteSpan(type, start, end))
            }
        }.distinct().sortedWith(compareBy({ it.start }, { it.end }, { it.type }))
    }

    fun save(context: Context, notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { note ->
            val spans = JSONArray()
            note.spans.forEach { span ->
                spans.put(
                    JSONObject()
                        .put("type", span.type)
                        .put("start", span.start)
                        .put("end", span.end)
                )
            }
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("body", note.body)
                    .put("createdAt", note.createdAt)
                    .put("updatedAt", note.updatedAt)
                    .put("spans", spans)
                    .put("folderId", note.folderId)
            )
        }
        val file = File(context.filesDir, FILE_NAME)
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        temp.writeText(array.toString())
        if (!temp.renameTo(file)) {
            file.writeText(array.toString())
            temp.delete()
        }
    }
}
