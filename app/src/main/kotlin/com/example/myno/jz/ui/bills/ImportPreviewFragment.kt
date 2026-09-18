package com.example.myno.jz.ui.bills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myno.jz.R
import com.example.myno.jz.data.imports.CsvBillParser
import com.example.myno.jz.data.imports.ImportBillItem
import com.example.myno.jz.data.imports.ImportBillType
import com.example.myno.jz.data.imports.ImportDuplicateChecker
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.AccountType
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Transfer
import com.example.myno.jz.data.repository.FinanceRepository
import com.example.myno.jz.databinding.FragmentImportPreviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

class ImportPreviewFragment : Fragment() {

    private var _binding: FragmentImportPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: FinanceRepository

    private var originalItems: List<ImportBillItem> = emptyList()

    private var preparedItems: MutableList<ImportBillItem> =
        mutableListOf()

    private var adapter: ImportPreviewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = FinanceRepository(requireContext())

        val pending = pendingItems

        if (pending != null) {
            originalItems = pending
            pendingItems = null
        } else {

            val content =
                arguments?.getString(ARG_CONTENT)

            if (!content.isNullOrBlank()) {

                originalItems =
                    try {
                        CsvBillParser.parse(content)
                    } catch (e: Exception) {
                        emptyList()
                    }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentImportPreviewBinding.inflate(
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

        setupRecyclerView()
        setupButtons()
        prepareItems()
    }

    // ============================================================
    // RecyclerView
    // ============================================================

    private fun setupRecyclerView() {

        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        adapter =
            ImportPreviewAdapter(
                preparedItems,
                object :
                    ImportPreviewAdapter
                    .OnSelectionChangedListener {

                    override fun onSelectionChanged() {
                        updateSummary()
                    }
                }
            )

        binding.recyclerView.adapter = adapter
    }

    // ============================================================
    // Buttons
    // ============================================================

    private fun setupButtons() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        binding.btnSelectAll.setOnClickListener {

            preparedItems.forEach { item ->

                val canSelect =
                    !item.duplicate &&
                            (
                                item.type !=
                                        ImportBillType.NEUTRAL ||
                                        item.transferResolved
                                )

                if (canSelect) {
                    item.selected = true
                }
            }

            adapter?.notifyDataSetChanged()

            updateSummary()
        }

        binding.btnClearSelection.setOnClickListener {

            preparedItems.forEach {
                it.selected = false
            }

            adapter?.notifyDataSetChanged()

            updateSummary()
        }

        binding.btnConfirmImport.setOnClickListener {
            confirmImport()
        }
    }

    // ============================================================
    // Prepare
    // ============================================================

    private fun prepareItems() {

        if (originalItems.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "没有可以导入的账单",
                Toast.LENGTH_LONG
            ).show()

            parentFragmentManager.popBackStack()

            return
        }

        /*
         * 自动确保微信账单可能涉及的隐藏系统账户存在：
         *
         * 经营账户
         * 日利加
         *
         * 这两个账户：
         *
         * enabled = false
         * includeInTotalAssets = false
         *
         * 所以不会显示在资产页面，
         * 也不会计入总资产。
         */
        ensureSystemAccounts()

        val existingBills =
            repository.getBills()

        val existingTransfers =
            repository.getTransfers()

        preparedItems =
            ImportDuplicateChecker
                .markDuplicates(
                    originalItems,
                    existingBills
                )
                .toMutableList()

        prepareTransfers(
            preparedItems,
            existingTransfers
        )

        adapter?.updateItems(
            preparedItems
        )

        updateSummary()
    }

    // ============================================================
    // System Accounts
    // ============================================================

    private fun ensureSystemAccounts() {

        val accounts =
            repository.getAccounts()

        createHiddenSystemAccountIfNeeded(
            accounts = accounts,
            name = "经营账户",
            icon = "经营",
            sortOrder = 9000
        )

        createHiddenSystemAccountIfNeeded(
            accounts = repository.getAccounts(),
            name = "日利加",
            icon = "日利",
            sortOrder = 9001
        )
    }

