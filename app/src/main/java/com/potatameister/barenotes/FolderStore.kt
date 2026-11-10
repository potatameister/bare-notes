package com.potatameister.barenotes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FolderStore {

    private const val FILE_NAME = "folders.json"

    fun load(context: Context): List<Folder> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            decode(JSONArray(file.readText()))
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, folders: List<Folder>) {
        val json = encode(folders).toString()
        val file = File(context.filesDir, FILE_NAME)
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        temp.writeText(json)
        if (!temp.renameTo(file)) {
            file.writeText(json)
            temp.delete()
        }
    }

    fun encode(folders: List<Folder>): JSONArray {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject()
                    .put("id", folder.id)
                    .put("name", folder.name)
            )
        }
        return array
    }

    fun decode(array: JSONArray): List<Folder> = buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val name = item.optString("name").trim()
            if (id.isNotBlank() && name.isNotBlank()) add(Folder(id, name))
        }
    }
        .distinctBy { it.id }
        .sortedBy { it.name.lowercase() }
}
