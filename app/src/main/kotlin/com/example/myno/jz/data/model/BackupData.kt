package com.example.myno.jz.data.model

/**
 * MoneyBook 本地完整备份数据结构。
 *
 * 使用 Any 是为了保持与 Gson JSON 结构兼容，同时允许旧版本备份继续读取。
 */
data class BackupData(
    val version: Int = 2,
    val backupTime: String = "",
    val records: Any? = null,
    val accounts: Any? = null,
    val categories: Any? = null,
    val budgets: Any? = null,
    val transfers: Any? = null,
    val settings: Any? = null
)