    private fun createHiddenSystemAccountIfNeeded(
        accounts: List<Account>,
        name: String,
        icon: String,
        sortOrder: Int
    ) {

        val exists =
            accounts.any {

                normalizeAccountName(
                    it.name
                ) == normalizeAccountName(name)
            }

        if (exists) {
            return
        }

        val account =
            Account(
                id = UUID.randomUUID().toString(),

                name = name,

                type = AccountType.OTHER,

                /*
                 * 隐藏系统账户不参与正常资产余额。
                 */
                balance = 0.0,

                icon = icon,

                includeInTotalAssets = false,

                /*
                 * false：
                 * 不显示在资产页面的正常账户列表。
                 *
                 * 导入转账时我们会读取全部账户，
                 * 因此仍然可以正常作为转账账户使用。
                 */
                enabled = false,

                createdAt =
                    System.currentTimeMillis(),

                sortOrder = sortOrder
            )

        repository.addAccount(account)
    }

    // ============================================================
    // Transfer Prepare
    // ============================================================

    private fun prepareTransfers(
        items: MutableList<ImportBillItem>,
        existingTransfers: List<Transfer>
    ) {

        /*
         * 这里非常重要：
         *
         * 不再使用 filter { enabled }
         *
         * 因为“经营账户”和“日利加”
         * 是隐藏系统账户。
         */
        val accounts =
            repository
                .getAccounts()

        items.forEach { item ->

            if (
                item.type !=
                ImportBillType.NEUTRAL
            ) {
                return@forEach
            }

            val mapping =
                resolveTransferAccounts(
                    item,
                    accounts
                )

            if (mapping == null) {

                item.transferResolved = false
                item.selected = false

                return@forEach
            }

            item.transferFromAccountId =
                mapping.first.id

            item.transferToAccountId =
                mapping.second.id

            item.transferFromAccountName =
                mapping.first.name

            item.transferToAccountName =
                mapping.second.name

            item.transferResolved = true

            val duplicate =
                isDuplicateTransfer(
                    item,
                    existingTransfers,
                    mapping.first.id,
                    mapping.second.id
                )

            item.duplicate = duplicate

            item.selected = !duplicate
        }
    }

    // ============================================================
    // Resolve Transfer
    // ============================================================

