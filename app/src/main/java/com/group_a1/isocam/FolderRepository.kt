package com.group_a1.isocam

import android.content.Context
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
            .map { FolderItem(name = it.name, path = it.absolutePath, photoCount = listPhotos(it).size) }

    /** All .jpg files in a folder, newest first. */
    fun listPhotos(folder: File): List<File> =
        folder.listFiles { f -> f.isFile && f.extension.equals("jpg", ignoreCase = true) }
            .orEmpty()
            .sortedByDescending { it.lastModified() }

    /**
     * Strips anything except letters, digits, space, '-' and '_' so a name like "../../x"
     * can never escape the folders/ directory.
     */
    fun createFolder(context: Context, rawName: String): CreateResult {
        val name = rawName.replace(Regex("[^\\w\\- ]"), "").trim()
        if (name.isEmpty()) return CreateResult.Error("Use letters, numbers, spaces, - or _")

        val target = File(root(context), name)
        if (target.exists()) return CreateResult.Error("\"$name\" already exists")

        return if (target.mkdir()) CreateResult.Success(target)
        else CreateResult.Error("Couldn't create folder")
    }

    /**
     * Hook for the camera teammate: ask for a fresh file, write the JPEG bytes into it.
     * Example: val file = FolderRepository.newPhotoFile(File(folderPath))
     */
    fun newPhotoFile(folder: File): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(folder, "IMG_$stamp.jpg")
    }
}
