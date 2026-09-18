package com.example.myno.jz.data.model

data class EmailVerification(
    val email: String,
    val codeHash: String,
    val createdAt: Long,
    val expiresAt: Long,
    val attempts: Int = 0
)