    private fun resolveTransferAccounts(
        item: ImportBillItem,
        accounts: List<Account>
    ): Pair<Account, Account>? {

        if (accounts.isEmpty()) {
            return null
        }

        val text =
            buildSearchText(item)

        // --------------------------------------------------------
        // 1. 微信 / 零钱通 → 银行卡
        // --------------------------------------------------------

        if (
            containsAny(
                text,
                "零钱通转出",
                "转出零钱通",
                "微信转出",
                "微信余额转出",
                "零钱提现"
            )
        ) {

            val fromAccount =
                findWechatAccount(accounts)
                    ?: return null

            val toAccount =
                findMatchingBankAccount(
                    item,
                    accounts
                )?.takeUnless {
                    it.id == fromAccount.id
                }

            return if (toAccount != null) {
                fromAccount to toAccount
            } else {
                null
            }
        }

        // --------------------------------------------------------
        // 2. 银行卡 → 微信 / 零钱通
        // --------------------------------------------------------

        if (
            containsAny(
                text,
                "转入零钱通",
                "零钱通转入",
                "来自微信",
                "转入微信"
            )
        ) {

            val fromAccount =
                findMatchingBankAccount(
                    item,
                    accounts
                )

            val toAccount =
                findWechatAccount(accounts)

            if (
                fromAccount == null ||
                toAccount == null ||
                fromAccount.id == toAccount.id
            ) {
                return null
            }

            return fromAccount to toAccount
        }

        // --------------------------------------------------------
        // 3. 经营账户 → 银行卡
        // --------------------------------------------------------

        if (
            containsAny(
                text,
                "经营账户提现",
                "经营账户取现",
                "经营账户转出"
            )
        ) {

            val fromAccount =
                findBusinessAccount(accounts)
                    ?: return null

            val toAccount =
                findMatchingBankAccount(
                    item,
                    accounts
                )?.takeUnless {
                    it.id == fromAccount.id
                }

            return if (toAccount != null) {
                fromAccount to toAccount
            } else {
                null
            }
        }

        // --------------------------------------------------------
        // 4. 日利加
        // --------------------------------------------------------

        if (
            containsAny(
                text,
                "日利加转出",
                "转出日利加",
                "转入日利加",
                "日利加转入"
            )
        ) {

            val dailyAccount =
                findDailyAccount(accounts)

            val businessAccount =
                findBusinessAccount(accounts)

            if (
                dailyAccount == null ||
                businessAccount == null ||
                dailyAccount.id ==
                businessAccount.id
            ) {
                return null
            }

            // 日利加 → 经营账户
            if (
                containsAny(
                    text,
                    "日利加转出",
                    "转出日利加"
                )
            ) {
                return dailyAccount to businessAccount
            }

            // 经营账户 → 日利加
            if (
                containsAny(
                    text,
                    "转入日利加",
                    "日利加转入"
                )
            ) {
                return businessAccount to dailyAccount
            }

            return null
        }

        // --------------------------------------------------------
        // 5. 支付宝 → 银行卡
        // --------------------------------------------------------

        if (
            containsAny(
                text,
                "支付宝转出",
                "余额转出",
                "余额宝转出"
            )
        ) {

            val fromAccount =
                findAlipayAccount(accounts)
                    ?: return null

            val toAccount =
                findMatchingBankAccount(
                    item,
                    accounts
                )?.takeUnless {
                    it.id == fromAccount.id
                }

            return if (toAccount != null) {
                fromAccount to toAccount
            } else {
                null
            }
        }

        // --------------------------------------------------------
        // 6. 银行卡 → 支付宝
        // --------------------------------------------------------

        if (
            containsAny(
                text,
                "支付宝转入",
                "转入支付宝"
            )
        ) {

            val fromAccount =
                findMatchingBankAccount(
                    item,
                    accounts
                )

            val toAccount =
                findAlipayAccount(accounts)

            if (
                fromAccount == null ||
                toAccount == null ||
                fromAccount.id ==
                toAccount.id
            ) {
                return null
            }

            return fromAccount to toAccount
        }

        // --------------------------------------------------------
        // 7. 通用转账
        // --------------------------------------------------------

        val sourceAccount =
            resolveGenericTransferSource(
                item,
                accounts
            )
                ?: return null

        val targetAccount =
            resolveGenericTransferTarget(
                item,
                accounts,
                sourceAccount.id
            )
                ?: return null

        if (
            sourceAccount.id ==
            targetAccount.id
        ) {
            return null
        }

        return sourceAccount to targetAccount
    }

    // ============================================================
    // Generic Transfer
    // ============================================================

    private fun resolveGenericTransferSource(
        item: ImportBillItem,
        accounts: List<Account>
    ): Account? {

        val text =
            buildSearchText(item)

        if (
            containsAny(
                text,
                "微信",
                "零钱",
                "零钱通"
            )
        ) {
            return findWechatAccount(accounts)
        }

        if (
            containsAny(
                text,
                "支付宝",
                "花呗",
                "余额宝"
            )
        ) {
            return findAlipayAccount(accounts)
        }

        if (
            containsAny(
                text,
                "经营账户"
            )
        ) {
            return findBusinessAccount(accounts)
        }

        if (
            containsAny(
                text,
                "日利加"
            )
        ) {
            return findDailyAccount(accounts)
        }

        return findBankOrOtherSourceAccount(
            item,
            accounts
        )
    }

