package com.example.myno.jz.utils

import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * MoneyBook Excel 导出器
 *
 * 当前版本重点：
 * 1. 生成 XLSX
 * 2. 详细记录导出过程
 * 3. 导出完成后验证 XLSX ZIP 结构
 * 4. 验证三个工作表是否存在
 * 5. 验证账单明细是否真正写入 sheet1.xml
 */
class ExcelExporter {

    companion object {
        private const val TAG = "ExcelExporter"
    }

    /**
     * XLSX 验证结果
     */
    data class ValidationResult(
        val valid: Boolean,
        val message: String,
        val entryCount: Int,
        val sheet1HasData: Boolean,
        val sheet2Exists: Boolean,
        val sheet3Exists: Boolean
    )

    /**
     * 导出 Excel
     */
    fun export(
        outputStream: OutputStream,
        bills: List<Bill>,
        accounts: List<Account>,
        categories: List<Category>
    ) {

        AppLogger.i(
            TAG,
            "export() 开始：账单=${bills.size}，账户=${accounts.size}，分类=${categories.size}"
        )

        try {

            val sortedBills =
                bills.sortedByDescending {
                    it.timestamp
                }

            AppLogger.i(
                TAG,
                "账单排序完成：${sortedBills.size} 条"
            )

            val accountMap =
                accounts.associateBy {
                    it.id
                }

            val categoryMap =
                categories.associateBy {
                    it.id
                }

            AppLogger.i(
                TAG,
                "账户 Map 创建完成：${accountMap.size}"
            )

            AppLogger.i(
                TAG,
                "分类 Map 创建完成：${categoryMap.size}"
            )

            ZipOutputStream(outputStream).use { zip ->

                AppLogger.i(
                    TAG,
                    "ZipOutputStream 创建成功"
                )

                // =================================================
                // XLSX 基础文件
                // =================================================

                AppLogger.i(
                    TAG,
                    "开始写入 [Content_Types].xml"
                )

                writeEntry(
                    zip,
                    "[Content_Types].xml",
                    contentTypesXml()
                )

                AppLogger.i(
                    TAG,
                    "开始写入 _rels/.rels"
                )

                writeEntry(
                    zip,
                    "_rels/.rels",
                    rootRelsXml()
                )

                AppLogger.i(
                    TAG,
                    "开始写入 xl/workbook.xml"
                )

                writeEntry(
                    zip,
                    "xl/workbook.xml",
                    workbookXml()
                )

                AppLogger.i(
                    TAG,
                    "开始写入 xl/_rels/workbook.xml.rels"
                )

                writeEntry(
                    zip,
                    "xl/_rels/workbook.xml.rels",
                    workbookRelsXml()
                )

                // =================================================
                // 工作表 1
                // =================================================

                AppLogger.i(
                    TAG,
                    "开始生成 sheet1：账单明细"
                )

                val detailXml =
                    detailSheetXml(
                        sortedBills,
                        accountMap,
                        categoryMap
                    )

                AppLogger.i(
                    TAG,
                    "sheet1 XML 生成完成：${detailXml.length} 字符"
                )

                writeEntry(
                    zip,
                    "xl/worksheets/sheet1.xml",
                    detailXml
                )

                // =================================================
                // 工作表 2
                // =================================================

                AppLogger.i(
                    TAG,
                    "开始生成 sheet2：统计汇总"
                )

                val summaryXml =
                    summarySheetXml(
                        sortedBills,
                        categoryMap
                    )

                AppLogger.i(
                    TAG,
                    "sheet2 XML 生成完成：${summaryXml.length} 字符"
                )

                writeEntry(
                    zip,
                    "xl/worksheets/sheet2.xml",
                    summaryXml
                )

                // =================================================
                // 工作表 3
                // =================================================

                AppLogger.i(
                    TAG,
                    "开始生成 sheet3：账户汇总"
                )

                val accountXml =
                    accountSheetXml(
                        sortedBills,
                        accounts
                    )

                AppLogger.i(
                    TAG,
                    "sheet3 XML 生成完成：${accountXml.length} 字符"
                )

                writeEntry(
                    zip,
                    "xl/worksheets/sheet3.xml",
                    accountXml
                )

                AppLogger.i(
                    TAG,
                    "所有 XLSX ZIP Entry 写入完成"
                )
            }

            AppLogger.i(
                TAG,
                "ZipOutputStream 已关闭，export() 完成"
            )

        } catch (e: Exception) {

            AppLogger.e(
                TAG,
                "export() 发生异常",
                e
            )

            throw e
        }
    }

