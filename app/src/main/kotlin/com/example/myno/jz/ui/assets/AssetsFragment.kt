package com.example.myno.jz.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.AccountType
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Transfer
import com.example.myno.jz.databinding.FragmentAssetsBinding
import com.example.myno.jz.ui.main.MainViewModel
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class AssetsFragment : Fragment() {

    private var _binding: FragmentAssetsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssetsBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        observeData()
    }

    private fun setupButtons() {

        binding.fabAddAccount.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    AddAccountFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        binding.btnTransfer.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    TransferFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeData() {

        viewModel.accounts.observe(
            viewLifecycleOwner
        ) {
            renderAccounts()
        }

        viewModel.bills.observe(
            viewLifecycleOwner
        ) {
            renderAccounts()
        }
    }

    private fun renderAccounts() {

        val accounts =
            viewModel.accounts.value
                ?.filter { it.enabled }
                ?.sortedBy { it.sortOrder }
                ?: emptyList()

        val bills =
            viewModel.bills.value ?: emptyList()

        val transfers =
            viewModel.getRepository().getTransfers()

        renderTotalAssets(
            accounts,
            bills,
            transfers
        )

        renderAccountList(
            accounts,
            bills,
            transfers
        )
    }

    private fun renderTotalAssets(
        accounts: List<Account>,
        bills: List<Bill>,
        transfers: List<Transfer>
    ) {

        val total = accounts
            .filter { it.includeInTotalAssets }
            .sumOf {
                calculateAccountBalance(
                    it,
                    bills,
                    transfers
                )
            }

        binding.tvTotalAssets.text =
            String.format(
                Locale.getDefault(),
                "¥%.2f",
                total
            )
    }

    private fun renderAccountList(
        accounts: List<Account>,
        bills: List<Bill>,
        transfers: List<Transfer>
    ) {

        binding.accountContainer.removeAllViews()

        if (accounts.isEmpty()) {

            val emptyText = TextView(requireContext())

            emptyText.text = "暂无账户\n点击右下角“添加账户”创建账户"
            emptyText.textSize = 15f
            emptyText.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_secondary
                )
            )

            emptyText.setPadding(
                20,
                40,
                20,
                40
            )

            binding.accountContainer.addView(
                emptyText
            )

            return
        }

        accounts.forEach { account ->

            val card =
                createAccountCard(
                    account,
                    bills,
                    transfers
                )

            binding.accountContainer.addView(card)
        }
    }

    private fun createAccountCard(
        account: Account,
        bills: List<Bill>,
        transfers: List<Transfer>
    ): View {

        val card =
            MaterialCardView(requireContext())

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            0,
            0,
            0,
            dp(12)
        )

        card.layoutParams = params

        card.radius = dp(18).toFloat()
        card.cardElevation = 0f

        val container =
            LinearLayout(requireContext())

        container.orientation =
            LinearLayout.HORIZONTAL

        container.setPadding(
            dp(20),
            dp(18),
            dp(20),
            dp(18)
        )

        val iconView =
            ImageView(requireContext())

        iconView.setImageResource(
            getAccountIconRes(account)
        )

        // 200x200 的方形图标，完整显示不裁剪
        iconView.scaleType =
            ImageView.ScaleType.FIT_CENTER

        val iconParams =
            LinearLayout.LayoutParams(
                dp(52),
                dp(52)
            )

        iconParams.setMargins(
            0,
            0,
            dp(14),
            0
        )

        container.addView(
            iconView,
            iconParams
        )

        val textContainer =
            LinearLayout(requireContext())

        textContainer.orientation =
            LinearLayout.VERTICAL

        val nameView =
            TextView(requireContext())

        nameView.text = account.name
        nameView.textSize = 17f
        nameView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.text_primary
            )
        )

        nameView.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val balance =
            calculateAccountBalance(
                account,
                bills,
                transfers
            )

        val balanceView =
            TextView(requireContext())

        balanceView.text =
            String.format(
                Locale.getDefault(),
                "¥%.2f",
                balance
            )

        balanceView.textSize = 15f
        balanceView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.text_secondary
            )
        )

        val typeView =
            TextView(requireContext())

        typeView.text =
            getAccountTypeName(account)

        typeView.textSize = 12f
        typeView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.text_secondary
            )
        )

        textContainer.addView(nameView)
        textContainer.addView(balanceView)
        textContainer.addView(typeView)

        container.addView(
            textContainer,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        card.addView(container)

        card.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    AccountDetailFragment.newInstance(
                        account.id
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        return card
    }

    /**
     * 将 dp 转换为当前设备对应的像素值。
     */
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun calculateAccountBalance(
        account: Account,
        bills: List<Bill>,
        transfers: List<Transfer>
    ): Double {

        var balance = account.balance

        bills
            .filter {
                it.accountId == account.id
            }
            .forEach {

                if (it.type == BillType.INCOME) {
                    balance += it.amount
                } else {
                    balance -= it.amount
                }
            }

        transfers
            .filter {
                it.fromAccountId == account.id
            }
            .forEach {
                balance -= it.amount
            }

        transfers
            .filter {
                it.toAccountId == account.id
            }
            .forEach {
                balance += it.amount
            }

        return balance
    }

    private fun getAccountTypeName(
        account: Account
    ): String {

        return when (account.type.name) {
            "CASH" -> "现金"
            "WECHAT" -> "微信"
            "ALIPAY" -> "支付宝"
            "BANK_CARD" -> "银行卡"
            else -> "其他账户"
        }
    }

    /**
     * 账户类型对应的图标资源
     *
     * 现金   -> xj.png
     * 微信   -> wx.png
     * 支付宝 -> zfb.png
     * 银行卡 -> yxk.png
     * 其他   -> 内置钱包图标兜底
     */
    private fun getAccountIconRes(
        account: Account
    ): Int {

        return when (account.type) {

            AccountType.CASH ->
                R.drawable.xj

            AccountType.WECHAT ->
                R.drawable.wx

            AccountType.ALIPAY ->
                R.drawable.zfb

            AccountType.BANK_CARD ->
                R.drawable.yxk

            AccountType.OTHER ->
                R.drawable.ic_wallet
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}