    private fun resolveGenericTransferTarget(
        item: ImportBillItem,
        accounts: List<Account>,
        sourceAccountId: String
    ): Account? {

        val text =
            buildSearchText(item)

        if (
            containsAny(
                text,
                "微信",
                "零钱",
                "零钱通"
            )
        ) {

            return findWechatAccount(
                accounts
            )?.takeUnless {
                it.id == sourceAccountId
            }
        }

        if (
            containsAny(
                text,
                "支付宝",
                "花呗",
                "余额宝"
            )
        ) {

            return findAlipayAccount(
                accounts
            )?.takeUnless {
                it.id == sourceAccountId
            }
        }

        if (
            containsAny(
                text,
                "日利加"
            )
        ) {

            return findDailyAccount(
                accounts
            )?.takeUnless {
                it.id == sourceAccountId
            }
        }

        if (
            containsAny(
                text,
                "经营账户"
            )
        ) {

            return findBusinessAccount(
                accounts
            )?.takeUnless {
                it.id == sourceAccountId
            }
        }

        return findBankOrOtherTargetAccount(
            item,
            accounts,
            sourceAccountId
        )
    }

    // ============================================================
    // Account Finders
    // ============================================================

    private fun findWechatAccount(
        accounts: List<Account>
    ): Account? {

        return accounts.firstOrNull {
            it.type == AccountType.WECHAT
        }
            ?: accounts.firstOrNull {

                val name =
                    normalizeAccountName(
                        it.name
                    )

                name.contains("微信") ||
                        name.contains("零钱通")
            }
    }

    private fun findAlipayAccount(
        accounts: List<Account>
    ): Account? {

        return accounts.firstOrNull {
            it.type == AccountType.ALIPAY
        }
            ?: accounts.firstOrNull {

                val name =
                    normalizeAccountName(
                        it.name
                    )

                name.contains("支付宝") ||
                        name.contains("余额宝")
            }
    }

    private fun findBusinessAccount(
        accounts: List<Account>
    ): Account? {

        return accounts.firstOrNull {

            normalizeAccountName(
                it.name
            ).contains("经营账户")
        }
            ?: accounts.firstOrNull {

                normalizeAccountName(
                    it.name
                ).contains("经营")
            }
    }

    private fun findDailyAccount(
        accounts: List<Account>
    ): Account? {

        return accounts.firstOrNull {

            val name =
                normalizeAccountName(
                    it.name
                )

            name.contains("日利加") ||
                    name.contains("日利佳")
        }
    }

    // ============================================================
    // Bank
    // ============================================================

    private fun findBankOrOtherSourceAccount(
        item: ImportBillItem,
        accounts: List<Account>
    ): Account? {

        return findMatchingBankAccount(
            item,
            accounts
        )
    }

    private fun findBankOrOtherTargetAccount(
        item: ImportBillItem,
        accounts: List<Account>,
        sourceAccountId: String
    ): Account? {

        return findMatchingBankAccount(
            item,
            accounts
        )?.takeUnless {
            it.id == sourceAccountId
        }
    }

