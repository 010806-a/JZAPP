package com.example.myno.jz.data.local

import android.content.Context
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.AppSettings
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.Budget
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.model.Transfer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class JsonDataStore(context: Context) {

    private val appContext = context.applicationContext

    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .create()

    companion object {
        private const val RECORDS_FILE = "records.json"
        private const val ACCOUNTS_FILE = "accounts.json"
        private const val CATEGORIES_FILE = "categories.json"
        private const val BUDGETS_FILE = "budgets.json"
        private const val TRANSFERS_FILE = "transfers.json"
        private const val SETTINGS_FILE = "settings.json"
    }

    private fun getFile(fileName: String): File {
        return File(appContext.filesDir, fileName)
    }

    /**
     * 通用列表读取
     */
    private fun <T> readList(
        fileName: String,
        typeToken: TypeToken<List<T>>
    ): List<T> {

        return try {

            val file = getFile(fileName)

            if (!file.exists()) {
                return emptyList()
            }

            val json = file.readText(Charsets.UTF_8)

            if (json.isBlank()) {
                return emptyList()
            }

            gson.fromJson<List<T>>(
                json,
                typeToken.type
            ) ?: emptyList()

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }

    /**
     * 通用列表写入
     */
    private fun <T> writeList(
        fileName: String,
        data: List<T>
    ): Boolean {

        return try {

            val file = getFile(fileName)

            val json =
                gson.toJson(data)

            file.writeText(
                json,
                Charsets.UTF_8
            )

            file.exists() &&
                    file.length() > 0L

        } catch (e: Exception) {

            e.printStackTrace()

            false
        }
    }

    /**
     * 通用新增
     */
    private fun <T> addItem(
        currentItems: List<T>,
        item: T,
        save: (List<T>) -> Boolean
    ): Boolean {

        val items =
            currentItems.toMutableList()

        items.add(item)

        return save(items)
    }

    /**
     * 通用更新
     */
    private fun <T> updateItem(
        currentItems: List<T>,
        item: T,
        idOf: (T) -> String,
        save: (List<T>) -> Boolean
    ): Boolean {

        val items =
            currentItems.toMutableList()

        val index =
            items.indexOfFirst {
                idOf(it) == idOf(item)
            }

        if (index == -1) {
            return false
        }

        items[index] = item

        return save(items)
    }

    /**
     * 通用删除
     */
    private fun <T> deleteItem(
        currentItems: List<T>,
        id: String,
        idOf: (T) -> String,
        save: (List<T>) -> Boolean
    ): Boolean {

        val items =
            currentItems.toMutableList()

        val removed =
            items.removeAll {
                idOf(it) == id
            }

        if (!removed) {
            return false
        }

        return save(items)
    }

    // --------------------------------------------------
    // Bill
    // --------------------------------------------------

    fun getBills(): List<Bill> {

        return readList(
            RECORDS_FILE,
            object : TypeToken<List<Bill>>() {}
        )
    }

    fun saveBills(
        bills: List<Bill>
    ): Boolean {

        return writeList(
            RECORDS_FILE,
            bills
        )
    }

    fun addBill(
        bill: Bill
    ): Boolean {

        return addItem(
            currentItems = getBills(),
            item = bill,
            save = ::saveBills
        )
    }

    fun updateBill(
        bill: Bill
    ): Boolean {

        return updateItem(
            currentItems = getBills(),
            item = bill,
            idOf = { it.id },
            save = ::saveBills
        )
    }

    fun deleteBill(
        billId: String
    ): Boolean {

        return deleteItem(
            currentItems = getBills(),
            id = billId,
            idOf = { it.id },
            save = ::saveBills
        )
    }

    // --------------------------------------------------
    // Account
    // --------------------------------------------------

    fun getAccounts(): List<Account> {

        return readList(
            ACCOUNTS_FILE,
            object : TypeToken<List<Account>>() {}
        )
    }

    fun saveAccounts(
        accounts: List<Account>
    ): Boolean {

        return writeList(
            ACCOUNTS_FILE,
            accounts
        )
    }

    fun addAccount(
        account: Account
    ): Boolean {

        return addItem(
            currentItems = getAccounts(),
            item = account,
            save = ::saveAccounts
        )
    }

    fun updateAccount(
        account: Account
    ): Boolean {

        return updateItem(
            currentItems = getAccounts(),
            item = account,
            idOf = { it.id },
            save = ::saveAccounts
        )
    }

    fun deleteAccount(
        accountId: String
    ): Boolean {

        return deleteItem(
            currentItems = getAccounts(),
            id = accountId,
            idOf = { it.id },
            save = ::saveAccounts
        )
    }

    // --------------------------------------------------
    // Category
    // --------------------------------------------------

    fun getCategories(): List<Category> {

        return readList(
            CATEGORIES_FILE,
            object : TypeToken<List<Category>>() {}
        )
    }

    fun saveCategories(
        categories: List<Category>
    ): Boolean {

        return writeList(
            CATEGORIES_FILE,
            categories
        )
    }

    fun addCategory(
        category: Category
    ): Boolean {

        return addItem(
            currentItems = getCategories(),
            item = category,
            save = ::saveCategories
        )
    }

    fun updateCategory(
        category: Category
    ): Boolean {

        return updateItem(
            currentItems = getCategories(),
            item = category,
            idOf = { it.id },
            save = ::saveCategories
        )
    }

    fun deleteCategory(
        categoryId: String
    ): Boolean {

        return deleteItem(
            currentItems = getCategories(),
            id = categoryId,
            idOf = { it.id },
            save = ::saveCategories
        )
    }

    // --------------------------------------------------
    // Budget
    // --------------------------------------------------

    fun getBudgets(): List<Budget> {

        return readList(
            BUDGETS_FILE,
            object : TypeToken<List<Budget>>() {}
        )
    }

    fun saveBudgets(
        budgets: List<Budget>
    ): Boolean {

        return writeList(
            BUDGETS_FILE,
            budgets
        )
    }

    fun addBudget(
        budget: Budget
    ): Boolean {

        return addItem(
            currentItems = getBudgets(),
            item = budget,
            save = ::saveBudgets
        )
    }

    fun updateBudget(
        budget: Budget
    ): Boolean {

        return updateItem(
            currentItems = getBudgets(),
            item = budget,
            idOf = { it.id },
            save = ::saveBudgets
        )
    }

    fun deleteBudget(
        budgetId: String
    ): Boolean {

        return deleteItem(
            currentItems = getBudgets(),
            id = budgetId,
            idOf = { it.id },
            save = ::saveBudgets
        )
    }

    // --------------------------------------------------
    // Transfer
    // --------------------------------------------------

    fun getTransfers(): List<Transfer> {

        return readList(
            TRANSFERS_FILE,
            object : TypeToken<List<Transfer>>() {}
        )
    }

    fun saveTransfers(
        transfers: List<Transfer>
    ): Boolean {

        return writeList(
            TRANSFERS_FILE,
            transfers
        )
    }

    fun addTransfer(
        transfer: Transfer
    ): Boolean {

        return addItem(
            currentItems = getTransfers(),
            item = transfer,
            save = ::saveTransfers
        )
    }

    fun updateTransfer(
        transfer: Transfer
    ): Boolean {

        return updateItem(
            currentItems = getTransfers(),
            item = transfer,
            idOf = { it.id },
            save = ::saveTransfers
        )
    }

    fun deleteTransfer(
        transferId: String
    ): Boolean {

        return deleteItem(
            currentItems = getTransfers(),
            id = transferId,
            idOf = { it.id },
            save = ::saveTransfers
        )
    }

    // --------------------------------------------------
    // Settings
    // --------------------------------------------------

    fun getSettings(): AppSettings {

        return try {

            val file =
                getFile(SETTINGS_FILE)

            if (!file.exists()) {
                return AppSettings()
            }

            val json =
                file.readText(Charsets.UTF_8)

            if (json.isBlank()) {
                return AppSettings()
            }

            gson.fromJson(
                json,
                AppSettings::class.java
            ) ?: AppSettings()

        } catch (e: Exception) {

            e.printStackTrace()

            AppSettings()
        }
    }

    fun saveSettings(
        settings: AppSettings
    ): Boolean {

        return try {

            val file =
                getFile(SETTINGS_FILE)

            file.writeText(
                gson.toJson(settings),
                Charsets.UTF_8
            )

            file.exists() &&
                    file.length() > 0L

        } catch (e: Exception) {

            e.printStackTrace()

            false
        }
    }

    // --------------------------------------------------
    // Clear
    // --------------------------------------------------

    fun clearAllData(): Boolean {

        return try {

            getFile(RECORDS_FILE).delete()
            getFile(ACCOUNTS_FILE).delete()
            getFile(CATEGORIES_FILE).delete()
            getFile(BUDGETS_FILE).delete()
            getFile(TRANSFERS_FILE).delete()
            getFile(SETTINGS_FILE).delete()

            true

        } catch (e: Exception) {

            e.printStackTrace()

            false
        }
    }
}