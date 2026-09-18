package com.example.myno.jz.ui.bills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.AfterAddAction
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.model.CategoryType
import com.example.myno.jz.data.model.DefaultBillType
import com.example.myno.jz.databinding.FragmentAddBillBinding
import com.example.myno.jz.ui.home.HomeFragment
import com.example.myno.jz.ui.main.MainViewModel

class AddBillFragment : Fragment() {

    private var isQuickEntry = false

    private var _binding: FragmentAddBillBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var currentType = BillType.EXPENSE

    private var selectedExpenseCategoryId: String? = null
    private var selectedIncomeCategoryId: String? = null

    private var expenseCategories: List<Category> = emptyList()
    private var incomeCategories: List<Category> = emptyList()

    private var accounts: List<Account> = emptyList()

    companion object {

        private const val ARG_QUICK_ENTRY =
            "arg_quick_entry"

        fun newQuickEntryInstance(): AddBillFragment {

            return AddBillFragment().apply {

                arguments = Bundle().apply {

                    putBoolean(
                        ARG_QUICK_ENTRY,
                        true
                    )
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
            FragmentAddBillBinding.inflate(
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

        isQuickEntry =
            arguments?.getBoolean(
                ARG_QUICK_ENTRY,
                false
            ) ?: false

        loadDefaultBillType()

        setupTypeSwitch()
        observeData()
        setupCategorySelectionListener()
        setupSaveButton()
        setupBackButton()
    }

    /**
     * 读取默认记账类型
     */
    private fun loadDefaultBillType() {

        val settings =
            viewModel
                .getRepository()
                .getSettings()

        currentType =
            when (settings.defaultBillType) {

                DefaultBillType.INCOME ->
                    BillType.INCOME

                DefaultBillType.EXPENSE ->
                    BillType.EXPENSE
            }

        if (!settings.defaultCategoryId.isNullOrBlank()) {

            when (currentType) {

                BillType.EXPENSE -> {

                    selectedExpenseCategoryId =
                        settings.defaultCategoryId
                }

                BillType.INCOME -> {

                    selectedIncomeCategoryId =
                        settings.defaultCategoryId
                }
            }
        }
    }

    /**
     * 支出 / 收入切换
     */
    private fun setupTypeSwitch() {

        updateTypeButtons()

        binding.btnExpense.setOnClickListener {

            currentType =
                BillType.EXPENSE

            updateTypeButtons()
            updateCategorySpinner()
        }

        binding.btnIncome.setOnClickListener {

            currentType =
                BillType.INCOME

            updateTypeButtons()
            updateCategorySpinner()
        }
    }

    /**
     * 更新支出 / 收入按钮状态
     */
    private fun updateTypeButtons() {

        binding.btnExpense.isChecked =
            currentType == BillType.EXPENSE

        binding.btnIncome.isChecked =
            currentType == BillType.INCOME
    }

    /**
     * 观察分类和账户数据
     */
    private fun observeData() {

        viewModel.categories.observe(
            viewLifecycleOwner
        ) {

            expenseCategories =
                it.filter { category ->

                    category.type ==
                            CategoryType.EXPENSE &&
                            category.visible

                }.sortedBy {

                    it.sortOrder
                }

            incomeCategories =
                it.filter { category ->

                    category.type ==
                            CategoryType.INCOME &&
                            category.visible

                }.sortedBy {

                    it.sortOrder
                }

            updateCategorySpinner()
        }

        viewModel.accounts.observe(
            viewLifecycleOwner
        ) {

            accounts =
                it.filter { account ->

                    account.enabled

                }.sortedBy {

                    it.sortOrder
                }

            updateAccountSpinner()
        }
    }

    /**
     * 分类选择监听
     */
    private fun setupCategorySelectionListener() {

        binding.spinnerCategory
            .onItemSelectedListener =
            object :
                android.widget.AdapterView
                    .OnItemSelectedListener {

                override fun onItemSelected(
                    parent:
                        android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    val list =
                        if (
                            currentType ==
                            BillType.EXPENSE
                        ) {

                            expenseCategories

                        } else {

                            incomeCategories
                        }

                    if (
                        position !in
                        list.indices
                    ) {

                        return
                    }

                    saveCurrentCategorySelection(
                        list[position].id
                    )
                }

                override fun onNothingSelected(
                    parent:
                        android.widget.AdapterView<*>?
                ) {
                    // 不处理
                }
            }
    }

    /**
     * 保存当前分类选择
     */
    private fun saveCurrentCategorySelection(
        categoryId: String
    ) {

        when (currentType) {

            BillType.EXPENSE -> {

                selectedExpenseCategoryId =
                    categoryId
            }

            BillType.INCOME -> {

                selectedIncomeCategoryId =
                    categoryId
            }
        }
    }

    /**
     * 更新分类下拉框
     */
    private fun updateCategorySpinner() {

        val list =
            if (
                currentType ==
                BillType.EXPENSE
            ) {

                expenseCategories

            } else {

                incomeCategories
            }

        val names =
            list.map {

                "${it.icon}  ${it.name}"
            }

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                names
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerCategory.adapter =
            adapter

        if (list.isEmpty()) {

            return
        }

        val selectedId =
            if (
                currentType ==
                BillType.EXPENSE
            ) {

                selectedExpenseCategoryId

            } else {

                selectedIncomeCategoryId
            }

        val selectedIndex =
            list.indexOfFirst {

                it.id == selectedId
            }

        if (selectedIndex >= 0) {

            binding.spinnerCategory
                .setSelection(
                    selectedIndex
                )

        } else {

            binding.spinnerCategory
                .setSelection(0)

            saveCurrentCategorySelection(
                list[0].id
            )
        }
    }

    /**
     * 更新账户下拉框
     */
    private fun updateAccountSpinner() {

        val accountNames =
            accounts.map {

                it.name
            }

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                accountNames
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerAccount.adapter =
            adapter

        if (accounts.isEmpty()) {

            return
        }

        val defaultAccountId =
            viewModel
                .getRepository()
                .getSettings()
                .defaultAccountId

        if (!defaultAccountId.isNullOrBlank()) {

            val defaultIndex =
                accounts.indexOfFirst {

                    it.id ==
                            defaultAccountId
                }

            if (defaultIndex >= 0) {

                binding.spinnerAccount
                    .setSelection(
                        defaultIndex
                    )
            }
        }
    }

    /**
     * 保存按钮
     */
    private fun setupSaveButton() {

        binding.btnSave.setOnClickListener {

            saveBill()
        }
    }

    /**
     * 保存账单
     */
    private fun saveBill() {

        val amountText =
            binding.etAmount.text
                ?.toString()
                ?.trim()
                ?: ""

        if (amountText.isBlank()) {

            binding.etAmount.error =
                "请输入金额"

            return
        }

        val amount =
            amountText.toDoubleOrNull()

        if (
            amount == null ||
            amount <= 0.0
        ) {

            binding.etAmount.error =
                "请输入正确的金额"

            return
        }

        val categoryList =
            if (
                currentType ==
                BillType.EXPENSE
            ) {

                expenseCategories

            } else {

                incomeCategories
            }

        if (categoryList.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "没有可用分类",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (accounts.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "没有可用账户",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val categoryPosition =
            binding.spinnerCategory
                .selectedItemPosition

        val accountPosition =
            binding.spinnerAccount
                .selectedItemPosition

        if (
            categoryPosition !in
            categoryList.indices
        ) {

            Toast.makeText(
                requireContext(),
                "请选择分类",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            accountPosition !in
            accounts.indices
        ) {

            Toast.makeText(
                requireContext(),
                "请选择账户",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val category =
            categoryList[
                categoryPosition
            ]

        val account =
            accounts[
                accountPosition
            ]

        val note =
            binding.etNote.text
                ?.toString()
                ?.trim()
                ?: ""

        val repository =
            viewModel.getRepository()

        val success =
            repository.createBill(
                type = currentType,
                amount = amount,
                categoryId = category.id,
                accountId = account.id,
                note = note,
                timestamp =
                    System.currentTimeMillis()
            )

        if (!success) {

            Toast.makeText(
                requireContext(),
                "保存失败，请检查本地存储",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "账单已保存",
            Toast.LENGTH_SHORT
        ).show()

        handleAfterAddAction()
    }

    /**
     * 根据设置处理记账成功后的行为
     */
    private fun handleAfterAddAction() {

        /*
         * 桌面快捷方式进入快速记账模式
         *
         * 快速记账完成后直接结束
         * QuickEntryActivity。
         *
         * 注意：
         * 只有 isQuickEntry == true 时才允许 finish Activity。
         */
        if (isQuickEntry) {

            requireActivity().finish()

            return
        }

        val settings =
            viewModel
                .getRepository()
                .getSettings()

        when (settings.afterAddAction) {

            AfterAddAction.HOME -> {

                /*
                 * 普通应用内部记账：
                 *
                 * AddBillFragment 是从 HomeFragment
                 * addToBackStack 进入的。
                 *
                 * 保存成功后直接弹出当前 Fragment，
                 * 回到原来的 HomeFragment。
                 *
                 * 不再重新创建 HomeFragment。
                 */
                parentFragmentManager.popBackStack()
            }

            AfterAddAction.BILL_LIST -> {

                /*
                 * 同样先清除当前 AddBillFragment，
                 * 再进入账单列表。
                 */
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        BillsFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }

            AfterAddAction.CONTINUE -> {

                clearInputForNextBill()
            }
        }
    }

    /**
     * 继续记账
     *
     * 保存成功后清空金额和备注，
     * 保留当前收入/支出、分类和账户选择。
     */
    private fun clearInputForNextBill() {

        binding.etAmount
            .text
            ?.clear()

        binding.etNote
            .text
            ?.clear()

        binding.etAmount.error = null

        binding.etAmount.requestFocus()
    }

    /**
     * 返回按钮
     *
     * 普通应用内部进入：
     * AddBillFragment -> HomeFragment
     *
     * 桌面快速记账进入：
     * QuickEntryActivity -> 直接关闭 Activity
     */
    private fun setupBackButton() {

        binding.btnBack.setOnClickListener {

            if (isQuickEntry) {

                requireActivity().finish()

                return@setOnClickListener
            }

            if (
                parentFragmentManager.backStackEntryCount > 0
            ) {

                parentFragmentManager.popBackStack()

            } else {

                /*
                 * 理论上普通模式从首页进入时
                 * 一定存在返回栈。
                 *
                 * 如果没有返回栈，
                 * 不直接 finish MainActivity，
                 * 而是安全地回到首页。
                 */
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeFragment()
                    )
                    .commit()
            }
        }
    }

    /**
     * 系统返回键
     *
     * 防止普通记账页面按系统返回键时
     * 直接结束 MainActivity。
     */
    private fun setupSystemBackButton() {

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {

                    override fun handleOnBackPressed() {

                        if (isQuickEntry) {

                            requireActivity().finish()

                            return
                        }

                        if (
                            parentFragmentManager
                                .backStackEntryCount > 0
                        ) {

                            parentFragmentManager
                                .popBackStack()

                        } else {

                            parentFragmentManager
                                .beginTransaction()
                                .replace(
                                    R.id.fragmentContainer,
                                    HomeFragment()
                                )
                                .commit()
                        }
                    }
                }
            )
    }

    override fun onStart() {
        super.onStart()

        setupSystemBackButton()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}