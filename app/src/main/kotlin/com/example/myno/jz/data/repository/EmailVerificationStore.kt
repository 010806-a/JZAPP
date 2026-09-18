package com.example.myno.jz.data.repository

import android.content.Context
import java.security.MessageDigest

class EmailVerificationStore(context: Context) {

    companion object {
        private const val PREFS = "email_verification"
        private const val KEY_EMAIL = "email"
        private const val KEY_CODE_HASH = "code_hash"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ATTEMPTS = "attempts"

        const val CODE_EXPIRE_TIME = 5 * 60 * 1000L
        const val RESEND_INTERVAL = 60 * 1000L
        const val MAX_ATTEMPTS = 5
    }

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun saveCode(
        email: String,
        code: String
    ): Boolean {

        val now = System.currentTimeMillis()

        return preferences.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_CODE_HASH, hash(code))
            .putLong(KEY_CREATED_AT, now)
            .putLong(
                KEY_EXPIRES_AT,
                now + CODE_EXPIRE_TIME
            )
            .putInt(KEY_ATTEMPTS, 0)
            .commit()
    }

    fun canResend(): Boolean {

        val createdAt =
            preferences.getLong(
                KEY_CREATED_AT,
                0L
            )

        if (createdAt == 0L) {
            return true
        }

        return System.currentTimeMillis() - createdAt >=
            RESEND_INTERVAL
    }

    fun getRemainingSeconds(): Long {

        val createdAt =
            preferences.getLong(
                KEY_CREATED_AT,
                0L
            )

        if (createdAt == 0L) {
            return 0L
        }

        val elapsed =
            System.currentTimeMillis() - createdAt

        val remaining =
            RESEND_INTERVAL - elapsed

        return if (remaining > 0) {
            (remaining + 999) / 1000
        } else {
            0L
        }
    }

    fun verify(
        email: String,
        code: String
    ): VerificationResult {

        val savedEmail =
            preferences.getString(
                KEY_EMAIL,
                ""
            ).orEmpty()

        if (
            savedEmail.isBlank() ||
            savedEmail != email
        ) {
            return VerificationResult.EMAIL_MISMATCH
        }

        val expiresAt =
            preferences.getLong(
                KEY_EXPIRES_AT,
                0L
            )

        if (
            expiresAt == 0L ||
            System.currentTimeMillis() > expiresAt
        ) {
            clear()
            return VerificationResult.EXPIRED
        }

        val attempts =
            preferences.getInt(
                KEY_ATTEMPTS,
                0
            )

        if (attempts >= MAX_ATTEMPTS) {
            return VerificationResult.TOO_MANY_ATTEMPTS
        }

        val savedHash =
            preferences.getString(
                KEY_CODE_HASH,
                ""
            ).orEmpty()

        if (hash(code) != savedHash) {

            preferences.edit()
                .putInt(
                    KEY_ATTEMPTS,
                    attempts + 1
                )
                .apply()

            return VerificationResult.INVALID_CODE
        }

        clear()

        return VerificationResult.SUCCESS
    }

    fun clear() {

        preferences.edit()
            .clear()
            .apply()
    }

    private fun hash(
        value: String
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        val bytes =
            digest.digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}

enum class VerificationResult {
    SUCCESS,
    EMAIL_MISMATCH,
    EXPIRED,
    INVALID_CODE,
    TOO_MANY_ATTEMPTS
}