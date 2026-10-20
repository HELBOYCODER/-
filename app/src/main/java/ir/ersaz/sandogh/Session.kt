package ir.ersaz.sandogh

import javax.crypto.SecretKey

/** In-memory session state. Cleared when the app is locked again. */
object Session {
    @Volatile var key: SecretKey? = null
    @Volatile var expectingResult: Boolean = false

    val isUnlocked: Boolean get() = key != null

    fun lock() {
        key = null
        expectingResult = false
    }
}
