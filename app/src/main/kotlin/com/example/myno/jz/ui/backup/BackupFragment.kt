package com.example.myno.jz.ui.backup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.myno.jz.data.repository.BackupRepository
import com.example.myno.jz.data.repository.FinanceRepository
import com.example.myno.jz.databinding.FragmentBackupBinding
import com.example.myno.jz.utils.ExcelExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFragment : Fragment() {

private var _binding: FragmentBackupBinding? = null
private val binding get() = _binding!!

private lateinit var backupRepository: BackupRepository
private lateinit var financeRepository: FinanceRepository
private lateinit var excelExporter: ExcelExporter

/**
 * 创建 Excel 文件
 */
private val createExcelDocument =
    registerForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri: Uri? ->

        if (uri == null) {
            return@registerForActivityResult
        }

        exportExcel(uri)
    }

/**
 * 选择恢复备份文件
 */
private val restoreDocument =
    registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri == null) {
            return@registerForActivityResult
        }

        restoreFromUri(uri)
    }

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
    super.onViewCreated(
        view,
        savedInstanceState
    )

    backupRepository =
        BackupRepository(requireContext())

    financeRepository =
        FinanceRepository(requireContext())

    excelExporter =
        ExcelExporter()

    setupViews()
    updateBackupInfo()
    updateBackupList()
}

private fun setupViews() {

    // 返回
    binding.btnBack.setOnClickListener {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    // Excel 导出
    binding.btnExportExcel.setOnClickListener {

        val fileName =
            "MoneyBook_账单_" +
                System.currentTimeMillis() +
                ".xlsx"

        createExcelDocument.launch(fileName)
    }

    // 完整备份
    binding.btnBackup.setOnClickListener {
        createBackup()
    }

    // 恢复备份
    binding.btnRestore.setOnClickListener {
        restoreBackup()
    }
}

/**
 * 导出 Excel
 */
private fun exportExcel(uri: Uri) {
    try {
        val bills = financeRepository.getBills()
        val accounts = financeRepository.getAccounts()
        val categories = financeRepository.getCategories()

        Toast.makeText(
            requireContext(),
            "准备导出：账单 ${bills.size} 条，账户 ${accounts.size} 个，分类 ${categories.size} 个",
            Toast.LENGTH_LONG
        ).show()

        requireContext()
            .contentResolver
            .openOutputStream(uri)
            ?.use { outputStream ->
                excelExporter.export(
                    outputStream = outputStream,
                    bills = bills,
                    accounts = accounts,
                    categories = categories
                )
            }
            ?: throw IllegalStateException("无法打开文件输出流")

        Toast.makeText(
            requireContext(),
            "Excel 导出成功：账单 ${bills.size} 条",
            Toast.LENGTH_LONG
        ).show()

    } catch (e: Exception) {
        Toast.makeText(
            requireContext(),
            "Excel 导出失败：${e.message ?: "未知错误"}",
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * 创建完整备份
 */
private fun createBackup() {

    try {

        val backupFile =
            backupRepository.createBackup()

        Toast.makeText(
            requireContext(),
            "备份成功：${backupFile.name}",
            Toast.LENGTH_SHORT
        ).show()

        updateBackupList()

    } catch (e: Exception) {

        Toast.makeText(
            requireContext(),
            "备份失败：${e.message ?: "未知错误"}",
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * 打开备份文件选择器
 */
private fun restoreBackup() {

    restoreDocument.launch(
        arrayOf(
            "application/json",
            "application/octet-stream",
            "*/*"
        )
    )
}

/**
 * 将系统选择的 Uri 转换成临时 File，
 * 然后交给 BackupRepository.restoreBackup(File)
 */
private fun restoreFromUri(uri: Uri) {

    var tempFile: File? = null

    try {

        val inputStream =
            requireContext()
                .contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "无法打开备份文件"
                )

        tempFile =
            File.createTempFile(
                "MoneyBook_restore_",
                ".json",
                requireContext().cacheDir
            )

        inputStream.use { input ->

            tempFile.outputStream().use { output ->

                input.copyTo(output)
            }
        }

        val result =
            backupRepository.restoreBackup(
                tempFile
            )

        if (result) {

            Toast.makeText(
                requireContext(),
                "恢复成功，请重新进入页面查看数据",
                Toast.LENGTH_LONG
            ).show()

            updateBackupInfo()

        } else {

            Toast.makeText(
                requireContext(),
                "恢复失败：备份文件无效或版本不支持",
                Toast.LENGTH_LONG
            ).show()
        }

    } catch (e: Exception) {

        Toast.makeText(
            requireContext(),
            "恢复失败：${e.message ?: "未知错误"}",
            Toast.LENGTH_LONG
        ).show()

    } finally {

        tempFile?.delete()
    }
}

/**
 * 更新本地数据统计
 */
private fun updateBackupInfo() {

    try {

        val bills =
            financeRepository.getBills()

        val accounts =
            financeRepository.getAccounts()

        val categories =
            financeRepository.getCategories()

        binding.tvBackupInfo.text =
            buildString {

                append("账单：")
                append(bills.size)
                append(" 条\n")

                append("账户：")
                append(accounts.size)
                append(" 个\n")

                append("分类：")
                append(categories.size)
                append(" 个\n\n")

                append("支持完整备份、恢复以及 Excel 导出")
            }

    } catch (_: Exception) {

        binding.tvBackupInfo.text =
            "本地数据读取失败"
    }
}

/**
 * 更新历史备份列表
 */
private fun updateBackupList() {

    try {

        val backupFiles =
            backupRepository.getBackupFiles()

        if (backupFiles.isEmpty()) {

            binding.tvEmpty.visibility =
                View.VISIBLE

            binding.tvBackupList.text = ""

            return
        }

        binding.tvEmpty.visibility =
            View.GONE

        val formatter =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            )

        binding.tvBackupList.text =
            buildString {

                backupFiles.forEachIndexed { index, file ->

                    if (index > 0) {
                        append("\n\n")
                    }

                    append("备份 ")
                    append(index + 1)
                    append("\n")

                    append("文件：")
                    append(file.name)
                    append("\n")

                    append("时间：")
                    append(
                        formatter.format(
                            Date(file.lastModified())
                        )
                    )
                    append("\n")

                    append("大小：")
                    append(formatFileSize(file.length()))
                }
            }

    } catch (_: Exception) {

        binding.tvEmpty.visibility =
            View.VISIBLE

        binding.tvEmpty.text =
            "暂无备份"

        binding.tvBackupList.text = ""
    }
}

/**
 * 文件大小格式化
 */
private fun formatFileSize(size: Long): String {

    return when {

        size < 1024 ->
            "$size B"

        size < 1024 * 1024 ->
            String.format(
                Locale.getDefault(),
                "%.1f KB",
                size / 1024.0
            )

        else ->
            String.format(
                Locale.getDefault(),
                "%.1f MB",
                size / (1024.0 * 1024.0)
            )
    }
}

override fun onResume() {
    super.onResume()

    if (_binding != null) {
        updateBackupInfo()
        updateBackupList()
    }
}

override fun onDestroyView() {

    super.onDestroyView()

    _binding = null
}

}