package com.example.myno.jz.data.model

/**
 * MoneyBook 本地备份数据
 *
 * 后续增加预算、账户、标签等数据时，
 * 可以继续在这里扩展字段。
 */
data class BackupData(
    val version: Int = 1,
    val backupTime: String = "",
    val records: Any? = null,
    val categories: Any? = null,
    val settings: Any? = null
)