package com.example.myno.jz.data.model

data class EmailConfig(
    val email: String = "",
    val provider: String = "CUSTOM",
    val imapHost: String = "",
    val imapPort: Int = 993,
    val imapSsl: Boolean = true,
    val smtpHost: String = "",
    val smtpPort: Int = 465,
    val smtpSsl: Boolean = true,
    val configured: Boolean = false,
    val verified: Boolean = false,
    val updatedAt: Long = 0L
)