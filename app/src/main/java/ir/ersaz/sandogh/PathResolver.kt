package ir.ersaz.sandogh

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/**
 * Best-effort resolution of a document/content Uri to a real filesystem path,
 * so the original can be deleted via java.io.File once copied into the vault.
 * Returns null when the path can't be resolved (then deletion falls back to
 * DocumentsContract or is reported as failed).
 */
object PathResolver {

    fun getPath(context: Context, uri: Uri): String? {
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                when (uri.authority) {
                    "com.android.externalstorage.documents" -> {
                        val parts = docId.split(":")
                        val type = parts[0]
                        val rel = if (parts.size > 1) parts[1] else ""
                        if (type.equals("primary", true)) {
                            return Environment.getExternalStorageDirectory().absolutePath + "/" + rel
                        }
                        // non-primary volumes (SD card)
                        val sd = "/storage/$type/$rel"
                        if (File(sd).exists()) return sd
                    }
                    "com.android.providers.downloads.documents" -> {
                        if (docId.startsWith("raw:")) return docId.removePrefix("raw:")
                        val id = docId.toLongOrNull()
                        if (id != null) {
                            val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), id
                            )
                            return queryData(context, contentUri)
                        }
                    }
                    "com.android.providers.media.documents" -> {
                        val parts = docId.split(":")
                        val type = parts[0]
                        val id = if (parts.size > 1) parts[1] else return null
                        val base = when (type) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> MediaStore.Files.getContentUri("external")
                        }
                        val contentUri = ContentUris.withAppendedId(base, id.toLong())
                        return queryData(context, contentUri)
                    }
                }
            }
            if ("content".equals(uri.scheme, true)) return queryData(context, uri)
            if ("file".equals(uri.scheme, true)) return uri.path
        } catch (_: Exception) {
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun queryData(context: Context, uri: Uri): String? {
        val col = MediaStore.MediaColumns.DATA
        context.contentResolver.query(uri, arrayOf(col), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(col)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return null
    }
}
