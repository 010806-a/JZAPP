package com.example.myno.jz.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.databinding.FragmentAccountFlowBinding
import com.example.myno.jz.ui.bills.BillDetailFragment
import com.example.myno.jz.ui.main.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountFlowFragment : Fragment() {

    private var _binding: FragmentAccountFlowBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var accountId: String? = null

    private var currentFilter = FlowFilter.ALL

    private lateinit var flowAdapter: AccountFlowAdapter

    enum class FlowFilter {
        ALL,
        INCOME,
        EXPENSE,
        TRANSFER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountId = arguments?.getString(ARG_ACCOUNT_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAccountFlowBinding.inflate(
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

        if (accountId.isNullOrBlank()) {

            Toast.makeText(
                requireContext(),
                "账户信息不存在",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()

            return
        }

        setupRecyclerView()
        setupButtons()
        observeData()

        updateAccountInfo()
        updateFlowList()
    }

    /**
     * RecyclerView
     */
    private fun setupRecyclerView() {

        flowAdapter = AccountFlowAdapter(

            onBillClick = { billId ->
                openBillDetail(billId)
            },

            onTransferClick = { transferId ->
                openTransferDetail(transferId)
            }
        )

        binding.recyclerView.apply {

            layoutManager =
                LinearLayoutManager(requireContext())

            adapter = flowAdapter

            setHasFixedSize(false)
        }
    }

    /**
     * 页面按钮
     */
    private fun setupButtons() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        binding.chipAll.setOnClickListener {

            currentFilter = FlowFilter.ALL

            updateFlowList()
        }

        binding.chipIncome.setOnClickListener {

            currentFilter = FlowFilter.INCOME

            updateFlowList()
        }

        binding.chipExpense.setOnClickListener {

            currentFilter = FlowFilter.EXPENSE

            updateFlowList()
        }

        binding.chipTransfer.setOnClickListener {

            currentFilter = FlowFilter.TRANSFER

            updateFlowList()
        }

        /**
         * 搜索实时过滤
         */
        binding.etSearch.doAfterTextChanged {

            updateFlowList()
        }
    }

    /**
     * 监听数据
     */
    private fun observeData() {

        viewModel.accounts.observe(
            viewLifecycleOwner
        ) {

            updateAccountInfo()
            updateFlowList()
        }

        viewModel.bills.observe(
            viewLifecycleOwner
        ) {

            updateAccountInfo()
            updateFlowList()
        }

        viewModel.categories.observe(
            viewLifecycleOwner
        ) {

            updateFlowList()
        }
    }

    /**
     * 更新账户顶部信息
     */
    private fun updateAccountInfo() {

        val id = accountId ?: return

        val account =
            viewModel.accounts.value
                ?.firstOrNull {
                    it.id == id
                }
                ?: return

        binding.tvTitle.text =
            "${account.name}流水"

        binding.tvAccountName.text =
            account.name

        val bills =
            viewModel.bills.value.orEmpty()

        val transfers =
            viewModel.getRepository()
                .getTransfers()

        var balance =
            account.balance

        var monthIncome =
            0.0

        var monthExpense =
            0.0

        val now =
            Calendar.getInstance()

        val currentYear =
            now.get(Calendar.YEAR)

        val currentMonth =
            now.get(Calendar.MONTH)

        /**
         * 计算账单
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
                    timeInMillis = bill.timestamp
                }

            val sameMonth =
                calendar.get(Calendar.YEAR) ==
                        currentYear &&
                        calendar.get(Calendar.MONTH) ==
                        currentMonth

            if (sameMonth) {

                when (bill.type) {

                    BillType.INCOME -> {

                        monthIncome += bill.amount
                    }

                    BillType.EXPENSE -> {

                        monthExpense += bill.amount
                    }
                }
            }
        }

        /**
         * 计算转账
         */
        transfers.forEach { transfer ->

            if (transfer.fromAccountId == id) {

                balance -= transfer.amount
            }

            if (transfer.toAccountId == id) {

                balance += transfer.amount
            }
        }

        binding.tvBalance.text =
            formatMoneyWithSymbol(balance)

        binding.tvMonthIncome.text =
            "+${formatMoneyWithSymbol(monthIncome)}"

        binding.tvMonthExpense.text =
            "-${formatMoneyWithSymbol(monthExpense)}"
    }

    /**
     * 更新流水列表
     */
    private fun updateFlowList() {

        val id = accountId ?: return

        val searchText =
            binding.etSearch.text
                ?.toString()
                ?.trim()
                ?.lowercase(
                    Locale.getDefault()
                )
                .orEmpty()

        val bills =
            viewModel.bills.value.orEmpty()

        val transfers =
            viewModel.getRepository()
                .getTransfers()

        val accounts =
            viewModel.accounts.value.orEmpty()

        val categories =
            viewModel.categories.value.orEmpty()

        val flowList =
            mutableListOf<AccountFlowItem>()

        /**
         * ==========================
         * 普通账单
         * ==========================
         */
        bills.forEach { bill ->

            if (bill.accountId != id) {
                return@forEach
            }

            /**
             * 分类名称
             */
            val categoryName =
                categories
                    .firstOrNull {
                        it.id == bill.categoryId
                    }
                    ?.name
                    ?: ""

            /**
             * 搜索内容
             */
            val searchableText =
                buildString {

                    append(bill.note)
                    append(" ")

                    append(categoryName)
                    append(" ")

                    append(bill.source)

                }.lowercase(
                    Locale.getDefault()
                )

            if (
                searchText.isNotBlank() &&
                !searchableText.contains(
                    searchText
                )
            ) {
                return@forEach
            }

            /**
             * 类型筛选
             */
            when (currentFilter) {

                FlowFilter.ALL -> {
                    // 全部
                }

                FlowFilter.INCOME -> {

                    if (
                        bill.type !=
                        BillType.INCOME
                    ) {
                        return@forEach
                    }
                }

                FlowFilter.EXPENSE -> {

                    if (
                        bill.type !=
                        BillType.EXPENSE
                    ) {
                        return@forEach
                    }
                }

                FlowFilter.TRANSFER -> {

                    return@forEach
                }
            }

            flowList.add(
                AccountFlowItem.BillItem(
                    bill = bill,
                    categoryName = categoryName
                )
            )
        }

        /**
         * ==========================
         * 转账
         * ==========================
         */
        transfers.forEach { transfer ->

            if (
                transfer.fromAccountId != id &&
                transfer.toAccountId != id
            ) {
                return@forEach
            }

            if (
                currentFilter != FlowFilter.ALL &&
                currentFilter != FlowFilter.TRANSFER
            ) {
                return@forEach
            }

            val fromAccount =
                accounts.firstOrNull {
                    it.id ==
                            transfer.fromAccountId
                }

            val toAccount =
                accounts.firstOrNull {
                    it.id ==
                            transfer.toAccountId
                }

            val fromName =
                fromAccount?.name
                    ?: "未知账户"

            val toName =
                toAccount?.name
                    ?: "未知账户"

            /**
             * 转账搜索
             */
            val searchableText =
                buildString {

                    append(fromName)
                    append(" ")

                    append(toName)
                    append(" ")

                    append(transfer.note)

                }.lowercase(
                    Locale.getDefault()
                )

            if (
                searchText.isNotBlank() &&
                !searchableText.contains(
                    searchText
                )
            ) {
                return@forEach
            }

            flowList.add(
                AccountFlowItem.TransferItem(
                    transfer = transfer,
                    fromAccountName = fromName,
                    toAccountName = toName,
                    isTransferIn =
                        transfer.toAccountId == id
                )
            )
        }

        /**
         * 按时间倒序
         */
        flowList.sortByDescending {
            it.timestamp
        }

        /**
         * 日期分组
         */
        val groupedList =
            buildGroupedList(flowList)

        flowAdapter.submitList(
            groupedList
        )

        /**
         * 空状态
         */
        if (flowList.isEmpty()) {

            binding.layoutEmpty.visibility =
                View.VISIBLE

            binding.recyclerView.visibility =
                View.GONE

        } else {

            binding.layoutEmpty.visibility =
                View.GONE

            binding.recyclerView.visibility =
                View.VISIBLE
        }
    }

    /**
     * 日期分组
     */
    private fun buildGroupedList(
        list: List<AccountFlowItem>
    ): List<AccountFlowDisplayItem> {

        if (list.isEmpty()) {
            return emptyList()
        }

        val result =
            mutableListOf<AccountFlowDisplayItem>()

        var lastDate = ""

        list.forEach { item ->

            val dateKey =
                getDateKey(
                    item.timestamp
                )

            if (dateKey != lastDate) {

                result.add(
                    AccountFlowDisplayItem.DateHeader(
                        date =
                            getDisplayDate(
                                item.timestamp
                            )
                    )
                )

                lastDate = dateKey
            }

            result.add(
                AccountFlowDisplayItem.Flow(
                    item = item
                )
            )
        }

        return result
    }

    /**
     * 日期 Key
     */
    private fun getDateKey(
        timestamp: Long
    ): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(
            Date(timestamp)
        )
    }

    /**
     * 日期显示
     */
    private fun getDisplayDate(
        timestamp: Long
    ): String {

        val target =
            Calendar.getInstance().apply {
                timeInMillis = timestamp
            }

        val today =
            Calendar.getInstance()

        if (
            target.get(Calendar.YEAR) ==
            today.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) ==
            today.get(Calendar.DAY_OF_YEAR)
        ) {

            return "今天"
        }

        val yesterday =
            Calendar.getInstance().apply {

                add(
                    Calendar.DAY_OF_YEAR,
                    -1
                )
            }

        if (
            target.get(Calendar.YEAR) ==
            yesterday.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) ==
            yesterday.get(Calendar.DAY_OF_YEAR)
        ) {

            return "昨天"
        }

        return SimpleDateFormat(
            "yyyy年MM月dd日",
            Locale.getDefault()
        ).format(
            Date(timestamp)
        )
    }

    /**
     * 金额格式
     */
    private fun formatMoney(
        value: Double
    ): String {

        return String.format(
            Locale.getDefault(),
            "%.2f",
            value
        )
    }

    private fun formatMoneyWithSymbol(
        value: Double
    ): String {

        return "¥${formatMoney(value)}"
    }

    /**
     * 打开账单详情
     */
    private fun openBillDetail(
        billId: String
    ) {

        val billExists =
            viewModel.bills.value
                ?.any {
                    it.id == billId
                }
                ?: false

        if (!billExists) {

            Toast.makeText(
                requireContext(),
                "账单不存在或已被删除",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        parentFragmentManager
            .beginTransaction()
            .replace(
                com.example.myno.jz.R.id.fragmentContainer,
                BillDetailFragment.newInstance(
                    billId
                )
            )
            .addToBackStack(null)
            .commit()
    }

    /**
     * 打开转账详情
     */
    private fun openTransferDetail(
        transferId: String
    ) {

        val transferExists =
            viewModel.getRepository()
                .getTransfers()
                .any {
                    it.id == transferId
                }

        if (!transferExists) {

            Toast.makeText(
                requireContext(),
                "转账记录不存在或已被删除",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        parentFragmentManager
            .beginTransaction()
            .replace(
                com.example.myno.jz.R.id.fragmentContainer,
                TransferDetailFragment.newInstance(
                    transferId
                )
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()

        /**
         * 从账单详情 / 转账详情返回时
         * 再刷新一次数据
         */
        if (_binding != null) {

            viewModel.refresh()

            updateAccountInfo()
            updateFlowList()
        }
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
        ): AccountFlowFragment {

            return AccountFlowFragment().apply {

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