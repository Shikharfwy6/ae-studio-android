package com.app.aestudio.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.app.aestudio.ui.state.MediaItem
import com.app.aestudio.ui.state.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(
    val folderName: String,
    val items: List<MediaItem>,
    val truncated: Boolean,
)

/** Walks a folder picked with the system folder picker and sorts files into video, audio and images. */
object MediaImporter {
    private const val MAX_FILES = 1000
    private val videoExt = setOf("mp4", "mov", "mkv", "webm", "avi", "3gp", "m4v")
    private val audioExt = setOf("mp3", "wav", "aac", "m4a", "ogg", "flac", "opus")
    private val imageExt = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic")

    suspend fun scan(context: Context, treeUri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Cannot open this folder")
        val found = ArrayList<MediaItem>()
        var truncated = false

        fun walk(dir: DocumentFile) {
            for (file in dir.listFiles()) {
                if (found.size >= MAX_FILES) {
                    truncated = true
                    return
                }
                if (file.isDirectory) {
                    walk(file)
                } else {
                    val kind = classify(file) ?: continue
                    val uri = file.uri.toString()
                    found += MediaItem(uri, file.name ?: "file", kind, uri, file.length())
                }
            }
        }
        walk(root)
        ImportResult(root.name ?: "Imported", found, truncated)
    }

    private fun classify(file: DocumentFile): MediaKind? {
        val mime = file.type ?: ""
        return when {
            mime.startsWith("video/") -> MediaKind.Video
            mime.startsWith("audio/") -> MediaKind.Audio
            mime.startsWith("image/") -> MediaKind.Image
            else -> when (file.name?.substringAfterLast('.', "")?.lowercase() ?: "") {
                in videoExt -> MediaKind.Video
                in audioExt -> MediaKind.Audio
                in imageExt -> MediaKind.Image
                else -> null
            }
        }
    }
}