    /**
     * 写入 ZIP Entry
     */
    private fun writeEntry(
        zip: ZipOutputStream,
        path: String,
        content: String
    ) {

        val bytes =
            content.toByteArray(
                Charsets.UTF_8
            )

        AppLogger.i(
            TAG,
            "写入 ZIP Entry：$path，${bytes.size} bytes"
        )

        zip.putNextEntry(
            ZipEntry(path)
        )

        zip.write(bytes)

        zip.closeEntry()

        AppLogger.i(
            TAG,
            "ZIP Entry 写入完成：$path"
        )
    }

    // =============================================================
    // 1. 账单明细
    // =============================================================

    private fun detailSheetXml(
        bills: List<Bill>,
        accountMap: Map<String, Account>,
        categoryMap: Map<String, Category>
    ): String {

        AppLogger.i(
            TAG,
            "detailSheetXml() 开始，账单数量=${bills.size}"
        )

        val rows =
            StringBuilder()

        rows.append(
            row(
                1,
                listOf(
                    textCell("A1", "日期"),
                    textCell("B1", "收支类型"),
                    textCell("C1", "分类"),
                    textCell("D1", "金额"),
                    textCell("E1", "账户"),
                    textCell("F1", "备注")
                )
            )

        )

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            )

        var rowNumber = 2

        for (bill in bills) {

            val date =
                dateFormat.format(
                    Date(bill.timestamp)
                )

            val type =
                when (bill.type) {

                    BillType.INCOME ->
                        "收入"

                    BillType.EXPENSE ->
                        "支出"
                }

            val category =
                categoryMap[bill.categoryId]?.name
                    ?: "未分类"

            val account =
                accountMap[bill.accountId]?.name
                    ?: "未知账户"

            val note =
                bill.note ?: ""

            AppLogger.i(
                TAG,
                "生成账单行：Excel第${rowNumber}行，billId=${bill.id}，类型=$type，金额=${bill.amount}，分类=$category，账户=$account，备注长度=${note.length}"
            )

            rows.append(
                row(
                    rowNumber,
                    listOf(
                        textCell(
                            "A$rowNumber",
                            date
                        ),
                        textCell(
                            "B$rowNumber",
                            type
                        ),
                        textCell(
                            "C$rowNumber",
                            category
                        ),
                        numberCell(
                            "D$rowNumber",
                            bill.amount
                        ),
                        textCell(
                            "E$rowNumber",
                            account
                        ),
                        textCell(
                            "F$rowNumber",
                            note
                        )
                    )
                )
            )

            rowNumber++
        }

        val result =
            worksheetXml(
                rows = rows.toString()
            )

        AppLogger.i(
            TAG,
            "detailSheetXml() 完成：数据行=${rowNumber - 2}，XML长度=${result.length}"
        )

