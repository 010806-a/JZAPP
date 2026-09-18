package com.example.myno.jz.data.model

data class PrivacyLockConfig(
    val enabled: Boolean = false,
    val type: PrivacyLockType = PrivacyLockType.NONE,
    val credentialHash: String = "",
    val salt: String = "",
    val updatedAt: Long = 0L
)

enum class PrivacyLockType {
    NONE,
    PASSWORD,
    PATTERN
}