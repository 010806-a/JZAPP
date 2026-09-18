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

            // 写完以后再次确认文件存在且不为空
            file.exists() &&
                    file.length() > 0L

        } catch (e: Exception) {

            e.printStackTrace()

            false
        }
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

        val bills =
            getBills().toMutableList()

        bills.add(bill)

        return saveBills(bills)
    }

    fun updateBill(
        bill: Bill
    ): Boolean {

        val bills =
            getBills().toMutableList()

        val index =
            bills.indexOfFirst {
                it.id == bill.id
            }

        if (index == -1) {
            return false
        }

        bills[index] = bill

        return saveBills(bills)
    }

    fun deleteBill(
        billId: String
    ): Boolean {

        val bills =
            getBills().toMutableList()

        val removed =
            bills.removeAll {
                it.id == billId
            }

        if (!removed) {
            return false
        }

        return saveBills(bills)
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

        val accounts =
            getAccounts().toMutableList()

        accounts.add(account)

        return saveAccounts(accounts)
    }

    fun updateAccount(
        account: Account
    ): Boolean {

        val accounts =
            getAccounts().toMutableList()

        val index =
            accounts.indexOfFirst {
                it.id == account.id
            }

        if (index == -1) {
            return false
        }

        accounts[index] = account

        return saveAccounts(accounts)
    }

    fun deleteAccount(
        accountId: String
    ): Boolean {

        val accounts =
            getAccounts().toMutableList()

        val removed =
            accounts.removeAll {
                it.id == accountId
            }

        if (!removed) {
            return false
        }

        return saveAccounts(accounts)
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

        val categories =
            getCategories().toMutableList()

        categories.add(category)

        return saveCategories(categories)
    }

    fun updateCategory(
        category: Category
    ): Boolean {

        val categories =
            getCategories().toMutableList()

        val index =
            categories.indexOfFirst {
                it.id == category.id
            }

        if (index == -1) {
            return false
        }

        categories[index] = category

        return saveCategories(categories)
    }

    fun deleteCategory(
        categoryId: String
    ): Boolean {

        val categories =
            getCategories().toMutableList()

        val removed =
            categories.removeAll {
                it.id == categoryId
            }

        if (!removed) {
            return false
        }

        return saveCategories(categories)
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

    val budgets =
        getBudgets().toMutableList()

    budgets.add(budget)

    return saveBudgets(budgets)
}

fun updateBudget(
    budget: Budget
): Boolean {

    val budgets =
        getBudgets().toMutableList()

    val index =
        budgets.indexOfFirst {
            it.id == budget.id
        }

    if (index == -1) {
        return false
    }

    budgets[index] = budget

    return saveBudgets(budgets)
}

fun deleteBudget(
    budgetId: String
): Boolean {

    val budgets =
        getBudgets().toMutableList()

    val removed =
        budgets.removeAll {
            it.id == budgetId
        }

    if (!removed) {
        return false
    }

    return saveBudgets(budgets)
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

        val transfers =
            getTransfers().toMutableList()

        transfers.add(transfer)

        return saveTransfers(transfers)
    }
    
    fun updateTransfer(
    transfer: Transfer
): Boolean {

    val transfers =
        getTransfers().toMutableList()

    val index =
        transfers.indexOfFirst {
            it.id == transfer.id
        }

    if (index == -1) {
        return false
    }

    transfers[index] = transfer

    return saveTransfers(transfers)
}
    
    fun deleteTransfer(
    transferId: String
): Boolean {

    val transfers =
        getTransfers().toMutableList()

    val removed =
        transfers.removeAll {
            it.id == transferId
        }

    if (!removed) {
        return false
    }

    return saveTransfers(transfers)
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