        return result
    }

    // =============================================================
    // 2. 统计汇总
    // =============================================================

    private fun summarySheetXml(
        bills: List<Bill>,
        categoryMap: Map<String, Category>
    ): String {

        AppLogger.i(
            TAG,
            "summarySheetXml() 开始"
        )

        val rows =
            StringBuilder()

        val totalIncome =
            bills
                .filter {
                    it.type == BillType.INCOME
                }
                .sumOf {
                    it.amount
                }

        val totalExpense =
            bills
                .filter {
                    it.type == BillType.EXPENSE
                }
                .sumOf {
                    it.amount
                }

        val balance =
            totalIncome - totalExpense

        AppLogger.i(
            TAG,
            "统计计算：收入=$totalIncome，支出=$totalExpense，结余=$balance"
        )

        rows.append(
            row(
                1,
                listOf(
                    textCell(
                        "A1",
                        "记账统计汇总"
                    )
                )
            )
        )

        rows.append(
            row(
                3,
                listOf(
                    textCell(
                        "A3",
                        "统计项目"
                    ),
                    textCell(
                        "B3",
                        "金额"
                    )
                )
            )
        )

        rows.append(
            row(
                4,
                listOf(
                    textCell(
                        "A4",
                        "总收入"
                    ),
                    numberCell(
                        "B4",
                        totalIncome
                    )
                )
            )
        )

        rows.append(
            row(
                5,
                listOf(
                    textCell(
                        "A5",
                        "总支出"
                    ),
                    numberCell(
                        "B5",
                        totalExpense
                    )
                )
            )

        )

        rows.append(
            row(
                6,
                listOf(
                    textCell(
                        "A6",
                        "结余"
                    ),
                    numberCell(
                        "B6",
                        balance
                    )
                )
            )
        )

        rows.append(
            row(
                7,
                listOf(
                    textCell(
                        "A7",
                        "账单笔数"
                    ),
                    numberCell(
                        "B7",
                        bills.size.toDouble()
                    )
                )
            )
        )

        rows.append(
            row(
                9,
                listOf(
                    textCell(
                        "A9",
                        "收入分类统计"
                    )
                )
            )
        )

        rows.append(
            row(
                10,
                listOf(
                    textCell(
                        "A10",
                        "分类"
                    ),
                    textCell(
                        "B10",
                        "金额"
                    )
                )
            )
        )

        val incomeGroups =
            bills
                .filter {
                    it.type == BillType.INCOME
                }
                .groupBy {
                    it.categoryId
                }

        var incomeRow = 11

        for ((categoryId, categoryBills) in incomeGroups) {

            val categoryName =
                categoryMap[categoryId]?.name
                    ?: "未分类"

            val amount =
                categoryBills.sumOf {
                    it.amount
                }

            AppLogger.i(
                TAG,
                "收入分类：$categoryName=$amount"
            )

            rows.append(
                row(
                    incomeRow,
                    listOf(
                        textCell(
                            "A$incomeRow",
                            categoryName
                        ),
                        numberCell(
                            "B$incomeRow",
                            amount
                        )
                    )
                )
            )

            incomeRow++
        }

        val expenseTitleRow =
            incomeRow + 1

        rows.append(
            row(
                expenseTitleRow,
                listOf(
                    textCell(
                        "A$expenseTitleRow",
                        "支出分类统计"
                    )
                )
            )

        )

        val expenseHeaderRow =
            expenseTitleRow + 1

        rows.append(
            row(
                expenseHeaderRow,
                listOf(
                    textCell(
                        "A$expenseHeaderRow",
                        "分类"
                    ),
                    textCell(
                        "B$expenseHeaderRow",
                        "金额"
                    )
                )
            )
        )

        val expenseGroups =
            bills
                .filter {
                    it.type == BillType.EXPENSE
                }
                .groupBy {
                    it.categoryId
                }

        var expenseRow =
            expenseHeaderRow + 1

        for ((categoryId, categoryBills) in expenseGroups) {

            val categoryName =
                categoryMap[categoryId]?.name
                    ?: "未分类"

            val amount =
                categoryBills.sumOf {
                    it.amount
                }

            AppLogger.i(
                TAG,
                "支出分类：$categoryName=$amount"
            )

            rows.append(
                row(
                    expenseRow,
                    listOf(
                        textCell(
                            "A$expenseRow",
                            categoryName
                        ),
                        numberCell(
                            "B$expenseRow",
                            amount
                        )
                    )
                )
            )

            expenseRow++
        }

        val result =
            worksheetXml(
                rows = rows.toString()
            )

        AppLogger.i(
            TAG,
            "summarySheetXml() 完成：XML长度=${result.length}"
        )

        return result
    }

    // =============================================================
    // 3. 账户汇总
    // =============================================================

    private fun accountSheetXml(
        bills: List<Bill>,
        accounts: List<Account>
    ): String {

        AppLogger.i(
            TAG,
            "accountSheetXml() 开始，账户数量=${accounts.size}"
        )

        val rows =
            StringBuilder()

        rows.append(
            row(
                1,
                listOf(
                    textCell(
                        "A1",
                        "账户汇总"
                    )
                )
            )
        )

        rows.append(
            row(
                3,
                listOf(
                    textCell(
                        "A3",
                        "账户名称"
                    ),
                    textCell(
                        "B3",
                        "初始余额"
                    ),
                    textCell(
                        "C3",
                        "收入"
                    ),
                    textCell(
                        "D3",
                        "支出"
                    ),
                    textCell(
                        "E3",
                        "当前余额"
                    )
                )
            )
        )

        var rowNumber = 4

        for (account in accounts) {

            val accountBills =
                bills.filter {
                    it.accountId == account.id
                }

            val income =
                accountBills
                    .filter {
                        it.type == BillType.INCOME
                    }
                    .sumOf {
                        it.amount
                    }

            val expense =
                accountBills
                    .filter {
                        it.type == BillType.EXPENSE
                    }
                    .sumOf {
                        it.amount
                    }

            val currentBalance =
                account.balance +
                    income -
                    expense

            AppLogger.i(
                TAG,
                "生成账户行：第${rowNumber}行，账户=${account.name}，初始=${account.balance}，收入=$income，支出=$expense，当前=$currentBalance"
            )

            rows.append(
                row(
                    rowNumber,
                    listOf(
                        textCell(
                            "A$rowNumber",
                            account.name
                        ),
                        numberCell(
                            "B$rowNumber",
                            account.balance
                        ),
                        numberCell(
                            "C$rowNumber",
                            income
                        ),
                        numberCell(
                            "D$rowNumber",
                            expense
                        ),
                        numberCell(
                            "E$rowNumber",
                            currentBalance
                        )
                    )
                )
            )

            rowNumber++
        }

        val result =
            worksheetXml(
                rows = rows.toString()
            )

        AppLogger.i(
            TAG,
            "accountSheetXml() 完成：数据行=${rowNumber - 4}，XML长度=${result.length}"
        )

        return result
    }

    // =============================================================
    // XLSX 验证
    // =============================================================

    fun validate(
        inputStream: InputStream
    ): ValidationResult {

        AppLogger.i(
            TAG,
            "validate() 开始验证导出的 XLSX"
        )

        val entries =
            mutableMapOf<String, String>()

        try {

            ZipInputStream(
                inputStream
            ).use { zip ->

                var entry: ZipEntry?

                while (true) {

                    entry =
                        zip.nextEntry
                            ?: break

                    val name =
                        entry!!.name

                    AppLogger.i(
                        TAG,
                        "验证发现 ZIP Entry：$name"
                    )

                    val content =
                        zip.readBytes()
                            .toString(
                                Charsets.UTF_8
                            )

                    entries[name] =
                        content

                    zip.closeEntry()
                }
            }

            AppLogger.i(
                TAG,
                "XLSX ZIP 读取完成，共 ${entries.size} 个 Entry"
            )

            val requiredEntries =
                listOf(
                    "[Content_Types].xml",
                    "_rels/.rels",
                    "xl/workbook.xml",
                    "xl/_rels/workbook.xml.rels",
                    "xl/worksheets/sheet1.xml",
                    "xl/worksheets/sheet2.xml",
                    "xl/worksheets/sheet3.xml"
                )

            val missing =
                requiredEntries.filter {
                    !entries.containsKey(it)
                }

            if (missing.isNotEmpty()) {

                val message =
                    "缺少 Entry：${missing.joinToString()}"

                AppLogger.e(
                    TAG,
                    message
                )

                return ValidationResult(
                    valid = false,
                    message = message,
                    entryCount = entries.size,
                    sheet1HasData = false,
                    sheet2Exists =
                        entries.containsKey(
                            "xl/worksheets/sheet2.xml"
                        ),
                    sheet3Exists =
                        entries.containsKey(
                            "xl/worksheets/sheet3.xml"
                        )
                )
            }

            val sheet1 =
                entries[
                    "xl/worksheets/sheet1.xml"
                ].orEmpty()

            val sheet2Exists =
                entries.containsKey(
                    "xl/worksheets/sheet2.xml"
                )

            val sheet3Exists =
                entries.containsKey(
                    "xl/worksheets/sheet3.xml"
                )

            val sheet1HasData =
                sheet1.contains(
                    "<row r=\"2\">"
                ) &&
                    sheet1.contains(
                        "A2"
                    )

            AppLogger.i(
                TAG,
                "sheet1 是否存在账单数据：$sheet1HasData"
            )

            AppLogger.i(
                TAG,
                "sheet2 是否存在：$sheet2Exists"
            )

            AppLogger.i(
                TAG,
                "sheet3 是否存在：$sheet3Exists"
            )

            if (!sheet1HasData) {

                val message =
                    "sheet1.xml 存在，但没有检测到账单数据行"

                AppLogger.e(
                    TAG,
                    message
                )

                return ValidationResult(
                    valid = false,
                    message = message,
                    entryCount = entries.size,
                    sheet1HasData = false,
                    sheet2Exists = sheet2Exists,
                    sheet3Exists = sheet3Exists
                )
            }

            val message =
                "XLSX 结构完整，sheet1/sheet2/sheet3 均存在，sheet1 检测到账单数据"

            AppLogger.i(
                TAG,
                message
            )

            return ValidationResult(
                valid = true,
                message = message,
                entryCount = entries.size,
                sheet1HasData = true,
                sheet2Exists = sheet2Exists,
                sheet3Exists = sheet3Exists
            )

        } catch (e: Exception) {

            AppLogger.e(
                TAG,
                "validate() 验证 XLSX 时发生异常",
                e
            )

            return ValidationResult(
                valid = false,
                message =
                    "验证异常：${e.message ?: "未知错误"}",
                entryCount = entries.size,
                sheet1HasData = false,
                sheet2Exists =
                    entries.containsKey(
                        "xl/worksheets/sheet2.xml"
                    ),
                sheet3Exists =
                    entries.containsKey(
                        "xl/worksheets/sheet3.xml"
                    )
            )
        }
    }

    // =============================================================
    // Worksheet
    // =============================================================

    private fun worksheetXml(
        rows: String
    ): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                    $rows
                </sheetData>
            </worksheet>
        """.trimIndent()
    }

    // =============================================================
    // Row
    // =============================================================

    private fun row(
        rowNumber: Int,
        cells: List<String>
    ): String {

        return """
            <row r="$rowNumber">
                ${cells.joinToString("")}
            </row>
        """.trimIndent()
    }

    // =============================================================
    // 文本单元格
    // =============================================================

    private fun textCell(
        reference: String,
        value: String
    ): String {

        val safeValue =
            escapeXml(value)

        return """
            <c r="$reference" t="inlineStr">
                <is>
                    <t xml:space="preserve">$safeValue</t>
                </is>
            </c>
        """.trimIndent()
    }

    // =============================================================
    // 数字单元格
    // =============================================================

    private fun numberCell(
        reference: String,
        value: Double
    ): String {

        val formatted =
            String.format(
                Locale.US,
                "%.2f",
                value
            )

        return """
            <c r="$reference">
                <v>$formatted</v>
            </c>
        """.trimIndent()
    }

    // =============================================================
    // Workbook
    // =============================================================

    private fun workbookXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook
                xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">

                <sheets>

                    <sheet
                        name="账单明细"
                        sheetId="1"
                        r:id="rId1"/>

                    <sheet
                        name="统计汇总"
                        sheetId="2"
                        r:id="rId2"/>

                    <sheet
                        name="账户汇总"
                        sheetId="3"
                        r:id="rId3"/>

                </sheets>

            </workbook>
        """.trimIndent()
    }

    // =============================================================
    // Workbook Relationships
    // =============================================================

    private fun workbookRelsXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>

            <Relationships
                xmlns="http://schemas.openxmlformats.org/package/2006/relationships">

                <Relationship
                    Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet1.xml"/>

                <Relationship
                    Id="rId2"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet2.xml"/>

                <Relationship
                    Id="rId3"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet3.xml"/>

            </Relationships>
        """.trimIndent()
    }

    // =============================================================
    // Root Relationships
    // =============================================================

    private fun rootRelsXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>

            <Relationships
                xmlns="http://schemas.openxmlformats.org/package/2006/relationships">

                <Relationship
                    Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                    Target="xl/workbook.xml"/>

            </Relationships>
        """.trimIndent()
    }

    // =============================================================
    // Content Types
    // =============================================================

    private fun contentTypesXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>

            <Types
                xmlns="http://schemas.openxmlformats.org/package/2006/content-types">

                <Default
                    Extension="rels"
                    ContentType="application/vnd.openxmlformats-package.relationships+xml"/>

                <Default
                    Extension="xml"
                    ContentType="application/xml"/>

                <Override
                    PartName="/xl/workbook.xml"
                    ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>

                <Override
                    PartName="/xl/worksheets/sheet1.xml"
                    ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>

                <Override
                    PartName="/xl/worksheets/sheet2.xml"
                    ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>

                <Override
                    PartName="/xl/worksheets/sheet3.xml"
                    ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>

            </Types>
        """.trimIndent()
    }

    // =============================================================
    // XML 转义
    // =============================================================

    private fun escapeXml(
        value: String
    ): String {

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}