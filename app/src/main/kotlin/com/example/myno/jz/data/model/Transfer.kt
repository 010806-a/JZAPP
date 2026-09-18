package com.example.myno.jz.data.model

data class Transfer(
val id: String,
val fromAccountId: String,
val toAccountId: String,
val amount: Double,
val note: String = "",
val timestamp: Long,
val createdAt: Long = System.currentTimeMillis(),
val source: String = "",
val sourceTransactionId: String = ""
)