package com.example.myno.jz.data.model

/**
 * 预算类型
 */
enum class BudgetType {

    /**
     * 总预算
     */
    TOTAL,

    /**
     * 分类预算
     */
    CATEGORY
}

/**
 * 月度预算
 */
data class Budget(

    /**
     * 唯一 ID
     */
    val id: String,

    /**
     * 年
     */
    val year: Int,

    /**
     * 月
     */
    val month: Int,

    /**
     * 预算类型
     */
    val type: BudgetType,

    /**
     * 分类 ID
     *
     * TOTAL 类型可以为空
     */
    val categoryId: String? = null,

    /**
     * 预算金额
     */
    val amount: Double,

    /**
     * 是否启用
     */
    val enabled: Boolean = true,

    /**
     * 70% / 90% / 100% 等预警比例
     */
    val warningPercent: Int = 80
)