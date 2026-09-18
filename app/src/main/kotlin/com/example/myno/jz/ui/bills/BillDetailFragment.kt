package com.example.myno.jz.ui.bills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.model.CategoryType
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.databinding.FragmentBillDetailBinding
import com.example.myno.jz.ui.main.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillDetailFragment : Fragment() {

private var _binding: FragmentBillDetailBinding? = null
private val binding get() = _binding!!

private val viewModel: MainViewModel by activityViewModels()

private var billId: String = ""

private var bill: Bill? = null

private var expenseCategories: List<Category> = emptyList()
private var incomeCategories: List<Category> = emptyList()
private var accounts: List<Account> = emptyList()

private var currentType = BillType.EXPENSE

companion object {

    private const val ARG_BILL_ID = "bill_id"

    fun newInstance(
        billId: String
    ): BillDetailFragment {

        return BillDetailFragment().apply {

            arguments = Bundle().apply {
                putString(ARG_BILL_ID, billId)
            }
        }
    }
}

override fun onCreate(
    savedInstanceState: Bundle?
) {
    super.onCreate(savedInstanceState)

    billId =
        arguments?.getString(ARG_BILL_ID)
            ?: ""
}

override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
): View {

    _binding =
        FragmentBillDetailBinding.inflate(
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

    setupButtons()
    observeData()
}

private fun setupButtons() {

    binding.btnBack.setOnClickListener {

        parentFragmentManager.popBackStack()
    }

    binding.btnDelete.setOnClickListener {

        confirmDelete()
    }

    binding.btnSave.setOnClickListener {

        saveChanges()
    }
}

private fun observeData() {

    viewModel.categories.observe(
        viewLifecycleOwner
    ) { categories ->

        expenseCategories =
            categories
                .filter {
                    it.type == CategoryType.EXPENSE &&
                            it.visible
                }
                .sortedBy {
                    it.sortOrder
                }

        incomeCategories =
            categories
                .filter {
                    it.type == CategoryType.INCOME &&
                            it.visible
                }
                .sortedBy {
                    it.sortOrder
                }

        setupIfReady()
    }

    viewModel.accounts.observe(
        viewLifecycleOwner
    ) { accountList ->

        accounts =
            accountList
                .filter {
                    it.enabled
                }
                .sortedBy {
                    it.sortOrder
                }

        setupIfReady()
    }

    viewModel.bills.observe(
        viewLifecycleOwner
    ) { bills ->

        bill =
            bills.firstOrNull {
                it.id == billId
            }

        setupIfReady()
    }
}

private fun setupIfReady() {

    val currentBill =
        bill ?: return

    if (accounts.isEmpty()) {
        return
    }

    currentType =
        currentBill.type

    binding.etAmount.setText(
        String.format(
            Locale.getDefault(),
            "%.2f",
            currentBill.amount
        )
    )

    binding.etNote.setText(
        currentBill.note
    )

    binding.tvTime.text =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        ).format(
            Date(currentBill.timestamp)
        )

    binding.btnExpense.isChecked =
        currentType == BillType.EXPENSE

    binding.btnIncome.isChecked =
        currentType == BillType.INCOME

    updateCategorySpinner(
        currentBill.categoryId
    )

    updateAccountSpinner(
        currentBill.accountId
    )
}

private fun updateCategorySpinner(
    selectedCategoryId: String
) {

    val list =
        if (currentType == BillType.EXPENSE) {
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

    val position =
        list.indexOfFirst {
            it.id == selectedCategoryId
        }

    if (position >= 0) {
        binding.spinnerCategory.setSelection(
            position
        )
    }
}

private fun updateAccountSpinner(
    selectedAccountId: String
) {

    val names =
        accounts.map {
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

    binding.spinnerAccount.adapter =
        adapter

    val position =
        accounts.indexOfFirst {
            it.id == selectedAccountId
        }

    if (position >= 0) {
        binding.spinnerAccount.setSelection(
            position
        )
    }
}

private fun saveChanges() {

    val oldBill =
        bill ?: return

    val amountText =
        binding.etAmount.text
            ?.toString()
            ?.trim()
            ?: ""

    val amount =
        amountText.toDoubleOrNull()

    if (amount == null || amount <= 0.0) {

        binding.etAmount.error =
            "请输入正确的金额"

        return
    }

    val categoryList =
        if (currentType == BillType.EXPENSE) {
            expenseCategories
        } else {
            incomeCategories
        }

    val categoryPosition =
        binding.spinnerCategory.selectedItemPosition

    val accountPosition =
        binding.spinnerAccount.selectedItemPosition

    if (categoryPosition !in categoryList.indices) {

        Toast.makeText(
            requireContext(),
            "请选择分类",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    if (accountPosition !in accounts.indices) {

        Toast.makeText(
            requireContext(),
            "请选择账户",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    val note =
        binding.etNote.text
            ?.toString()
            ?.trim()
            ?: ""

    val newBill =
        oldBill.copy(
            type = currentType,
            amount = amount,
            categoryId =
                categoryList[categoryPosition].id,
            accountId =
                accounts[accountPosition].id,
            note = note,
            updatedAt =
                System.currentTimeMillis()
        )

    val success =
        viewModel
            .getRepository()
            .updateBill(newBill)

    if (success) {

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "修改已保存",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()

    } else {

        Toast.makeText(
            requireContext(),
            "保存修改失败",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun confirmDelete() {

    AlertDialog.Builder(
        requireContext()
    )
        .setTitle("删除账单")
        .setMessage(
            "确定要删除这笔账单吗？删除后无法恢复。"
        )
        .setNegativeButton(
            "取消",
            null
        )
        .setPositiveButton(
            "删除"
        ) { _, _ ->

            deleteBill()
        }
        .show()
}

private fun deleteBill() {

    val currentBill =
        bill ?: return

    val success =
        viewModel
            .getRepository()
            .deleteBill(
                currentBill.id
            )

    if (success) {

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "账单已删除",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()

    } else {

        Toast.makeText(
            requireContext(),
            "删除失败",
            Toast.LENGTH_LONG
        ).show()
    }
}

override fun onDestroyView() {

    super.onDestroyView()

    _binding = null
}

}