package ge.renovatori.app.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONException
import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** What survives an app restart: the token pair and the last known user (shown while `/me` refreshes it). */
data class StoredSession(val accessToken: String, val refreshToken: String, val user: User)

/**
 * Encrypted session storage: an AES/GCM key that never leaves the Android Keystore encrypts the
 * session JSON; only IV + ciphertext are written to private preferences. The preferences file is
 * excluded from backups (res/xml/backup_rules.xml), since a restored ciphertext could not be
 * decrypted on a new device anyway.
 */
class TokenStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): StoredSession? {
        val iv = prefs.getString(KEY_IV, null)
        val ciphertext = prefs.getString(KEY_DATA, null)
        if (iv == null || ciphertext == null) return null

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)))
            val json = JSONObject(String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8))
            StoredSession(
                accessToken = json.getString("accessToken"),
                refreshToken = json.getString("refreshToken"),
                user = User.fromJson(json.getJSONObject("user"))
            )
        } catch (e: GeneralSecurityException) {
            // Key gone or data tampered with: the session is unusable, start signed out.
            clear()
            null
        } catch (e: JSONException) {
            clear()
            null
        } catch (e: IllegalArgumentException) {
            clear()
            null
        }
    }

    fun save(session: StoredSession) {
        val json = JSONObject()
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("user", session.user.toJson())
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_DATA, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "auth_session"
        const val KEY_IV = "iv"
        const val KEY_DATA = "data"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "renovatori_auth_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
