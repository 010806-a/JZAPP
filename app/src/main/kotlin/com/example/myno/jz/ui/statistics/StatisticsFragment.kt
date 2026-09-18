package com.example.myno.jz.ui.statistics

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.repository.FinanceRepository
import com.example.myno.jz.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var repository: FinanceRepository

    private var currentYear = 0
    private var currentMonth = 0

    private lateinit var pieChart: PieChart
    private lateinit var trendChart: LineChart

    private val moneyFormat =
        DecimalFormat("#,##0.00")

    private enum class TrendType {
        EXPENSE,
        INCOME,
        BALANCE
    }

    private var currentTrendType =
        TrendType.EXPENSE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentStatisticsBinding.inflate(
                inflater,
                container,
                false
            )

        repository =
            FinanceRepository(requireContext())

        val calendar =
            Calendar.getInstance()

        currentYear =
            calendar.get(Calendar.YEAR)

        currentMonth =
            calendar.get(Calendar.MONTH) + 1

        setupMonthSelector()
        setupTrendTabs()
        setupPieChart()
        setupTrendChart()

        updateMonthText()
        refreshStatistics()

        return binding.root
    }

    override fun onDestroyView() {

        if (::pieChart.isInitialized) {
            pieChart.clear()
        }

        if (::trendChart.isInitialized) {
            trendChart.clear()
        }

        super.onDestroyView()

        _binding = null
    }

    /**
     * 月份选择
     *
     * 点击月份直接打开年月选择器。
     */
    private fun setupMonthSelector() {

        binding.tvMonth.setOnClickListener {
            showMonthPicker()
        }

        binding.monthSelector.setOnClickListener {
            showMonthPicker()
        }
    }

    /**
     * 打开月份选择器。
     *
     * DatePickerDialog 只使用年月，
     * 日期本身不会影响统计结果。
     */
    private fun showMonthPicker() {

        val dialog =
            DatePickerDialog(
                requireContext(),
                { _, year, month, _ ->

                    currentYear = year
                    currentMonth = month + 1

                    updateMonthText()
                    refreshStatistics()
                },
                currentYear,
                currentMonth - 1,
                1
            )

        dialog.datePicker.init(
            currentYear,
            currentMonth - 1,
            1,
            null
        )

        dialog.show()
    }

    /**
     * 显示：
     * 2026年9月⌄
     */
    private fun updateMonthText() {

        binding.tvMonth.text =
            String.format(
                Locale.getDefault(),
                "%d年%d月⌄",
                currentYear,
                currentMonth
            )
    }

    /**
     * 趋势 Tab。
     */
    private fun setupTrendTabs() {

        binding.trendTabs.removeAllViews()

        addTrendTab(
            title = "支出",
            type = TrendType.EXPENSE
        )

        addTrendTab(
            title = "收入",
            type = TrendType.INCOME
        )

        addTrendTab(
            title = "结余",
            type = TrendType.BALANCE
        )

        updateTrendTabStyle()
    }

    private fun addTrendTab(
        title: String,
        type: TrendType
    ) {

        val tab =
            TextView(requireContext()).apply {

                text = title

                gravity = Gravity.CENTER

                textSize = 13f

                isClickable = true

                isFocusable = true

                setPadding(
                    dp(17),
                    0,
                    dp(17),
                    0
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(36)
                    ).apply {
                        marginEnd = dp(8)
                    }

                setOnClickListener {

                    currentTrendType = type

                    updateTrendTabStyle()
                    updateTrendChart(
                        repository.getBills()
                    )
                }
            }

        binding.trendTabs.addView(tab)
    }

