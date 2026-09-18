package com.example.myno.jz.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EmailCredentialStore(
    context: Context
) {

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MoneyBookEmailKey"

        private const val PREFS = "email_credentials"
        private const val KEY_AUTH_CODE = "auth_code"
    }

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    private fun getOrCreateKey(): SecretKey {

        val keyStore =
            java.security.KeyStore.getInstance(KEYSTORE)

        keyStore.load(null)

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE
            )

        val spec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()

        keyGenerator.init(spec)

        return keyGenerator.generateKey()
    }

    fun saveAuthCode(
        authCode: String
    ): Boolean {

        return try {

            val key = getOrCreateKey()

            val cipher =
                Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

            cipher.init(
                Cipher.ENCRYPT_MODE,
                key
            )

            val encrypted =
                cipher.doFinal(
                    authCode.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

            val iv =
                Base64.encodeToString(
                    cipher.iv,
                    Base64.NO_WRAP
                )

            val data =
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP
                )

            preferences.edit()
                .putString(
                    "iv",
                    iv
                )
                .putString(
                    KEY_AUTH_CODE,
                    data
                )
                .commit()

        } catch (e: Exception) {
            false
        }
    }

    fun getAuthCode(): String {

        return try {

            val ivString =
                preferences.getString(
                    "iv",
                    null
                )

            val encryptedString =
                preferences.getString(
                    KEY_AUTH_CODE,
                    null
                )

            if (
                ivString.isNullOrBlank() ||
                encryptedString.isNullOrBlank()
            ) {
                return ""
            }

            val iv =
                Base64.decode(
                    ivString,
                    Base64.NO_WRAP
                )

            val encrypted =
                Base64.decode(
                    encryptedString,
                    Base64.NO_WRAP
                )

            val key = getOrCreateKey()

            val cipher =
                Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(
                    128,
                    iv
                )
            )

            String(
                cipher.doFinal(encrypted),
                StandardCharsets.UTF_8
            )

        } catch (e: Exception) {
            ""
        }
    }

    fun clear() {

        preferences.edit()
            .clear()
            .apply()
    }
}