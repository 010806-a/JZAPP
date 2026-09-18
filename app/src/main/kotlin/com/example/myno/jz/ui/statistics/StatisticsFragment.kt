package com.example.myno.jz.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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

    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    private lateinit var pieChart: PieChart
    private lateinit var expenseTrendChart: LineChart
    private lateinit var monthlyTrendChart: LineChart

    private val moneyFormat =
        DecimalFormat("#,##0.00")

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

        val calendar = Calendar.getInstance()

        currentYear =
            calendar.get(Calendar.YEAR)

        currentMonth =
            calendar.get(Calendar.MONTH) + 1

        setupMonthSelector()
        setupPieChart()
        setupExpenseTrendChart()
        setupMonthlyTrendChart()

        updateMonthText()
        refreshStatistics()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (::pieChart.isInitialized) {
            pieChart.clear()
        }

        if (::expenseTrendChart.isInitialized) {
            expenseTrendChart.clear()
        }

        _binding = null
    }
    
    private fun setupMonthlyTrendChart() {

    monthlyTrendChart =
        binding.monthlyTrendChart

    monthlyTrendChart.description.isEnabled =
        false

    monthlyTrendChart.setTouchEnabled(true)

    monthlyTrendChart.setDragEnabled(true)

    monthlyTrendChart.setScaleEnabled(false)

    monthlyTrendChart.setPinchZoom(false)

    monthlyTrendChart.setDoubleTapToZoomEnabled(false)

    monthlyTrendChart.axisRight.isEnabled =
        false

    monthlyTrendChart.legend.isEnabled =
        true

    monthlyTrendChart.legend.textSize =
        12f

    monthlyTrendChart.setExtraOffsets(
        8f,
        8f,
        8f,
        16f
    )

    monthlyTrendChart.axisLeft.apply {

        axisMinimum = 0f

        textSize = 11f

        setDrawGridLines(true)

        valueFormatter =
            object : ValueFormatter() {

                override fun getFormattedValue(
                    value: Float
                ): String {

                    return "¥${
                        moneyFormat.format(
                            value.toDouble()
                        )
                    }"
                }
            }
    }

    monthlyTrendChart.xAxis.apply {

        position =
            XAxis.XAxisPosition.BOTTOM

        granularity = 1f

        setGranularityEnabled(true)

        setDrawGridLines(false)

        textSize = 10f

        labelRotationAngle = 0f
    }
}

    private fun setupMonthSelector() {

        binding.btnPreviousMonth.setOnClickListener {

            currentMonth--

            if (currentMonth < 1) {
                currentMonth = 12
                currentYear--
            }

            updateMonthText()
            refreshStatistics()
        }

        binding.btnNextMonth.setOnClickListener {

            currentMonth++

            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }

            updateMonthText()
            refreshStatistics()
        }
    }

    private fun updateMonthText() {

        binding.tvMonth.text =
            String.format(
                Locale.getDefault(),
                "%d年%02d月",
                currentYear,
                currentMonth
            )
    }

    /**
     * 饼图初始化
     */
    private fun setupPieChart() {

        pieChart = binding.pieChart

        pieChart.description.isEnabled = false

        pieChart.setUsePercentValues(true)

        pieChart.setDrawEntryLabels(false)

        pieChart.isDrawHoleEnabled = true

        pieChart.holeRadius = 58f

        pieChart.transparentCircleRadius = 62f

        pieChart.centerText = "支出"

        pieChart.setCenterTextSize(16f)

        pieChart.legend.isEnabled = true

        pieChart.legend.textSize = 12f

        pieChart.setExtraOffsets(
            10f,
            10f,
            10f,
            10f
        )
    }

    /**
     * 每日支出趋势图初始化
     */
    private fun setupExpenseTrendChart() {

        expenseTrendChart =
            binding.expenseTrendChart

        expenseTrendChart.description.isEnabled =
            false

        expenseTrendChart.setTouchEnabled(true)

        expenseTrendChart.setDragEnabled(true)

        expenseTrendChart.setScaleEnabled(false)

        expenseTrendChart.setPinchZoom(false)

        expenseTrendChart.setDoubleTapToZoomEnabled(false)

        expenseTrendChart.axisRight.isEnabled =
            false

        expenseTrendChart.legend.isEnabled =
            false

        expenseTrendChart.setExtraOffsets(
            8f,
            8f,
            8f,
            16f
        )

        expenseTrendChart.axisLeft.apply {

            axisMinimum = 0f

            textSize = 11f

            setDrawGridLines(true)

            valueFormatter =
                object : ValueFormatter() {

                    override fun getFormattedValue(
                        value: Float
                    ): String {

                        return "¥${
                            moneyFormat.format(
                                value.toDouble()
                            )
                        }"
                    }
                }
        }

        expenseTrendChart.xAxis.apply {

            position =
                XAxis.XAxisPosition.BOTTOM

            granularity = 1f

            setGranularityEnabled(true)

            setDrawGridLines(false)

            textSize = 10f

            labelRotationAngle = 0f

            valueFormatter =
                object : ValueFormatter() {

                    override fun getFormattedValue(
                        value: Float
                    ): String {

                        return "${value.toInt()}日"
                    }
                }
        }
    }

    /**
     * 刷新当前月份全部统计数据
     */
    private fun refreshStatistics() {

        val bills =
            repository.getBills()

        val categories =
            repository.getCategories()

        val monthStart =
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

        val monthEnd =
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

                add(
                    Calendar.MONTH,
                    1
                )
            }

        val startTime =
            monthStart.timeInMillis

        val endTime =
            monthEnd.timeInMillis

        val monthBills =
            bills.filter {

                it.timestamp >= startTime &&
                        it.timestamp < endTime
            }

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

        binding.tvTotalIncome.text =
            "¥${moneyFormat.format(totalIncome)}"

        binding.tvTotalExpense.text =
            "¥${moneyFormat.format(totalExpense)}"

        binding.tvBalance.text =
            "¥${moneyFormat.format(balance)}"

        updateCategoryStatistics(
            expenseBills,
            categories
        )

        updatePieChart(
            expenseBills,
            categories
        )

        updateExpenseTrendChart(
            expenseBills
        )
        updateMonthlyTrendChart()
    }
    
    private fun updateMonthlyTrendChart() {

    val bills =
        repository.getBills()

    val incomeEntries =
        mutableListOf<Entry>()

    val expenseEntries =
        mutableListOf<Entry>()

    val monthLabels =
        mutableListOf<String>()

    /**
     * 当前选择月份作为最后一个月，
     * 向前统计6个月。
     */
    for (index in 5 downTo 0) {

        val calendar =
            Calendar.getInstance()

        calendar.clear()

        calendar.set(
            currentYear,
            currentMonth - 1,
            1,
            0,
            0,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        calendar.add(
            Calendar.MONTH,
            -index
        )

        val year =
            calendar.get(
                Calendar.YEAR
            )

        val month =
            calendar.get(
                Calendar.MONTH
            ) + 1

        val monthStart =
            calendar.timeInMillis

        val monthEndCalendar =
            calendar.clone() as Calendar

        monthEndCalendar.add(
            Calendar.MONTH,
            1
        )

        val monthEnd =
            monthEndCalendar.timeInMillis

        val monthBills =
            bills.filter {

                it.timestamp >= monthStart &&
                        it.timestamp < monthEnd
            }

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

        val x =
            (5 - index).toFloat()

        incomeEntries.add(
            Entry(
                x,
                income.toFloat()
            )
        )

        expenseEntries.add(
            Entry(
                x,
                expense.toFloat()
            )
        )

        monthLabels.add(
            String.format(
                Locale.getDefault(),
                "%02d月",
                month
            )
        )
    }

    /**
     * 收入折线
     */
    val incomeDataSet =
        LineDataSet(
            incomeEntries,
            "收入"
        ).apply {

            lineWidth = 2.5f

            circleRadius = 4f

            circleHoleRadius = 2f

            setDrawValues(false)

            setDrawCircles(true)

            setDrawFilled(false)

            mode =
                LineDataSet.Mode.CUBIC_BEZIER

            color =
                Color.rgb(
                    55,
                    160,
                    95
                )

            setCircleColor(
                Color.rgb(
                    55,
                    160,
                    95
                )
            )
        }

    /**
     * 支出折线
     */
    val expenseDataSet =
        LineDataSet(
            expenseEntries,
            "支出"
        ).apply {

            lineWidth = 2.5f

            circleRadius = 4f

            circleHoleRadius = 2f

            setDrawValues(false)

            setDrawCircles(true)

            setDrawFilled(false)

            mode =
                LineDataSet.Mode.CUBIC_BEZIER

            color =
                Color.rgb(
                    220,
                    75,
                    75
                )

            setCircleColor(
                Color.rgb(
                    220,
                    75,
                    75
                )
            )
        }

    val lineData =
        LineData(
            incomeDataSet,
            expenseDataSet
        )

    /**
     * X轴月份
     */
    monthlyTrendChart.xAxis.valueFormatter =
        object : ValueFormatter() {

            override fun getFormattedValue(
                value: Float
            ): String {

                val index =
                    value.toInt()

                return if (
                    index in monthLabels.indices
                ) {
                    monthLabels[index]
                } else {
                    ""
                }
            }
        }

    monthlyTrendChart.data =
        lineData

    /**
     * Y轴最大值
     */
    val maxIncome =
        incomeEntries.maxOfOrNull {
            it.y
        } ?: 0f

    val maxExpense =
        expenseEntries.maxOfOrNull {
            it.y
        } ?: 0f

    val maxValue =
        max(
            maxIncome,
            maxExpense
        )

    monthlyTrendChart.axisLeft.apply {

        axisMinimum = 0f

        axisMaximum =
            if (maxValue <= 0f) {
                10f
            } else {
                max(
                    maxValue * 1.2f,
                    10f
                )
            }
    }

    monthlyTrendChart.xAxis.apply {

        axisMinimum = 0f

        axisMaximum = 5f

        labelCount = 6

        granularity = 1f

        setGranularityEnabled(true)
    }

    monthlyTrendChart.invalidate()

    monthlyTrendChart.animateX(500)
}

    /**
     * 分类统计
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

            return
        }

        binding.tvCategoryEmpty.visibility =
            View.GONE

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

                    val categoryName =
                        categoryMap[categoryId]
                            ?.name
                            ?: "其他"

                    Triple(
                        categoryName,
                        amount,
                        bills.size
                    )
                }
                .sortedByDescending {
                    it.second
                }

        val totalExpense =
            expenseBills.sumOf {
                it.amount
            }

        grouped.forEach { item ->

            val percentage =
                if (totalExpense > 0) {

                    item.second /
                            totalExpense *
                            100

                } else {
                    0.0
                }

            addCategoryItem(
                categoryName = item.first,
                amount = item.second,
                percentage = percentage,
                count = item.third
            )
        }
    }

    private fun addCategoryItem(
        categoryName: String,
        amount: Double,
        percentage: Double,
        count: Int
    ) {

        val context =
            requireContext()

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    12,
                    0,
                    12
                )
            }

        val nameView =
            TextView(context).apply {

                text =
                    categoryName

                textSize = 15f

                setTextColor(
                    resources.getColor(
                        R.color.text_primary,
                        null
                    )
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val amountView =
            TextView(context).apply {

                text =
                    "¥${moneyFormat.format(amount)}"

                textSize = 15f

                setTextColor(
                    resources.getColor(
                        R.color.text_primary,
                        null
                    )
                )

                gravity =
                    android.view.Gravity.END
            }

        val detailView =
            TextView(context).apply {

                text =
                    "  ${
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            percentage
                        )
                    }% · ${count}笔"

                textSize = 13f

                setTextColor(
                    resources.getColor(
                        R.color.text_secondary,
                        null
                    )
                )
            }

        row.addView(nameView)

        row.addView(amountView)

        row.addView(detailView)

        binding.layoutCategoryStatistics
            .addView(row)
    }

    /**
     * 支出分类饼图
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

                    val categoryName =
                        categoryMap[categoryId]
                            ?.name
                            ?: "其他"

                    categoryName to amount
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

        val dataSet =
            PieDataSet(
                entries,
                ""
            ).apply {

                sliceSpace = 2f

                selectionShift = 5f

                valueTextSize = 11f

                valueTextColor =
                    Color.WHITE
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

        pieChart.centerText =
            "本月支出\n¥${
                moneyFormat.format(
                    expenseBills.sumOf {
                        it.amount
                    }
                )
            }"

        pieChart.invalidate()

        pieChart.animateY(500)
    }

    /**
     * 每日支出趋势
     */
    private fun updateExpenseTrendChart(
        expenseBills: List<Bill>
    ) {

        val calendar =
            Calendar.getInstance()

        calendar.clear()

        calendar.set(
            currentYear,
            currentMonth - 1,
            1,
            0,
            0,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        val daysInMonth =
            calendar.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

        /**
         * 每日支出
         *
         * key = 日期
         * value = 当天总支出
         */
        val dailyExpenses =
            mutableMapOf<Int, Double>()

        expenseBills.forEach { bill ->

            val billCalendar =
                Calendar.getInstance()

            billCalendar.timeInMillis =
                bill.timestamp

            val day =
                billCalendar.get(
                    Calendar.DAY_OF_MONTH
                )

            dailyExpenses[day] =
                (dailyExpenses[day] ?: 0.0) +
                        bill.amount
        }

        /**
         * 生成每天的数据点
         */
        val entries =
            mutableListOf<Entry>()

        for (day in 1..daysInMonth) {

            val amount =
                dailyExpenses[day] ?: 0.0

            entries.add(
                Entry(
                    day.toFloat(),
                    amount.toFloat()
                )
            )
        }

        /**
         * 没有任何支出
         */
        if (expenseBills.isEmpty()) {

            expenseTrendChart.clear()

            expenseTrendChart.setNoDataText(
                "本月暂无支出"
            )

            expenseTrendChart.setNoDataTextColor(
                resources.getColor(
                    R.color.text_secondary,
                    null
                )
            )

            expenseTrendChart.invalidate()

            binding.tvDailyAverage.text =
                "¥0.00"

            binding.tvHighestDaily.text =
                "¥0.00"

            binding.tvHighestDailyDate.text =
                "暂无"

            return
        }

        /**
         * 找出最高单日
         */
        val highestDay =
            dailyExpenses.maxByOrNull {
                it.value
            }

        val highestAmount =
            highestDay?.value ?: 0.0

        val highestDate =
            highestDay?.key ?: 0

        /**
         * 计算日均支出
         *
         * 按当月自然日计算。
         *
         * 例如：
         * 9月有30天
         * 本月支出320.14
         * 日均 = 320.14 / 30
         */
        val totalExpense =
            expenseBills.sumOf {
                it.amount
            }

        val dailyAverage =
            if (daysInMonth > 0) {
                totalExpense /
                        daysInMonth
            } else {
                0.0
            }

        binding.tvDailyAverage.text =
            "¥${moneyFormat.format(dailyAverage)}"

        binding.tvHighestDaily.text =
            "¥${moneyFormat.format(highestAmount)}"

        binding.tvHighestDailyDate.text =
            "${highestDate}日"

        /**
         * 创建折线
         */
        val dataSet =
            LineDataSet(
                entries,
                "每日支出"
            ).apply {

                lineWidth = 2.5f

                circleRadius = 4f

                circleHoleRadius = 2f

                setDrawValues(false)

                setDrawCircles(true)

                setDrawFilled(false)

                mode =
                    LineDataSet.Mode.CUBIC_BEZIER

                color =
                    Color.rgb(
                        55,
                        115,
                        255
                    )

                setCircleColor(
                    Color.rgb(
                        55,
                        115,
                        255
                    )
                )
            }

        val lineData =
            LineData(dataSet)

        expenseTrendChart.data =
            lineData

        /**
         * 根据最高支出自动设置 Y 轴范围
         */
        val maxValue =
            entries.maxOfOrNull {
                it.y
            } ?: 0f

        expenseTrendChart.axisLeft.apply {

            axisMinimum = 0f

            axisMaximum =
                if (maxValue <= 0f) {
                    10f
                } else {
                    max(
                        maxValue * 1.2f,
                        10f
                    )
                }
        }

        /**
         * X轴显示优化
         */
        expenseTrendChart.xAxis.apply {

            axisMinimum = 1f

            axisMaximum =
                daysInMonth.toFloat()

            labelCount =
                if (daysInMonth <= 7) {
                    daysInMonth
                } else {
                    7
                }

            granularity = 1f

            setGranularityEnabled(true)
        }

        expenseTrendChart.invalidate()

        expenseTrendChart.animateX(500)
    }
}