package ir.ersaz.sandogh

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

/**
 * Launcher screen: shows the rotary safe dial (HTML/JS inside a WebView).
 * The dial's JS calls AndroidVault.onUnlock(code) when the user lands on the
 * secret number; we derive the AES key from that code and open the vault.
 */
class LockActivity : AppCompatActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block screenshots / recents preview of the lock+vault.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        web = WebView(this)
        setContentView(web)
        web.setBackgroundColor(0xFF120D09.toInt())
        web.settings.javaScriptEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "AndroidVault")
        web.loadUrl("file:///android_asset/lock.html")
    }

    override fun onResume() {
        super.onResume()
        // Reload a fresh locked dial each time we return here.
        if (!Session.isUnlocked) {
            web.clearHistory()
            web.loadUrl("file:///android_asset/lock.html")
        }
    }

    inner class Bridge {
        /** Called from lock.html when the dial reaches the secret number. */
        @JavascriptInterface
        fun onUnlock(code: String) {
            runOnUiThread {
                Session.key = Crypto.deriveKey(code, KeyStoreSalt.get(this@LockActivity))
                startActivity(Intent(this@LockActivity, VaultActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }
}
