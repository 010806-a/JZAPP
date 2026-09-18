package com.example.myno.jz.ui.assets

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.Transfer
import com.example.myno.jz.databinding.FragmentEditTransferBinding
import com.example.myno.jz.ui.main.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditTransferFragment : Fragment() {

    private var _binding: FragmentEditTransferBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var transferId: String? = null

    private var accounts: List<Account> = emptyList()

    /**
     * 当前正在编辑的转账时间
     */
    private var selectedTimestamp: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transferId = arguments?.getString(ARG_TRANSFER_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditTransferBinding.inflate(
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

        loadTransfer()
    }

    /**
     * 加载原转账记录
     */
    private fun loadTransfer() {

        val id = transferId

        if (id.isNullOrBlank()) {
            showErrorAndBack("转账信息不存在")
            return
        }

        val transfer = viewModel
            .getRepository()
            .getTransfers()
            .firstOrNull { it.id == id }

        if (transfer == null) {
            showErrorAndBack("转账记录不存在或已被删除")
            return
        }

        // 保存原来的时间
        selectedTimestamp = transfer.timestamp

        loadAccounts(transfer)

        showTransfer(transfer)

        setupButtons()
    }

    /**
     * 加载账户
     */
    private fun loadAccounts(
        transfer: Transfer
    ) {

        accounts = viewModel.accounts.value
            ?.filter { it.enabled }
            ?.sortedBy { it.sortOrder }
            ?: emptyList()

        if (accounts.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "暂无可用账户",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val names = accounts.map {
            "${it.icon}  ${it.name}"
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            names
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerFromAccount.adapter = adapter
        binding.spinnerToAccount.adapter = adapter

        // 自动定位原转出账户
        val fromIndex = accounts.indexOfFirst {
            it.id == transfer.fromAccountId
        }

        // 自动定位原转入账户
        val toIndex = accounts.indexOfFirst {
            it.id == transfer.toAccountId
        }

        if (fromIndex >= 0) {
            binding.spinnerFromAccount.setSelection(fromIndex)
        }

        if (toIndex >= 0) {
            binding.spinnerToAccount.setSelection(toIndex)
        }
    }

    /**
     * 显示原转账数据
     */
    private fun showTransfer(
        transfer: Transfer
    ) {

        binding.etAmount.setText(
            String.format(
                Locale.getDefault(),
                "%.2f",
                transfer.amount
            )
        )

        binding.etNote.setText(
            transfer.note
        )

        updateDateTimeText()
    }

    /**
     * 按钮事件
     */
    private fun setupButtons() {

        // 返回
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 修改日期
        binding.btnDate.setOnClickListener {
            showDatePicker()
        }

        // 修改时间
        binding.btnTime.setOnClickListener {
            showTimePicker()
        }

        // 保存
        binding.btnSave.setOnClickListener {
            saveTransfer()
        }
    }

    /**
     * 日期选择
     */
    private fun showDatePicker() {

        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedTimestamp
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                calendar.set(
                    Calendar.YEAR,
                    year
                )

                calendar.set(
                    Calendar.MONTH,
                    month
                )

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    dayOfMonth
                )

                selectedTimestamp = calendar.timeInMillis

                updateDateTimeText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * 时间选择
     */
    private fun showTimePicker() {

        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedTimestamp
        }

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->

                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    hourOfDay
                )

                calendar.set(
                    Calendar.MINUTE,
                    minute
                )

                calendar.set(
                    Calendar.SECOND,
                    0
                )

                calendar.set(
                    Calendar.MILLISECOND,
                    0
                )

                selectedTimestamp = calendar.timeInMillis

                updateDateTimeText()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    /**
     * 更新日期和时间按钮文字
     */
    private fun updateDateTimeText() {

        val dateFormat = SimpleDateFormat(
            "yyyy年MM月dd日",
            Locale.getDefault()
        )

        val timeFormat = SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        )

        val date = Date(selectedTimestamp)

        binding.btnDate.text = dateFormat.format(date)

        binding.btnTime.text = timeFormat.format(date)
    }

    /**
     * 保存修改
     */
    private fun saveTransfer() {

        if (accounts.size < 2) {

            Toast.makeText(
                requireContext(),
                "至少需要两个可用账户才能转账",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val fromPosition =
            binding.spinnerFromAccount.selectedItemPosition

        val toPosition =
            binding.spinnerToAccount.selectedItemPosition

        if (fromPosition !in accounts.indices ||
            toPosition !in accounts.indices
        ) {

            Toast.makeText(
                requireContext(),
                "请选择转出和转入账户",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // 不允许自己转给自己
        if (fromPosition == toPosition) {

            Toast.makeText(
                requireContext(),
                "转出账户和转入账户不能相同",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // 获取金额
        val amountText = binding.etAmount.text
            ?.toString()
            ?.trim()
            ?: ""

        if (amountText.isBlank()) {

            binding.etAmount.error = "请输入转账金额"

            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null) {

            binding.etAmount.error = "请输入正确的金额"

            return
        }

        if (amount <= 0.0) {

            binding.etAmount.error = "转账金额必须大于 0"

            return
        }

        // 获取原转账
        val oldTransfer = viewModel
            .getRepository()
            .getTransfers()
            .firstOrNull {
                it.id == transferId
            }

        if (oldTransfer == null) {

            Toast.makeText(
                requireContext(),
                "转账记录不存在或已被删除",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val fromAccount = accounts[fromPosition]

        val toAccount = accounts[toPosition]

        val note = binding.etNote.text
            ?.toString()
            ?.trim()
            ?: ""

        /**
         * 保留原 ID 和创建时间
         * 只修改用户编辑的字段
         */
        val newTransfer = oldTransfer.copy(
            fromAccountId = fromAccount.id,
            toAccountId = toAccount.id,
            amount = amount,
            note = note,
            timestamp = selectedTimestamp
        )

        val success = viewModel
            .getRepository()
            .updateTransfer(newTransfer)

        if (success) {

            // 刷新全局数据
            viewModel.refresh()

            Toast.makeText(
                requireContext(),
                "转账已修改",
                Toast.LENGTH_SHORT
            ).show()

            // 返回上一页，也就是转账详情
            parentFragmentManager.popBackStack()

        } else {

            Toast.makeText(
                requireContext(),
                "保存失败，请检查本地存储",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 出错后返回
     */
    private fun showErrorAndBack(
        message: String
    ) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_TRANSFER_ID = "transfer_id"

        fun newInstance(
            transferId: String
        ): EditTransferFragment {

            return EditTransferFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_TRANSFER_ID,
                        transferId
                    )
                }
            }
        }
    }
}