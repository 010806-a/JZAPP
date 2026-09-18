package com.example.myno.jz.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.myno.jz.databinding.FragmentBackupBinding
import java.io.File

class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: BackupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBackupBinding.inflate(
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

        setupViews()
        observeData()

        viewModel.loadBackups()
    }

    private fun setupViews() {

        binding.btnBackup.setOnClickListener {

            AlertDialog.Builder(requireContext())
                .setTitle("备份数据")
                .setMessage(
                    "将备份当前记账数据。\n\n" +
                    "建议定期备份重要数据。"
                )
                .setNegativeButton("取消", null)
                .setPositiveButton("立即备份") { _, _ ->

                    viewModel.createBackup()
                }
                .show()
        }

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    private fun observeData() {

        viewModel.message.observe(
            viewLifecycleOwner
        ) { message ->

            if (!message.isNullOrBlank()) {

                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.backupFiles.observe(
            viewLifecycleOwner
        ) { files ->

            updateBackupList(files)
        }
    }

    private fun updateBackupList(
        files: List<File>
    ) {

        if (files.isEmpty()) {

            binding.tvEmpty.visibility =
                View.VISIBLE

            binding.tvBackupList.visibility =
                View.GONE

            return
        }

        binding.tvEmpty.visibility =
            View.GONE

        binding.tvBackupList.visibility =
            View.VISIBLE

        binding.tvBackupList.text =
            files.joinToString("\n\n") {
                "• ${it.name}"
            }

        binding.tvBackupList.setOnClickListener {

            if (files.isNotEmpty()) {

                showRestoreDialog(files)
            }
        }
    }

    private fun showRestoreDialog(
        files: List<File>
    ) {

        val names =
            files.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("选择备份")

            .setItems(names) { _, which ->

                confirmRestore(files[which])
            }

            .setNegativeButton("取消", null)

            .show()
    }

    private fun confirmRestore(file: File) {

        AlertDialog.Builder(requireContext())
            .setTitle("确认恢复？")
            .setMessage(
                "恢复后，当前记账数据将被备份文件中的数据替换。\n\n" +
                "备份文件：\n${file.name}\n\n" +
                "建议先备份当前数据。"
            )
            .setNegativeButton(
                "取消",
                null
            )
            .setPositiveButton(
                "确认恢复"
            ) { _, _ ->

                viewModel.restoreBackup(file)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}