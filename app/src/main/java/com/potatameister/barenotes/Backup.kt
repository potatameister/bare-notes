package com.potatameister.barenotes

import org.json.JSONObject

object Backup {

    private const val VERSION = 1

    fun encode(notes: List<Note>, folders: List<Folder>): String =
        JSONObject()
            .put("format", "bare-notes")
            .put("version", VERSION)
            .put("notes", NoteStore.encode(notes))
            .put("folders", FolderStore.encode(folders))
            .toString()

    fun decode(text: String): BackupData? = try {
        val root = JSONObject(text)
        if (root.optString("format") != "bare-notes") {
            null
        } else {
            BackupData(
                notes = root.optJSONArray("notes")?.let(NoteStore::decode).orEmpty(),
                folders = root.optJSONArray("folders")?.let(FolderStore::decode).orEmpty()
            )
        }
    } catch (_: Exception) {
        null
    }

    data class BackupData(
        val notes: List<Note>,
        val folders: List<Folder>
    )
}
