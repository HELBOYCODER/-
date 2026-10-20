package ir.ersaz.sandogh

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import ir.ersaz.sandogh.databinding.ActivityVaultBinding
import java.io.File
import java.util.concurrent.Executors

class VaultActivity : AppCompatActivity() {

    private lateinit var b: ActivityVaultBinding
    private lateinit var store: VaultStore
    private lateinit var adapter: VaultAdapter
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (!uris.isNullOrEmpty()) doImport(uris)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Session.isUnlocked) { finish(); return }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        b = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(b.root)

        store = VaultStore(this)
        clearDecryptCache() // clean any leftovers from a previous session

        adapter = VaultAdapter(emptyList()) { openItem(it) }
        b.grid.layoutManager = GridLayoutManager(this, 3)
        b.grid.adapter = adapter

        b.fab.setOnClickListener {
            Session.expectingResult = true
            try {
                picker.launch(arrayOf("*/*"))
            } catch (e: Exception) {
                Session.expectingResult = false
                toast("انتخاب‌گر فایل در دسترس نیست")
            }
        }
        b.btnLock.setOnClickListener { lockAndExit() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (!Session.isUnlocked) { finish(); return }
        Session.expectingResult = false
        refresh()
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock when the app leaves the foreground (but not while picking a
        // file or opening the viewer, which set expectingResult = true).
        if (!Session.expectingResult && !isChangingConfigurations && !isFinishing) {
            lockAndExit()
        }
    }

    private fun refresh() {
        val items = store.list()
        adapter.submit(items)
        b.empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openItem(item: VaultItem) {
        Session.expectingResult = true
        startActivity(Intent(this, ViewerActivity::class.java).putExtra("id", item.id))
    }

    private fun doImport(uris: List<Uri>) {
        val key = Session.key ?: return
        b.overlay.visibility = View.VISIBLE
        io.execute {
            val res = ImportHelper.importUris(this, uris, key, store)
            main.post {
                b.overlay.visibility = View.GONE
                refresh()
                val sb = StringBuilder("${res.added} فایل منتقل شد")
                if (res.failedDelete > 0) sb.append("، ${res.failedDelete} نسخه اصلی حذف نشد")
                if (res.errors > 0) sb.append("، ${res.errors} خطا")
                toast(sb.toString())
                if (res.failedDelete > 0 && !Permissions.hasAllFilesAccess(this)) promptAllFiles()
            }
        }
    }

    private fun promptAllFiles() {
        AlertDialog.Builder(this)
            .setTitle("حذف خودکار نسخه اصلی")
            .setMessage("برای اینکه فایل‌ها بعد از انتقال از گالری و فایل‌منیجر کاملاً پاک شوند، به دسترسی «مدیریت همه فایل‌ها» نیاز است.")
            .setPositiveButton("فعال کردن") { _, _ ->
                Session.expectingResult = true
                Permissions.requestAllFilesAccess(this)
            }
            .setNegativeButton("بعداً", null)
            .show()
    }

    private fun lockAndExit() {
        clearDecryptCache()
        Session.lock()
        finish()
    }

    private fun clearDecryptCache() {
        try {
            File(cacheDir, "open").listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
