package com.example.myno.jz.ui.category

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.model.CategoryType
import com.example.myno.jz.databinding.FragmentCategoryManageBinding
import com.example.myno.jz.ui.main.MainViewModel
import java.util.UUID

/**
 * 分类管理
 *
 * 功能：
 * 1. 支出 / 收入分类切换
 * 2. 新增分类
 * 3. 编辑分类
 * 4. 隐藏 / 显示分类
 * 5. 删除自定义分类
 * 6. 系统分类禁止删除
 */
class CategoryManageFragment : Fragment() {

    private var _binding: FragmentCategoryManageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var currentType = BillType.EXPENSE

    private var expenseCategories: List<Category> = emptyList()
    private var incomeCategories: List<Category> = emptyList()
    private val itemTouchHelper =
    ItemTouchHelper(
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or
                ItemTouchHelper.DOWN,
            0
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                val fromPosition =
                    viewHolder.bindingAdapterPosition

                val toPosition =
                    target.bindingAdapterPosition

                if (
                    fromPosition == RecyclerView.NO_POSITION ||
                    toPosition == RecyclerView.NO_POSITION
                ) {
                    return false
                }

                val list =
                    adapter
                        .getCategories()
                        .toMutableList()

                val item =
                    list.removeAt(
                        fromPosition
                    )

                list.add(
                    toPosition,
                    item
                )

                adapter.updateData(
                    list
                )

                return true
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                // 不支持左右滑动删除
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {

                super.clearView(
                    recyclerView,
                    viewHolder
                )

                saveCurrentOrder()
            }
        }
    )

    private lateinit var adapter: CategoryManageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentCategoryManageBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTypeSwitch()
        observeData()
        setupButtons()
    }

    /**
     * RecyclerView
     */
