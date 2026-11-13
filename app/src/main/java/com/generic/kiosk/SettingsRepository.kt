package com.generic.kiosk

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("kiosk_settings")

data class KioskSettings(
    val startUrl: String = "https://example.com",
    val autoStart: Boolean = true,
    val fullscreen: Boolean = true,
    val lockTask: Boolean = false,
    val keepScreenOn: Boolean = true,
    val allowExternalLinks: Boolean = false,
    val allowDownloads: Boolean = false,
    val orientation: String = "system",
    val reloadSeconds: Int = 0,
    val loadTimeoutSeconds: Int = 45,
    val adminPinHash: String = ""
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val url = stringPreferencesKey("start_url")
        val auto = booleanPreferencesKey("auto_start")
        val full = booleanPreferencesKey("fullscreen")
        val lock = booleanPreferencesKey("lock_task")
        val keep = booleanPreferencesKey("keep_screen_on")
        val external = booleanPreferencesKey("allow_external")
        val downloads = booleanPreferencesKey("allow_downloads")
        val orientation = stringPreferencesKey("orientation")
        val reload = intPreferencesKey("reload_seconds")
        val timeout = intPreferencesKey("load_timeout_seconds")
        val pin = stringPreferencesKey("admin_pin_hash")
    }

    suspend fun load(): KioskSettings {
        val p = context.dataStore.data.first()
        return KioskSettings(p[Keys.url] ?: "https://example.com", p[Keys.auto] ?: true,
            p[Keys.full] ?: true, p[Keys.lock] ?: false, p[Keys.keep] ?: true,
            p[Keys.external] ?: false, p[Keys.downloads] ?: false, p[Keys.orientation] ?: "system",
            p[Keys.reload] ?: 0, p[Keys.timeout] ?: 45, p[Keys.pin] ?: AdminAuthenticator.hash("1234"))
    }

    suspend fun save(s: KioskSettings) {
        require(UrlValidator.validate(s.startUrl) != null) { "URL HTTP/HTTPS no válida" }
        context.dataStore.edit { p ->
            p[Keys.url] = s.startUrl.trim(); p[Keys.auto] = s.autoStart; p[Keys.full] = s.fullscreen
            p[Keys.lock] = s.lockTask; p[Keys.keep] = s.keepScreenOn; p[Keys.external] = s.allowExternalLinks
            p[Keys.downloads] = s.allowDownloads; p[Keys.orientation] = s.orientation
            p[Keys.reload] = s.reloadSeconds.coerceIn(0, 86400); p[Keys.timeout] = s.loadTimeoutSeconds.coerceIn(10, 300)
            p[Keys.pin] = s.adminPinHash
        }
    }

    suspend fun setPin(pin: String) { require(pin.length in 4..12 && pin.all(Char::isDigit)); save(load().copy(adminPinHash = AdminAuthenticator.hash(pin))) }
}

object AdminAuthenticator {
    private const val KEY_ALIAS = "generic_kiosk_admin_pin_hmac"

    private fun key(): javax.crypto.SecretKey {
        val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? javax.crypto.SecretKey)?.let { return it }
        val generator = javax.crypto.KeyGenerator.getInstance(android.security.keystore.KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_SIGN or android.security.keystore.KeyProperties.PURPOSE_VERIFY)
            .setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256).build())
        return generator.generateKey()
    }

    fun hash(pin: String): String = javax.crypto.Mac.getInstance("HmacSHA256").run {
        init(key()); doFinal(pin.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
    fun verify(pin: String, expectedHash: String): Boolean = java.security.MessageDigest.isEqual(hash(pin).toByteArray(), expectedHash.toByteArray())
}
