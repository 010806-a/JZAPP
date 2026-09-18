package com.example.myno.jz.ui.bills

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.myno.jz.R
import com.example.myno.jz.data.imports.CsvBillParser
import com.example.myno.jz.data.imports.XlsxBillParser
import com.example.myno.jz.databinding.FragmentImportBillBinding

class ImportBillFragment : Fragment() {

    private var _binding: FragmentImportBillBinding? = null
    private val binding get() = _binding!!

    private var selectedUri: Uri? = null

    private val filePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) {
                return@registerForActivityResult
            }

            selectedUri = uri

            showSelectedFile(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentImportBillBinding.inflate(
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
    }

    private fun setupButtons() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        binding.btnSelectFile.setOnClickListener {

            openFilePicker()
        }

        binding.btnStartImport.setOnClickListener {

            val uri = selectedUri

            if (uri == null) {

                Toast.makeText(
                    requireContext(),
                    "请先选择账单文件",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            parseSelectedFile(uri)
        }
    }

    private fun openFilePicker() {

        filePicker.launch(
            arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "text/csv",
                "text/plain",
                "application/csv",
                "*/*"
            )
        )
    }

    private fun parseSelectedFile(
        uri: Uri
    ) {

        try {

            val fileName =
                getFileName(uri)
                    ?: ""

            val lowerName =
                fileName.lowercase()

            val inputStream =
                requireContext()
                    .contentResolver
                    .openInputStream(uri)

            if (inputStream == null) {

                Toast.makeText(
                    requireContext(),
                    "无法读取文件",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            val items =

                if (
                    lowerName.endsWith(".xlsx")
                ) {

                    XlsxBillParser.parse(
                        inputStream
                    )

                } else {

                    val content =
                        inputStream
                            .bufferedReader(
                                Charsets.UTF_8
                            )
                            .use {
                                it.readText()
                            }

                    CsvBillParser.parse(
                        content
                    )
                }

            inputStream.close()

            if (items.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "没有识别到有效账单，请确认这是微信支付账单明细 XLSX 文件",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            ImportPreviewFragment
                .showParsedItems(
                    parentFragmentManager,
                    items
                )

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "账单解析失败：${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showSelectedFile(
        uri: Uri
    ) {

        val fileName =
            getFileName(uri)

        binding.tvFileName.text =
            fileName ?: "未知文件"

        binding.tvFileStatus.text =
            if (
                fileName
                    ?.lowercase()
                    ?.endsWith(".xlsx") == true
            ) {
                "已识别为 Excel XLSX 文件，可以开始解析"
            } else {
                "文件已选择，可以开始解析"
            }

        binding.tvFileStatus.visibility =
            View.VISIBLE

        binding.btnStartImport.isEnabled =
            true
    }

    private fun getFileName(
        uri: Uri
    ): String? {

        var result: String? = null

        requireContext()
            .contentResolver
            .query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val index =
                        cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                        )

                    if (index >= 0) {

                        result =
                            cursor.getString(index)
                    }
                }
            }

        return result
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}