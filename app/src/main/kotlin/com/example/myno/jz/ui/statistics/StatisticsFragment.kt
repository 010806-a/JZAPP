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

    /**
     * 当前选择的统计年份和月份。
     */
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

    private enum class TrendPeriod {
        DAY,
        MONTH,
        YEAR
    }

    private var currentTrendType =
        TrendType.EXPENSE

    private var currentTrendPeriod =
        TrendPeriod.DAY

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
        setupTrendPeriodSelector()
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
     * 月份选择器。
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
     * 选择趋势统计周期。
     */
    private fun setupTrendPeriodSelector() {

        binding.tvTrendPeriod.setOnClickListener {

            val items =
                arrayOf(
                    "按日",
                    "按月",
                    "按年"
                )

            val checkedItem =
                when (currentTrendPeriod) {
                    TrendPeriod.DAY -> 0
                    TrendPeriod.MONTH -> 1
                    TrendPeriod.YEAR -> 2
                }

            androidx.appcompat.app.AlertDialog.Builder(
                requireContext()
            )
                .setTitle("选择统计周期")
                .setSingleChoiceItems(
                    items,
                    checkedItem
                ) { dialog, which ->

                    currentTrendPeriod =
                        when (which) {
                            0 -> TrendPeriod.DAY
                            1 -> TrendPeriod.MONTH
                            else -> TrendPeriod.YEAR
                        }

                    binding.tvTrendPeriod.text =
                        items[which]

                    dialog.dismiss()

                    updateTrendChart(
                        repository.getBills()
                    )
                }
                .show()
        }
    }

    /**
     * 打开月份选择器。
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
     * 更新顶部月份文字。
     *
     * 箭头由 XML 的 ic_arrow_down 提供。
     */
    private fun updateMonthText() {

        binding.tvMonth.text =
            String.format(
                Locale.getDefault(),
                "%d年%d月",
                currentYear,
                currentMonth
            )
    }

    /**
     * 初始化趋势 Tab。
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

    /**
     * 创建一个趋势 Tab。
     */
    private fun addTrendTab(
        title: String,
        type: TrendType
    ) {

        val tabView =
            TextView(requireContext())

        tabView.text = title
        tabView.gravity = Gravity.CENTER
        tabView.textSize = 13f
        tabView.isClickable = true
        tabView.isFocusable = true

        tabView.setPadding(
            dp(17),
            0,
            dp(17),
            0
        )

        tabView.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(36)
            ).apply {
                marginEnd = dp(8)
            }

        tabView.setOnClickListener {

            currentTrendType = type

            updateTrendTabStyle()

            updateTrendChart(
                repository.getBills()
            )
        }

        binding.trendTabs.addView(
            tabView
        )
    }

    /**
     * 更新三个趋势 Tab 的样式。
     */
    private fun updateTrendTabStyle() {

        for (index in 0 until binding.trendTabs.childCount) {

            val tabView =
                binding.trendTabs.getChildAt(index)
                    as TextView

            val type =
                when (index) {
                    0 -> TrendType.EXPENSE
                    1 -> TrendType.INCOME
                    else -> TrendType.BALANCE
                }

            if (type == currentTrendType) {

                tabView.background =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.bg_quick_action
                    )

                tabView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )
                )

                tabView.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

            } else {

                tabView.background =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.bg_bill_summary_clip
                    )

                tabView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_secondary
                    )
                )

                tabView.setTypeface(
                    null,
                    android.graphics.Typeface.NORMAL
                )
            }
        }
    }

    /**
     * 初始化支出圆环图。
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
     * 初始化趋势折线图。
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

                        return if (value >= 10000f) {

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

        updateTrendChart(bills)

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
     * 获取指定年月的账单。
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

        return bills.filter { bill ->

            bill.timestamp >=
                    start.timeInMillis &&
                    bill.timestamp <
                    end.timeInMillis
        }
    }

    /**
     * 根据当前统计周期更新趋势图。
     */
    private fun updateTrendChart(
        bills: List<Bill>
    ) {

        when (currentTrendPeriod) {

            TrendPeriod.DAY ->
                updateDailyTrendChart(bills)

            TrendPeriod.MONTH ->
                updateMonthlyTrendChart(bills)

            TrendPeriod.YEAR ->
                updateYearlyTrendChart(bills)
        }
    }

    /**
     * 按日统计。
     *
     * 当前选择月份的每日数据。
     */
    private fun updateDailyTrendChart(
        bills: List<Bill>
    ) {

        val calendar =
            Calendar.getInstance().apply {

                clear()

                set(
                    currentYear,
                    currentMonth - 1,
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

        val startTime =
            calendar.timeInMillis

        val daysInMonth =
            calendar.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

        calendar.add(
            Calendar.MONTH,
            1
        )

        val endTime =
            calendar.timeInMillis

        val monthBills =
            bills.filter { bill ->

                bill.timestamp >= startTime &&
                        bill.timestamp < endTime
            }

        val expenseByDay =
            DoubleArray(daysInMonth)

        val incomeByDay =
            DoubleArray(daysInMonth)

        monthBills.forEach { bill ->

            val billCalendar =
                Calendar.getInstance()

            billCalendar.timeInMillis =
                bill.timestamp

            val day =
                billCalendar.get(
                    Calendar.DAY_OF_MONTH
                )

            if (day in 1..daysInMonth) {

                when (bill.type) {

                    BillType.EXPENSE ->
                        expenseByDay[day - 1] +=
                            bill.amount

                    BillType.INCOME ->
                        incomeByDay[day - 1] +=
                            bill.amount

                    else -> Unit
                }
            }
        }

        val entries =
            mutableListOf<Entry>()

        var cumulativeBalance =
            0.0

        for (day in 1..daysInMonth) {

            val income =
                incomeByDay[day - 1]

            val expense =
                expenseByDay[day - 1]

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

        applyTrendData(
            entries = entries,
            labels = { value ->

                val day =
                    value.toInt()

                if (day in 1..daysInMonth) {
                    "$day/$currentMonth"
                } else {
                    ""
                }
            }
        )

        val previousBills =
            getBillsForMonth(
                bills,
                getPreviousYear(),
                getPreviousMonth()
            )

        updateTrendSummary(
            currentPeriodBills = monthBills,
            previousPeriodBills = previousBills,
            periodDays = daysInMonth
        )
    }

    /**
     * 按月统计。
     *
     * 当前选择年份的1月至12月。
     */
    private fun updateMonthlyTrendChart(
        bills: List<Bill>
    ) {

        val selectedYear =
            currentYear

        val entries =
            mutableListOf<Entry>()

        for (month in 1..12) {

            val monthBills =
                getBillsForMonth(
                    bills,
                    selectedYear,
                    month
                )

            val income =
                monthBills
                    .filter {
                        it.type == BillType.INCOME
                    }
                    .sumOf {
                        it.amount
                    }

            val expense =
                monthBills
                    .filter {
                        it.type == BillType.EXPENSE
                    }
                    .sumOf {
                        it.amount
                    }

            val value =
                when (currentTrendType) {

                    TrendType.EXPENSE ->
                        expense

                    TrendType.INCOME ->
                        income

                    TrendType.BALANCE ->
                        income - expense
                }

            entries.add(
                Entry(
                    month.toFloat(),
                    value.toFloat()
                )
            )
        }

        applyTrendData(
            entries = entries,
            labels = { value ->

                val month =
                    value.toInt()

                if (month in 1..12) {
                    "${month}月"
                } else {
                    ""
                }
            }
        )

        val currentPeriodBills =
            bills.filter { bill ->

                val billCalendar =
                    Calendar.getInstance()

                billCalendar.timeInMillis =
                    bill.timestamp

                billCalendar.get(
                    Calendar.YEAR
                ) == selectedYear
            }

        val previousPeriodBills =
            bills.filter { bill ->

                val billCalendar =
                    Calendar.getInstance()

                billCalendar.timeInMillis =
                    bill.timestamp

                billCalendar.get(
                    Calendar.YEAR
                ) == selectedYear - 1
            }

        val daysInYear =
            Calendar.getInstance().apply {

                clear()

                set(
                    selectedYear,
                    Calendar.JANUARY,
                    1
                )
            }.getActualMaximum(
                Calendar.DAY_OF_YEAR
            )

        updateTrendSummary(
            currentPeriodBills = currentPeriodBills,
            previousPeriodBills = previousPeriodBills,
            periodDays = daysInYear
        )
    }

    /**
     * 按年统计。
     *
     * 以当前选择年份为基准，
     * 显示最近5年。
     */
    private fun updateYearlyTrendChart(
        bills: List<Bill>
    ) {

        val selectedYear =
            currentYear

        val startYear =
            selectedYear - 4

        val entries =
            mutableListOf<Entry>()

        for (year in startYear..selectedYear) {

            val yearBills =
                bills.filter { bill ->

                    val billCalendar =
                        Calendar.getInstance()

                    billCalendar.timeInMillis =
                        bill.timestamp

                    billCalendar.get(
                        Calendar.YEAR
                    ) == year
                }

            val income =
                yearBills
                    .filter {
                        it.type == BillType.INCOME
                    }
                    .sumOf {
                        it.amount
                    }

            val expense =
                yearBills
                    .filter {
                        it.type == BillType.EXPENSE
                    }
                    .sumOf {
                        it.amount
                    }

            val value =
                when (currentTrendType) {

                    TrendType.EXPENSE ->
                        expense

                    TrendType.INCOME ->
                        income

                    TrendType.BALANCE ->
                        income - expense
                }

            entries.add(
                Entry(
                    year.toFloat(),
                    value.toFloat()
                )
            )
        }

        applyTrendData(
            entries = entries,
            labels = { value ->

                val year =
                    value.toInt()

                if (year in startYear..selectedYear) {
                    "${year}年"
                } else {
                    ""
                }
            }
        )

        val currentPeriodBills =
            bills.filter { bill ->

                val billCalendar =
                    Calendar.getInstance()

                billCalendar.timeInMillis =
                    bill.timestamp

                billCalendar.get(
                    Calendar.YEAR
                ) == selectedYear
            }

        val previousPeriodBills =
            bills.filter { bill ->

                val billCalendar =
                    Calendar.getInstance()

                billCalendar.timeInMillis =
                    bill.timestamp

                billCalendar.get(
                    Calendar.YEAR
                ) == selectedYear - 1
            }

        val daysInSelectedYear =
            Calendar.getInstance().apply {

                clear()

                set(
                    selectedYear,
                    Calendar.JANUARY,
                    1
                )
            }.getActualMaximum(
                Calendar.DAY_OF_YEAR
            )

        updateTrendSummary(
            currentPeriodBills = currentPeriodBills,
            previousPeriodBills = previousPeriodBills,
            periodDays = daysInSelectedYear
        )
    }

    /**
     * 将数据应用到折线图。
     */
    private fun applyTrendData(
        entries: List<Entry>,
        labels: (Float) -> String
    ) {

        if (entries.isEmpty()) {

            trendChart.clear()

            trendChart.invalidate()

            return
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
                    entries.size <= 15
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

        val minX =
            entries.minOfOrNull {
                it.x
            } ?: 1f

        val maxX =
            entries.maxOfOrNull {
                it.x
            } ?: 1f

        trendChart.xAxis.apply {

            axisMinimum =
                minX

            axisMaximum =
                if (maxX == minX) {
                    maxX + 1f
                } else {
                    maxX
                }

            labelCount =
                when (currentTrendPeriod) {

                    TrendPeriod.DAY ->
                        5

                    TrendPeriod.MONTH ->
                        6

                    TrendPeriod.YEAR ->
                        5
                }

            granularity = 1f

            setGranularityEnabled(true)

            valueFormatter =
                object : ValueFormatter() {

                    override fun getFormattedValue(
                        value: Float
                    ): String {

                        return labels(value)
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

            axisMinimum =
                if (
                    currentTrendType ==
                    TrendType.BALANCE &&
                    minY < 0f
                ) {

                    minY * 1.15f

                } else {

                    0f
                }

            axisMaximum =
                if (maxY == minY) {

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

        trendChart.invalidate()

        trendChart.animateX(400)
    }

    /**
     * 更新趋势统计摘要。
     */
    private fun updateTrendSummary(
        currentPeriodBills: List<Bill>,
        previousPeriodBills: List<Bill>,
        periodDays: Int
    ) {

        val currentValue =
            when (currentTrendType) {

                TrendType.EXPENSE ->
                    currentPeriodBills
                        .filter {
                            it.type == BillType.EXPENSE
                        }
                        .sumOf {
                            it.amount
                        }

                TrendType.INCOME ->
                    currentPeriodBills
                        .filter {
                            it.type == BillType.INCOME
                        }
                        .sumOf {
                            it.amount
                        }

                TrendType.BALANCE -> {

                    val income =
                        currentPeriodBills
                            .filter {
                                it.type == BillType.INCOME
                            }
                            .sumOf {
                                it.amount
                            }

                    val expense =
                        currentPeriodBills
                            .filter {
                                it.type == BillType.EXPENSE
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
                    previousPeriodBills
                        .filter {
                            it.type == BillType.EXPENSE
                        }
                        .sumOf {
                            it.amount
                        }

                TrendType.INCOME ->
                    previousPeriodBills
                        .filter {
                            it.type == BillType.INCOME
                        }
                        .sumOf {
                            it.amount
                        }

                TrendType.BALANCE -> {

                    val income =
                        previousPeriodBills
                            .filter {
                                it.type == BillType.INCOME
                            }
                            .sumOf {
                                it.amount
                            }

                    val expense =
                        previousPeriodBills
                            .filter {
                                it.type == BillType.EXPENSE
                            }
                            .sumOf {
                                it.amount
                            }

                    income - expense
                }
            }

        val label =
            when (currentTrendPeriod) {

                TrendPeriod.DAY -> {
                    when (currentTrendType) {

                        TrendType.EXPENSE ->
                            "本月支出"

                        TrendType.INCOME ->
                            "本月收入"

                        TrendType.BALANCE ->
                            "本月结余"
                    }
                }

                TrendPeriod.MONTH -> {
                    when (currentTrendType) {

                        TrendType.EXPENSE ->
                            "本年支出"

                        TrendType.INCOME ->
                            "本年收入"

                        TrendType.BALANCE ->
                            "本年结余"
                    }
                }

                TrendPeriod.YEAR -> {
                    when (currentTrendType) {

                        TrendType.EXPENSE ->
                            "年度支出"

                        TrendType.INCOME ->
                            "年度收入"

                        TrendType.BALANCE ->
                            "年度结余"
                    }
                }
            }

        binding.tvTrendLabel.text =
            label

        binding.tvTrendValue.text =
            "¥${moneyFormat.format(currentValue)}"

        binding.tvTrendChange.text =
            calculateChangeText(
                currentValue,
                previousValue
            )

        /**
         * 只有按日 + 支出模式显示
         * 日均和最高日数据。
         */
        if (
            currentTrendPeriod ==
            TrendPeriod.DAY &&
            currentTrendType ==
            TrendType.EXPENSE
        ) {

            val total =
                currentPeriodBills
                    .filter {
                        it.type == BillType.EXPENSE
                    }
                    .sumOf {
                        it.amount
                    }

            val average =
                if (periodDays > 0) {
                    total / periodDays
                } else {
                    0.0
                }

            val daily =
                currentPeriodBills
                    .filter {
                        it.type == BillType.EXPENSE
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
                    .mapValues { (_, bills) ->

                        bills.sumOf(
                            Bill::amount
                        )
                    }

            val highest =
                daily.maxByOrNull {
                    it.value
                }

            binding.dailySummaryLayout.visibility =
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

            binding.dailySummaryLayout.visibility =
                View.GONE
        }
    }

    /**
     * 计算较上期变化。
     */
    private fun calculateChangeText(
        current: Double,
        previous: Double
    ): String {

        if (previous == 0.0) {

            return if (current == 0.0) {
                "较上期 0%"
            } else {
                "较上期 新增"
            }
        }

        val percent =
            (current - previous) /
                    kotlin.math.abs(previous) *
                    100.0

        return String.format(
            Locale.getDefault(),
            "较上期 %+.1f%%",
            percent
        )
    }

    /**
     * 获取上一期年份。
     */
    private fun getPreviousYear(): Int {

        return if (currentMonth == 1) {
            currentYear - 1
        } else {
            currentYear
        }
    }

    /**
     * 获取上一期月份。
     */
    private fun getPreviousMonth(): Int {

        return if (currentMonth == 1) {
            12
        } else {
            currentMonth - 1
        }
    }

    /**
     * 更新支出圆环图。
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
                .map { (categoryId, categoryBills) ->

                    val amount =
                        categoryBills.sumOf {
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
            grouped.map { item ->

                PieEntry(
                    item.second.toFloat(),
                    item.first
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

    /**
     * 创建圆环图颜色。
     */
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
     * 更新分类统计。
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
                .map { (categoryId, categoryBills) ->

                    val category =
                        categoryMap[categoryId]

                    val amount =
                        categoryBills.sumOf {
                            it.amount
                        }

                    CategoryStatistic(
                        categoryId = categoryId,
                        name =
                            category?.name
                                ?: "其他",
                        amount = amount,
                        count = categoryBills.size
                    )
                }
                .sortedByDescending {
                    it.amount
                }

        grouped.forEach { item ->

            val percentage =
                if (totalExpense > 0.0) {

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
     * 添加分类统计项目。
     */
private fun addCategoryItem(
    item: CategoryStatistic,
    percentage: Double
) {

    val context = requireContext()

    val container =
        LinearLayout(context).apply {

            orientation =
                LinearLayout.VERTICAL

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(17)
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
                getCategoryIcon(item.name)
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
                    marginStart = dp(11)
                }
        }

    val nameText =
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

    val amountText =
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
                    topMargin = dp(3)
                }
        }

    info.addView(nameText)
    info.addView(amountText)

    row.addView(info)

    val percentText =
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

    row.addView(percentText)

    container.addView(row)

    val progressBackground =
        LinearLayout(context).apply {

            orientation =
                LinearLayout.HORIZONTAL

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(5)
                ).apply {
                    topMargin = dp(8)
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
                            .coerceIn(
                                0f,
                                100f
                            )
                }

            setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.primary
                )
            )
        }

    progressBackground.addView(progress)

    container.addView(
        progressBackground
    )

    binding.layoutCategoryStatistics.addView(
        container
    )
}

    /**
     * 根据分类名称匹配图标。
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
    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                    resources.displayMetrics.density +
                    0.5f
            ).toInt()
    }
}