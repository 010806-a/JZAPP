package com.example.myno.jz.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.databinding.FragmentAccountDetailBinding
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.ui.main.MainViewModel
import java.util.Calendar
import java.util.Locale

class AccountDetailFragment : Fragment() {

    private var _binding: FragmentAccountDetailBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var accountId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAccountDetailBinding.inflate(
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

        accountId =
            arguments?.getString(
                ARG_ACCOUNT_ID
            )

        if (accountId.isNullOrBlank()) {

            Toast.makeText(
                requireContext(),
                "账户信息不存在",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()

            return
        }

        setupButtons()
        observeData()
    }

    private fun setupButtons() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        /*
         * 账户设置。
         */
        binding.cardAccountSettings.setOnClickListener {

            val id =
                accountId
                    ?: return@setOnClickListener

            parentFragmentManager
                .beginTransaction()
                .replace(
                    com.example.myno.jz.R.id.fragmentContainer,
                    AccountSettingsFragment.newInstance(id)
                )
                .addToBackStack(null)
                .commit()
        }

        /*
         * 账户流水。
         *
         * 下一步我们会创建 AccountFlowFragment。
         */
  binding.cardAccountFlow.setOnClickListener {
    val id = accountId ?: return@setOnClickListener

    parentFragmentManager.beginTransaction()
        .replace(
            com.example.myno.jz.R.id.fragmentContainer,
            AccountFlowFragment.newInstance(id)
        )
        .addToBackStack(null)
        .commit()
}
    }

    private fun observeData() {

        viewModel.accounts.observe(
            viewLifecycleOwner
        ) {

            updateAccountOverview()
        }

        viewModel.bills.observe(
            viewLifecycleOwner
        ) {

            updateAccountOverview()
        }
    }

    private fun updateAccountOverview() {

        val id =
            accountId
                ?: return

        val account =
            viewModel.accounts.value
                ?.firstOrNull {
                    it.id == id
                }
                ?: return

        binding.tvAccountName.text =
            account.name

        val bills =
            viewModel.bills.value.orEmpty()

        val transfers =
            viewModel
                .getRepository()
                .getTransfers()

        var balance =
            account.balance

        var monthIncome = 0.0
        var monthExpense = 0.0
        var monthTransferIn = 0.0
        var monthTransferOut = 0.0

        val now =
            Calendar.getInstance()

        val currentYear =
            now.get(Calendar.YEAR)

        val currentMonth =
            now.get(Calendar.MONTH)

        /*
         * 账单。
         */
        bills.forEach { bill ->

            if (bill.accountId != id) {
                return@forEach
            }

            when (bill.type) {

                BillType.INCOME -> {

                    balance += bill.amount
                }

                BillType.EXPENSE -> {

                    balance -= bill.amount
                }
            }

            val calendar =
                Calendar.getInstance().apply {
                    timeInMillis =
                        bill.timestamp
                }

            val sameMonth =
                calendar.get(Calendar.YEAR) ==
                    currentYear &&
                    calendar.get(Calendar.MONTH) ==
                    currentMonth

            if (sameMonth) {

                when (bill.type) {

                    BillType.INCOME -> {

                        monthIncome +=
                            bill.amount
                    }

                    BillType.EXPENSE -> {

                        monthExpense +=
                            bill.amount
                    }
                }
            }
        }

        /*
         * 转账。
         */
        transfers.forEach { transfer ->

            if (
                transfer.fromAccountId == id
            ) {

                balance -=
                    transfer.amount
            }

            if (
                transfer.toAccountId == id
            ) {

                balance +=
                    transfer.amount
            }

            val calendar =
                Calendar.getInstance().apply {
                    timeInMillis =
                        transfer.timestamp
                }

            val sameMonth =
                calendar.get(Calendar.YEAR) ==
                    currentYear &&
                    calendar.get(Calendar.MONTH) ==
                    currentMonth

            if (sameMonth) {

                if (
                    transfer.toAccountId == id
                ) {

                    monthTransferIn +=
                        transfer.amount
                }

                if (
                    transfer.fromAccountId == id
                ) {

                    monthTransferOut +=
                        transfer.amount
                }
            }
        }

        /*
         * 当前余额。
         */
        binding.tvCurrentBalance.text =
            "¥${formatMoney(balance)}"

        /*
         * 本月收入。
         */
        binding.tvMonthIncome.text =
            "+¥${formatMoney(monthIncome)}"

        /*
         * 本月支出。
         */
        binding.tvMonthExpense.text =
            "-¥${formatMoney(monthExpense)}"

        /*
         * 本月净转账。
         */
        val netTransfer =
            monthTransferIn -
                monthTransferOut

        binding.tvMonthTransfer.text =
            if (netTransfer >= 0) {

                "+¥${formatMoney(netTransfer)}"

            } else {

                "-¥${formatMoney(-netTransfer)}"
            }

        /*
         * 流水入口提示。
         */
        val flowCount =
            bills.count {
                it.accountId == id
            } +
                transfers.count {
                    it.fromAccountId == id ||
                        it.toAccountId == id
                }

        binding.tvFlowHint.text =
            if (flowCount == 0) {
                "暂无账户流水"
            } else {
                "$flowCount 笔流水 · 查看全部收入、支出和转账"
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
        ): AccountDetailFragment {

            return AccountDetailFragment().apply {

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