package ir.ersaz.sandogh

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

/** Random per-install salt for PBKDF2, persisted in private prefs. */
object KeyStoreSalt {
    private const val PREFS = "sandogh_prefs"
    private const val KEY = "salt"

    fun get(ctx: Context): ByteArray {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.getString(KEY, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        p.edit().putString(KEY, Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
        return salt
    }
}