    private fun findMatchingBankAccount(
        item: ImportBillItem,
        accounts: List<Account>
    ): Account? {

        val text =
            buildSearchText(item)

        /*
         * 只把真正银行卡类型作为银行卡候选。
         *
         * OTHER 中包含：
         * 经营账户
         * 日利加
         *
         * 不能再把它们当银行卡。
         */
        val bankAccounts =
            accounts.filter {
                it.type ==
                        AccountType.BANK_CARD
            }

        if (bankAccounts.isEmpty()) {
            return null
        }

        val bankKeywords =
            listOf(
                "招商银行",
                "中国银行",
                "工商银行",
                "中国工商银行",
                "建设银行",
                "中国建设银行",
                "农业银行",
                "中国农业银行",
                "交通银行",
                "邮储银行",
                "邮政储蓄银行",
                "中信银行",
                "民生银行",
                "兴业银行",
                "光大银行",
                "浦发银行",
                "平安银行",
                "华夏银行",
                "广发银行",
                "长沙银行",
                "湖南银行",
                "浙商银行",
                "北京银行",
                "上海银行",
                "广东华兴银行",
                "华兴银行",
                "宁波银行",
                "江苏银行",
                "南京银行",
                "杭州银行",
                "徽商银行",
                "渤海银行",
                "恒丰银行",
                "天津银行",
                "重庆银行",
                "成都银行",
                "东莞银行",
                "广州银行",
                "微众银行",
                "网商银行"
            )

        val matchedBank =
            bankKeywords
                .sortedByDescending {
                    it.length
                }
                .firstOrNull {
                    text.contains(it)
                }

        val fourDigits =
            Regex(
                "(?<!\\d)\\d{4}(?!\\d)"
            )
                .find(text)
                ?.value

        data class MatchResult(
            val account: Account,
            val score: Int
        )

        val candidates =
            bankAccounts.mapNotNull { account ->

                val name =
                    normalizeAccountName(
                        account.name
                    )

                if (name.isBlank()) {
                    return@mapNotNull null
                }

                var score = 0

                if (
                    matchedBank != null &&
                    name.contains(
                        normalizeAccountName(
                            matchedBank
                        )
                    )
                ) {
                    score += 100
                }

                if (
                    !fourDigits.isNullOrBlank() &&
                    name.contains(fourDigits)
                ) {
                    score += 200
                }

                /*
                 * 如果账户名称本身就是“银行卡”，
                 * 而当前系统只有一个银行卡账户，
                 * 后面会统一兜底。
                 */

                if (
                    name.length >= 2 &&
                    text.contains(name)
                ) {
                    score += 150
                }

                if (
                    account.type ==
                    AccountType.BANK_CARD
                ) {
                    score += 10
                }

                if (score > 0) {
                    MatchResult(
                        account,
                        score
                    )
                } else {
                    null
                }
            }

        val best =
            candidates.maxByOrNull {
                it.score
            }

        if (best != null) {
            return best.account
        }

        /*
         * 你的资产页面目前只有一个“银行卡”账户。
         *
         * 因此：
         *
         * 招商银行(7833)
         * 建设银行(8091)
         * 农业银行(8072)
         *
         * 最终都归入：
         *
         * 银行卡
         */
        if (bankAccounts.size == 1) {
            return bankAccounts.first()
        }

        return null
    }

    // ============================================================
    // Duplicate Transfer
    // ============================================================

    private fun isDuplicateTransfer(
        item: ImportBillItem,
        existingTransfers: List<Transfer>,
        fromAccountId: String,
        toAccountId: String
    ): Boolean {

        if (
            item.source.isNotBlank() &&
            item.transactionId.isNotBlank()
        ) {

            val exact =
                existingTransfers.any {

                    it.source.equals(
                        item.source,
                        ignoreCase = true
                    ) &&
                            it.sourceTransactionId ==
                            item.transactionId
                }

            if (exact) {
                return true
            }
        }

        val timestamp =
            item.timestamp
                ?: return false

        return existingTransfers.any { transfer ->

            transfer.fromAccountId ==
                    fromAccountId &&

                    transfer.toAccountId ==
                    toAccountId &&

                    abs(
                        transfer.amount -
                                item.amount
                    ) <= 0.01 &&

                    abs(
                        transfer.timestamp -
                                timestamp
                    ) <= 60_000L
        }
    }

    // ============================================================
    // Search Text
    // ============================================================

