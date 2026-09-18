package com.example.myno.jz.data.model

enum class BillType {
    EXPENSE,
    INCOME
}

data class Bill(
    val id: String,
    val type: BillType,
    val amount: Double,
    val categoryId: String,
    val accountId: String,
    val note: String = "",
    val timestamp: Long,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // 数据来源，例如 WECHAT / ALIPAY / MANUAL
    val source: String = "",

    // 来源平台的原始交易单号
    val sourceTransactionId: String = ""
)