package com.example.myno.jz.data.local

import android.content.Context
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.AccountType
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.model.CategoryType

/**
 * MoneyBook 默认数据初始化器
 */
object DefaultDataInitializer {

    fun initialize(
        context: Context
    ) {

        val dataStore =
            JsonDataStore(
                context.applicationContext
            )

        initializeAccounts(
            dataStore
        )

        initializeCategories(
            dataStore
        )
    }

    private fun initializeAccounts(
        dataStore: JsonDataStore
    ) {

        if (dataStore.getAccounts().isNotEmpty()) {
            return
        }

        val accounts = listOf(

            Account(
                id = "cash",
                name = "现金",
                type = AccountType.CASH,
                icon = "cash",
                balance = 0.0,
                sortOrder = 0
            ),

            Account(
                id = "wechat",
                name = "微信",
                type = AccountType.WECHAT,
                icon = "wechat",
                balance = 0.0,
                sortOrder = 1
            ),

            Account(
                id = "alipay",
                name = "支付宝",
                type = AccountType.ALIPAY,
                icon = "alipay",
                balance = 0.0,
                sortOrder = 2
            ),

            Account(
                id = "bank",
                name = "银行卡",
                type = AccountType.BANK_CARD,
                icon = "bank",
                balance = 0.0,
                sortOrder = 3
            )
        )

        dataStore.saveAccounts(
            accounts
        )
    }

    private fun initializeCategories(
        dataStore: JsonDataStore
    ) {

        if (dataStore.getCategories().isNotEmpty()) {
            return
        }

        val categories = listOf(

            // ============================
            // 支出
            // ============================

            Category(
                id = "food",
                name = "餐饮",
                type = CategoryType.EXPENSE,
                icon = "food",
                isSystem = true,
                sortOrder = 0
            ),

            Category(
                id = "shopping",
                name = "购物",
                type = CategoryType.EXPENSE,
                icon = "shopping",
                isSystem = true,
                sortOrder = 1
            ),

            Category(
                id = "transport",
                name = "交通",
                type = CategoryType.EXPENSE,
                icon = "transport",
                isSystem = true,
                sortOrder = 2
            ),

            Category(
                id = "housing",
                name = "住房",
                type = CategoryType.EXPENSE,
                icon = "housing",
                isSystem = true,
                sortOrder = 3
            ),

            Category(
                id = "entertainment",
                name = "娱乐",
                type = CategoryType.EXPENSE,
                icon = "entertainment",
                isSystem = true,
                sortOrder = 4
            ),

            Category(
                id = "medical",
                name = "医疗",
                type = CategoryType.EXPENSE,
                icon = "medical",
                isSystem = true,
                sortOrder = 5
            ),

            Category(
                id = "education",
                name = "教育",
                type = CategoryType.EXPENSE,
                icon = "education",
                isSystem = true,
                sortOrder = 6
            ),

            Category(
                id = "communication",
                name = "通讯",
                type = CategoryType.EXPENSE,
                icon = "communication",
                isSystem = true,
                sortOrder = 7
            ),

            Category(
                id = "pet",
                name = "宠物",
                type = CategoryType.EXPENSE,
                icon = "pet",
                isSystem = true,
                sortOrder = 8
            ),

            Category(
                id = "love",
                name = "恋爱",
                type = CategoryType.EXPENSE,
                icon = "love",
                isSystem = true,
                sortOrder = 9
            ),

            Category(
                id = "gift",
                name = "人情往来",
                type = CategoryType.EXPENSE,
                icon = "gift",
                isSystem = true,
                sortOrder = 10
            ),

            Category(
                id = "other_expense",
                name = "其他",
                type = CategoryType.EXPENSE,
                icon = "other",
                isSystem = true,
                sortOrder = 11
            ),

            // ============================
            // 收入
            // ============================

            Category(
                id = "salary",
                name = "工资",
                type = CategoryType.INCOME,
                icon = "salary",
                isSystem = true,
                sortOrder = 100
            ),

            Category(
                id = "bonus",
                name = "奖金",
                type = CategoryType.INCOME,
                icon = "bonus",
                isSystem = true,
                sortOrder = 101
            ),

            Category(
                id = "part_time",
                name = "兼职",
                type = CategoryType.INCOME,
                icon = "part_time",
                isSystem = true,
                sortOrder = 102
            ),

            Category(
                id = "other_income",
                name = "其他收入",
                type = CategoryType.INCOME,
                icon = "other",
                isSystem = true,
                sortOrder = 103
            )
        )

        dataStore.saveCategories(
            categories
        )
    }
}