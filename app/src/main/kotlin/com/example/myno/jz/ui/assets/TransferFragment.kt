package com.example.myno.jz.ui.assets

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
import com.example.myno.jz.databinding.FragmentTransferBinding
import com.example.myno.jz.ui.main.MainViewModel
import java.util.UUID

class TransferFragment : Fragment() {

private var _binding: FragmentTransferBinding? = null
private val binding get() = _binding!!

private val viewModel: MainViewModel by activityViewModels()

private var accounts: List<Account> = emptyList()

override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
): View {
    _binding = FragmentTransferBinding.inflate(inflater, container, false)
    return binding.root
}

override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
) {
    super.onViewCreated(view, savedInstanceState)

    loadAccounts()
    setupButtons()
}

private fun loadAccounts() {
    accounts = viewModel.accounts.value
        ?.filter { it.enabled }
        ?.sortedBy { it.sortOrder }
        ?: emptyList()

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

    if (accounts.size >= 2) {
        binding.spinnerFromAccount.setSelection(0)
        binding.spinnerToAccount.setSelection(1)
    }
}

private fun setupButtons() {

    binding.btnBack.setOnClickListener {
        parentFragmentManager.popBackStack()
    }

    binding.btnSave.setOnClickListener {
        saveTransfer()
    }
}

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

    if (fromPosition == toPosition) {
        Toast.makeText(
            requireContext(),
            "转出账户和转入账户不能相同",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val amountText =
        binding.etAmount.text?.toString()?.trim() ?: ""

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

    val fromAccount = accounts[fromPosition]
    val toAccount = accounts[toPosition]

    val note =
        binding.etNote.text?.toString()?.trim() ?: ""

    val transfer = Transfer(
        id = UUID.randomUUID().toString(),
        fromAccountId = fromAccount.id,
        toAccountId = toAccount.id,
        amount = amount,
        note = note,
        timestamp = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis()
    )

    val success =
        viewModel.getRepository().addTransfer(transfer)

    if (success) {

        viewModel.refresh()

        Toast.makeText(
            requireContext(),
            "转账成功",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()

    } else {

        Toast.makeText(
            requireContext(),
            "转账保存失败，请检查本地存储",
            Toast.LENGTH_LONG
        ).show()
    }
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}

}