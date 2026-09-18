package com.example.myno.jz.ui.home

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.BudgetType
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.databinding.FragmentHomeBinding
import com.example.myno.jz.ui.bills.AddBillFragment
import com.example.myno.jz.ui.main.MainViewModel
import com.example.myno.jz.utils.FinanceCalculator
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var adapter: RecentBillAdapter
    private lateinit var quickActionAdapter: QuickActionAdapter

private lateinit var quickActionStore: QuickActionStore

private var isQuickActionManagementMode = false

private val allQuickActions by lazy {

    listOf(
        QuickAction(
            id = "add_bill",
            title = "记一笔",
            iconRes = R.drawable.ic_add_a,
            order = 0
        ),
        QuickAction(
            id = "statistics",
            title = "账单统计",
            iconRes = R.drawable.ic_statistics,
            order = 1
        ),
        QuickAction(
            id = "category",
            title = "分类管理",
            iconRes = R.drawable.ic_category_chart,
            order = 2
        ),
        QuickAction(
            id = "calendar",
            title = "日历账单",
            iconRes = R.drawable.ic_calendar,
            order = 3
        ),
        QuickAction(
            id = "budget",
            title = "预算管理",
            iconRes = R.drawable.ic_wallet,
            order = 4
        )
    )
}

    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0

    private var isMoneyVisible = true
    private var isBalanceFlipped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val calendar = Calendar.getInstance()

        selectedYear = calendar.get(Calendar.YEAR)
        selectedMonth = calendar.get(Calendar.MONTH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(
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

        setupGreeting()
        setupRecyclerView()
        setupQuickActions()
setupClickListeners()
observeData()

        updateMonthTitle()
        updateMoneyVisibility()
        updateHome()
    }
    /** 首页标题：Money 深色，Book 蓝色，保持与设计稿一致。 */
    private fun setupGreeting() {
        val title = SpannableString("MoneyBook")
        title.setSpan(
            ForegroundColorSpan(requireContext().getColor(R.color.text_primary)),
            0,
            5,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        title.setSpan(
            ForegroundColorSpan(requireContext().getColor(R.color.primary)),
            5,
            title.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvGreeting.text = title
    }

    /**
     * 快捷方式全部隐藏后的状态：整张卡片显示“提示背景”。
     * 背景只负责视觉提示，不承担点击添加功能。
     * bg_quick_method_empty 与 bg_quick_empty 两张图始终互斥，不会叠加。
     */
    private fun updateQuickActionEmptyState() {
        val isEmpty = quickActionAdapter.getActions().isEmpty()

        binding.recyclerQuickActions.visibility =
            if (isEmpty) View.GONE else View.VISIBLE

        val resourceId = resources.getIdentifier(
            "bg_quick_method_empty",
            "drawable",
            requireContext().packageName
        )

        binding.ivQuickEmptyGuide.animate().cancel()

        if (!isEmpty || isQuickActionManagementMode || resourceId == 0) {
            binding.ivQuickEmptyGuide.alpha = 0f
            binding.ivQuickEmptyGuide.visibility = View.GONE
            binding.ivQuickEmptyGuide.setOnClickListener(null)
            binding.cardQuickActions.setOnClickListener(null)
            return
        }

        binding.ivQuickEmptyGuide.setImageResource(resourceId)
        binding.ivQuickEmptyGuide.visibility = View.VISIBLE
        binding.ivQuickEmptyGuide.alpha = 1f

        // 这里仅用于“提示”，绝对不能让整个快捷方式卡片变成添加入口。
        // 添加快捷方式必须先进入“管理”模式，再点击“＋ 添加快捷方式”。
        binding.ivQuickEmptyGuide.setOnClickListener(null)
        binding.cardQuickActions.setOnClickListener(null)

        syncQuickBackgroundSize()
    }

    private fun setupQuickActions() {

    quickActionStore =
        QuickActionStore(requireContext())

    quickActionAdapter =
        QuickActionAdapter(
            onActionClick = { action ->

                handleQuickActionClick(
                    action
                )
            },
            onDeleteClick = { action ->

                hideQuickAction(
                    action
                )
            }
        )

    binding.recyclerQuickActions.apply {

        layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        adapter =
            quickActionAdapter

        itemAnimator =
            androidx.recyclerview.widget.DefaultItemAnimator()

        setHasFixedSize(true)

        overScrollMode =
            View.OVER_SCROLL_NEVER
    }

    setupQuickActionDrag()

    loadQuickActions()

    binding.tvQuickManage.setOnClickListener {

        toggleQuickActionManagementMode()
    }

    binding.tvQuickAdd.setOnClickListener {

        showAddQuickActionDialog()
    }

    setupQuickManagementBackground()
}

/**
 * 快捷方式管理背景。背景图不参与父布局测量，始终同步为整个 CardView 的实际宽高。
 */
private fun setupQuickManagementBackground() {
    val resourceId = resources.getIdentifier(
        "bg_quick_empty",
        "drawable",
        requireContext().packageName
    )

    binding.ivQuickManagementBackground.apply {
        if (resourceId != 0) {
            setImageResource(resourceId)
        }
        visibility = View.GONE
        alpha = 0f
    }

    binding.cardQuickActions.clipToOutline = true
    binding.cardQuickActions.post {
        syncQuickBackgroundSize()
    }
}

/** 两张 3040×1472 背景图都强制等于快捷方式 CardView 的实际尺寸。 */
private fun syncQuickBackgroundSize() {
    val card = binding.cardQuickActions
    if (card.width <= 0 || card.height <= 0) {
        card.post { syncQuickBackgroundSize() }
        return
    }

    fun sync(view: View) {
        val lp = view.layoutParams
        lp.width = card.width
        lp.height = card.height
        view.layoutParams = lp
    }

    sync(binding.ivQuickManagementBackground)
    sync(binding.ivQuickEmptyGuide)
}

private fun updateQuickManagementBackground(animate: Boolean = true) {
    val managementBackground = binding.ivQuickManagementBackground
    val emptyGuide = binding.ivQuickEmptyGuide
    val resourceId = resources.getIdentifier(
        "bg_quick_empty",
        "drawable",
        requireContext().packageName
    )

    managementBackground.animate().cancel()
    emptyGuide.animate().cancel()

    // 两张图绝不同时显示。
    emptyGuide.alpha = 0f
    emptyGuide.visibility = View.GONE

    if (isQuickActionManagementMode && resourceId != 0) {
        binding.cardQuickActions.setOnClickListener(null)
        managementBackground.setImageResource(resourceId)
        managementBackground.visibility = View.VISIBLE
        syncQuickBackgroundSize()

        if (!animate) {
            managementBackground.alpha = 1f
            return
        }

        managementBackground.alpha = 0f
        managementBackground.animate()
            .alpha(1f)
            .setDuration(260L)
            .start()
        return
    }

    managementBackground.alpha = 0f
    managementBackground.visibility = View.GONE

    if (!isQuickActionManagementMode && quickActionAdapter.getActions().isEmpty()) {
        updateQuickActionEmptyState()
    } else {
        binding.cardQuickActions.setOnClickListener(null)
    }
}

private fun loadQuickActions() {

    val savedActions =
        quickActionStore.load()

    val actions =
        if (savedActions.isEmpty()) {

            allQuickActions

        } else {

            val savedMap =
                savedActions.associateBy {
                    it.id
                }

            savedActions
                .filter {
                    it.visible
                }
                .sortedBy {
                    it.order
                }
                .mapNotNull { saved ->

                    allQuickActions
                        .firstOrNull {
                            it.id == saved.id
                        }
                }
        }

    quickActionAdapter.submitList(
        actions
    )
    updateQuickActionEmptyState()
}
private fun handleQuickActionClick(
    action: QuickAction
) {

    when (action.id) {

        "add_bill" -> {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    AddBillFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        "statistics" -> {
            openHomeSubFragment(
                com.example.myno.jz.ui.statistics.StatisticsFragment()
            )
        }

        "category" -> {
            openHomeSubFragment(
                com.example.myno.jz.ui.category.CategoryManageFragment()
            )
        }

        "calendar" -> {
            // 当前项目尚无独立日历页，先进入完整账单页，避免出现“后续版本”占位提示。
            openHomeSubFragment(
                com.example.myno.jz.ui.bills.BillsFragment()
            )
        }

        "budget" -> {
            openHomeSubFragment(
                com.example.myno.jz.ui.budget.BudgetManageFragment()
            )
        }
    }
}
private fun toggleQuickActionManagementMode() {

    isQuickActionManagementMode =
        !isQuickActionManagementMode

    quickActionAdapter.setManagementMode(
        isQuickActionManagementMode
    )

    updateQuickManagementBackground(animate = true)
    binding.cardQuickActions.post {
        syncQuickBackgroundSize()
    }

    if (isQuickActionManagementMode) {

        binding.tvQuickManage.text =
            "完成"

        binding.tvQuickAdd.visibility =
            View.VISIBLE

    } else {

        binding.tvQuickManage.text =
            "管理"

        binding.tvQuickAdd.visibility =
            View.GONE

        saveQuickActions()
    }
}
private fun hideQuickAction(
    action: QuickAction
) {

    val current =
        quickActionAdapter
            .getActions()
            .toMutableList()

    val index =
        current.indexOfFirst {
            it.id == action.id
        }

    if (index < 0) {
        return
    }

    current.removeAt(index)

    quickActionAdapter.submitList(
        current
    )
    updateQuickActionEmptyState()
    updateQuickManagementBackground(animate = false)

    saveQuickActions()

    Toast.makeText(
        requireContext(),
        "${action.title}已隐藏",
        Toast.LENGTH_SHORT
    ).show()
}
private fun setupQuickActionDrag() {

    val callback =
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or
                    ItemTouchHelper.RIGHT,
            0
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                if (!isQuickActionManagementMode) {
                    return false
                }

                val from =
                    viewHolder.bindingAdapterPosition

                val to =
                    target.bindingAdapterPosition

                if (
                    from == RecyclerView.NO_POSITION ||
                    to == RecyclerView.NO_POSITION
                ) {
                    return false
                }

                quickActionAdapter.moveItem(
                    from,
                    to
                )

                return true
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {

                super.clearView(
                    recyclerView,
                    viewHolder
                )

                if (isQuickActionManagementMode) {
                    saveQuickActions()
                }
            }
        }

    ItemTouchHelper(callback)
        .attachToRecyclerView(
            binding.recyclerQuickActions
        )
}
private fun saveQuickActions() {

    val visibleActions =
        quickActionAdapter
            .getActions()

    val visibleIds =
        visibleActions
            .map {
                it.id
            }
            .toSet()

    val savedActions =
        allQuickActions.mapIndexed { index, action ->

            val visible =
                visibleIds.contains(
                    action.id
                )

            val currentOrder =
                visibleActions
                    .indexOfFirst {
                        it.id == action.id
                    }

            QuickAction(
                id = action.id,
                title = action.title,
                iconRes = action.iconRes,
                order =
                    if (currentOrder >= 0) {
                        currentOrder
                    } else {
                        index + 100
                    },
                visible = visible
            )
        }

    quickActionStore.save(
        savedActions
    )
}
private fun showAddQuickActionDialog() {

    val currentIds =
        quickActionAdapter
            .getActions()
            .map {
                it.id
            }
            .toSet()

    val hiddenActions =
        allQuickActions
            .filter {
                !currentIds.contains(
                    it.id
                )
            }

    if (hiddenActions.isEmpty()) {

        Toast.makeText(
            requireContext(),
            "没有可添加的快捷方式",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    val titles =
        hiddenActions
            .map {
                it.title
            }
            .toTypedArray()

    AlertDialog.Builder(
        requireContext()
    )
        .setTitle("添加快捷方式")
        .setItems(
            titles
        ) { dialog, which ->

            val action =
                hiddenActions[which]

            addQuickAction(
                action
            )

            dialog.dismiss()
        }
        .setNegativeButton(
            "取消",
            null
        )
        .show()
}
private fun addQuickAction(
    action: QuickAction
) {

    val current =
        quickActionAdapter
            .getActions()
            .toMutableList()

    if (
        current.any {
            it.id == action.id
        }
    ) {
        return
    }

    current.add(
        action.copy(
            visible = true,
            order = current.size
        )
    )

    quickActionAdapter.submitList(
        current
    )
    updateQuickActionEmptyState()
    updateQuickManagementBackground(animate = false)

    saveQuickActions()

    Toast.makeText(
        requireContext(),
        "${action.title}已添加",
        Toast.LENGTH_SHORT
    ).show()
}


    /**
     * 近期账单列表。
     */
    private fun setupRecyclerView() {

        adapter = RecentBillAdapter { bill ->
            openHomeSubFragment(
                com.example.myno.jz.ui.bills.BillDetailFragment.newInstance(bill.id)
            )
        }

        binding.recyclerTodayBills.adapter = adapter
    }

    /**
     * 首页点击事件。
     */
    private fun setupClickListeners() {

        

        /*
         * 本月结余卡片翻转
         */
        binding.cardMonthBalanceFront.setOnClickListener {
            flipBalanceCard()
        }

        binding.cardMonthBalanceBack.setOnClickListener {
            flipBalanceCard()
        }

        /*
         * 金额显示 / 隐藏
         */
        binding.btnToggleMoneyVisibility.setOnClickListener {

            isMoneyVisible = !isMoneyVisible

            updateMoneyVisibility()
        }

        /*
         * 月份选择
         */
        binding.tvCurrentMonth.setOnClickListener {
            showMonthPicker()
        }

        /*
         * 翻转卡背面的“查看全部”
         */
        binding.tvBudgetDetail.setOnClickListener {
            showCategoryBudgetDialog()
        }

        /*
         * 今日全部
         */
        binding.tvTodayViewAll.setOnClickListener {
            openHomeSubFragment(
                com.example.myno.jz.ui.bills.BillsFragment()
            )
        }

        /*
         * 近期账单全部
         */
        binding.tvRecentViewAll.setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    com.example.myno.jz.ui.bills.BillsFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        /*
         * 本月支出分类详情
         */
        binding.tvCategoryViewAll.setOnClickListener {
            openHomeSubFragment(
                com.example.myno.jz.ui.statistics.StatisticsFragment()
            )
        }
    }

    private fun openHomeSubFragment(fragment: Fragment) {
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * 监听账单、分类数据变化。
     */
    private fun observeData() {

        viewModel.bills.observe(
            viewLifecycleOwner
        ) {
            updateHome()
        }

        viewModel.categories.observe(
            viewLifecycleOwner
        ) {
            updateHome()
        }
    }

    /**
     * 更新首页全部数据。
     */
    private fun updateHome() {

        if (_binding == null) {
            return
        }

        val bills =
            viewModel.bills.value ?: emptyList()

        val categories =
            viewModel.categories.value ?: emptyList()

        updateMonthSummary(bills)

        updateTodaySummary(bills)

        updateRecentBills(
            bills,
            categories
        )

        updateBudget()

        updateCategorySummary(
            bills,
            categories
        )

        updateMonthTitle()
    }

    /**
     * 本月结余 ⇄ 本月预算
     */
    private fun flipBalanceCard() {

        if (isBalanceFlipped) {

            binding.monthBalanceFlipper.setInAnimation(
                requireContext(),
                android.R.anim.fade_in
            )

            binding.monthBalanceFlipper.setOutAnimation(
                requireContext(),
                android.R.anim.fade_out
            )

            binding.monthBalanceFlipper.showPrevious()

            isBalanceFlipped = false

        } else {

            binding.monthBalanceFlipper.setInAnimation(
                requireContext(),
                android.R.anim.fade_in
            )

            binding.monthBalanceFlipper.setOutAnimation(
                requireContext(),
                android.R.anim.fade_out
            )

            binding.monthBalanceFlipper.showNext()

            isBalanceFlipped = true
        }
    }

    /**
     * 月份选择。
     */
    private fun showMonthPicker() {

        val months =
            FinanceCalculator.getRecentMonths(24)

        val labels =
            months.map { pair ->

                val year = pair.first
                val month = pair.second

                "${year}年${month + 1}月"
            }

        var selectedIndex =
            months.indexOfFirst {

                it.first == selectedYear &&
                        it.second == selectedMonth
            }

        if (selectedIndex < 0) {
            selectedIndex = 0
        }

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("选择月份")
            .setSingleChoiceItems(
                labels.toTypedArray(),
                selectedIndex
            ) { dialog, which ->

                val selected =
                    months[which]

                selectedYear =
                    selected.first

                selectedMonth =
                    selected.second

                updateMonthTitle()
                updateHome()

                dialog.dismiss()
            }
            .setNegativeButton(
                "取消",
                null
            )
            .show()
    }

    /**
     * 更新月份标题。
     */
    private fun updateMonthTitle() {

        binding.tvCurrentMonth.text =
            "${selectedYear}年${selectedMonth + 1}月"
    }

    /**
     * 更新本月结余、收入、支出。
     */
    private fun updateMonthSummary(
        bills: List<Bill>
    ) {

        val start =
            FinanceCalculator.getStartOfMonth(
                selectedYear,
                selectedMonth
            )

        val end =
            FinanceCalculator.getEndOfMonth(
                selectedYear,
                selectedMonth
            )

        val income =
            FinanceCalculator.calculateIncome(
                bills,
                start,
                end
            )

        val expense =
            FinanceCalculator.calculateExpense(
                bills,
                start,
                end
            )

        val balance =
            income - expense

        if (isMoneyVisible) {

            binding.tvMonthBalance.text =
                formatMoney(balance)

            binding.tvMonthIncome.text =
                formatMoney(income)

            binding.tvMonthExpense.text =
                formatMoney(expense)

        } else {

            binding.tvMonthBalance.text =
                "¥ ****.**"

            binding.tvMonthIncome.text =
                "¥ ****.**"

            binding.tvMonthExpense.text =
                "¥ ****.**"
        }
    }

    /**
     * 更新金额显示状态。
     */
    private fun updateMoneyVisibility() {

        if (isMoneyVisible) {

            binding.btnToggleMoneyVisibility.setImageResource(
                R.drawable.ic_visibility
            )

            binding.btnToggleMoneyVisibility.contentDescription =
                "隐藏金额"

        } else {

            binding.btnToggleMoneyVisibility.setImageResource(
                R.drawable.ic_visibility_off
            )

            binding.btnToggleMoneyVisibility.contentDescription =
                "显示金额"
        }

        updateMonthSummary(
            viewModel.bills.value ?: emptyList()
        )
    }

    /**
     * 更新今日收支。
     */
    private fun updateTodaySummary(
        bills: List<Bill>
    ) {

        val start =
            FinanceCalculator.getStartOfToday()

        val end =
            FinanceCalculator.getEndOfToday()

        val todayBills =
            bills.filter {

                it.timestamp >= start &&
                        it.timestamp <= end
            }

        val income =
            todayBills
                .filter {
                    it.type == BillType.INCOME
                }
                .sumOf {
                    it.amount
                }

        val expense =
            todayBills
                .filter {
                    it.type == BillType.EXPENSE
                }
                .sumOf {
                    it.amount
                }

        val balance =
            income - expense

        binding.tvTodayIncome.text =
            formatMoney(income)

        binding.tvTodayExpense.text =
            formatMoney(expense)

        binding.tvTodayBalance.text =
            formatMoney(balance)
    }

    /**
     * 更新近期账单。
     *
     * 首页这里展示全局最近 4 笔账单，而不是只展示“今天”的账单。
     * 新增、编辑或删除账单后，LiveData 会触发 updateHome()，因此列表会实时刷新。
     */
    private fun updateRecentBills(
        bills: List<Bill>,
        categories: List<Category>
    ) {
        val recentBills =
            bills
                .sortedByDescending { it.timestamp }
                .take(4)

        if (recentBills.isEmpty()) {
            binding.tvEmptyToday.visibility = View.VISIBLE
            binding.recyclerTodayBills.visibility = View.GONE
            return
        }

        binding.tvEmptyToday.visibility = View.GONE
        binding.recyclerTodayBills.visibility = View.VISIBLE
        adapter.submitData(recentBills, categories)
    }

    /**
     * 更新本月预算。
     *
     * 注意：
     * 现在预算已经不是独立卡片，
     * 而是显示在 monthBalanceFlipper 的背面。
     */
    private fun updateBudget() {

        val repository =
            viewModel.getRepository()

        val budgets =
            repository
                .getBudgets()
                .filter {

                    it.year == selectedYear &&
                            it.month == selectedMonth + 1 &&
                            it.enabled
                }

        val bills =
            viewModel.bills.value
                ?: emptyList()

        val categories =
            viewModel.categories.value
                ?: emptyList()

        val start =
            FinanceCalculator.getStartOfMonth(
                selectedYear,
                selectedMonth
            )

        val end =
            FinanceCalculator.getEndOfMonth(
                selectedYear,
                selectedMonth
            )

        val expenseBills =
            bills.filter {

                it.type == BillType.EXPENSE &&
                        it.timestamp >= start &&
                        it.timestamp <= end
            }

        val totalBudget =
            budgets.firstOrNull {
                it.type == BudgetType.TOTAL
            }

        /*
         * 没有设置总预算。
         */
        if (totalBudget == null) {

            binding.tvBudgetUsed.text =
                "暂未设置"

            binding.tvBudgetTotal.text =
                "¥0.00"

            binding.tvBudgetPercent.text =
                "—"

            binding.progressBudget.progress =
                0

            binding.tvBudgetDescription.text =
                "设置本月预算后，可以查看消费进度"

            binding.tvBudgetRemaining.text =
                "点击查看分类预算"

            binding.layoutCategoryBudgets.removeAllViews()

            return
        }

        val expense =
            expenseBills.sumOf {
                it.amount
            }

        val budgetAmount =
            totalBudget.amount

        val remaining =
            budgetAmount - expense

        val percent =
            if (budgetAmount > 0.0) {

                expense /
                        budgetAmount *
                        100.0

            } else {
                0.0
            }

        val safeProgress =
            percent
                .coerceAtLeast(0.0)
                .coerceAtMost(100.0)
                .toInt()

        binding.tvBudgetUsed.text =
            formatMoney(expense)

        binding.tvBudgetTotal.text =
            formatMoney(budgetAmount)

        binding.tvBudgetPercent.text =
            String.format(
                Locale.CHINA,
                "%.0f%%",
                percent
            )

        binding.progressBudget.progress =
            safeProgress

        when {

            percent >= 100.0 -> {

                binding.tvBudgetDescription.text =
                    "已超出本月预算"

                binding.tvBudgetRemaining.text =
                    "超支 ${formatMoney(-remaining)}"
            }

            percent >= totalBudget.warningPercent -> {

                binding.tvBudgetDescription.text =
                    "预算使用接近上限"

                binding.tvBudgetRemaining.text =
                    "剩余 ${formatMoney(remaining)}"
            }

            else -> {

                binding.tvBudgetDescription.text =
                    "本月预算使用情况"

                binding.tvBudgetRemaining.text =
                    "剩余 ${formatMoney(remaining)}"
            }
        }

        updateCategoryBudgetPreview(
            budgets,
            expenseBills,
            categories
        )
    }

    /**
     * 在“本月预算”翻转卡内部显示分类预算。
     */
    private fun updateCategoryBudgetPreview(
        budgets: List<com.example.myno.jz.data.model.Budget>,
        expenseBills: List<Bill>,
        categories: List<Category>
    ) {

        binding.layoutCategoryBudgets.removeAllViews()

        val categoryBudgets =
            budgets
                .filter {
                    it.type == BudgetType.CATEGORY
                }
                .take(5)

        if (categoryBudgets.isEmpty()) {
            return
        }

        categoryBudgets.forEach { budget ->

            val category =
                categories.firstOrNull {
                    it.id == budget.categoryId
                }

            val categoryName =
                category?.name ?: "其他"

            val used =
                expenseBills
                    .filter {
                        it.categoryId == budget.categoryId
                    }
                    .sumOf {
                        it.amount
                    }

            val percent =
                if (budget.amount > 0.0) {

                    used /
                            budget.amount *
                            100.0

                } else {
                    0.0
                }

            val item =
                LinearLayout(
                    requireContext()
                ).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        dpToPx(14),
                        dpToPx(10),
                        dpToPx(14),
                        dpToPx(10)
                    )

                    setBackgroundColor(
                        requireContext().getColor(
                            R.color.background
                        )
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            dpToPx(145),
                            dpToPx(72)
                        ).apply {

                            marginEnd =
                                dpToPx(8)
                        }
                }

            val title =
                TextView(
                    requireContext()
                ).apply {

                    text =
                        categoryName

                    textSize =
                        12f

                    setTextColor(
                        requireContext().getColor(
                            R.color.text_primary
                        )
                    )

                    gravity =
                        Gravity.CENTER
                }

            val amount =
                TextView(
                    requireContext()
                ).apply {

                    text =
                        "${formatMoney(used)} / ${formatMoney(budget.amount)}"

                    textSize =
                        10f

                    setTextColor(
                        requireContext().getColor(
                            R.color.text_secondary
                        )
                    )

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        0,
                        dpToPx(3),
                        0,
                        0
                    )
                }

            val progress =
                ProgressBar(
                    requireContext(),
                    null,
                    android.R.attr.progressBarStyleHorizontal
                ).apply {

                    max = 100

                    this.progress =
                        percent
                            .coerceAtLeast(0.0)
                            .coerceAtMost(100.0)
                            .toInt()

                    layoutParams =
                        LinearLayout.LayoutParams(
                            dpToPx(115),
                            dpToPx(5)
                        ).apply {

                            topMargin =
                                dpToPx(5)
                        }
                }

            item.addView(title)
            item.addView(amount)
            item.addView(progress)

            item.setOnClickListener {
                showCategoryBudgetDialog()
            }

            binding.layoutCategoryBudgets.addView(
                item
            )
        }
    }

    /**
     * 显示完整分类预算。
     */
    private fun showCategoryBudgetDialog() {

        val repository =
            viewModel.getRepository()

        val budgets =
            repository
                .getBudgets()
                .filter {

                    it.year == selectedYear &&
                            it.month == selectedMonth + 1 &&
                            it.enabled
                }

        val categoryBudgets =
            budgets.filter {

                it.type == BudgetType.CATEGORY
            }

        val categories =
            viewModel.categories.value
                ?: emptyList()

        val bills =
            viewModel.bills.value
                ?: emptyList()

        val start =
            FinanceCalculator.getStartOfMonth(
                selectedYear,
                selectedMonth
            )

        val end =
            FinanceCalculator.getEndOfMonth(
                selectedYear,
                selectedMonth
            )

        val expenseBills =
            bills.filter {

                it.type == BillType.EXPENSE &&
                        it.timestamp >= start &&
                        it.timestamp <= end
            }

        if (categoryBudgets.isEmpty()) {

            AlertDialog.Builder(
                requireContext()
            )
                .setTitle(
                    "${selectedYear}年${selectedMonth + 1}月分类预算"
                )
                .setMessage(
                    "当前月份还没有设置分类预算。\n\n" +
                            "可以为餐饮、购物、交通、居住等分类分别设置预算。"
                )
                .setPositiveButton(
                    "知道了",
                    null
                )
                .show()

            return
        }

        val container =
            LinearLayout(
                requireContext()
            ).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dpToPx(20),
                    dpToPx(4),
                    dpToPx(20),
                    dpToPx(8)
                )
            }

        categoryBudgets
            .sortedBy {

                categories
                    .firstOrNull { category ->
                        category.id == it.categoryId
                    }
                    ?.sortOrder
                    ?: Int.MAX_VALUE
            }
            .forEach { budget ->

                val category =
                    categories.firstOrNull {
                        it.id == budget.categoryId
                    }

                val categoryName =
                    category?.name ?: "其他"

                val usedAmount =
                    expenseBills
                        .filter {
                            it.categoryId ==
                                    budget.categoryId
                        }
                        .sumOf {
                            it.amount
                        }

                val remaining =
                    budget.amount - usedAmount

                val percent =
                    if (budget.amount > 0.0) {

                        usedAmount /
                                budget.amount *
                                100.0

                    } else {
                        0.0
                    }

                val title =
                    TextView(
                        requireContext()
                    ).apply {

                        text =
                            categoryName

                        textSize =
                            15f

                        setTextColor(
                            requireContext().getColor(
                                R.color.text_primary
                            )
                        )

                        setTypeface(
                            null,
                            android.graphics.Typeface.BOLD
                        )
                    }

                val amount =
                    TextView(
                        requireContext()
                    ).apply {

                        text =
                            "${formatMoney(usedAmount)} / ${
                                formatMoney(budget.amount)
                            }"

                        textSize =
                            12f

                        setTextColor(
                            requireContext().getColor(
                                R.color.text_secondary
                            )
                        )

                        gravity =
                            Gravity.END
                    }

                val titleLayout =
                    LinearLayout(
                        requireContext()
                    ).apply {

                        orientation =
                            LinearLayout.HORIZONTAL

                        gravity =
                            Gravity.CENTER_VERTICAL

                        addView(
                            title,
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        )

                        addView(
                            amount
                        )
                    }

                val progress =
                    ProgressBar(
                        requireContext(),
                        null,
                        android.R.attr.progressBarStyleHorizontal
                    ).apply {

                        max = 100

                        this.progress =
                            percent
                                .coerceAtLeast(0.0)
                                .coerceAtMost(100.0)
                                .toInt()

                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                dpToPx(7)
                            ).apply {

                                topMargin =
                                    dpToPx(8)
                            }
                    }

                val description =
                    TextView(
                        requireContext()
                    ).apply {

                        textSize =
                            12f

                        setPadding(
                            0,
                            dpToPx(5),
                            0,
                            0
                        )

                        when {

                            percent >= 100.0 -> {

                                text =
                                    "⚠ 已超支 ${
                                        formatMoney(-remaining)
                                    }"

                                setTextColor(
                                    requireContext().getColor(
                                        R.color.expense_color
                                    )
                                )
                            }

                            percent >= budget.warningPercent -> {

                                text =
                                    "已用 ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.0f",
                                            percent
                                        )
                                    }%，剩余 ${
                                        formatMoney(remaining)
                                    }"

                                setTextColor(
                                    requireContext().getColor(
                                        R.color.expense_color
                                    )
                                )
                            }

                            else -> {

                                text =
                                    "已用 ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.0f",
                                            percent
                                        )
                                    }%，剩余 ${
                                        formatMoney(remaining)
                                    }"

                                setTextColor(
                                    requireContext().getColor(
                                        R.color.text_secondary
                                    )
                                )
                            }
                        }
                    }

                container.addView(
                    titleLayout
                )

                container.addView(
                    progress
                )

                container.addView(
                    description
                )

                val divider =
                    View(
                        requireContext()
                    ).apply {

                        setBackgroundColor(
                            requireContext().getColor(
                                R.color.divider
                            )
                        )

                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                dpToPx(1)
                            ).apply {

                                topMargin =
                                    dpToPx(14)

                                bottomMargin =
                                    dpToPx(14)
                            }
                    }

                container.addView(
                    divider
                )
            }

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(
                "${selectedYear}年${selectedMonth + 1}月分类预算"
            )
            .setView(container)
            .setPositiveButton(
                "关闭",
                null
            )
            .show()
    }

    /**
     * 更新本月支出分类。
     *
     * 数据来源为当前选择月份的真实账单：按 categoryId 汇总支出金额，
     * 环形图的每个扇区和右侧明细均由同一份汇总数据实时计算。
     */
    private fun updateCategorySummary(
        bills: List<Bill>,
        categories: List<Category>
    ) {
        val start = FinanceCalculator.getStartOfMonth(
            selectedYear,
            selectedMonth
        )
        val end = FinanceCalculator.getEndOfMonth(
            selectedYear,
            selectedMonth
        )

        val expenseBills = bills.filter {
            it.type == BillType.EXPENSE &&
                it.timestamp >= start &&
                it.timestamp <= end
        }

        val totalExpense = expenseBills.sumOf { it.amount }
        binding.tvCategoryTotalAmount.text = formatMoney(totalExpense)
        binding.layoutCategorySummary.removeAllViews()

        if (expenseBills.isEmpty()) {
            binding.ivCategoryChart.setSegments(emptyList())
            val empty = TextView(requireContext()).apply {
                text = "本月还没有支出记录"
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                setPadding(0, dpToPx(6), 0, dpToPx(6))
            }
            binding.layoutCategorySummary.addView(empty)
            return
        }

        val palette = listOf(
            android.graphics.Color.rgb(66, 126, 245),
            android.graphics.Color.rgb(72, 190, 145),
            android.graphics.Color.rgb(249, 174, 67),
            android.graphics.Color.rgb(133, 92, 224),
            android.graphics.Color.rgb(236, 103, 103),
            android.graphics.Color.rgb(71, 184, 205)
        )

        val categoryAmounts = expenseBills
            .groupBy { it.categoryId }
            .map { (categoryId, categoryBills) ->
                val category = categories.firstOrNull { it.id == categoryId }
                category to categoryBills.sumOf { it.amount }
            }
            .sortedByDescending { it.second }

        val chartSegments = categoryAmounts.mapIndexed { index, item ->
            CategoryDonutChartView.Segment(
                amount = item.second,
                color = palette[index % palette.size]
            )
        }
        binding.ivCategoryChart.setSegments(chartSegments)

        categoryAmounts.forEachIndexed { index, item ->
            val category = item.first
            val amount = item.second
            val percent = if (totalExpense > 0.0) {
                amount / totalExpense * 100.0
            } else {
                0.0
            }
            val color = palette[index % palette.size]

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(5), 0, dpToPx(5))
            }

            val dot = View(requireContext()).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(color)
                }
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(9),
                    dpToPx(9)
                )
            }

            val name = TextView(requireContext()).apply {
                text = category?.name ?: "其他"
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.text_primary))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(8)
                }
            }

            val amountText = TextView(requireContext()).apply {
                text = formatMoney(amount)
                textSize = 11f
                setTextColor(requireContext().getColor(R.color.text_primary))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(72),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val percentText = TextView(requireContext()).apply {
                text = String.format(Locale.CHINA, "%.1f%%", percent)
                textSize = 11f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(48),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            row.addView(dot)
            row.addView(name)
            row.addView(amountText)
            row.addView(percentText)
            binding.layoutCategorySummary.addView(row)
        }
    }

    /**
     * 根据分类名称复用现有分类图片资源。
     */
    private fun getCategoryIcon(
        categoryName: String?
    ): Int {

        return when (categoryName) {

            "餐饮",
            "吃饭",
            "美食" ->
                R.drawable.ic_category_food

            "购物" ->
                R.drawable.ic_category_shopping

            "交通" ->
                R.drawable.ic_category_transport

            "工资" ->
                R.drawable.ic_category_salary

            "居住",
            "住房",
            "房租" ->
                R.drawable.ic_category_house

            "医疗" ->
                R.drawable.ic_category_medical

            "教育" ->
                R.drawable.ic_category_education

            "娱乐" ->
                R.drawable.ic_category_entertainment

            "通讯" ->
                R.drawable.ic_category_communication

            "旅行",
            "旅游" ->
                R.drawable.ic_category_travel

            else ->
                R.drawable.ic_category_other
        }
    }

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
            dp *
                resources.displayMetrics.density
            ).toInt()
    }

    private fun formatMoney(
        amount: Double
    ): String {

        return String.format(
            Locale.CHINA,
            "¥%.2f",
            amount
        )
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