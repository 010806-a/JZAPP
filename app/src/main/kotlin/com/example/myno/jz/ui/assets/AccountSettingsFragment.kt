package com.example.myno.jz.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.AccountType
import com.example.myno.jz.databinding.FragmentAccountSettingsBinding
import com.example.myno.jz.ui.main.MainViewModel
import java.util.Locale

class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var currentAccount: Account? = null

    private val accountTypes =
        listOf(
            AccountType.BANK_CARD,
            AccountType.WECHAT,
            AccountType.ALIPAY,
            AccountType.CASH,
            AccountType.OTHER
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAccountSettingsBinding.inflate(
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

        setupSpinner()
        setupButtons()
        observeAccount()
    }

    private fun setupSpinner() {

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

        binding.btnDelete.setOnClickListener {

            confirmDelete()
        }
    }

    private fun observeAccount() {

        val accountId =
            arguments?.getString(ARG_ACCOUNT_ID)

        if (accountId.isNullOrBlank()) {

            Toast.makeText(
                requireContext(),
                "账户信息不存在",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()

            return
        }

        viewModel.accounts.observe(
            viewLifecycleOwner
        ) { accounts ->

            val account =
                accounts.firstOrNull {
                    it.id == accountId
                }

            if (account == null) {

                Toast.makeText(
                    requireContext(),
                    "账户不存在",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()

                return@observe
            }

            currentAccount =
                account

            displayAccount(account)
        }
    }

    private fun displayAccount(
        account: Account
    ) {

        binding.etAccountName.setText(
            account.name
        )

        binding.etInitialBalance.setText(
            formatMoney(account.balance)
        )

        binding.switchIncludeAssets.isChecked =
            account.includeInTotalAssets

        val index =
            accountTypes.indexOf(account.type)

        if (index >= 0) {

            binding.spinnerAccountType.setSelection(
                index
            )
        }
    }

    private fun saveAccount() {

        val account =
            currentAccount
                ?: return

        val name =
            binding.etAccountName
                .text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (name.isBlank()) {

            Toast.makeText(
                requireContext(),
                "账户名称不能为空",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        var balanceText =
            binding.etInitialBalance
                .text
                ?.toString()
                ?.trim()
                .orEmpty()

        balanceText =
            balanceText
                .replace("¥", "")
                .replace(",", "")
                .trim()

        val balance =
            balanceText.toDoubleOrNull()

        if (balance == null) {

            Toast.makeText(
                requireContext(),
                "请输入正确的初始余额",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val position =
            binding.spinnerAccountType.selectedItemPosition

        val type =
            accountTypes.getOrNull(position)
                ?: account.type

        val includeInAssets =
            binding.switchIncludeAssets.isChecked

        val updatedAccount =
            account.copy(
                name = name,
                type = type,
                balance = balance,
                includeInTotalAssets =
                    includeInAssets
            )

        val success =
            viewModel.updateAccount(
                updatedAccount
            )

        if (!success) {

            Toast.makeText(
                requireContext(),
                "保存失败",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            requireContext(),
            "账户修改成功",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    private fun confirmDelete() {

        val account =
            currentAccount
                ?: return

        if (
            account.name == "经营账户" ||
            account.name == "日利加"
        ) {

            Toast.makeText(
                requireContext(),
                "系统账户不能删除",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("删除账户")
            .setMessage(
                "确定要删除「${account.name}」吗？\n\n" +
                    "如果该账户已经存在账单或转账记录，系统将阻止删除，避免历史数据失去账户归属。"
            )
            .setNegativeButton(
                "取消",
                null
            )
            .setPositiveButton(
                "删除"
            ) { _, _ ->

                deleteAccount(account)
            }
            .show()
    }

    private fun deleteAccount(
        account: Account
    ) {

        val bills =
            viewModel.bills.value.orEmpty()

        val hasBills =
            bills.any {
                it.accountId == account.id
            }

        val transfers =
            viewModel
                .getRepository()
                .getTransfers()

        val hasTransfers =
            transfers.any {
                it.fromAccountId == account.id ||
                    it.toAccountId == account.id
            }

        if (hasBills || hasTransfers) {

            AlertDialog.Builder(
                requireContext()
            )
                .setTitle("无法删除账户")
                .setMessage(
                    "这个账户已经存在历史账单或转账记录。\n\n" +
                        "建议保留账户，并关闭“计入总资产”。"
                )
                .setPositiveButton(
                    "知道了",
                    null
                )
                .show()

            return
        }

        val success =
            viewModel.deleteAccount(
                account.id
            )

        if (success) {

            Toast.makeText(
                requireContext(),
                "账户已删除",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()

        } else {

            Toast.makeText(
                requireContext(),
                "删除失败",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getAccountTypeName(
        type: AccountType
    ): String {

        return when (type) {

            AccountType.BANK_CARD ->
                "银行卡"

            AccountType.WECHAT ->
                "微信"

            AccountType.ALIPAY ->
                "支付宝"

            AccountType.CASH ->
                "现金"

            AccountType.OTHER ->
                "其他账户"
        }
    }

    private fun formatMoney(
        value: Double
    ): String {

        return String.format(
            Locale.getDefault(),
            "%.2f",
            value
        )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val ARG_ACCOUNT_ID =
            "accountId"

        fun newInstance(
            accountId: String
        ): AccountSettingsFragment {

            return AccountSettingsFragment().apply {

                arguments =
                    Bundle().apply {

                        putString(
                            ARG_ACCOUNT_ID,
                            accountId
                        )
                    }
            }
        }
    }
}