private fun updateTrendTabStyle() {

    for (index in 0 until binding.trendTabs.childCount) {

        val tab =
            binding.trendTabs.getChildAt(index) as TextView

        val type =
            when (index) {
                0 -> TrendType.EXPENSE
                1 -> TrendType.INCOME
                else -> TrendType.BALANCE
            }

        if (type == currentTrendType) {

            tab.background =
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_quick_action
                )

            tab.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primary
                )
            )

            tab.setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )

        } else {

            tab.background =
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_bill_summary_clip
                )

            tab.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_secondary
                )
            )

            tab.setTypeface(
                null,
                android.graphics.Typeface.NORMAL
            )
        }
    }
}

    /**
     * 圆环图初始化。
     */
    private fun setupPieChart() {

        pieChart =
            binding.pieChart

        pieChart.description.isEnabled =
            false

        pieChart.setUsePercentValues(true)

        pieChart.setDrawEntryLabels(false)

        pieChart.isDrawHoleEnabled =
            true

        pieChart.holeRadius =
            58f

        pieChart.transparentCircleRadius =
            62f

        pieChart.centerText =
            "本月支出"

        pieChart.setCenterTextSize(
            15f
        )

        pieChart.setCenterTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.text_primary
            )
        )

        pieChart.legend.isEnabled =
            false

        pieChart.setExtraOffsets(
            10f,
            8f,
            10f,
            8f
        )

        pieChart.setDrawSlicesUnderHole(
            true
        )

        pieChart.setTouchEnabled(false)
    }

    /**
     * 趋势图初始化。
     */
    private fun setupTrendChart() {

        trendChart =
            binding.expenseTrendChart

        trendChart.description.isEnabled =
            false

        trendChart.setTouchEnabled(true)

        trendChart.setDragEnabled(true)

        trendChart.setScaleEnabled(false)

        trendChart.setPinchZoom(false)

        trendChart.setDoubleTapToZoomEnabled(
            false
        )

        trendChart.axisRight.isEnabled =
            false

        trendChart.legend.isEnabled =
            false

        trendChart.setExtraOffsets(
            8f,
            8f,
            8f,
            12f
        )

        trendChart.axisLeft.apply {

            axisMinimum = 0f

            textSize = 10f

            setDrawGridLines(true)

            gridColor =
                Color.rgb(
                    240,
                    241,
                    245
                )

            valueFormatter =
                object : ValueFormatter() {

                    override fun getFormattedValue(
                        value: Float
                    ): String {

                        return if (
                            value >= 10000
                        ) {
                            String.format(
                                Locale.getDefault(),
                                "¥%.1fw",
                                value / 10000f
                            )
                        } else {
                            String.format(
                                Locale.getDefault(),
                                "¥%.0f",
                                value
                            )
                        }
                    }
                }
        }

        trendChart.xAxis.apply {

            position =
                XAxis.XAxisPosition.BOTTOM

            granularity = 1f

            setGranularityEnabled(true)

            setDrawGridLines(false)

            textSize = 9f

            labelRotationAngle = 0f
        }
    }

    /**
     * 刷新整个统计页面。
     */
    private fun refreshStatistics() {

        val bills =
            repository.getBills()

        val categories =
            repository.getCategories()

        val monthBills =
            getBillsForMonth(
                bills,
                currentYear,
                currentMonth
            )

        val incomeBills =
            monthBills.filter {
                it.type == BillType.INCOME
            }

        val expenseBills =
            monthBills.filter {
                it.type == BillType.EXPENSE
            }

        val totalIncome =
            incomeBills.sumOf {
                it.amount
            }

        val totalExpense =
            expenseBills.sumOf {
                it.amount
            }

        val balance =
            totalIncome - totalExpense

        binding.tvBalance.text =
            "¥${moneyFormat.format(balance)}"

        binding.tvTotalIncome.text =
            "¥${moneyFormat.format(totalIncome)}"

        binding.tvTotalExpense.text =
            "¥${moneyFormat.format(totalExpense)}"

        binding.tvCategoryTotal.text =
            "¥${moneyFormat.format(totalExpense)}"

        updateTrendChart(
            bills
        )

        updatePieChart(
            expenseBills,
            categories
        )

        updateCategoryStatistics(
            expenseBills,
            categories
        )
    }

    /**
     * 获取指定月份账单。
     */
    private fun getBillsForMonth(
        bills: List<Bill>,
        year: Int,
        month: Int
    ): List<Bill> {

        val start =
            Calendar.getInstance().apply {

                clear()

                set(
                    year,
                    month - 1,
                    1,
                    0,
                    0,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val end =
            Calendar.getInstance().apply {

                timeInMillis =
                    start.timeInMillis

                add(
                    Calendar.MONTH,
                    1
                )
            }

        return bills.filter {

            it.timestamp >=
                    start.timeInMillis &&
                    it.timestamp <
                    end.timeInMillis
        }
    }

    /**
     * 收支趋势。
     *
     * 支出：
     * 每天支出金额。
     *
     * 收入：
     * 每天收入金额。
     *
     * 结余：
     * 本月每天累计收入 - 累计支出。
     */
    private fun updateTrendChart(
        bills: List<Bill>
    ) {

        val monthBills =
            getBillsForMonth(
                bills,
                currentYear,
                currentMonth
            )

        val previousBills =
            getBillsForMonth(
                bills,
                getPreviousYear(),
                getPreviousMonth()
            )

        val monthCalendar =
            Calendar.getInstance().apply {

                clear()

                set(
                    currentYear,
                    currentMonth - 1,
                    1
                )
            }

        val daysInMonth =
            monthCalendar.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

        val dailyIncome =
            mutableMapOf<Int, Double>()

        val dailyExpense =
            mutableMapOf<Int, Double>()

        monthBills.forEach { bill ->

            val calendar =
                Calendar.getInstance()

            calendar.timeInMillis =
                bill.timestamp

            val day =
                calendar.get(
                    Calendar.DAY_OF_MONTH
                )

            if (
                bill.type ==
                BillType.INCOME
            ) {

                dailyIncome[day] =
                    (dailyIncome[day] ?: 0.0) +
                            bill.amount

            } else if (
                bill.type ==
                BillType.EXPENSE
            ) {

                dailyExpense[day] =
                    (dailyExpense[day] ?: 0.0) +
                            bill.amount
            }
        }

        val entries =
            mutableListOf<Entry>()

        var cumulativeBalance =
            0.0

        for (day in 1..daysInMonth) {

            val income =
                dailyIncome[day] ?: 0.0

            val expense =
                dailyExpense[day] ?: 0.0

            val value =
                when (currentTrendType) {

                    TrendType.EXPENSE ->
                        expense

                    TrendType.INCOME ->
                        income

                    TrendType.BALANCE -> {

                        cumulativeBalance +=
                            income - expense

                        cumulativeBalance
                    }
                }

            entries.add(
                Entry(
                    day.toFloat(),
                    value.toFloat()
                )
            )
        }

        val dataSet =
            LineDataSet(
                entries,
                ""
            ).apply {

                lineWidth = 2.8f

                circleRadius = 3.5f

                circleHoleRadius = 1.5f

                setDrawValues(false)

                setDrawCircles(
                    daysInMonth <= 15
                )

                setDrawFilled(false)

                mode =
                    LineDataSet.Mode.CUBIC_BEZIER

                color =
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )

                setCircleColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )
                )
            }

        trendChart.data =
            LineData(dataSet)

        trendChart.xAxis.apply {

            axisMinimum = 1f

            axisMaximum =
                daysInMonth.toFloat()

            labelCount =
                when {
                    daysInMonth <= 7 ->
                        daysInMonth

                    daysInMonth <= 15 ->
                        6

                    else ->
                        5
                }

            granularity = 1f

            valueFormatter =
                object : ValueFormatter() {

                    override fun getFormattedValue(
                        value: Float
                    ): String {

                        val day =
                            value.toInt()

                        return if (
                            day in 1..daysInMonth
                        ) {
                            "$day/${currentMonth}"
                        } else {
                            ""
                        }
                    }
                }
        }

        val maxY =
            entries.maxOfOrNull {
                it.y
            } ?: 0f

        val minY =
            entries.minOfOrNull {
                it.y
            } ?: 0f

        trendChart.axisLeft.apply {

            if (
                currentTrendType ==
                TrendType.BALANCE &&
                minY < 0
            ) {

                axisMinimum =
                    minY * 1.15f

            } else {

                axisMinimum =
                    0f
            }

            axisMaximum =
                if (
                    maxY == minY
                ) {

                    if (maxY <= 0f) {
                        10f
                    } else {
                        maxY * 1.2f
                    }

                } else {

                    max(
                        maxY * 1.2f,
                        10f
                    )
                }
        }

        updateTrendSummary(
            monthBills,
            previousBills,
            daysInMonth
        )

        trendChart.invalidate()
        trendChart.animateX(400)
    }

    /**
     * 更新趋势卡片顶部数据。
     */
    private fun updateTrendSummary(
        monthBills: List<Bill>,
        previousBills: List<Bill>,
        daysInMonth: Int
    ) {

        val currentValue =
            when (currentTrendType) {

                TrendType.EXPENSE ->
                    monthBills
                        .filter {
                            it.type ==
                                    BillType.EXPENSE
                        }
                        .sumOf {
                            it.amount
                        }

                TrendType.INCOME ->
                    monthBills
                        .filter {
                            it.type ==
                                    BillType.INCOME
                        }
                        .sumOf {
                            it.amount
                        }

                TrendType.BALANCE -> {

                    val income =
                        monthBills
                            .filter {
                                it.type ==
                                        BillType.INCOME
                            }
                            .sumOf {
                                it.amount
                            }

                    val expense =
                        monthBills
                            .filter {
                                it.type ==
                                        BillType.EXPENSE
                            }
                            .sumOf {
                                it.amount
                            }

                    income - expense
                }
            }

        val previousValue =
            when (currentTrendType) {

                TrendType.EXPENSE ->
                    previousBills
                        .filter {
                            it.type ==
                                    BillType.EXPENSE
                        }
                        .sumOf {
                            it.amount
                        }

                TrendType.INCOME ->
                    previousBills
                        .filter {
                            it.type ==
                                    BillType.INCOME
                        }
                        .sumOf {
                            it.amount
                        }

                TrendType.BALANCE -> {

                    val income =
                        previousBills
                            .filter {
                                it.type ==
                                        BillType.INCOME
                            }
                            .sumOf {
                                it.amount
                            }

                    val expense =
                        previousBills
                            .filter {
                                it.type ==
                                        BillType.EXPENSE
                            }
                            .sumOf {
                                it.amount
                            }

                    income - expense
                }
            }

        val label =
            when (currentTrendType) {

                TrendType.EXPENSE ->
                    "本月支出"

                TrendType.INCOME ->
                    "本月收入"

                TrendType.BALANCE ->
                    "本月结余"
            }

        binding.tvTrendLabel.text =
            label

        binding.tvTrendValue.text =
            "¥${moneyFormat.format(currentValue)}"

        val changeText =
            calculateChangeText(
                currentValue,
                previousValue
            )

        binding.tvTrendChange.text =
            changeText

        if (
            currentTrendType ==
            TrendType.EXPENSE
        ) {

            val total =
                monthBills
                    .filter {
                        it.type ==
                                BillType.EXPENSE
                    }
                    .sumOf {
                        it.amount
                    }

            val average =
                if (daysInMonth > 0) {
                    total / daysInMonth
                } else {
                    0.0
                }

            val daily =
                monthBills
                    .filter {
                        it.type ==
                                BillType.EXPENSE
                    }
                    .groupBy { bill ->

                        val calendar =
                            Calendar.getInstance()

                        calendar.timeInMillis =
                            bill.timestamp

                        calendar.get(
                            Calendar.DAY_OF_MONTH
                        )
                    }
                    .mapValues {
                        it.value.sumOf(
                            Bill::amount
                        )
                    }

            val highest =
                daily.maxByOrNull {
                    it.value
                }

            binding.dailySummaryLayout
                .visibility =
                View.VISIBLE

            binding.tvDailyAverage.text =
                "¥${moneyFormat.format(average)}"

            binding.tvHighestDaily.text =
                "¥${moneyFormat.format(
                    highest?.value ?: 0.0
                )}"

            binding.tvHighestDailyDate.text =
                if (highest != null) {
                    "${highest.key}日"
                } else {
                    "暂无"
                }

        } else {

            binding.dailySummaryLayout
                .visibility =
                View.GONE
        }
    }

    /**
     * 上月变化。
     */
    private fun calculateChangeText(
        current: Double,
        previous: Double
    ): String {

        if (previous == 0.0) {

            return if (current == 0.0) {
                "较上月 0%"
            } else {
                "较上月 新增"
            }
        }

        val percent =
            (current - previous) /
                    kotlin.math.abs(previous) *
                    100.0

        return String.format(
            Locale.getDefault(),
            "较上月 %+.1f%%",
            percent
        )
    }

    private fun getPreviousYear(): Int {

        return if (currentMonth == 1) {
            currentYear - 1
        } else {
            currentYear
        }
    }

    private fun getPreviousMonth(): Int {

        return if (currentMonth == 1) {
            12
        } else {
            currentMonth - 1
        }
    }

    /**
     * 更新圆环图。
     */
    private fun updatePieChart(
        expenseBills: List<Bill>,
        categories: List<Category>
    ) {

        if (expenseBills.isEmpty()) {

            pieChart.clear()

            pieChart.centerText =
                "暂无支出"

            pieChart.invalidate()

            return
        }

        val categoryMap =
            categories.associateBy {
                it.id
            }

        val grouped =
            expenseBills
                .groupBy {
                    it.categoryId
                }
                .map { (categoryId, bills) ->

                    val amount =
                        bills.sumOf {
                            it.amount
                        }

                    val name =
                        categoryMap[categoryId]
                            ?.name
                            ?: "其他"

                    name to amount
                }
                .sortedByDescending {
                    it.second
                }

        val entries =
            grouped.map {

                PieEntry(
                    it.second.toFloat(),
                    it.first
                )
            }

        val colors =
            createPieColors(
                entries.size
            )

        val dataSet =
            PieDataSet(
                entries,
                ""
            ).apply {

                sliceSpace = 2f

                selectionShift = 3f

                valueTextSize = 10f

                valueTextColor =
                    Color.WHITE

                setColors(colors)
            }

        val pieData =
            PieData(dataSet)

        pieData.setValueFormatter(
            object : ValueFormatter() {

                override fun getFormattedValue(
                    value: Float
                ): String {

                    return String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        value
                    )
                }
            }
        )

        pieChart.data =
            pieData

        val total =
            expenseBills.sumOf {
                it.amount
            }

        pieChart.centerText =
            "本月支出\n¥${
                moneyFormat.format(total)
            }"

        pieChart.invalidate()

        pieChart.animateY(450)
    }

    private fun createPieColors(
        count: Int
    ): List<Int> {

        val baseColors =
            listOf(
                Color.rgb(79, 110, 247),
                Color.rgb(105, 126, 244),
                Color.rgb(87, 166, 255),
                Color.rgb(94, 192, 144),
                Color.rgb(244, 173, 78),
                Color.rgb(235, 105, 119),
                Color.rgb(153, 122, 214),
                Color.rgb(120, 133, 151)
            )

        return List(count) { index ->
            baseColors[
                index % baseColors.size
            ]
        }
    }

    /**
     * 分类列表。
     *
     * 按原型：
     *
     * 图标
     * 分类名称
     * 金额
     * 百分比
     * 进度条
     */
    private fun updateCategoryStatistics(
        expenseBills: List<Bill>,
        categories: List<Category>
    ) {

        binding.layoutCategoryStatistics
            .removeAllViews()

        if (expenseBills.isEmpty()) {

            binding.tvCategoryEmpty.visibility =
                View.VISIBLE

            pieChart.visibility =
                View.GONE

            return
        }

        binding.tvCategoryEmpty.visibility =
            View.GONE

        pieChart.visibility =
            View.VISIBLE

        val categoryMap =
            categories.associateBy {
                it.id
            }

        val totalExpense =
            expenseBills.sumOf {
                it.amount
            }

        val grouped =
            expenseBills
                .groupBy {
                    it.categoryId
                }
                .map { (categoryId, bills) ->

                    val category =
                        categoryMap[categoryId]

                    val amount =
                        bills.sumOf {
                            it.amount
                        }

                    CategoryStatistic(
                        categoryId = categoryId,
                        name =
                            category?.name
                                ?: "其他",
                        amount = amount,
                        count = bills.size
                    )
                }
                .sortedByDescending {
                    it.amount
                }

        grouped.forEach { item ->

            val percentage =
                if (totalExpense > 0) {
                    item.amount /
                            totalExpense *
                            100.0
                } else {
                    0.0
                }

            addCategoryItem(
                item,
                percentage
            )
        }
    }

    private data class CategoryStatistic(
        val categoryId: String,
        val name: String,
        val amount: Double,
        val count: Int
    )

    /**
     * 添加单个分类。
     */
    private fun addCategoryItem(
        item: CategoryStatistic,
        percentage: Double
    ) {

        val context =
            requireContext()

        val container =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin =
                            dp(17)
                    }
            }

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val icon =
            ImageView(context).apply {

                layoutParams =
                    LinearLayout.LayoutParams(
                        dp(40),
                        dp(40)
                    )

                setPadding(
                    dp(9),
                    dp(9),
                    dp(9),
                    dp(9)
                )

                background =
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.bg_bill_icon
                    )

                setImageResource(
                    getCategoryIcon(
                        item.name
                    )
                )

                contentDescription =
                    item.name
            }

        row.addView(icon)

        val info =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginStart =
                            dp(11)
                    }
            }

        val name =
            TextView(context).apply {

                text =
                    item.name

                textSize = 13f

                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.text_primary
                    )
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
            }

 val amount =
    TextView(context).apply {

        text =
            "¥${moneyFormat.format(item.amount)}"

        textSize = 11f

        setTextColor(
            ContextCompat.getColor(
                context,
                R.color.text_secondary
            )
        )

        layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin =
                    dp(3)
            }
    }

        info.addView(name)
        info.addView(amount)

        row.addView(info)

        val percent =
            TextView(context).apply {

                text =
                    String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        percentage
                    )

                textSize = 13f

                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.text_primary
                    )
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
            }

        row.addView(percent)

        container.addView(row)

        val progressBackground =
            LinearLayout(context).apply {

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(5)
                    ).apply {
                        topMargin =
                            dp(8)
                    }

                setBackgroundColor(
                    Color.rgb(
                        240,
                        241,
                        245
                    )
                )
            }

        val progress =
            View(context).apply {

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        dp(5)
                    ).apply {

                        weight =
                            percentage
                                .toFloat()
                                .coerceAtLeast(0f)
                                .coerceAtMost(100f)
                    }

                setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.primary
                    )
                )
            }

        progressBackground.addView(
            progress
        )

        container.addView(
            progressBackground
        )

        binding.layoutCategoryStatistics
            .addView(container)
    }

    /**
     * 根据现有分类资源匹配图标。
     *
     * 不新增图片资源。
     */
    private fun getCategoryIcon(
        name: String
    ): Int {

        return when {

            name.contains("餐") ||
                    name.contains("吃") ||
                    name.contains("饭") ||
                    name.contains("食品") ->
                R.drawable.ic_category_food

            name.contains("交通") ||
                    name.contains("公交") ||
                    name.contains("地铁") ||
                    name.contains("打车") ||
                    name.contains("出行") ->
                R.drawable.ic_category_transport

            name.contains("购物") ||
                    name.contains("服饰") ||
                    name.contains("日用") ->
                R.drawable.ic_category_shopping

            name.contains("通信") ||
                    name.contains("通讯") ||
                    name.contains("手机") ->
                R.drawable.ic_category_communication

            name.contains("旅游") ||
                    name.contains("旅行") ->
                R.drawable.ic_category_travel

            else ->
                R.drawable.ic_category_chart
        }
    }

    /**
     * dp 转 px。
     */
    private fun dp(value: Int): Int {

        return (
            value *
                    resources.displayMetrics.density +
                    0.5f
            ).toInt()
    }
}