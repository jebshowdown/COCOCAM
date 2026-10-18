package com.group_a1.isocam

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FolderItem(
    val name: String,
    val path: String,
    val photoCount: Int,
    val isLocked: Boolean = false,
    val icon: String = "📁"
)

sealed interface CreateResult {
    data class Success(val folder: File) : CreateResult
    data class Error(val message: String) : CreateResult
}

/** All file-system logic for isocam lives here so the UI stays dumb. */
object FolderRepository {

    private const val ROOT_NAME = "folders"

    /** context.filesDir/folders/ (created if missing). */
    fun root(context: Context): File =
        File(context.filesDir, ROOT_NAME).also { it.mkdirs() }

    fun listFolders(context: Context): List<FolderItem> =
        root(context).listFiles { f -> f.isDirectory }
            .orEmpty()
            .sortedBy { it.name.lowercase() }
            .map {
                val dir = File(it.absolutePath)
                val settings = loadSettings(dir)
                FolderItem(
                    name = it.name,
                    path = it.absolutePath,
                    photoCount = listPhotos(dir).size,
                    isLocked = isLocked(dir),
                    icon = settings.icon
                )
            }

    /** All .jpg files in a folder, newest first. */
    fun listPhotos(folder: File): List<File> =
        folder.listFiles { f -> f.isFile && f.extension.equals("jpg", ignoreCase = true) }
            .orEmpty()
            .sortedByDescending { it.lastModified() }

    fun renameFolder(context: Context, oldName: String, rawNewName: String): CreateResult {
        val newName = sanitize(rawNewName)
        if (newName.isBlank()) return CreateResult.Error("Invalid name, use letters, numbers, spaces, _ or -")

        val oldDir = File(root(context), oldName)
        if (!oldDir.isDirectory) return CreateResult.Error("Folder no longer exists")
        if (newName == oldName) return CreateResult.Success(oldDir) // nothing to rename

        val newDir = File(root(context), newName)
        if (newDir.exists()) return CreateResult.Error("\"$newName\" already exists")

        return if (oldDir.renameTo(newDir)) CreateResult.Success(newDir)
        else CreateResult.Error("Couldn't rename folder")
    }

    fun createFolder(context: Context, rawName: String): CreateResult {
        val name = sanitize(rawName)
        if (name.isBlank()) return CreateResult.Error("Invalid name")

        val target = File(root(context), name)
        if (target.exists()) return CreateResult.Error("\"$name\" already exists")

        return if (target.mkdir()) CreateResult.Success(target)
        else CreateResult.Error("Couldn't create folder")
    }

    /**
     * Strips anything except letters, digits, space, '-', '_' and ':'
     */
    fun sanitize(rawName: String): String {
        return rawName.replace(Regex("[^\\w\\- :]"), "").trim()
    }

    /**
     * Hook for the camera teammate: ask for a fresh file, write the JPEG bytes into it.
     */
    fun newPhotoFile(folder: File): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(folder, "IMG_$stamp.jpg")
    }

    data class FolderSettings(
        val retentionDays: Int = 0,
        val icon: String = "📁",
        val passcode: String? = null
    )

    private const val SETTINGS_FILE = "folder_settings.json"

    fun loadSettings(folder: File): FolderSettings = try {
        val json = JSONObject(File(folder, SETTINGS_FILE).readText())
        FolderSettings(
            retentionDays = json.optInt("retentionDays", 0),
            icon = json.optString("icon", "📁"),
            passcode = json.optString("passcode", "").takeIf { it.isNotEmpty() }
        )
    } catch (_: Exception) { FolderSettings() }

    fun saveSettings(folder: File, s: FolderSettings) {
        val json = JSONObject()
            .put("retentionDays", s.retentionDays)
            .put("icon", s.icon)
        if (!s.passcode.isNullOrEmpty()) {
            json.put("passcode", s.passcode)
        }
        File(folder, SETTINGS_FILE).writeText(json.toString())
    }

    fun isLocked(folder: File): Boolean =
        !loadSettings(folder).passcode.isNullOrEmpty()

    fun setPasscode(folder: File, passcode: String) {
        val current = loadSettings(folder)
        saveSettings(folder, current.copy(passcode = passcode))
    }

    fun verifyPasscode(folder: File, input: String): Boolean {
        val code = loadSettings(folder).passcode
        return code != null && code == input
    }
}
