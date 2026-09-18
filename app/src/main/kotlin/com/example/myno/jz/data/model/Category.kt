package com.example.myno.jz.data.model

/**
 * 分类类型
 */
enum class CategoryType {

    EXPENSE,

    INCOME
}

/**
 * 账单分类
 */
data class Category(

    /**
     * 分类 ID
     */
    val id: String,

    /**
     * 分类名称
     */
    val name: String,

    /**
     * 支出 / 收入
     */
    val type: CategoryType,

    /**
     * 图标名称
     */
    val icon: String,

    /**
     * 是否为系统默认分类
     */
    val isSystem: Boolean = false,

    /**
     * 是否显示在快捷记账页面
     */
    val visible: Boolean = true,

    /**
     * 排序
     */
    val sortOrder: Int = 0,

    /**
     * 创建时间
     */
    val createdAt: Long = System.currentTimeMillis()
)