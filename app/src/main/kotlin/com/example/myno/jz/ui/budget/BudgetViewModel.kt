package com.example.myno.jz.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Budget
import com.example.myno.jz.data.model.BudgetType
import com.example.myno.jz.data.repository.FinanceRepository
import com.example.myno.jz.utils.AppLogger
import java.util.Calendar

data class BudgetItem(
    val budget: Budget,
    val categoryName: String,
    val usedAmount: Double,
    val remainingAmount: Double,
    val usedPercent: Double
)

/**
 * 当前月份预算概览
 *
 * 例如：
 *
 * 总预算：800
 * 分类预算：600
 * 未分配预算：200
 *
 * 总支出：320.14
 * 分类预算覆盖支出：25.90
 * 其他分类支出：294.24
 * 未分配预算剩余：-94.24
 */
data class BudgetOverview(
    val totalBudget: Double,
    val totalExpense: Double,
    val categoryBudget: Double,
    val unallocatedBudget: Double,
    val unallocatedExpense: Double,
    val unallocatedRemaining: Double
)

class BudgetViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        FinanceRepository(application)

    // =========================================================
    // 预算列表
    // =========================================================

    private val _budgetItems =
        MutableLiveData<List<BudgetItem>>()

    val budgetItems: LiveData<List<BudgetItem>> =
        _budgetItems

    // =========================================================
    // 预算概览
    // =========================================================

    private val _budgetOverview =
        MutableLiveData<BudgetOverview>()

    val budgetOverview: LiveData<BudgetOverview> =
        _budgetOverview

    fun getRepository(): FinanceRepository {
        return repository
    }

    fun refresh(
        year: Int,
        month: Int
    ) {

        val allBudgets =
            repository.getBudgets()

        AppLogger.i(
            "Budget",
            "========== 开始刷新预算 =========="
        )

        AppLogger.i(
            "Budget",
            "页面请求年月：$year-$month"
        )

        AppLogger.i(
            "Budget",
            "读取到预算数量：${allBudgets.size}"
        )

        // =========================================================
        // 当前月份预算
        // =========================================================

        val budgets =
            allBudgets.filter {
                it.year == year &&
                        it.month == month
            }

        val bills =
            repository.getBills()

        val categories =
            repository.getCategories()

        // =========================================================
        // 当前月份时间范围
        // =========================================================

        val monthStart =
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

        val startTime =
            monthStart.timeInMillis

        val monthEnd =
            monthStart.clone() as Calendar

        monthEnd.add(
            Calendar.MONTH,
            1
        )

        val endTime =
            monthEnd.timeInMillis

        // =========================================================
        // 当前月份所有支出
        // =========================================================

        val expenseBills =
            bills.filter {

                it.type == BillType.EXPENSE &&
                        it.timestamp >= startTime &&
                        it.timestamp < endTime
            }

        // =========================================================
        // 1. 总预算
        // =========================================================

        val totalBudget =
            budgets.firstOrNull {
                it.type == BudgetType.TOTAL
            }

        val totalBudgetAmount =
            totalBudget?.amount ?: 0.0

        // =========================================================
        // 2. 分类预算总额
        //
        // 例如：
        //
        // 总预算 = 800
        // 餐饮 = 600
        //
        // 分类预算总额 = 600
        // 未分配预算 = 200
        // =========================================================

        val categoryBudgetAmount =
            budgets
                .filter {
                    it.type == BudgetType.CATEGORY
                }
                .sumOf {
                    it.amount
                }

        val unallocatedBudget =
            totalBudgetAmount -
                    categoryBudgetAmount

        // =========================================================
        // 3. 获取设置了分类预算的分类 ID
        // =========================================================

        val categoryBudgetIds =
            budgets
                .filter {
                    it.type == BudgetType.CATEGORY
                }
                .mapNotNull {
                    it.categoryId
                }
                .toSet()

        // =========================================================
        // 4. 已经被分类预算覆盖的支出
        //
        // 例如：
        //
        // 餐饮预算 = 600
        // 餐饮支出 = 25.90
        //
        // 25.90 属于餐饮自己的预算，
        // 不应该再从未分配预算 200 中扣除。
        // =========================================================

        val allocatedCategoryExpense =
            expenseBills
                .filter {
                    it.categoryId in categoryBudgetIds
                }
                .sumOf {
                    it.amount
                }

        // =========================================================
        // 5. 其他分类支出
        //
        // 总支出 = 320.14
        // 餐饮 = 25.90
        //
        // 其他分类：
        //
        // 320.14 - 25.90
        // = 294.24
        // =========================================================

        val totalExpense =
            expenseBills.sumOf {
                it.amount
            }

        val unallocatedExpense =
            totalExpense -
                    allocatedCategoryExpense

        // =========================================================
        // 6. 未分配预算剩余
        //
        // 200 - 294.24
        // = -94.24
        //
        // 负数表示超支。
        // =========================================================

        val unallocatedRemaining =
            unallocatedBudget -
                    unallocatedExpense

        // =========================================================
        // 7. 发布预算概览
        // =========================================================

        _budgetOverview.value =
            BudgetOverview(
                totalBudget = totalBudgetAmount,
                totalExpense = totalExpense,
                categoryBudget = categoryBudgetAmount,
                unallocatedBudget = unallocatedBudget,
                unallocatedExpense = unallocatedExpense,
                unallocatedRemaining = unallocatedRemaining
            )

        // =========================================================
        // 8. 生成预算列表
        // =========================================================

        val result =
            budgets.map { budget ->

                val usedAmount =
                    when (budget.type) {

                        // 总预算显示本月全部支出
                        BudgetType.TOTAL -> {

                            totalExpense
                        }

                        // 分类预算只显示对应分类支出
                        BudgetType.CATEGORY -> {

                            expenseBills
                                .filter {
                                    it.categoryId ==
                                            budget.categoryId
                                }
                                .sumOf {
                                    it.amount
                                }
                        }
                    }

                val remainingAmount =
                    budget.amount -
                            usedAmount

                val usedPercent =
                    if (budget.amount > 0.0) {

                        usedAmount /
                                budget.amount *
                                100.0

                    } else {
                        0.0
                    }

                val categoryName =
                    when (budget.type) {

                        BudgetType.TOTAL ->
                            "全部支出"

                        BudgetType.CATEGORY ->

                            categories
                                .firstOrNull {
                                    it.id ==
                                            budget.categoryId
                                }
                                ?.name
                                ?: "未知分类"
                    }

                BudgetItem(
                    budget = budget,
                    categoryName = categoryName,
                    usedAmount = usedAmount,
                    remainingAmount = remainingAmount,
                    usedPercent = usedPercent
                )
            }

        // =========================================================
        // 9. 更新预算列表
        // =========================================================

        _budgetItems.value =
            result

        AppLogger.i(
            "Budget",
            "总预算：$totalBudgetAmount"
        )

        AppLogger.i(
            "Budget",
            "本月总支出：$totalExpense"
        )

        AppLogger.i(
            "Budget",
            "分类预算：$categoryBudgetAmount"
        )

        AppLogger.i(
            "Budget",
            "未分配预算：$unallocatedBudget"
        )

        AppLogger.i(
            "Budget",
            "其他分类支出：$unallocatedExpense"
        )

        AppLogger.i(
            "Budget",
            "未分配预算剩余：$unallocatedRemaining"
        )

        AppLogger.i(
            "Budget",
            "当前预算项目数量：${result.size}"
        )

        AppLogger.i(
            "Budget",
            "========== 预算刷新结束 =========="
        )
    }
}