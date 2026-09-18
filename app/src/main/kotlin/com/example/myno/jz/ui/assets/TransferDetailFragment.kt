package com.example.myno.jz.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myno.jz.data.model.Transfer
import com.example.myno.jz.databinding.FragmentTransferDetailBinding
import com.example.myno.jz.ui.main.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransferDetailFragment : Fragment() {

    private var _binding: FragmentTransferDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var transferId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transferId =
            arguments?.getString(ARG_TRANSFER_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentTransferDetailBinding.inflate(
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

        setupButtons()
        loadTransfer()
    }

 private fun setupButtons() {

    binding.btnBack.setOnClickListener {
        parentFragmentManager.popBackStack()
    }

    binding.btnEdit.setOnClickListener {

        val id = transferId
            ?: return@setOnClickListener

        parentFragmentManager.beginTransaction()
            .replace(
                com.example.myno.jz.R.id.fragmentContainer,
                EditTransferFragment.newInstance(id)
            )
            .addToBackStack(null)
            .commit()
    }

    binding.btnDelete.setOnClickListener {
        showDeleteConfirmDialog()
    }
}

    private fun loadTransfer() {

        val id = transferId

        if (id.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                "转账信息不存在",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
            return
        }

        val transfer =
            viewModel.getRepository()
                .getTransfers()
                .firstOrNull {
                    it.id == id
                }

        if (transfer == null) {

            Toast.makeText(
                requireContext(),
                "转账记录不存在或已被删除",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
            return
        }

        showTransfer(transfer)
    }

    private fun showTransfer(
        transfer: Transfer
    ) {

        val accounts =
            viewModel.accounts.value.orEmpty()

        val fromAccount =
            accounts.firstOrNull {
                it.id == transfer.fromAccountId
            }

        val toAccount =
            accounts.firstOrNull {
                it.id == transfer.toAccountId
            }

        binding.tvFromAccount.text =
            fromAccount?.let {
                "${it.icon}  ${it.name}"
            } ?: "未知账户"

        binding.tvToAccount.text =
            toAccount?.let {
                "${it.icon}  ${it.name}"
            } ?: "未知账户"

        binding.tvAmount.text =
            "¥${formatMoney(transfer.amount)}"

        binding.tvTime.text =
            formatDateTime(transfer.timestamp)

        binding.tvNote.text =
            if (transfer.note.isBlank()) {
                "无备注"
            } else {
                transfer.note
            }
    }

    private fun showDeleteConfirmDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle("删除转账")
            .setMessage(
                "确定要删除这笔转账记录吗？\n\n" +
                        "删除后会同时影响两个账户的余额和账户流水。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                deleteTransfer()
            }
            .show()
    }

    private fun deleteTransfer() {

        val id = transferId

        if (id.isNullOrBlank()) {
            return
        }

        val success =
            viewModel.getRepository()
                .deleteTransfer(id)

        if (success) {

            viewModel.refresh()

            Toast.makeText(
                requireContext(),
                "转账已删除",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()

        } else {

            Toast.makeText(
                requireContext(),
                "删除失败，请稍后重试",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun formatMoney(
        value: Double
    ): String {

        return String.format(
            Locale.getDefault(),
            "%.2f",
            value
        )
    }

    private fun formatDateTime(
        timestamp: Long
    ): String {

        return SimpleDateFormat(
            "yyyy年MM月dd日 HH:mm",
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_TRANSFER_ID =
            "transfer_id"

        fun newInstance(
            transferId: String
        ): TransferDetailFragment {

            return TransferDetailFragment().apply {

                arguments =
                    Bundle().apply {
                        putString(
                            ARG_TRANSFER_ID,
                            transferId
                        )
                    }
            }
        }
    }
}