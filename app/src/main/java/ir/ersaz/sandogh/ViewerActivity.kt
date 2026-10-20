package ir.ersaz.sandogh

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import ir.ersaz.sandogh.databinding.ActivityViewerBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors

class ViewerActivity : AppCompatActivity() {

    private lateinit var b: ActivityViewerBinding
    private var tmp: File? = null
    private var mp: MediaPlayer? = null
    private var category: String = VaultItem.DOC
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Session.isUnlocked) { finish(); return }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        b = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(b.root)

        val id = intent.getStringExtra("id")
        val item = VaultStore(this).list().find { it.id == id }
        if (item == null) { finish(); return }
        category = item.category
        decryptAndShow(item)
    }

    private fun decryptAndShow(item: VaultItem) {
        val key = Session.key ?: run { finish(); return }
        val store = VaultStore(this)
        b.progress.visibility = View.VISIBLE
        io.execute {
            try {
                val dir = File(cacheDir, "open").apply { mkdirs() }
                val out = File(dir, safeName(item.name))
                FileInputStream(store.encFile(item)).use { ins ->
                    FileOutputStream(out).use { o -> Crypto.decrypt(ins, o, key) }
                }
                tmp = out
                main.post {
                    b.progress.visibility = View.GONE
                    show(item, out)
                }
            } catch (e: Exception) {
                main.post {
                    b.progress.visibility = View.GONE
                    toast("خطا در باز کردن فایل")
                    finish()
                }
            }
        }
    }

    private fun show(item: VaultItem, file: File) {
        when (item.category) {
            VaultItem.IMAGE -> {
                b.image.visibility = View.VISIBLE
                b.image.setImageURI(Uri.fromFile(file))
            }
            VaultItem.VIDEO -> {
                b.video.visibility = View.VISIBLE
                val mc = MediaController(this)
                mc.setAnchorView(b.video)
                b.video.setMediaController(mc)
                b.video.setVideoURI(Uri.fromFile(file))
                b.video.setOnPreparedListener { it.start() }
                b.video.setOnErrorListener { _, _, _ ->
                    toast("پخش این ویدیو ممکن نیست"); true
                }
            }
            VaultItem.AUDIO -> {
                b.audioPanel.visibility = View.VISIBLE
                b.audioName.text = item.name
                setupAudio(file)
            }
            else -> openExternal(item, file)
        }
    }

    private fun setupAudio(file: File) {
        try {
            mp = MediaPlayer().apply {
                setDataSource(file.path)
                prepare()
                setOnCompletionListener {
                    b.playPause.setImageResource(R.drawable.ic_play)
                }
            }
            b.playPause.setOnClickListener { togglePlay() }
            togglePlay()
        } catch (e: Exception) {
            toast("خطا در پخش صدا")
        }
    }

    private fun togglePlay() {
        val p = mp ?: return
        if (p.isPlaying) {
            p.pause()
            b.playPause.setImageResource(R.drawable.ic_play)
        } else {
            p.start()
            b.playPause.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun openExternal(item: VaultItem, file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val i = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Session.expectingResult = true
            startActivity(Intent.createChooser(i, item.name))
        } catch (e: Exception) {
            toast("برنامه‌ای برای باز کردن این سند پیدا نشد")
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { mp?.release() } catch (_: Exception) {}
        mp = null
        // Keep decrypted docs until the vault is locked (an external app may
        // still be reading them); clean up media temp files immediately.
        if (category != VaultItem.DOC) {
            try { tmp?.delete() } catch (_: Exception) {}
        }
    }

    private fun safeName(name: String): String {
        val cleaned = name.replace(Regex("[^A-Za-z0-9._\\-]"), "_")
        return if (cleaned.isBlank()) "file_" + System.currentTimeMillis() else cleaned
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
