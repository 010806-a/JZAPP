package com.example.myno.jz.data.email

import com.example.myno.jz.data.model.EmailConfig
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import java.util.Properties

object EmailConnectionTester {
     
     fun testImap(
    config: EmailConfig,
    password: String
): Result<Unit> {

    return try {

        val protocol =
            if (config.imapSsl) {
                "imaps"
            } else {
                "imap"
            }

        val properties =
            java.util.Properties().apply {

                put(
                    "mail.$protocol.auth",
                    "true"
                )

                put(
                    "mail.$protocol.connectiontimeout",
                    "10000"
                )

                put(
                    "mail.$protocol.timeout",
                    "10000"
                )

                put(
                    "mail.$protocol.writetimeout",
                    "10000"
                )

                if (config.imapSsl) {

                    put(
                        "mail.imaps.ssl.enable",
                        "true"
                    )

                } else {

                    put(
                        "mail.imap.ssl.enable",
                        "false"
                    )

                    put(
                        "mail.imap.starttls.enable",
                        "true"
                    )
                }
            }

        val session =
            jakarta.mail.Session.getInstance(
                properties
            )

        val store =
            session.getStore(protocol)

        store.connect(
            config.imapHost,
            config.imapPort,
            config.email,
            password
        )

        store.close()

        Result.success(Unit)

    } catch (e: Exception) {

        Result.failure(e)
    }
}

    fun testSmtp(
        config: EmailConfig,
        password: String
    ): Result<Unit> {

        return try {

            val properties =
                Properties().apply {

                    put(
                        "mail.smtp.host",
                        config.smtpHost
                    )

                    put(
                        "mail.smtp.port",
                        config.smtpPort.toString()
                    )

                    put(
                        "mail.smtp.auth",
                        "true"
                    )

                    put(
                        "mail.smtp.connectiontimeout",
                        "10000"
                    )

                    put(
                        "mail.smtp.timeout",
                        "10000"
                    )

                    put(
                        "mail.smtp.writetimeout",
                        "10000"
                    )

                    if (config.smtpSsl) {

                        put(
                            "mail.smtp.ssl.enable",
                            "true"
                        )

                        put(
                            "mail.smtp.starttls.enable",
                            "false"
                        )

                    } else {

                        put(
                            "mail.smtp.ssl.enable",
                            "false"
                        )

                        put(
                            "mail.smtp.starttls.enable",
                            "true"
                        )
                    }
                }

            val session =
                Session.getInstance(
                    properties,
                    object : Authenticator() {

                        override fun getPasswordAuthentication():
                            PasswordAuthentication {

                            return PasswordAuthentication(
                                config.email,
                                password
                            )
                        }
                    }
                )

            val transport =
                session.getTransport("smtp")

            transport.connect()

            transport.close()

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

fun sendVerificationCode(
    config: EmailConfig,
    authCode: String,
    verificationCode: String
): Result<Unit> {

    return try {

        if (config.smtpHost.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "SMTP服务器地址不能为空"
                )
            )
        }

        if (config.smtpPort <= 0) {
            return Result.failure(
                IllegalArgumentException(
                    "SMTP端口无效"
                )
            )
        }

        if (config.email.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "邮箱地址不能为空"
                )
            )
        }

        val properties =
            java.util.Properties()

        /*
         * 关键：
         * 明确指定 SMTP Host 和 Port。
         * 防止 Jakarta Mail 回退到 localhost。
         */
        properties["mail.smtp.host"] =
            config.smtpHost

        properties["mail.smtp.port"] =
            config.smtpPort.toString()

        properties["mail.smtp.auth"] =
            "true"

        properties["mail.smtp.connectiontimeout"] =
            "10000"

        properties["mail.smtp.timeout"] =
            "10000"

        properties["mail.smtp.writetimeout"] =
            "10000"

        if (config.smtpSsl) {

            properties["mail.smtp.ssl.enable"] =
                "true"

            properties["mail.smtp.starttls.enable"] =
                "false"

            properties["mail.smtp.ssl.checkserveridentity"] =
                "true"

        } else {

            properties["mail.smtp.ssl.enable"] =
                "false"

            properties["mail.smtp.starttls.enable"] =
                "true"

            properties["mail.smtp.starttls.required"] =
                "true"
        }

        val session =
            jakarta.mail.Session.getInstance(
                properties
            )

        val message =
            jakarta.mail.internet.MimeMessage(
                session
            )

        message.setFrom(
            jakarta.mail.internet.InternetAddress(
                config.email
            )
        )

        message.setRecipients(
            jakarta.mail.Message.RecipientType.TO,
            arrayOf(
                jakarta.mail.internet.InternetAddress(
                    config.email
                )
            )
        )

        message.subject =
            "MoneyBook 安全验证码"

        message.setText(
            """
            MoneyBook

            您正在进行账户安全验证。

            本次验证码：

            $verificationCode

            验证码有效期为 5 分钟。

            如果不是您本人操作，请忽略此邮件。

            此邮件由 MoneyBook 自动发送，请勿直接回复。
            """.trimIndent(),
            "UTF-8"
        )

        val transport =
            session.getTransport("smtp")

        try {

            transport.connect(
                config.smtpHost,
                config.smtpPort,
                config.email,
                authCode
            )

            transport.sendMessage(
                message,
                message.allRecipients
            )

        } finally {

            try {
                transport.close()
            } catch (_: Exception) {
            }
        }

        Result.success(Unit)

    } catch (e: Exception) {

        Result.failure(e)
    }
}
}