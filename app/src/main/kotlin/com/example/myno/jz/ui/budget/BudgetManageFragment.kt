package com.example.myno.jz.ui.budget

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Budget
import com.example.myno.jz.data.model.BudgetType
import com.example.myno.jz.data.model.CategoryType
import com.example.myno.jz.databinding.FragmentBudgetManageBinding
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class BudgetManageFragment : Fragment() {

    private var _binding: FragmentBudgetManageBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel: BudgetViewModel by viewModels()

    private lateinit var adapter: BudgetAdapter

    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val calendar = Calendar.getInstance()

        currentYear =
            calendar.get(Calendar.YEAR)

        currentMonth =
            calendar.get(Calendar.MONTH) + 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentBudgetManageBinding.inflate(
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
        observeData()

        updateMonthText()
        refreshData()
    }

    private fun setupRecyclerView() {

        adapter =
            BudgetAdapter(
                onClick = { item ->
                    showBudgetDialog(
                        editBudget = item.budget
                    )
                },
                onDelete = { item ->
                    deleteBudget(
                        item.budget
                    )
                }
            )

        binding.recyclerBudgets.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerBudgets.adapter =
            adapter
    }

    private fun setupButtons() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }

        binding.btnPreviousMonth.setOnClickListener {

            currentMonth--

            if (currentMonth < 1) {
                currentMonth = 12
                currentYear--
            }

            updateMonthText()
            refreshData()
        }

        binding.btnNextMonth.setOnClickListener {

            currentMonth++

            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }

            updateMonthText()
            refreshData()
        }

        binding.fabAddBudget.setOnClickListener {

            showBudgetDialog()
        }
    }

    private fun observeData() {

        // =========================================================
        // 观察预算列表
        // =========================================================

        viewModel.budgetItems.observe(
            viewLifecycleOwner
        ) { items ->

            adapter.updateData(items)

            if (items.isEmpty()) {

                binding.recyclerBudgets.visibility =
                    View.GONE

                binding.tvEmpty.visibility =
                    View.VISIBLE

            } else {

                binding.recyclerBudgets.visibility =
                    View.VISIBLE

                binding.tvEmpty.visibility =
                    View.GONE
            }
        }

        // =========================================================
        // 观察预算概览
        //
        // 所有概览数据统一来自 BudgetViewModel
        // 不再在 Fragment 中重复读取账单和预算。
        // =========================================================

        viewModel.budgetOverview.observe(
            viewLifecycleOwner
        ) { overview ->

            updateBudgetOverview(
                overview
            )
        }
    }

    /**
     * 更新预算概览
     *
     * 数据来源：
     * BudgetViewModel.budgetOverview
     *
     * 例如：
     *
     * 总预算：800
     * 总支出：320.14
     * 分类预算：600
     * 未分配预算：200
     * 其他分类支出：294.24
     * 未分配预算剩余：-94.24
     */
    private fun updateBudgetOverview(
        overview: BudgetOverview
    ) {

        binding.tvOverviewTotalBudget.text =
            formatMoney(
                overview.totalBudget
            )

        binding.tvOverviewTotalExpense.text =
            formatMoney(
                overview.totalExpense
            )

        binding.tvOverviewCategoryBudget.text =
            formatMoney(
                overview.categoryBudget
            )

        binding.tvOverviewUnallocatedBudget.text =
            formatMoney(
                overview.unallocatedBudget
            )

        binding.tvOverviewUnallocatedExpense.text =
            formatMoney(
                overview.unallocatedExpense
            )

        // =========================================================
        // 未分配预算是否超支
        // =========================================================

        if (overview.unallocatedRemaining < 0.0) {

            binding.tvOverviewUnallocatedLabel.text =
                "未分配预算已超支"

            binding.tvOverviewUnallocatedRemaining.text =
                "超支 ${
                    formatMoney(
                        -overview.unallocatedRemaining
                    )
                }"

        } else {

            binding.tvOverviewUnallocatedLabel.text =
                "未分配预算剩余"

            binding.tvOverviewUnallocatedRemaining.text =
                formatMoney(
                    overview.unallocatedRemaining
                )
        }
    }

    private fun updateMonthText() {

        binding.tvMonth.text =
            String.format(
                Locale.getDefault(),
                "%d年%d月",
                currentYear,
                currentMonth
            )
    }

    private fun refreshData() {

        viewModel.refresh(
            currentYear,
            currentMonth
        )
    }

    /**
     * 新增 / 编辑预算
     */
    private fun showBudgetDialog(
        editBudget: Budget? = null
    ) {

        val context =
            requireContext()

        val isEdit =
            editBudget != null

        val categories =
            viewModel
                .getRepository()
                .getCategories()
                .filter {
                    it.type ==
                            CategoryType.EXPENSE
                }
                .sortedBy {
                    it.sortOrder
                }

        val container =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    48,
                    8,
                    48,
                    0
                )
            }

        val typeLabel =
            TextView(context).apply {
                text = "预算类型"
                textSize = 14f
            }

        val typeSpinner =
            Spinner(context)

        val typeItems =
            listOf(
                "总预算",
                "分类预算"
            )

        typeSpinner.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                typeItems
            )

        val categoryLabel =
            TextView(context).apply {
                text = "支出分类"
                textSize = 14f
                visibility = View.GONE
            }

        val categorySpinner =
            Spinner(context)

        val categoryNames =
            categories.map {
                it.name
            }

        categorySpinner.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                categoryNames
            )

        categoryLabel.visibility =
            if (
                editBudget?.type ==
                BudgetType.CATEGORY
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        categorySpinner.visibility =
            categoryLabel.visibility

        val amountLabel =
            TextView(context).apply {
                text = "预算金额"
                textSize = 14f
            }

        val amountEdit =
            EditText(context).apply {

                hint = "请输入预算金额"

                inputType =
                    android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

                setSingleLine(true)

                if (editBudget != null) {

                    setText(
                        String.format(
                            Locale.getDefault(),
                            "%.2f",
                            editBudget.amount
                        )
                    )
                }
            }

        val warningLabel =
            TextView(context).apply {
                text = "预警比例"
                textSize = 14f
            }

        val warningSpinner =
            Spinner(context)

        val warningValues =
            listOf(
                70,
                80,
                90,
                100
            )

        val warningNames =
            warningValues.map {
                "$it%"
            }

        warningSpinner.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                warningNames
            )

        if (editBudget != null) {

            val index =
                warningValues.indexOf(
                    editBudget.warningPercent
                )

            if (index >= 0) {

                warningSpinner.setSelection(
                    index
                )
            }
        }

        container.addView(typeLabel)
        container.addView(typeSpinner)

        container.addView(categoryLabel)

        container.addView(
            categorySpinner
        )

        container.addView(amountLabel)

        container.addView(amountEdit)

        container.addView(warningLabel)

        container.addView(
            warningSpinner
        )

        if (editBudget?.type ==
            BudgetType.CATEGORY
        ) {

            typeSpinner.setSelection(1)

        } else {

            typeSpinner.setSelection(0)
        }

        typeSpinner.setOnItemSelectedListener(
            object :
                android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    val showCategory =
                        position == 1

                    categoryLabel.visibility =
                        if (showCategory) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    categorySpinner.visibility =
                        if (showCategory) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }
            }
        )

        if (editBudget != null) {

            val categoryIndex =
                categories.indexOfFirst {
                    it.id ==
                            editBudget.categoryId
                }

            if (categoryIndex >= 0) {

                categorySpinner.setSelection(
                    categoryIndex
                )
            }
        }

        val dialog =
            AlertDialog.Builder(context)
                .setTitle(
                    if (isEdit) {
                        "编辑预算"
                    } else {
                        "新增预算"
                    }
                )
                .setView(container)
                .setNegativeButton(
                    "取消",
                    null
                )
                .setPositiveButton(
                    if (isEdit) {
                        "保存"
                    } else {
                        "添加"
                    },
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val amountText =
                    amountEdit.text
                        .toString()
                        .trim()

                val amount =
                    amountText.toDoubleOrNull()

                if (
                    amount == null ||
                    amount <= 0
                ) {

                    amountEdit.error =
                        "请输入正确的预算金额"

                    return@setOnClickListener
                }

                val type =
                    if (
                        typeSpinner.selectedItemPosition == 0
                    ) {

                        BudgetType.TOTAL

                    } else {

                        BudgetType.CATEGORY
                    }

                var categoryId: String? =
                    null

                if (
                    type ==
                    BudgetType.CATEGORY
                ) {

                    if (categories.isEmpty()) {

                        Toast.makeText(
                            context,
                            "请先创建支出分类",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    val index =
                        categorySpinner
                            .selectedItemPosition

                    if (
                        index < 0 ||
                        index >= categories.size
                    ) {

                        Toast.makeText(
                            context,
                            "请选择支出分类",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    categoryId =
                        categories[index].id
                }

                val warningPercent =
                    warningValues[
                        warningSpinner.selectedItemPosition
                    ]

                val duplicateBudget =
                    viewModel
                        .getRepository()
                        .getBudgets()
                        .firstOrNull { existing ->

                            existing.id !=
                                    editBudget?.id &&

                                    existing.year ==
                                    currentYear &&

                                    existing.month ==
                                    currentMonth &&

                                    existing.type ==
                                    type &&

                                    existing.categoryId ==
                                    categoryId
                        }

                if (duplicateBudget != null) {

                    val message =
                        if (
                            type ==
                            BudgetType.TOTAL
                        ) {

                            "本月已经存在总预算，不能重复添加"

                        } else {

                            "这个支出分类本月已经设置过预算，不能重复添加"
                        }

                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val budget =
                    Budget(
                        id =
                            editBudget?.id
                                ?: UUID.randomUUID()
                                    .toString(),

                        year =
                            currentYear,

                        month =
                            currentMonth,

                        type =
                            type,

                        categoryId =
                            categoryId,

                        amount =
                            amount,

                        enabled =
                            editBudget?.enabled
                                ?: true,

                        warningPercent =
                            warningPercent
                    )

                val success =
                    if (editBudget == null) {

                        viewModel
                            .getRepository()
                            .addBudget(
                                budget
                            )

                    } else {

                        viewModel
                            .getRepository()
                            .updateBudget(
                                budget
                            )
                    }

                if (success) {

                    Toast.makeText(
                        context,
                        if (isEdit) {
                            "预算已更新"
                        } else {
                            "预算添加成功"
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    refreshData()

                    dialog.dismiss()

                } else {

                    Toast.makeText(
                        context,
                        "保存失败，请重试",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

    /**
     * 删除预算确认
     */
    private fun deleteBudget(
        budget: Budget
    ) {

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("删除预算")
            .setMessage(
                "确定要删除这个预算吗？"
            )
            .setNegativeButton(
                "取消",
                null
            )
            .setPositiveButton(
                "删除"
            ) { _, _ ->

                val success =
                    viewModel
                        .getRepository()
                        .deleteBudget(
                            budget.id
                        )

                if (success) {

                    Toast.makeText(
                        requireContext(),
                        "预算已删除",
                        Toast.LENGTH_SHORT
                    ).show()

                    refreshData()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "删除失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
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

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}