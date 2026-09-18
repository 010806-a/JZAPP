package com.example.myno.jz.utils

import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import java.util.Calendar

object FinanceCalculator {

    /**
     * 获取当前月份开始时间
     */
    fun getStartOfMonth(): Long {
        return getStartOfMonth(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH)
        )
    }

    /**
     * 获取当前月份结束时间
     */
    fun getEndOfMonth(): Long {
        return getEndOfMonth(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH)
        )
    }

    /**
     * 获取指定月份开始时间
     *
     * month 使用 Calendar.MONTH：
     * 0 = 一月
     * 8 = 九月
     */
    fun getStartOfMonth(
        year: Int,
        month: Int
    ): Long {
        val calendar = Calendar.getInstance()

        calendar.clear()

        calendar.set(
            year,
            month,
            1,
            0,
            0,
            0
        )

        return calendar.timeInMillis
    }

    /**
     * 获取指定月份结束时间
     */
    fun getEndOfMonth(
        year: Int,
        month: Int
    ): Long {
        val calendar = Calendar.getInstance()

        calendar.clear()

        calendar.set(
            year,
            month,
            1,
            0,
            0,
            0
        )

        calendar.add(
            Calendar.MONTH,
            1
        )

        calendar.add(
            Calendar.MILLISECOND,
            -1
        )

        return calendar.timeInMillis
    }

    /**
     * 获取指定月份的下一月份
     */
    fun getNextMonth(
        year: Int,
        month: Int
    ): Pair<Int, Int> {

        val calendar = Calendar.getInstance()

        calendar.clear()

        calendar.set(
            year,
            month,
            1
        )

        calendar.add(
            Calendar.MONTH,
            1
        )

        return Pair(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
    }

    /**
     * 获取指定月份的上一月份
     */
    fun getPreviousMonth(
        year: Int,
        month: Int
    ): Pair<Int, Int> {

        val calendar = Calendar.getInstance()

        calendar.clear()

        calendar.set(
            year,
            month,
            1
        )

        calendar.add(
            Calendar.MONTH,
            -1
        )

        return Pair(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
    }

    /**
     * 获取当前月份之前指定数量的月份
     */
    fun getRecentMonths(
        count: Int
    ): List<Pair<Int, Int>> {

        val result = mutableListOf<Pair<Int, Int>>()

        val calendar = Calendar.getInstance()

        calendar.set(
            Calendar.DAY_OF_MONTH,
            1
        )

        for (i in 0 until count) {

            result.add(
                Pair(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH)
                )
            )

            calendar.add(
                Calendar.MONTH,
                -1
            )
        }

        return result
    }

    /**
     * 今日开始
     */
    fun getStartOfToday(): Long {

        val calendar = Calendar.getInstance()

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        return calendar.timeInMillis
    }

    /**
     * 今日结束
     */
    fun getEndOfToday(): Long {

        val calendar = Calendar.getInstance()

        calendar.set(
            Calendar.HOUR_OF_DAY,
            23
        )

        calendar.set(
            Calendar.MINUTE,
            59
        )

        calendar.set(
            Calendar.SECOND,
            59
        )

        calendar.set(
            Calendar.MILLISECOND,
            999
        )

        return calendar.timeInMillis
    }

    /**
     * 计算指定时间范围内的收入
     */
    fun calculateIncome(
        bills: List<Bill>,
        startTime: Long,
        endTime: Long
    ): Double {

        return bills
            .asSequence()
            .filter {
                it.type == BillType.INCOME &&
                        it.timestamp >= startTime &&
                        it.timestamp <= endTime
            }
            .sumOf {
                it.amount
            }
    }

    /**
     * 计算指定时间范围内的支出
     */
    fun calculateExpense(
        bills: List<Bill>,
        startTime: Long,
        endTime: Long
    ): Double {

        return bills
            .asSequence()
            .filter {
                it.type == BillType.EXPENSE &&
                        it.timestamp >= startTime &&
                        it.timestamp <= endTime
            }
            .sumOf {
                it.amount
            }
    }

    /**
     * 计算指定月份结余
     *
     * 注意：
     *
     * 这里的结余只是：
     *
     * 本月收入 - 本月支出
     *
     * 不包含上个月结余。
     *
     * 因此不会把期初余额错误计算成收入。
     */
    fun calculateBalance(
        bills: List<Bill>,
        startTime: Long,
        endTime: Long
    ): Double {

        val income =
            calculateIncome(
                bills,
                startTime,
                endTime
            )

        val expense =
            calculateExpense(
                bills,
                startTime,
                endTime
            )

        return income - expense
    }

    /**
     * 计算某个月份之前累计结余
     *
     * 这个值可以理解为：
     *
     * 该月份的期初累计流水变化。
     *
     * 注意：
     * 它不是收入。
     */
    fun calculateAccumulatedBalance(
        bills: List<Bill>,
        beforeTime: Long
    ): Double {

        val income =
            bills
                .filter {
                    it.type == BillType.INCOME &&
                            it.timestamp < beforeTime
                }
                .sumOf {
                    it.amount
                }

        val expense =
            bills
                .filter {
                    it.type == BillType.EXPENSE &&
                            it.timestamp < beforeTime
                }
                .sumOf {
                    it.amount
                }

        return income - expense
    }
}