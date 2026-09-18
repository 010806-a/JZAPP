package com.example.myno.jz.data.repository

import android.content.Context
import com.example.myno.jz.data.local.JsonDataStore
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.AppSettings
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Budget
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.model.Transfer
import java.util.UUID

class FinanceRepository(
    context: Context
) {

    private val dataStore =
        JsonDataStore(context)

    // --------------------------------------------------
    // Bill
    // --------------------------------------------------

    fun getBills(): List<Bill> {

        return dataStore
            .getBills()
            .sortedByDescending {
                it.timestamp
            }
    }

    /**
     * 创建账单
     *
     * source：
     * 数据来源，例如：
     * WECHAT
     * ALIPAY
     * MANUAL
     *
     * sourceTransactionId：
     * 微信/支付宝等平台的原始交易单号
     */
    fun createBill(
        type: BillType,
        amount: Double,
        categoryId: String,
        accountId: String,
        note: String,
        timestamp: Long,
        source: String = "",
        sourceTransactionId: String = ""
    ): Boolean {

        val now =
            System.currentTimeMillis()

        val bill =
            Bill(
                id = UUID.randomUUID().toString(),

                type = type,

                amount = amount,

                categoryId = categoryId,

                accountId = accountId,

                note = note,

                timestamp = timestamp,

                createdAt = now,

                updatedAt = now,

                source = source,

                sourceTransactionId =
                    sourceTransactionId
            )

        return dataStore.addBill(
            bill
        )
    }

    fun updateBill(
        bill: Bill
    ): Boolean {

        return dataStore.updateBill(
            bill
        )
    }

    fun deleteBill(
        billId: String
    ): Boolean {

        return dataStore.deleteBill(
            billId
        )
    }

    // --------------------------------------------------
    // Account
    // --------------------------------------------------

    fun getAccounts(): List<Account> {

        return dataStore
            .getAccounts()
            .sortedBy {
                it.sortOrder
            }
    }

    fun addAccount(
        account: Account
    ): Boolean {

        return dataStore.addAccount(
            account
        )
    }

    fun updateAccount(
        account: Account
    ): Boolean {

        return dataStore.updateAccount(
            account
        )
    }

    fun deleteAccount(
        accountId: String
    ): Boolean {

        return dataStore.deleteAccount(
            accountId
        )
    }

    // --------------------------------------------------
    // Category
    // --------------------------------------------------

    fun getCategories(): List<Category> {

        return dataStore
            .getCategories()
            .sortedBy {
                it.sortOrder
            }
    }

    fun addCategory(
        category: Category
    ): Boolean {

        return dataStore.addCategory(
            category
        )
    }

    fun updateCategory(
        category: Category
    ): Boolean {

        return dataStore.updateCategory(
            category
        )
    }

    fun deleteCategory(
        categoryId: String
    ): Boolean {

        return dataStore.deleteCategory(
            categoryId
        )
    }

 // Budget

fun getBudgets(): List<Budget> =
    dataStore.getBudgets()

fun addBudget(
    budget: Budget
): Boolean =
    dataStore.addBudget(budget)

fun updateBudget(
    budget: Budget
): Boolean =
    dataStore.updateBudget(budget)

fun deleteBudget(
    budgetId: String
): Boolean =
    dataStore.deleteBudget(budgetId)

    // --------------------------------------------------
    // Transfer
    // --------------------------------------------------

    fun getTransfers(): List<Transfer> {

        return dataStore
            .getTransfers()
            .sortedByDescending {
                it.timestamp
            }
    }

    fun addTransfer(
        transfer: Transfer
    ): Boolean {

        return dataStore.addTransfer(
            transfer
        )
    }
    fun updateTransfer(transfer: Transfer): Boolean =
    dataStore.updateTransfer(transfer)
    fun deleteTransfer(transferId: String): Boolean =
    dataStore.deleteTransfer(transferId)

    // --------------------------------------------------
    // Settings
    // --------------------------------------------------

    fun getSettings(): AppSettings {

        return dataStore.getSettings()
    }

    fun saveSettings(
        settings: AppSettings
    ): Boolean {

        return dataStore.saveSettings(
            settings
        )
    }

    // --------------------------------------------------
    // Clear
    // --------------------------------------------------

    fun clearAllData(): Boolean {

        return dataStore.clearAllData()
    }
}