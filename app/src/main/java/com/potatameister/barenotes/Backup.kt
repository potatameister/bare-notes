package com.potatameister.barenotes

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Backup {

    private const val VERSION = 2

    fun encode(notes: List<Note>, folders: List<Folder>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(
                JSONObject()
                    .put("format", "bare-notes")
                    .put("version", VERSION)
                    .toString()
                    .toByteArray()
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("folders.json"))
            zip.write(FolderStore.encode(folders).toString().toByteArray())
            zip.closeEntry()

            notes.forEach { note ->
                val id = note.id.ifBlank { return@forEach }
                zip.putNextEntry(ZipEntry("notes/$id.md"))
                zip.write(MarkdownCodec.encode(note).toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): BackupData? {
        if (bytes.isEmpty()) return null
        val first = bytes.firstOrNull { !it.toInt().toChar().isWhitespace() }
        return if (first?.toInt() == '{'.code) {
            decodeLegacyJson(String(bytes, Charsets.UTF_8))
        } else {
            decodeZip(bytes)
        }
    }

    private fun decodeZip(bytes: ByteArray): BackupData? {
        var folders: List<Folder> = emptyList()
        val notes = mutableListOf<Note>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name
                        val data = zip.readBytes()
                        when {
                            name == "folders.json" ->
                                folders = FolderStore.decode(
                                    JSONArray(String(data, Charsets.UTF_8))
                                )
                            name.endsWith(".md") -> {
                                val id = name.substringAfterLast('/').removeSuffix(".md")
                                MarkdownCodec.decode(
                                    id,
                                    String(data, Charsets.UTF_8)
                                )?.let { notes.add(it) }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (_: Exception) {
            return null
        }
        if (notes.isEmpty()) return null
        return BackupData(
            notes = notes.distinctBy { it.id },
            folders = folders
        )
    }

    private fun decodeLegacyJson(text: String): BackupData? = try {
        val root = JSONObject(text)
        if (root.optString("format") != "bare-notes") {
            null
        } else {
            BackupData(
                notes = root.optJSONArray("notes")
                    ?.let(NoteStore::decodeLegacy)
                    .orEmpty()
                    .distinctBy { it.id },
                folders = root.optJSONArray("folders")
                    ?.let(FolderStore::decode)
                    .orEmpty()
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
