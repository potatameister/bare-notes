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
                    add(
                        Note(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            body = item.optString("body"),
                            updatedAt = item.optLong("updatedAt")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("body", note.body)
                    .put("updatedAt", note.updatedAt)
            )
        }
        File(context.filesDir, FILE_NAME).writeText(array.toString())
    }
}
