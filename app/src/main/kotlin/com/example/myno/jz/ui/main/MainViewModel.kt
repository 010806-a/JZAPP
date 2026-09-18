package com.example.myno.jz.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.repository.FinanceRepository

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        FinanceRepository(application)

    private val _bills =
        MutableLiveData<List<Bill>>()

    val bills: LiveData<List<Bill>>
        get() = _bills

    private val _accounts =
        MutableLiveData<List<Account>>()

    val accounts: LiveData<List<Account>>
        get() = _accounts

    private val _categories =
        MutableLiveData<List<Category>>()

    val categories: LiveData<List<Category>>
        get() = _categories

    init {
        loadData()
    }

    fun loadData() {

        _bills.value =
            repository.getBills()

        _accounts.value =
            repository.getAccounts()

        _categories.value =
            repository.getCategories()
    }

    fun refresh() {
        loadData()
    }

    fun getRepository(): FinanceRepository {
        return repository
    }

    fun updateAccount(
        account: Account
    ): Boolean {

        val success =
            repository.updateAccount(
                account
            )

        if (success) {
            loadData()
        }

        return success
    }

    fun addAccount(
        account: Account
    ): Boolean {

        val success =
            repository.addAccount(
                account
            )

        if (success) {
            loadData()
        }

        return success
    }

    fun deleteAccount(
        accountId: String
    ): Boolean {

        val success =
            repository.deleteAccount(
                accountId
            )

        if (success) {
            loadData()
        }

        return success
    }
}