private fun setupRecyclerView() {

    adapter = CategoryManageAdapter(
        onClick = { category ->
            showCategoryDialog(category)
        },
        onStartDrag = { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }
    )

    binding.recyclerCategories.layoutManager =
        LinearLayoutManager(requireContext())

    binding.recyclerCategories.adapter =
        adapter

    itemTouchHelper.attachToRecyclerView(
        binding.recyclerCategories
    )
}
    /**
     * 支出 / 收入切换
     */
    private fun setupTypeSwitch() {

        binding.btnExpense.isChecked = true

        binding.btnExpense.setOnClickListener {

            currentType = BillType.EXPENSE

            renderList()
        }

        binding.btnIncome.setOnClickListener {

            currentType = BillType.INCOME

            renderList()
        }
    }

    /**
     * 观察分类
     */
    private fun observeData() {

        viewModel.categories.observe(
            viewLifecycleOwner
        ) { categories ->

            expenseCategories =
                categories
                    .filter {
                        it.type == CategoryType.EXPENSE
                    }
                    .sortedBy {
                        it.sortOrder
                    }

            incomeCategories =
                categories
                    .filter {
                        it.type == CategoryType.INCOME
                    }
                    .sortedBy {
                        it.sortOrder
                    }

            renderList()
        }
    }

    /**
     * 刷新当前分类列表
     */
    private fun renderList() {

        val list =
            if (currentType == BillType.EXPENSE) {
                expenseCategories
            } else {
                incomeCategories
            }

        adapter.updateData(list)

        binding.tvEmpty.visibility =
            if (list.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    /**
     * 返回 / 新增
     */
    private fun setupButtons() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        binding.fabAddCategory.setOnClickListener {

            showCategoryDialog(null)
        }
    }

    /**
     * 新增 / 编辑分类
     */
    private fun showCategoryDialog(
        category: Category?
    ) {

        val context = requireContext()

        val isEdit = category != null

        val container =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    8,
                    24,
                    8
                )
            }

        val nameInput =
            EditText(context).apply {

                hint = "分类名称"

                setSingleLine(true)

                setText(
                    category?.name ?: ""
                )
            }

        val iconInput =
            EditText(context).apply {

                hint = "图标，例如 🍚"

                setSingleLine(true)

                setText(
                    category?.icon ?: "📁"
                )
            }

        val typeText =
            TextView(context).apply {

                text =
                    if (
                        (category?.type
                            ?: if (
                                currentType ==
                                    BillType.EXPENSE
                            ) {
                                CategoryType.EXPENSE
                            } else {
                                CategoryType.INCOME
                            }) ==
                        CategoryType.EXPENSE
                    ) {
                        "分类类型：支出"
                    } else {
                        "分类类型：收入"
                    }

                textSize = 14f

                setPadding(
                    0,
                    16,
                    0,
                    8
                )
            }

        val visibleCheckBox =
            CheckBox(context).apply {

                text = "显示在快捷记账"

                isChecked =
                    category?.visible ?: true
            }

        container.addView(nameInput)

        container.addView(iconInput)

        container.addView(typeText)

        container.addView(visibleCheckBox)

        val dialog =
            AlertDialog.Builder(context)
                .setTitle(
                    if (isEdit) {
                        "编辑分类"
                    } else {
                        "新增分类"
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

        /*
         * 自定义分类才显示删除按钮。
         */
        if (
            category != null &&
            !category.isSystem
        ) {

            dialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                "删除",
                null as DialogInterface.OnClickListener?
            )
        }

        /*
         * 系统分类：
         * 名称和图标禁止修改。
         */
        if (
            category != null &&
            category.isSystem
        ) {

            nameInput.isEnabled = false

            iconInput.isEnabled = false

            typeText.text =
                "系统默认分类：只能隐藏 / 显示"
        }

        dialog.setOnShowListener {

            val positiveButton =
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                )

            positiveButton.setOnClickListener {

                val name =
                    nameInput.text
                        ?.toString()
                        ?.trim()
                        ?: ""

                val icon =
                    iconInput.text
                        ?.toString()
                        ?.trim()
                        ?: ""

                var valid = true

                if (name.isBlank()) {

                    nameInput.error =
                        "请输入分类名称"

                    valid = false
                }

                if (icon.isBlank()) {

                    iconInput.error =
                        "请输入分类图标"

                    valid = false
                }

                if (!valid) {
                    return@setOnClickListener
                }

                if (category == null) {

                    addCategory(
                        name = name,
                        icon = icon,
                        visible =
                            visibleCheckBox.isChecked
                    )

                } else {

                    updateCategory(
                        category = category,
                        name = name,
                        icon = icon,
                        visible =
                            visibleCheckBox.isChecked
                    )
                }

                dialog.dismiss()
            }

            if (
                category != null &&
                !category.isSystem
            ) {

                val deleteButton =
                    dialog.getButton(
                        AlertDialog.BUTTON_NEUTRAL
                    )

                deleteButton.setOnClickListener {

                    dialog.dismiss()

                    confirmDeleteCategory(
                        category
                    )
                }
            }
        }

        dialog.show()
    }
    private fun saveCurrentOrder() {

    val repository =
        viewModel.getRepository()

    val list =
        adapter.getCategories()

    var failed = false

    list.forEachIndexed { index, category ->

        if (category.sortOrder == index) {
            return@forEachIndexed
        }

        val updated =
            category.copy(
                sortOrder = index
            )

        val success =
            repository.updateCategory(
                updated
            )

        if (!success) {
            failed = true
        }
    }

    if (failed) {

        Toast.makeText(
            requireContext(),
            "分类排序保存失败",
            Toast.LENGTH_SHORT
        ).show()

    } else {

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "分类顺序已保存",
            Toast.LENGTH_SHORT
        ).show()
    }
}

    /**
     * 新增分类
     */
    private fun addCategory(
        name: String,
        icon: String,
        visible: Boolean
    ) {

        val repository =
            viewModel.getRepository()

        val type =
            if (
                currentType ==
                BillType.EXPENSE
            ) {
                CategoryType.EXPENSE
            } else {
                CategoryType.INCOME
            }

        val sameTypeCategories =
            repository
                .getCategories()
                .filter {
                    it.type == type
                }

        val nextSortOrder =
            (
                sameTypeCategories
                    .maxOfOrNull {
                        it.sortOrder
                    }
                    ?: -1
                ) + 1

        val category =
            Category(
                id =
                    UUID.randomUUID()
                        .toString(),

                name = name,

                type = type,

                icon = icon,

                isSystem = false,

                visible = visible,

                sortOrder = nextSortOrder
            )

        val success =
            repository.addCategory(
                category
            )

        if (!success) {

            Toast.makeText(
                requireContext(),
                "新增分类失败",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "分类已添加",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 修改分类
     */
    private fun updateCategory(
        category: Category,
        name: String,
        icon: String,
        visible: Boolean
    ) {

        val repository =
            viewModel.getRepository()

        val updated =
            category.copy(
                name =
                    if (category.isSystem) {
                        category.name
                    } else {
                        name
                    },

                icon =
                    if (category.isSystem) {
                        category.icon
                    } else {
                        icon
                    },

                visible = visible
            )

        val success =
            repository.updateCategory(
                updated
            )

        if (!success) {

            Toast.makeText(
                requireContext(),
                "保存分类失败",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "分类已保存",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 删除确认
     */
    private fun confirmDeleteCategory(
        category: Category
    ) {

        if (category.isSystem) {

            Toast.makeText(
                requireContext(),
                "系统默认分类不能删除",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("删除分类")
            .setMessage(
                "确定删除「${category.name}」吗？\n\n" +
                    "删除分类不会删除已有账单。"
            )
            .setNegativeButton(
                "取消",
                null
            )
            .setPositiveButton(
                "删除"
            ) { _, _ ->

                deleteCategory(
                    category
                )
            }
            .show()
    }

    /**
     * 删除分类
     */
    private fun deleteCategory(
        category: Category
    ) {

        if (category.isSystem) {

            Toast.makeText(
                requireContext(),
                "系统默认分类不能删除",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val success =
            viewModel
                .getRepository()
                .deleteCategory(
                    category.id
                )

        if (!success) {

            Toast.makeText(
                requireContext(),
                "删除分类失败",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "分类已删除",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}