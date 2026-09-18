package com.example.myno.jz.data.repository

import android.content.Context
import android.util.Base64
import com.example.myno.jz.data.model.PrivacyLockConfig
import com.example.myno.jz.data.model.PrivacyLockType
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PrivacyLockStore(context: Context) {

    companion object {
        private const val PREFS =
            "privacy_lock"

        private const val KEY_ENABLED =
            "enabled"

        private const val KEY_TYPE =
            "type"

        private const val KEY_HASH =
            "credential_hash"

        private const val KEY_SALT =
            "salt"

        private const val KEY_UPDATED_AT =
            "updated_at"

        private const val HASH_ITERATIONS =
            120000

        private const val KEY_LENGTH =
            256
    }

    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

    fun getConfig(): PrivacyLockConfig {

        val typeName =
            preferences.getString(
                KEY_TYPE,
                PrivacyLockType.NONE.name
            )

        val type =
            try {
                PrivacyLockType.valueOf(
                    typeName
                        ?: PrivacyLockType.NONE.name
                )
            } catch (_: Exception) {
                PrivacyLockType.NONE
            }

        return PrivacyLockConfig(
            enabled = preferences.getBoolean(
                KEY_ENABLED,
                false
            ),
            type = type,
            credentialHash =
                preferences.getString(
                    KEY_HASH,
                    ""
                ).orEmpty(),
            salt =
                preferences.getString(
                    KEY_SALT,
                    ""
                ).orEmpty(),
            updatedAt =
                preferences.getLong(
                    KEY_UPDATED_AT,
                    0L
                )
        )
    }

    fun savePassword(
        password: String
    ): Boolean {

        return saveCredential(
            password,
            PrivacyLockType.PASSWORD
        )
    }

    fun savePattern(
        pattern: String
    ): Boolean {

        return saveCredential(
            pattern,
            PrivacyLockType.PATTERN
        )
    }

    private fun saveCredential(
        credential: String,
        type: PrivacyLockType
    ): Boolean {

        val saltBytes =
            ByteArray(32)

        SecureRandom().nextBytes(
            saltBytes
        )

        val salt =
            Base64.encodeToString(
                saltBytes,
                Base64.NO_WRAP
            )

        val hash =
            hashCredential(
                credential,
                saltBytes
            )

        return preferences.edit()
            .putBoolean(
                KEY_ENABLED,
                true
            )
            .putString(
                KEY_TYPE,
                type.name
            )
            .putString(
                KEY_HASH,
                hash
            )
            .putString(
                KEY_SALT,
                salt
            )
            .putLong(
                KEY_UPDATED_AT,
                System.currentTimeMillis()
            )
            .commit()
    }

    fun verify(
        credential: String
    ): Boolean {

        val config =
            getConfig()

        if (!config.enabled) {
            return true
        }

        if (
            config.credentialHash.isBlank() ||
            config.salt.isBlank()
        ) {
            return false
        }

        val saltBytes =
            try {
                Base64.decode(
                    config.salt,
                    Base64.NO_WRAP
                )
            } catch (_: Exception) {
                return false
            }

        val hash =
            hashCredential(
                credential,
                saltBytes
            )

        return hash ==
            config.credentialHash
    }

    fun disable(): Boolean {

        return preferences.edit()
            .putBoolean(
                KEY_ENABLED,
                false
            )
            .putString(
                KEY_TYPE,
                PrivacyLockType.NONE.name
            )
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .putLong(
                KEY_UPDATED_AT,
                System.currentTimeMillis()
            )
            .commit()
    }

    fun clear() {

        preferences.edit()
            .clear()
            .apply()
    }

    private fun hashCredential(
        credential: String,
        salt: ByteArray
    ): String {

        val spec =
            PBEKeySpec(
                credential.toCharArray(),
                salt,
                HASH_ITERATIONS,
                KEY_LENGTH
            )

        return try {

            val factory =
                SecretKeyFactory
                    .getInstance(
                        "PBKDF2WithHmacSHA256"
                    )

            val hash =
                factory
                    .generateSecret(spec)
                    .encoded

            Base64.encodeToString(
                hash,
                Base64.NO_WRAP
            )

        } finally {

            spec.clearPassword()
        }
    }
}