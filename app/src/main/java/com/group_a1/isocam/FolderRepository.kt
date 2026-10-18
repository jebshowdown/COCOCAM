package com.group_a1.isocam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FolderItem(
    val name: String,
    val path: String,
    val photoCount: Int,
    val isLocked: Boolean = false,
    val expirationDate: Long? = null,
    val lastModified: Long = 0L
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
            .map {
                val dir = File(it.absolutePath)
                val settings = loadSettings(dir)
                FolderItem(
                    name = it.name,
                    path = it.absolutePath,
                    photoCount = listPhotos(dir).size,
                    isLocked = isLocked(dir),
                    expirationDate = settings.expirationDate,
                    lastModified = dir.lastModified()
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
        val passcode: String? = null,
        val expirationDate: Long? = null
    )

    private const val SETTINGS_FILE = "folder_settings.json"

    fun loadSettings(folder: File): FolderSettings = try {
        val json = JSONObject(File(folder, SETTINGS_FILE).readText())
        FolderSettings(
            retentionDays = json.optInt("retentionDays", 0),
            passcode = json.optString("passcode", "").takeIf { it.isNotEmpty() },
            expirationDate = if (json.has("expirationDate")) json.optLong("expirationDate") else null
        )
    } catch (_: Exception) { FolderSettings() }

    fun saveSettings(folder: File, s: FolderSettings) {
        val json = JSONObject()
            .put("retentionDays", s.retentionDays)
        if (!s.passcode.isNullOrEmpty()) {
            json.put("passcode", s.passcode)
        }
        if (s.expirationDate != null) {
            json.put("expirationDate", s.expirationDate)
        } else {
            json.remove("expirationDate")
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

    fun markFolderOpened(folder: File) {
        try {
            folder.setLastModified(System.currentTimeMillis())
        } catch (_: Exception) {}
    }

    fun formatTimedDeletion(expirationDate: Long?): String? {
        if (expirationDate == null) return null
        val timeLeft = expirationDate - System.currentTimeMillis()
        if (timeLeft <= 0) return "Expired"

        val totalMinutes = timeLeft / (1000 * 60)
        val totalHours = totalMinutes / 60
        val days = totalHours / 24
        val remainingHours = totalHours % 24
        val remainingMinutes = totalMinutes % 60

        return when {
            days >= 1 -> {
                if (remainingHours > 0) "Deletes in $days day${if (days == 1L) "" else "s"}, $remainingHours hr${if (remainingHours == 1L) "" else "s"}"
                else "Deletes in $days day${if (days == 1L) "" else "s"}"
            }
            totalHours >= 1 -> {
                if (remainingMinutes > 0) "Deletes in $totalHours hr${if (totalHours == 1L) "" else "s"}, $remainingMinutes min${if (remainingMinutes == 1L) "" else "s"}"
                else "Deletes in $totalHours hr${if (totalHours == 1L) "" else "s"}"
            }
            totalMinutes >= 1 -> "Deletes in $totalMinutes min${if (totalMinutes == 1L) "" else "s"}"
            else -> "Deletes in <1 min"
        }
    }

    fun exportFolderToPdf(context: Context, folder: File): File? {
        return exportPhotosToPdf(context, listPhotos(folder), folder.name)
    }

    fun exportPhotosToPdf(context: Context, photos: List<File>, pdfName: String): File? {
        if (photos.isEmpty()) return null

        val pdfDocument = PdfDocument()
        try {
            photos.forEachIndexed { index, photoFile ->
                // First pass: decode dimensions to downsample efficiently
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(photoFile.absolutePath, options)
                val origW = options.outWidth
                val origH = options.outHeight
                if (origW <= 0 || origH <= 0) return@forEachIndexed

                // Cap max dimension to 960px for lightweight multi-page PDFs
                val maxDim = 960f
                val scale = minOf(1f, maxDim / maxOf(origW, origH))
                val targetW = (origW * scale).toInt().coerceAtLeast(1)
                val targetH = (origH * scale).toInt().coerceAtLeast(1)

                var sampleSize = 1
                while (origW / (sampleSize * 2) >= targetW && origH / (sampleSize * 2) >= targetH) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                val rawBitmap = BitmapFactory.decodeFile(photoFile.absolutePath, decodeOptions) ?: return@forEachIndexed
                val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
                if (scaledBitmap != rawBitmap) {
                    rawBitmap.recycle()
                }

                // Compress to 60% JPEG byte stream for Crowdmark limits
                val stream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
                val compressedBytes = stream.toByteArray()
                val finalBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(finalBitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)

                scaledBitmap.recycle()
                finalBitmap.recycle()
            }
            val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
            val exportFile = File(exportDir, "$pdfName.pdf")
            FileOutputStream(exportFile).use { out ->
                pdfDocument.writeTo(out)
            }
            return exportFile
        } catch (_: Exception) {
            return null
        } finally {
            pdfDocument.close()
        }
    }
}
