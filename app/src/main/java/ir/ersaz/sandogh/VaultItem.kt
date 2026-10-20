package ir.ersaz.sandogh

import org.json.JSONObject

data class VaultItem(
    val id: String,
    val name: String,
    val mime: String,
    val category: String, // image | video | audio | doc
    val size: Long,
    val added: Long,
    val enc: String       // encrypted file name inside the vault dir
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("mime", mime)
        .put("category", category)
        .put("size", size)
        .put("added", added)
        .put("enc", enc)

    companion object {
        const val IMAGE = "image"
        const val VIDEO = "video"
        const val AUDIO = "audio"
        const val DOC = "doc"

        fun fromJson(o: JSONObject) = VaultItem(
            id = o.getString("id"),
            name = o.getString("name"),
            mime = o.optString("mime", "application/octet-stream"),
            category = o.optString("category", DOC),
            size = o.optLong("size", 0),
            added = o.optLong("added", 0),
            enc = o.getString("enc")
        )

        fun categoryOf(mime: String?): String {
            val m = mime?.lowercase() ?: ""
            return when {
                m.startsWith("image/") -> IMAGE
                m.startsWith("video/") -> VIDEO
                m.startsWith("audio/") -> AUDIO
                else -> DOC
            }
        }
    }
}
