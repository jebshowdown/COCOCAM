package com.group_a1.isocam

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FolderItem(val name: String, val path: String, val photoCount: Int)

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
                FolderItem(
                    name = it.name,
                    path = it.absolutePath,
                    photoCount = listPhotos(it).size
                )
            }

    /** All .jpg files in a folder, newest first. */
    fun listPhotos(folder: File): List<File> =
        folder.listFiles { f -> f.isFile && f.extension.equals("jpg", ignoreCase = true) }
            .orEmpty()
            .sortedByDescending { it.lastModified() }

    fun renameFolder(context: Context, oldName: String, rawNewName: String): CreateResult {
        val newName = sanitize(rawNewName)
        if (newName.isBlank()) return CreateResult.Error("Invalid name, User letters, numbers, spaces, _ or -")

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
     * Strips anything except letters, digits, space, '-' and '_' so a name like "../../x"
     * can never escape the folders/ directory.
     */
    fun sanitize(rawName: String): String {
        val name = rawName.replace(Regex("[^\\w\\- ]"), "").trim()
        return name
        }

    /**
     * Hook for the camera teammate: ask for a fresh file, write the JPEG bytes into it.
     * Example: val file = FolderRepository.newPhotoFile(File(folderPath))
     */
    fun newPhotoFile(folder: File): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(folder, "IMG_$stamp.jpg")
    }

    data class FolderSettings(val retentionDays: Int = 0, val icon: String = "\uD83D\uDCC1")
    private const val SETTINGS_FILE = "folder_settings.json"

    fun loadSettings(folder: File): FolderSettings = try {
        val json = JSONObject(File(folder, SETTINGS_FILE).readText())
        FolderSettings(json.optInt("retentionDays", 0), json.optString("icon", "📁"))
    } catch (e: Exception) { FolderSettings() }  // missing or corrupt file -> defaults

    fun saveSettings(folder: File, s: FolderSettings) {
        val json = JSONObject().put("retentionDays", s.retentionDays).put("icon", s.icon)
        File(folder, SETTINGS_FILE).writeText(json.toString())
    }
}
