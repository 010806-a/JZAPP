package com.example.myno.jz.ui.bills

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.databinding.FragmentBillsBinding
import com.example.myno.jz.ui.main.MainViewModel
import com.example.myno.jz.utils.FinanceCalculator
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class BillsFragment : Fragment() {

    private var _binding: FragmentBillsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var adapter: BillAdapter

    private var allBills: List<Bill> = emptyList()

    /**
     * 当前查看月份
     */
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0

    /**
     * 精确筛选
     *
     * 日期 + 开始时间 + 结束时间
     * 时间精确到分钟
     */
    private var filterDate: Calendar? = null

    private var filterStartMinute: Int = 0

    private var filterEndMinute: Int =
        23 * 60 + 59

    /**
     * FAB 拖动状态
     */
    private var isDraggingFab = false

    private var fabDownX = 0f
    private var fabDownY = 0f

    private var fabStartTranslationX = 0f
    private var fabStartTranslationY = 0f

    private var fabLongPressRunnable: Runnable? = null

    private val fabLongPressDuration = 420L

    private val fabPreferences by lazy {
        requireContext().getSharedPreferences(
            "bill_fab_position",
            Context.MODE_PRIVATE
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val calendar = Calendar.getInstance()

        selectedYear =
            calendar.get(Calendar.YEAR)

        selectedMonth =
            calendar.get(Calendar.MONTH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentBillsBinding.inflate(
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

        setupFilter()

        setupFilterButton()

        setupAddButton()

        setupImportButton()

        setupMonthSelector()

        observeData()

        updateMonthTitle()

        updateFilterButtonState()
    }

    /**
     * RecyclerView
     */
    private fun setupRecyclerView() {

        adapter =
            BillAdapter(
                bills = emptyList(),
                categories = emptyList()
            ) { bill ->

                openBillDetail(bill)
            }

        binding.recyclerBills.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.recyclerBills.adapter =
            adapter
    }

    /**
     * 打开账单详情
     */
    private fun openBillDetail(
        bill: Bill
    ) {

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                BillDetailFragment.newInstance(
                    bill.id
                )
            )
            .addToBackStack(null)
            .commit()
    }

    /**
     * ============================================================
     * 账单类型筛选
     * ============================================================
     *
     * 全部
     * 支出
     * 收入
     *
     * 这里统一由 ChipGroup 管理。
     *
     * 选中：
     * 蓝色背景 + 白色文字
     *
     * 未选中：
     * 浅色背景 + 深色文字
     *
     * 实际账单列表也会同步过滤。
     */
    private fun setupFilter() {

        binding.chipGroup.isSingleSelection = true

        binding.chipGroup.isSelectionRequired = true

        /*
         * 不再给三个 Chip 单独设置点击逻辑。
         *
         * 统一监听 ChipGroup。
         */
        binding.chipGroup.setOnCheckedStateChangeListener {
                _,
                checkedIds ->

            /*
             * 理论上 selectionRequired=true 后不会为空。
             *
             * 保险处理：
             * 如果没有选中，则自动恢复“全部”。
             */
            if (checkedIds.isEmpty()) {

                binding.chipGroup.check(
                    R.id.chipAll
                )

                return@setOnCheckedStateChangeListener
            }

            /*
             * Chip 状态发生变化以后，
             * 立即刷新实际账单列表。
             */
            applyCurrentFilters()
        }

        /*
         * 默认选中“全部”
         */
        binding.chipGroup.check(
            R.id.chipAll
        )

        /*
         * 再主动刷新一次。
         *
         * 防止首次进入页面时，
         * Chip 已经选中，但 RecyclerView
         * 没有立即按照当前状态刷新。
         */
        applyCurrentFilters()
    }

    /**
     * 获取当前选中的账单类型
     */
    private fun getSelectedBillType(): BillType? {

        return when (
            binding.chipGroup.checkedChipId
        ) {

            R.id.chipExpense ->
                BillType.EXPENSE

            R.id.chipIncome ->
                BillType.INCOME

            else ->
                null
        }
    }

    /**
     * 精确筛选按钮
     */
    private fun setupFilterButton() {

        binding.btnFilter.setOnClickListener {

            if (filterDate == null) {

                showPreciseFilterDialog()

            } else {

                showActiveFilterDialog()
            }
        }
    }

    /**
     * 显示当前精确筛选
     */
    private fun showActiveFilterDialog() {

        val date =
            filterDate ?: return

        val dateText =
            String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH)
            )

        val startText =
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                filterStartMinute / 60,
                filterStartMinute % 60
            )

        val endText =
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                filterEndMinute / 60,
                filterEndMinute % 60
            )

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("当前筛选")
            .setMessage(
                "日期：$dateText\n" +
                        "时间：$startText - $endText\n\n" +
                        "当前账单列表已应用上述精确筛选条件。"
            )
            .setNegativeButton(
                "清除筛选"
            ) { _, _ ->

                clearPreciseFilter()
            }
            .setPositiveButton(
                "修改筛选"
            ) { _, _ ->

                showPreciseFilterDialog()
            }
            .show()
    }

    /**
     * ============================================================
     * 精确日期筛选
     * ============================================================
     */
    private fun showPreciseFilterDialog() {

        val initial =
            (filterDate
                ?: Calendar.getInstance())
                .clone() as Calendar

        initial.set(
            Calendar.YEAR,
            selectedYear
        )

        initial.set(
            Calendar.MONTH,
            selectedMonth
        )

        initial.set(
            Calendar.DAY_OF_MONTH,
            minOf(
                initial.get(Calendar.DAY_OF_MONTH),
                initial.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )
            )
        )

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                val pickedDate =
                    Calendar.getInstance().apply {

                        set(
                            Calendar.YEAR,
                            year
                        )

                        set(
                            Calendar.MONTH,
                            month
                        )

                        set(
                            Calendar.DAY_OF_MONTH,
                            dayOfMonth
                        )

                        set(
                            Calendar.HOUR_OF_DAY,
                            0
                        )

                        set(
                            Calendar.MINUTE,
                            0
                        )

                        set(
                            Calendar.SECOND,
                            0
                        )

                        set(
                            Calendar.MILLISECOND,
                            0
                        )
                    }

                /*
                 * 精确筛选日期必须属于
                 * 当前选择的月份。
                 */
                if (
                    year != selectedYear ||
                    month != selectedMonth
                ) {

                    Toast.makeText(
                        requireContext(),
                        "筛选日期必须在当前选择月份内",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@DatePickerDialog
                }

                showFilterTimePicker(
                    pickedDate,
                    true
                )
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        ).apply {

            setTitle("选择筛选日期")

            val monthStart =
                FinanceCalculator.getStartOfMonth(
                    selectedYear,
                    selectedMonth
                )

            val monthEnd =
                FinanceCalculator.getEndOfMonth(
                    selectedYear,
                    selectedMonth
                )

            datePicker.minDate =
                monthStart

            datePicker.maxDate =
                monthEnd

            show()
        }
    }

    /**
     * 开始 / 结束时间选择
     */
    private fun showFilterTimePicker(
        date: Calendar,
        isStart: Boolean
    ) {

        val minute =
            if (isStart) {

                filterStartMinute

            } else {

                filterEndMinute
            }

        val hour =
            minute / 60

        val min =
            minute % 60

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->

                val selected =
                    selectedHour * 60 +
                            selectedMinute

                if (isStart) {

                    filterStartMinute =
                        selected

                    showFilterTimePicker(
                        date,
                        false
                    )

                } else {

                    /*
                     * 结束时间必须 >= 开始时间
                     */
                    if (
                        filterStartMinute >
                        selected
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "结束时间不能早于开始时间",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@TimePickerDialog
                    }

                    filterEndMinute =
                        selected

                    filterDate =
                        date

                    updateFilterButtonState()

                    applyCurrentFilters()
                }
            },
            hour,
            min,
            true
        ).apply {

            setTitle(
                if (isStart) {
                    "选择开始时间（精确到分钟）"
                } else {
                    "选择结束时间（精确到分钟）"
                }
            )

            show()
        }
    }

    /**
     * 清除精确筛选
     */
    private fun clearPreciseFilter() {

        filterDate = null

        filterStartMinute = 0

        filterEndMinute =
            23 * 60 + 59

        updateFilterButtonState()

        applyCurrentFilters()
    }

    /**
     * 更新筛选按钮显示
     */
    private fun updateFilterButtonState() {

        val active =
            filterDate != null

        binding.tvFilterLabel.text =
            if (active) {
                "已筛选"
            } else {
                "筛选"
            }

        val color =
            requireContext().getColor(
                if (active) {
                    R.color.primary
                } else {
                    R.color.text_primary
                }
            )

        binding.tvFilterLabel.setTextColor(
            color
        )

        binding.ivFilterIcon.setColorFilter(
            color
        )
    }

    /**
     * ============================================================
     * 月份选择
     * ============================================================
     */
    private fun setupMonthSelector() {

        binding.tvMonth.setOnClickListener {

            showMonthPicker()
        }
    }

    private fun showMonthPicker() {

        val months =
            FinanceCalculator.getRecentMonths(
                24
            )

        val labels =
            months.map { pair ->

                "${pair.first}年${pair.second + 1}月"
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

                /*
                 * 切换月份时清除日期筛选，
                 * 防止上一个月份的日期继续生效。
                 */
                clearPreciseFilter()

                updateMonthTitle()

                refreshSelectedMonth()

                dialog.dismiss()
            }
            .setNegativeButton(
                "取消",
                null
            )
            .show()
    }

    private fun updateMonthTitle() {

        binding.tvMonth.text =
            "${selectedYear}年${selectedMonth + 1}月"
    }

    /**
     * ============================================================
     * 获取当前月份账单
     * ============================================================
     */
    private fun getSelectedMonthBills(
        bills: List<Bill>
    ): List<Bill> {

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

        return bills.filter {

            it.timestamp >= start &&
                    it.timestamp <= end
        }
    }

    /**
     * 刷新当前月份
     */
    private fun refreshSelectedMonth() {

        updateSummary()

        applyCurrentFilters()
    }

    /**
     * ============================================================
     * 核心筛选逻辑
     * ============================================================
     *
     * 顺序：
     *
     * 1. 当前月份
     * 2. 全部 / 支出 / 收入
     * 3. 精确日期
     * 4. 精确时间
     */
    private fun applyCurrentFilters() {

        /*
         * 第一步：
         * 先获取当前月份账单
         */
        var filtered =
            getSelectedMonthBills(
                allBills
            )

        /*
         * 第二步：
         * 根据 Chip 类型过滤
         */
        when (
            getSelectedBillType()
        ) {

            BillType.EXPENSE -> {

                filtered =
                    filtered.filter {

                        it.type ==
                                BillType.EXPENSE
                    }
            }

            BillType.INCOME -> {

                filtered =
                    filtered.filter {

                        it.type ==
                                BillType.INCOME
                    }
            }

            null -> {
                /*
                 * 全部，不过滤
                 */
            }
        }

        /*
         * 第三步：
         * 精确日期 + 时间过滤
         */
        val selectedDate =
            filterDate

        if (selectedDate != null) {

            val dayStart =
                (selectedDate.clone()
                        as Calendar)
                    .apply {

                        set(
                            Calendar.HOUR_OF_DAY,
                            filterStartMinute / 60
                        )

                        set(
                            Calendar.MINUTE,
                            filterStartMinute % 60
                        )

                        set(
                            Calendar.SECOND,
                            0
                        )

                        set(
                            Calendar.MILLISECOND,
                            0
                        )
                    }
                    .timeInMillis

            val dayEnd =
                (selectedDate.clone()
                        as Calendar)
                    .apply {

                        set(
                            Calendar.HOUR_OF_DAY,
                            filterEndMinute / 60
                        )

                        set(
                            Calendar.MINUTE,
                            filterEndMinute % 60
                        )

                        set(
                            Calendar.SECOND,
                            59
                        )

                        set(
                            Calendar.MILLISECOND,
                            999
                        )
                    }
                    .timeInMillis

            filtered =
                filtered.filter {

                    it.timestamp in
                            dayStart..dayEnd
                }
        }

        /*
         * 最终刷新 RecyclerView
         */
        showBills(filtered)
    }

    /**
     * ============================================================
     * 观察数据
     * ============================================================
     */
    private fun observeData() {

        viewModel.bills.observe(
            viewLifecycleOwner
        ) {

            allBills = it

            updateSummary()

            applyCurrentFilters()
        }

        viewModel.categories.observe(
            viewLifecycleOwner
        ) {

            applyCurrentFilters()
        }
    }

    /**
     * ============================================================
     * 本月收支统计
     * ============================================================
     */
    private fun updateSummary() {

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

        val expense =
            FinanceCalculator.calculateExpense(
                allBills,
                start,
                end
            )

        val income =
            FinanceCalculator.calculateIncome(
                allBills,
                start,
                end
            )

        binding.tvExpense.text =
            String.format(
                Locale.getDefault(),
                "¥%.2f",
                expense
            )

        binding.tvIncome.text =
            String.format(
                Locale.getDefault(),
                "¥%.2f",
                income
            )
    }

    /**
     * ============================================================
     * 显示账单
     * ============================================================
     */
    private fun showBills(
        bills: List<Bill>
    ) {

        val sortedBills =
            bills.sortedByDescending {

                it.timestamp
            }

        adapter.updateData(
            sortedBills,
            viewModel.categories.value
                ?: emptyList()
        )

        /*
         * 空状态文字跟随当前 Chip。
         */
        binding.tvEmptyTitle.text =
            when (
                binding.chipGroup.checkedChipId
            ) {

                R.id.chipExpense ->
                    "暂无支出账单"

                R.id.chipIncome ->
                    "暂无收入账单"

                else ->
                    "暂无账单"
            }

        binding.emptyView.visibility =
            if (sortedBills.isEmpty()) {

                View.VISIBLE

            } else {

                View.GONE
            }

        binding.recyclerBills.visibility =
            if (sortedBills.isEmpty()) {

                View.GONE

            } else {

                View.VISIBLE
            }
    }

    /**
     * ============================================================
     * FAB
     * ============================================================
     */
    private fun setupAddButton() {

        binding.fabAddBill.setOnTouchListener {
                view,
                event ->

            when (
                event.actionMasked
            ) {

                MotionEvent.ACTION_DOWN -> {

                    fabDownX =
                        event.rawX

                    fabDownY =
                        event.rawY

                    fabStartTranslationX =
                        view.translationX

                    fabStartTranslationY =
                        view.translationY

                    isDraggingFab =
                        false

                    fabLongPressRunnable =
                        Runnable {

                            isDraggingFab =
                                true

                            view.animate()
                                .scaleX(1.08f)
                                .scaleY(1.08f)
                                .setDuration(120L)
                                .start()

                            view.performHapticFeedback(
                                android.view.HapticFeedbackConstants.LONG_PRESS
                            )
                        }

                    view.postDelayed(
                        fabLongPressRunnable!!,
                        fabLongPressDuration
                    )

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        event.rawX -
                                fabDownX

                    val dy =
                        event.rawY -
                                fabDownY

                    if (
                        !isDraggingFab &&
                        (
                            abs(dx) > 12f ||
                                    abs(dy) > 12f
                            )
                    ) {

                        cancelFabLongPress()

                        return@setOnTouchListener true
                    }

                    if (isDraggingFab) {

                        moveFab(
                            view,
                            dx,
                            dy
                        )
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    cancelFabLongPress()

                    if (isDraggingFab) {

                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120L)
                            .start()

                        saveFabPosition()

                    } else {

                        openAddBill()
                    }

                    isDraggingFab =
                        false

                    true
                }

                MotionEvent.ACTION_CANCEL -> {

                    cancelFabLongPress()

                    if (isDraggingFab) {

                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120L)
                            .start()

                        saveFabPosition()
                    }

                    isDraggingFab =
                        false

                    true
                }

                else -> true
            }
        }

        binding.fabAddBill.post {

            restoreFabPosition()
        }
    }

    private fun cancelFabLongPress() {

        fabLongPressRunnable?.let {

            binding.fabAddBill.removeCallbacks(
                it
            )
        }

        fabLongPressRunnable = null
    }

    private fun moveFab(
        view: View,
        dx: Float,
        dy: Float
    ) {

        val parent =
            view.parent as? View
                ?: return

        val maxX =
            max(
                0,
                parent.width -
                        view.width
            ).toFloat()

        val maxY =
            max(
                0,
                parent.height -
                        view.height
            ).toFloat()

        val baseLeft =
            view.left.toFloat()

        val baseTop =
            view.top.toFloat()

        val desiredLeft =
            baseLeft +
                    fabStartTranslationX +
                    dx

        val desiredTop =
            baseTop +
                    fabStartTranslationY +
                    dy

        val clampedLeft =
            desiredLeft.coerceIn(
                0f,
                maxX
            )

        val clampedTop =
            desiredTop.coerceIn(
                0f,
                maxY
            )

        view.translationX =
            clampedLeft -
                    baseLeft

        view.translationY =
            clampedTop -
                    baseTop
    }

    private fun saveFabPosition() {

        binding.fabAddBill.post {

            fabPreferences
                .edit()
                .putFloat(
                    "translation_x",
                    binding.fabAddBill.translationX
                )
                .putFloat(
                    "translation_y",
                    binding.fabAddBill.translationY
                )
                .apply()
        }
    }

    private fun restoreFabPosition() {

        if (!isAdded) {
            return
        }

        val x =
            fabPreferences.getFloat(
                "translation_x",
                0f
            )

        val y =
            fabPreferences.getFloat(
                "translation_y",
                0f
            )

        binding.fabAddBill.translationX =
            x

        binding.fabAddBill.translationY =
            y

        binding.fabAddBill.post {

            val parent =
                binding.fabAddBill.parent
                    as? View
                    ?: return@post

            val maxX =
                max(
                    0,
                    parent.width -
                            binding.fabAddBill.width
                ).toFloat()

            val maxY =
                max(
                    0,
                    parent.height -
                            binding.fabAddBill.height
                ).toFloat()

            binding.fabAddBill.translationX =
                x.coerceIn(
                    -binding.fabAddBill.left.toFloat(),
                    maxX -
                            binding.fabAddBill.left
                )

            binding.fabAddBill.translationY =
                y.coerceIn(
                    -binding.fabAddBill.top.toFloat(),
                    maxY -
                            binding.fabAddBill.top
                )
        }
    }

    /**
     * 添加账单
     */
    private fun openAddBill() {

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                AddBillFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    /**
     * 账单导入
     *
     * 保留原来的“账单导入”功能。
     */
    private fun setupImportButton() {

        binding.btnImportBill.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    ImportBillFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {

        super.onResume()

        viewModel.refresh()
    }

    override fun onDestroyView() {

        cancelFabLongPress()

        _binding = null

        super.onDestroyView()
    }
}