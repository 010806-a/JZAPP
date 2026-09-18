package com.example.myno.jz.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.AccountType
import com.example.myno.jz.databinding.FragmentAddAccountBinding
import com.example.myno.jz.ui.main.MainViewModel
import java.util.UUID

class AddAccountFragment : Fragment() {

private var _binding: FragmentAddAccountBinding? = null
private val binding
    get() = _binding!!

private val viewModel: MainViewModel by activityViewModels()

private val accountTypes =
    listOf(
        AccountType.CASH,
        AccountType.WECHAT,
        AccountType.ALIPAY,
        AccountType.BANK_CARD,
        AccountType.OTHER
    )

override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
): View {

    _binding =
        FragmentAddAccountBinding.inflate(
            inflater,
            container,
            false
        )

    return binding.root
}

override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
) {

    super.onViewCreated(
        view,
        savedInstanceState
    )

    setupTypeSpinner()
    setupButtons()
}

private fun setupTypeSpinner() {

    val names =
        accountTypes.map {
            getAccountTypeName(it)
        }

    val adapter =
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            names
        )

    adapter.setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item
    )

    binding.spinnerAccountType.adapter =
        adapter
}

private fun setupButtons() {

    binding.btnBack.setOnClickListener {

        parentFragmentManager.popBackStack()
    }

    binding.btnSave.setOnClickListener {

        saveAccount()
    }
}

private fun saveAccount() {

    val name =
        binding.etAccountName.text
            ?.toString()
            ?.trim()
            ?: ""

    if (name.isBlank()) {

        binding.etAccountName.error =
            "请输入账户名称"

        return
    }

    val balanceText =
        binding.etInitialBalance.text
            ?.toString()
            ?.trim()
            ?: ""

    val initialBalance =
        if (balanceText.isBlank()) {
            0.0
        } else {
            balanceText.toDoubleOrNull()
                ?: run {

                    binding.etInitialBalance.error =
                        "请输入正确的金额"

                    return
                }
        }

    if (initialBalance < 0.0) {

        binding.etInitialBalance.error =
            "初始余额不能小于 0"

        return
    }

    val position =
        binding.spinnerAccountType.selectedItemPosition

    if (position !in accountTypes.indices) {

        Toast.makeText(
            requireContext(),
            "请选择账户类型",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    val accountType =
        accountTypes[position]

    val includeInTotalAssets =
        binding.switchIncludeAssets.isChecked

    val account =
        Account(
            id = UUID.randomUUID().toString(),
            name = name,
            type = accountType,
            balance = initialBalance,
            icon = getAccountIcon(accountType),
            includeInTotalAssets =
                includeInTotalAssets,
            enabled = true,
            createdAt =
                System.currentTimeMillis(),
            sortOrder =
                (viewModel.accounts.value?.maxOfOrNull {
                    it.sortOrder
                } ?: -1) + 1
        )

    val success =
        viewModel
            .getRepository()
            .addAccount(account)

    if (success) {

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "账户已添加",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()

    } else {

        Toast.makeText(
            requireContext(),
            "账户保存失败，请检查本地存储",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun getAccountTypeName(
    type: AccountType
): String {

    return when (type) {

        AccountType.CASH ->
            "现金"

        AccountType.WECHAT ->
            "微信"

        AccountType.ALIPAY ->
            "支付宝"

        AccountType.BANK_CARD ->
            "银行卡"

        AccountType.OTHER ->
            "其他账户"
    }
}

private fun getAccountIcon(
    type: AccountType
): String {

    return when (type) {

        AccountType.CASH ->
            "💵"

        AccountType.WECHAT ->
            "💚"

        AccountType.ALIPAY ->
            "💙"

        AccountType.BANK_CARD ->
            "💳"

        AccountType.OTHER ->
            "👜"
    }
}

override fun onDestroyView() {

    super.onDestroyView()

    _binding = null
}

}