package com.example.myno.jz.data.model

/**
 * 账户类型
 */
enum class AccountType {

    BANK_CARD,

    WECHAT,

    ALIPAY,

    CASH,

    OTHER
}

/**
 * 资产账户
 *
 * balance 表示：
 * 账户的期初余额 / 初始余额。
 *
 * 它不是“当前余额”。
 *
 * 当前余额需要根据：
 *
 * 期初余额
 * + 收入
 * - 支出
 * + 转入
 * - 转出
 *
 * 动态计算。
 */
data class Account(

    /**
     * 唯一 ID
     */
    val id: String,

    /**
     * 账户名称
     */
    val name: String,

    /**
     * 账户类型
     */
    val type: AccountType,

    /**
     * 期初余额 / 初始余额
     *
     * 注意：
     * 这里不是当前余额。
     *
     * 当前余额由账户流水动态计算。
     */
    val balance: Double = 0.0,

    /**
     * 图标名称
     */
    val icon: String = "wallet",

    /**
     * 是否参与总资产统计
     */
    val includeInTotalAssets: Boolean = true,

    /**
     * 是否启用
     */
    val enabled: Boolean = true,

    /**
     * 创建时间
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * 排序
     */
    val sortOrder: Int = 0
)