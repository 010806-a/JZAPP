package com.example.myno.jz.data.imports

data class ImportBillItem(
    val rowIndex: Int,
    val timestamp: Long?,
    val type: ImportBillType,
    val amount: Double,
    val merchant: String = "",
    val note: String = "",
    val originalRow: String = "",
    val source: String = "",
    val transactionId: String = "",
    val merchantOrderId: String = "",
    val paymentMethod: String = "",
    val status: String = "",
    val transactionType: String = "",

    // 是否已经在已有账单/转账中发现重复
    var duplicate: Boolean = false,

    // 是否默认勾选
    var selected: Boolean = true,

    // 中性交易是否已经成功匹配到转出/转入账户
    var transferResolved: Boolean = false,

    // 转账转出账户
    var transferFromAccountId: String = "",

    // 转账转入账户
    var transferToAccountId: String = "",

    // 方便预览界面显示匹配结果
    var transferFromAccountName: String = "",

    var transferToAccountName: String = ""
)

enum class ImportBillType {
    EXPENSE,
    INCOME,
    NEUTRAL,
    UNKNOWN
}