package com.example.myno.jz.data.repository

import android.content.Context
import com.example.myno.jz.data.model.EmailConfig

class EmailConfigStore(
    context: Context
) {

    private val preferences =
        context.applicationContext.getSharedPreferences(
            "email_config",
            Context.MODE_PRIVATE
        )

    fun getConfig(): EmailConfig {

        return EmailConfig(
            email = preferences.getString(
                "email",
                ""
            ).orEmpty(),

            provider = preferences.getString(
                "provider",
                "CUSTOM"
            ).orEmpty(),

            imapHost = preferences.getString(
                "imapHost",
                ""
            ).orEmpty(),

            imapPort = preferences.getInt(
                "imapPort",
                993
            ),

            imapSsl = preferences.getBoolean(
                "imapSsl",
                true
            ),

            smtpHost = preferences.getString(
                "smtpHost",
                ""
            ).orEmpty(),

            smtpPort = preferences.getInt(
                "smtpPort",
                465
            ),

            smtpSsl = preferences.getBoolean(
                "smtpSsl",
                true
            ),

            configured = preferences.getBoolean(
                "configured",
                false
            ),

            verified = preferences.getBoolean(
                "verified",
                false
            ),

            updatedAt = preferences.getLong(
                "updatedAt",
                0L
            )
        )
    }

    fun saveConfig(
        config: EmailConfig
    ): Boolean {

        return preferences.edit()
            .putString(
                "email",
                config.email
            )
            .putString(
                "provider",
                config.provider
            )
            .putString(
                "imapHost",
                config.imapHost
            )
            .putInt(
                "imapPort",
                config.imapPort
            )
            .putBoolean(
                "imapSsl",
                config.imapSsl
            )
            .putString(
                "smtpHost",
                config.smtpHost
            )
            .putInt(
                "smtpPort",
                config.smtpPort
            )
            .putBoolean(
                "smtpSsl",
                config.smtpSsl
            )
            .putBoolean(
                "configured",
                config.configured
            )
            .putBoolean(
                "verified",
                config.verified
            )
            .putLong(
                "updatedAt",
                config.updatedAt
            )
            .commit()
    }

    fun clear() {
        preferences.edit()
            .clear()
            .apply()
    }
}