    private fun buildSearchText(
        item: ImportBillItem
    ): String {

        return listOf(
            item.transactionType,
            item.merchant,
            item.note,
            item.paymentMethod,
            item.status,
            item.originalRow
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(" ")
            .lowercase(
                Locale.getDefault()
            )
    }

    private fun normalizeAccountName(
        name: String
    ): String {

        return name
            .replace(" ", "")
            .replace("（", "(")
            .replace("）", ")")
            .lowercase(
                Locale.getDefault()
            )
    }

    private fun containsAny(
        text: String,
        vararg keywords: String
    ): Boolean {

        return keywords.any {
            text.contains(
                it.lowercase(
                    Locale.getDefault()
                )
            )
        }
    }

    // ============================================================
    // Summary
    // ============================================================

    private fun updateSummary() {

        val total =
            preparedItems.size

        val duplicateCount =
            preparedItems.count {
                it.duplicate
            }

        val selectedCount =
            preparedItems.count {

                it.selected &&
                        !it.duplicate &&
                        (
                            it.type !=
                                    ImportBillType.NEUTRAL ||
                                    it.transferResolved
                            )
            }

        val expenseCount =
            preparedItems.count {
                it.type ==
                        ImportBillType.EXPENSE
            }

        val incomeCount =
            preparedItems.count {
                it.type ==
                        ImportBillType.INCOME
            }

        val neutralCount =
            preparedItems.count {
                it.type ==
                        ImportBillType.NEUTRAL
            }

        val resolvedTransferCount =
            preparedItems.count {

                it.type ==
                        ImportBillType.NEUTRAL &&
                        it.transferResolved &&
                        !it.duplicate
            }

        val unresolvedTransferCount =
            preparedItems.count {

                it.type ==
                        ImportBillType.NEUTRAL &&
                        !it.transferResolved
            }

        binding.tvSummary.text =
            "共 $total 笔　" +
                    "支出 $expenseCount 笔　" +
                    "收入 $incomeCount 笔　" +
                    "转账 $neutralCount 笔\n" +
                    "可导入转账 $resolvedTransferCount 笔　" +
                    "无法匹配 $unresolvedTransferCount 笔\n" +
                    "重复 $duplicateCount 笔　" +
                    "已选择 $selectedCount 笔"

        binding.btnConfirmImport.isEnabled =
            selectedCount > 0
    }

    // ============================================================
    // Confirm Import
    // ============================================================

    private fun confirmImport() {

        val selectedItems =
            preparedItems.filter {

                it.selected &&
                        !it.duplicate &&
                        (
                            it.type !=
                                    ImportBillType.NEUTRAL ||
                                    it.transferResolved
                            )
            }

        if (selectedItems.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "没有选择需要导入的账单",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val account =
            findImportAccount()

        val normalBillItems =
            selectedItems.filter {
                it.type !=
                        ImportBillType.NEUTRAL
            }

        if (
            normalBillItems.isNotEmpty() &&
            account == null
        ) {

            Toast.makeText(
                requireContext(),
                "没有可用的账户，请先在资产页面添加账户",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        var billSuccessCount = 0
        var billFailCount = 0

        var transferSuccessCount = 0
        var transferFailCount = 0

        selectedItems.forEach { item ->

            // ----------------------------------------------------
            // Transfer
            // ----------------------------------------------------

            if (
                item.type ==
                ImportBillType.NEUTRAL
            ) {

                val fromId =
                    item.transferFromAccountId

                val toId =
                    item.transferToAccountId

                if (
                    fromId.isBlank() ||
                    toId.isBlank()
                ) {

                    transferFailCount++

                    return@forEach
                }

                val transfer =
                    Transfer(
                        id =
                            UUID.randomUUID()
                                .toString(),

                        fromAccountId =
                            fromId,

                        toAccountId =
                            toId,

                        amount =
                            item.amount,

                        note =
                            buildTransferNote(item),

                        timestamp =
                            item.timestamp
                                ?: System.currentTimeMillis(),

                        createdAt =
                            System.currentTimeMillis(),

                        source =
                            item.source,

                        sourceTransactionId =
                            item.transactionId
                    )

                val success =
                    repository.addTransfer(
                        transfer
                    )

                if (success) {
                    transferSuccessCount++
                } else {
                    transferFailCount++
                }

                return@forEach
            }

            // ----------------------------------------------------
            // Normal Bill
            // ----------------------------------------------------

            val billType =
                when (item.type) {

                    ImportBillType.EXPENSE ->
                        BillType.EXPENSE

                    ImportBillType.INCOME ->
                        BillType.INCOME

                    ImportBillType.NEUTRAL ->
                        return@forEach

                    ImportBillType.UNKNOWN ->
                        return@forEach
                }

            val categoryId =
                findCategoryId(item)

            val timestamp =
                item.timestamp
                    ?: System.currentTimeMillis()

            val note =
                buildBillNote(item)

            val success =
                repository.createBill(
                    type =
                        billType,

                    amount =
                        item.amount,

                    categoryId =
                        categoryId,

                    accountId =
                        account!!.id,

                    note =
                        note,

                    timestamp =
                        timestamp,

                    source =
                        item.source,

                    sourceTransactionId =
                        item.transactionId
                )

            if (success) {
                billSuccessCount++
            } else {
                billFailCount++
            }
        }

        val message =
            "导入完成\n" +
                    "账单成功 $billSuccessCount 笔\n" +
                    "账单失败 $billFailCount 笔\n" +
                    "转账成功 $transferSuccessCount 笔\n" +
                    "转账失败 $transferFailCount 笔"

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()

        parentFragmentManager.popBackStack()
    }

    // ============================================================
    // Import Account
    // ============================================================

    private fun findImportAccount(): Account? {

        val accounts =
            repository
                .getAccounts()
                .filter {
                    it.enabled
                }

        if (accounts.isEmpty()) {
            return null
        }

        val hasWechatBill =
            preparedItems.any {

                it.selected &&
                        !it.duplicate &&
                        it.type !=
                                ImportBillType.NEUTRAL &&
                        it.source.equals(
                            "WECHAT",
                            ignoreCase = true
                        )
            }

        if (hasWechatBill) {

            findWechatAccount(
                accounts
            )?.let {
                return it
            }
        }

        val hasAlipayBill =
            preparedItems.any {

                it.selected &&
                        !it.duplicate &&
                        it.type !=
                                ImportBillType.NEUTRAL &&
                        it.source.equals(
                            "ALIPAY",
                            ignoreCase = true
                        )
            }

        if (hasAlipayBill) {

            findAlipayAccount(
                accounts
            )?.let {
                return it
            }
        }

        val defaultAccountId =
            repository
                .getSettings()
                .defaultAccountId

        if (
            !defaultAccountId.isNullOrBlank()
        ) {

            accounts
                .firstOrNull {
                    it.id ==
                            defaultAccountId
                }
                ?.let {
                    return it
                }
        }

        return accounts.first()
    }

    // ============================================================
    // Category
    // ============================================================

    private fun findCategoryId(
        item: ImportBillItem
    ): String {

        val categories =
            repository
                .getCategories()
                .filter {
                    it.visible
                }

        val targetType =
            when (item.type) {

                ImportBillType.EXPENSE ->
                    com.example.myno.jz.data.model
                        .CategoryType.EXPENSE

                ImportBillType.INCOME ->
                    com.example.myno.jz.data.model
                        .CategoryType.INCOME

                ImportBillType.NEUTRAL ->
                    return ""

                ImportBillType.UNKNOWN ->
                    com.example.myno.jz.data.model
                        .CategoryType.EXPENSE
            }

        val targetCategories =
            categories.filter {
                it.type ==
                        targetType
            }

        val text =
            (
                item.merchant +
                        " " +
                        item.note +
                        " " +
                        item.originalRow
                )
                .lowercase(
                    Locale.getDefault()
                )

        val keywordRules =
            listOf(

                listOf(
                    "外卖",
                    "美团",
                    "饿了么",
                    "肯德基",
                    "麦当劳",
                    "饭",
                    "餐",
                    "奶茶",
                    "食品"
                ) to
                        listOf(
                            "吃饭",
                            "餐饮",
                            "饮食"
                        ),

                listOf(
                    "淘宝",
                    "京东",
                    "拼多多",
                    "购物",
                    "商城",
                    "零食"
                ) to
                        listOf(
                            "购物"
                        ),

                listOf(
                    "滴滴",
                    "高德",
                    "打车",
                    "地铁",
                    "公交",
                    "加油",
                    "停车"
                ) to
                        listOf(
                            "交通"
                        ),

                listOf(
                    "工资",
                    "薪资",
                    "奖金",
                    "薪酬"
                ) to
                        listOf(
                            "工资"
                        ),

                listOf(
                    "转账",
                    "红包"
                ) to
                        listOf(
                            "转账",
                            "其他"
                        )
            )

        for (rule in keywordRules) {

            val keywords =
                rule.first

            val categoryNames =
                rule.second

            val matched =
                keywords.any {

                    text.contains(
                        it.lowercase(
                            Locale.getDefault()
                        )
                    )
                }

            if (!matched) {
                continue
            }

            val category =
                targetCategories.firstOrNull {

                    category ->

                    categoryNames.any {

                        name ->

                        category.name.contains(
                            name
                        ) ||
                                name.contains(
                                    category.name
                                )
                    }
                }

            if (category != null) {
                return category.id
            }
        }

        return targetCategories
            .sortedBy {
                it.sortOrder
            }
            .firstOrNull()
            ?.id
            ?: ""
    }

    // ============================================================
    // Notes
    // ============================================================

    private fun buildBillNote(
        item: ImportBillItem
    ): String {

        val parts =
            mutableListOf<String>()

        if (
            item.merchant.isNotBlank()
        ) {
            parts.add(item.merchant)
        }

        if (
            item.note.isNotBlank() &&
            item.note != "/"
        ) {
            parts.add(item.note)
        }

        if (
            item.paymentMethod.isNotBlank() &&
            item.paymentMethod != "/"
        ) {
            parts.add(
                "支付方式:${item.paymentMethod}"
            )
        }

        if (
            item.status.isNotBlank() &&
            item.status != "/"
        ) {
            parts.add(
                "状态:${item.status}"
            )
        }

        if (
            item.transactionId.isNotBlank()
        ) {
            parts.add(
                "交易单号:${item.transactionId}"
            )
        }

        if (
            item.merchantOrderId.isNotBlank() &&
            item.merchantOrderId != "/"
        ) {
            parts.add(
                "商户单号:${item.merchantOrderId}"
            )
        }

        return parts.joinToString(" · ")
    }

    private fun buildTransferNote(
        item: ImportBillItem
    ): String {

        val parts =
            mutableListOf<String>()

        if (
            item.transactionType.isNotBlank()
        ) {
            parts.add(
                item.transactionType
            )
        }

        if (
            item.merchant.isNotBlank()
        ) {
            parts.add(
                item.merchant
            )
        }

        if (
            item.note.isNotBlank() &&
            item.note != "/"
        ) {
            parts.add(
                item.note
            )
        }

        if (
            item.paymentMethod.isNotBlank() &&
            item.paymentMethod != "/"
        ) {
            parts.add(
                "支付方式:${item.paymentMethod}"
            )
        }

        if (
            item.status.isNotBlank() &&
            item.status != "/"
        ) {
            parts.add(
                "状态:${item.status}"
            )
        }

        return parts.joinToString(" · ")
    }

    // ============================================================
    // Time
    // ============================================================

    private fun formatTime(
        timestamp: Long?
    ): String {

        if (timestamp == null) {
            return "未识别时间"
        }

        return try {

            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )

        } catch (e: Exception) {

            "未识别时间"
        }
    }

    // ============================================================
    // Destroy
    // ============================================================

    override fun onDestroyView() {

        super.onDestroyView()

        adapter = null
        _binding = null
    }

    // ============================================================
    // Companion
    // ============================================================

    companion object {

        private const val ARG_CONTENT =
            "arg_content"

        private var pendingItems:
                List<ImportBillItem>? =
            null

        fun newInstance(
            content: String
        ): ImportPreviewFragment {

            return ImportPreviewFragment()
                .apply {

                    arguments =
                        Bundle().apply {

                            putString(
                                ARG_CONTENT,
                                content
                            )
                        }
                }
        }

        fun showParsedItems(
            fragmentManager:
                androidx.fragment.app.FragmentManager,
            items: List<ImportBillItem>
        ) {

            pendingItems = items

            fragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    ImportPreviewFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }
}