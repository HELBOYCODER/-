package ir.ersaz.sandogh

import android.content.Context
import org.json.JSONArray
import java.io.File

/** Manages the encrypted-file store and its plaintext index (app-private). */
class VaultStore(context: Context) {

    private val filesDir = context.filesDir
    private val vaultDir = File(filesDir, "vault").apply { mkdirs() }
    private val indexFile = File(filesDir, "index.json")

    fun encFile(item: VaultItem) = File(vaultDir, item.enc)

    fun newEncFile(): File {
        var f: File
        do {
            f = File(vaultDir, "v_" + System.nanoTime() + ".enc")
        } while (f.exists())
        return f
    }

    @Synchronized
    fun list(): MutableList<VaultItem> {
        if (!indexFile.exists()) return mutableListOf()
        return try {
            val arr = JSONArray(indexFile.readText())
            val out = ArrayList<VaultItem>(arr.length())
            for (i in 0 until arr.length()) out.add(VaultItem.fromJson(arr.getJSONObject(i)))
            out.sortByDescending { it.added }
            out
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    @Synchronized
    fun add(item: VaultItem) {
        val all = list()
        all.add(0, item)
        persist(all)
    }

    @Synchronized
    fun remove(id: String) {
        val all = list()
        val it = all.find { it.id == id }
        if (it != null) {
            encFile(it).delete()
            all.remove(it)
            persist(all)
        }
    }

    private fun persist(all: List<VaultItem>) {
        val arr = JSONArray()
        all.forEach { arr.put(it.toJson()) }
        indexFile.writeText(arr.toString())
    }
}
