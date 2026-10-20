package ir.ersaz.sandogh

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.crypto.SecretKey

data class ImportResult(val added: Int, val failedDelete: Int, val errors: Int)

/**
 * Copies each picked file into the encrypted vault, then removes the original
 * from shared storage so it disappears from the gallery and file manager.
 */
object ImportHelper {

    fun importUris(
        context: Context,
        uris: List<Uri>,
        key: SecretKey,
        store: VaultStore
    ): ImportResult {
        var added = 0
        var failedDelete = 0
        var errors = 0
        val resolver = context.contentResolver

        for (uri in uris) {
            try {
                val (name, size) = queryMeta(context, uri)
                val mime = resolver.getType(uri) ?: guessMime(name)
                val category = VaultItem.categoryOf(mime)

                val enc = store.newEncFile()
                resolver.openInputStream(uri).use { input ->
                    if (input == null) { errors++; enc.delete(); continue }
                    FileOutputStream(enc).use { out ->
                        Crypto.encrypt(input, out, key)
                    }
                }

                store.add(
                    VaultItem(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        mime = mime,
                        category = category,
                        size = size,
                        added = System.currentTimeMillis(),
                        enc = enc.name
                    )
                )
                added++

                if (!deleteOriginal(context, uri)) failedDelete++
            } catch (e: Exception) {
                errors++
            }
        }
        return ImportResult(added, failedDelete, errors)
    }

    private fun deleteOriginal(context: Context, uri: Uri): Boolean {
        // 1) Try the SAF provider's own delete.
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (DocumentsContract.deleteDocument(context.contentResolver, uri)) return true
            }
        } catch (_: Exception) {
        }
        // 2) Resolve to a real path and delete the file directly.
        try {
            val path = PathResolver.getPath(context, uri)
            if (path != null) {
                val f = File(path)
                if (f.exists() && f.delete()) return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun queryMeta(context: Context, uri: Uri): Pair<String, Long> {
        var name = "file_" + System.currentTimeMillis()
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
        } catch (_: Exception) {
        }
        return name to size
    }

    private fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }
}
