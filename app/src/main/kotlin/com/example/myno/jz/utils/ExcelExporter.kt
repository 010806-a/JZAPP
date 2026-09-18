package com.example.myno.jz.utils

import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * MoneyBook Excel 导出器
 *
 * 生成标准 XLSX 文件。
 *
 * 工作表：
 * 1. 账单明细
 * 2. 统计汇总
 * 3. 账户汇总
 *
 * 本版本故意采用最小 XLSX 结构：
 * - 不使用 styles.xml
 * - 不使用 sharedStrings.xml
 * - 使用 inlineStr
 * - 数字直接写入 <v>
 *
 * 目的：
 * 最大程度降低 WPS / Excel / Android 文件管理器
 * 对手写 XLSX XML 的兼容性问题。
 */
class ExcelExporter {

    fun export(
        outputStream: OutputStream,
        bills: List<Bill>,
        accounts: List<Account>,
        categories: List<Category>
    ) {

        val sortedBills =
            bills.sortedByDescending { it.timestamp }

        val accountMap =
            accounts.associateBy { it.id }

        val categoryMap =
            categories.associateBy { it.id }

        ZipOutputStream(outputStream).use { zip ->

            // =====================================================
            // XLSX 基础文件
            // =====================================================

            writeEntry(
                zip,
                "[Content_Types].xml",
                contentTypesXml()
            )

            writeEntry(
                zip,
                "_rels/.rels",
                rootRelsXml()
            )

            writeEntry(
                zip,
                "xl/workbook.xml",
                workbookXml()
            )

            writeEntry(
                zip,
                "xl/_rels/workbook.xml.rels",
                workbookRelsXml()
            )

            // =====================================================
            // 工作表
            // =====================================================

            writeEntry(
                zip,
                "xl/worksheets/sheet1.xml",
                detailSheetXml(
                    sortedBills,
                    accountMap,
                    categoryMap
                )
            )

            writeEntry(
                zip,
                "xl/worksheets/sheet2.xml",
                summarySheetXml(
                    sortedBills,
                    categoryMap
                )
            )

            writeEntry(
                zip,
                "xl/worksheets/sheet3.xml",
                accountSheetXml(
                    sortedBills,
                    accounts
                )
            )
        }
    }

    // =============================================================
    // 写入 ZIP Entry
    // =============================================================

    private fun writeEntry(
        zip: ZipOutputStream,
        path: String,
        content: String
    ) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(
            content.toByteArray(Charsets.UTF_8)
        )
        zip.closeEntry()
    }

    // =============================================================
    // 1. 账单明细
    // =============================================================

    private fun detailSheetXml(
        bills: List<Bill>,
        accountMap: Map<String, Account>,
        categoryMap: Map<String, Category>
    ): String {

        val rows = StringBuilder()

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
                    BillType.INCOME -> "收入"
                    BillType.EXPENSE -> "支出"
                }

            val category =
                categoryMap[bill.categoryId]?.name
                    ?: "未分类"

            val account =
                accountMap[bill.accountId]?.name
                    ?: "未知账户"

            val note =
                bill.note ?: ""

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

        return worksheetXml(
            rows = rows.toString()
        )
    }

    // =============================================================
    // 2. 统计汇总
    // =============================================================

    private fun summarySheetXml(
        bills: List<Bill>,
        categoryMap: Map<String, Category>
    ): String {

        val rows = StringBuilder()

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

        // 标题
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

        // 基础统计标题
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

        // =====================================================
        // 收入分类
        // =====================================================

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

        // =====================================================
        // 支出分类
        // =====================================================

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

        return worksheetXml(
            rows = rows.toString()
        )
    }

    // =============================================================
    // 3. 账户汇总
    // =============================================================

    private fun accountSheetXml(
        bills: List<Bill>,
        accounts: List<Account>
    ): String {

        val rows = StringBuilder()

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

        return worksheetXml(
            rows = rows.toString()
        )
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