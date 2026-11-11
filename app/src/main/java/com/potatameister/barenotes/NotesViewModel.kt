package com.potatameister.barenotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface NoteFilter {
    data object All : NoteFilter
    data class InFolder(val id: String) : NoteFilter
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    var folders by mutableStateOf<List<Folder>>(emptyList())
        private set

    var loaded by mutableStateOf(false)
        private set

    var filter by mutableStateOf<NoteFilter>(NoteFilter.All)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var textSizeSp by mutableStateOf(SettingsPrefs.textSizeSp(application))
        private set

    var sortOrder by mutableStateOf(SettingsPrefs.sortOrder(application))
        private set

    var themeMode by mutableStateOf(ThemePrefs.mode(application))
        private set

    var newNoteFolderId by mutableStateOf("")
        private set

    var storageError by mutableStateOf<String?>(null)
        private set

    private val mutex = Mutex()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutex.withLock {
                val loadedData = withContext(Dispatchers.IO) {
                    runCatching {
                        NoteStore.load(getApplication()) to FolderStore.load(getApplication())
                    }
                }
                loadedData.onSuccess { (loadedNotes, loadedFolders) ->
                    notes = loadedNotes
                    folders = loadedFolders
                }.onFailure {
                    storageError = "Could not load notes."
                }
                loaded = true
            }
        }
    }

    val visibleNotes: List<Note>
        get() {
            val filtered = when (val current = filter) {
                NoteFilter.All -> notes
                is NoteFilter.InFolder -> notes.filter { it.folderId == current.id }
            }
            val searched = if (searchQuery.isBlank()) {
                filtered
            } else {
                val query = searchQuery.trim()
                filtered.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.body.contains(query, ignoreCase = true)
                }
            }
            return if (sortOrder == SettingsPrefs.SORT_CREATED) {
                searched.sortedByDescending { it.createdAt }
            } else {
                searched.sortedByDescending { it.updatedAt }
            }
        }

    fun noteById(id: String): Note? = notes.firstOrNull { it.id == id }

    fun folderName(id: String): String? = folders.firstOrNull { it.id == id }?.name

    fun noteCount(folderId: String): Int = notes.count { it.folderId == folderId }

    fun updateFilter(value: NoteFilter) {
        filter = value
    }

    fun updateSearchQuery(value: String) {
        searchQuery = value
    }

    fun prepareNewNote() {
        newNoteFolderId = (filter as? NoteFilter.InFolder)?.id.orEmpty()
    }

    fun newNote(folderId: String): Note {
        val now = System.currentTimeMillis()
        return Note(
            id = UUID.randomUUID().toString(),
            title = "",
            body = "",
            createdAt = now,
            updatedAt = now,
            folderId = folderId
        )
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            mutex.withLock {
                val next = notes.filterNot { it.id == note.id } + note
                notes = next
                val saved = withContext(Dispatchers.IO) {
                    runCatching { NoteStore.save(getApplication(), next) }.isSuccess
                }
                if (!saved) storageError = "Could not save note."
            }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            mutex.withLock {
                val next = notes.filterNot { it.id == id }
                notes = next
                val saved = withContext(Dispatchers.IO) {
                    runCatching { NoteStore.save(getApplication(), next) }.isSuccess
                }
                if (!saved) storageError = "Could not save notes."
            }
        }
    }

    fun clearStorageError() {
        storageError = null
    }

    fun createFolder(name: String): Folder? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        if (folders.any { it.name.equals(clean, ignoreCase = true) }) return null
        val folder = Folder(UUID.randomUUID().toString(), clean)
        val next = (folders + folder).sortedBy { it.name.lowercase() }
        folders = next
        viewModelScope.launch {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    FolderStore.save(getApplication(), next)
                }
            }
        }
        return folder
    }

    fun renameFolder(folder: Folder, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val next = folders.map {
            if (it.id == folder.id) it.copy(name = clean) else it
        }.sortedBy { it.name.lowercase() }
        folders = next
        viewModelScope.launch {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    FolderStore.save(getApplication(), next)
                }
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            mutex.withLock {
                val nextNotes = notes.map {
                    if (it.folderId == folder.id) it.copy(folderId = "") else it
                }
                val nextFolders = folders.filterNot { it.id == folder.id }
                notes = nextNotes
                folders = nextFolders
                if (filter == NoteFilter.InFolder(folder.id)) filter = NoteFilter.All
                withContext(Dispatchers.IO) {
                    NoteStore.save(getApplication(), nextNotes)
                    FolderStore.save(getApplication(), nextFolders)
                }
            }
        }
    }

    fun setTextSize(sp: Int) {
        val value = sp.coerceIn(14, 20)
        SettingsPrefs.setTextSizeSp(getApplication(), value)
        textSizeSp = value
    }

    fun updateSortOrder(order: String) {
        SettingsPrefs.setSortOrder(getApplication(), order)
        sortOrder = order
    }

    fun updateThemeMode(mode: ThemeMode) {
        ThemePrefs.save(getApplication(), mode)
        themeMode = mode
    }

    fun exportData(): ByteArray = Backup.encode(notes, folders)

    fun replaceAll(data: Backup.BackupData) {
        viewModelScope.launch {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    NoteStore.save(getApplication(), data.notes)
                    FolderStore.save(getApplication(), data.folders)
                }
                notes = data.notes
                folders = data.folders
                filter = NoteFilter.All
                searchQuery = ""
            }
        }
    }
}
