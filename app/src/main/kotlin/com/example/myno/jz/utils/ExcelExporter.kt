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
 * 标准 XLSX
 *
 * 工作表：
 * 1. 账单明细
 * 2. 统计汇总
 * 3. 账户汇总
 *
 * 使用 inlineStr，避免 sharedStrings 导致 WPS 兼容问题。
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

            writeEntry(
                zip,
                "xl/styles.xml",
                stylesXml()
            )

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

    private fun writeEntry(
        zip: ZipOutputStream,
        path: String,
        content: String
    ) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    // ============================================================
    // 账单明细
    // ============================================================

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
                    textCell("A1", "日期", 1),
                    textCell("B1", "收支类型", 1),
                    textCell("C1", "分类", 1),
                    textCell("D1", "金额", 1),
                    textCell("E1", "账户", 1),
                    textCell("F1", "备注", 1)
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

        val lastRow =
            if (bills.isEmpty()) {
                1
            } else {
                bills.size + 1
            }

        return worksheetXml(
            rows = rows.toString(),
            dimension = "A1:F$lastRow",
            widths = listOf(
                22,
                12,
                18,
                14,
                18,
                30
            )
        )
    }

    // ============================================================
    // 统计汇总
    // ============================================================

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
                        "记账统计汇总",
                        1
                    )
                )
            )
        )

        // 基础统计
        rows.append(
            row(
                3,
                listOf(
                    textCell(
                        "A3",
                        "统计项目",
                        1
                    ),
                    textCell(
                        "B3",
                        "金额",
                        1
                    )
                )
            )
        )

        rows.append(
            row(
                4,
                listOf(
                    textCell("A4", "总收入"),
                    numberCell("B4", totalIncome)
                )
            )
        )

        rows.append(
            row(
                5,
                listOf(
                    textCell("A5", "总支出"),
                    numberCell("B5", totalExpense)
                )
            )
        )

        rows.append(
            row(
                6,
                listOf(
                    textCell("A6", "结余"),
                    numberCell("B6", balance)
                )
            )
        )

        rows.append(
            row(
                7,
                listOf(
                    textCell("A7", "账单笔数"),
                    numberCell(
                        "B7",
                        bills.size.toDouble()
                    )
                )
            )
        )

        // 收入分类
        rows.append(
            row(
                9,
                listOf(
                    textCell(
                        "A9",
                        "收入分类统计",
                        1
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
                        "分类",
                        1
                    ),
                    textCell(
                        "B10",
                        "金额",
                        1
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

        var rowNumber = 11

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
                    rowNumber,
                    listOf(
                        textCell(
                            "A$rowNumber",
                            categoryName
                        ),
                        numberCell(
                            "B$rowNumber",
                            amount
                        )
                    )
                )
            )

            rowNumber++
        }

        // 支出分类
        val expenseTitleRow =
            rowNumber + 1

        rows.append(
            row(
                expenseTitleRow,
                listOf(
                    textCell(
                        "A$expenseTitleRow",
                        "支出分类统计",
                        1
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
                        "分类",
                        1
                    ),
                    textCell(
                        "B$expenseHeaderRow",
                        "金额",
                        1
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

        val lastRow =
            maxOf(
                10,
                expenseRow - 1
            )

        return worksheetXml(
            rows = rows.toString(),
            dimension = "A1:B$lastRow",
            widths = listOf(
                24,
                18
            )
        )
    }

    // ============================================================
    // 账户汇总
    // ============================================================

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
                        "账户汇总",
                        1
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
                        "账户名称",
                        1
                    ),
                    textCell(
                        "B3",
                        "初始余额",
                        1
                    ),
                    textCell(
                        "C3",
                        "收入",
                        1
                    ),
                    textCell(
                        "D3",
                        "支出",
                        1
                    ),
                    textCell(
                        "E3",
                        "当前余额",
                        1
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

        val lastRow =
            if (accounts.isEmpty()) {
                3
            } else {
                rowNumber - 1
            }

        return worksheetXml(
            rows = rows.toString(),
            dimension = "A1:E$lastRow",
            widths = listOf(
                22,
                16,
                16,
                16,
                18
            )
        )
    }

    // ============================================================
    // Worksheet
    // ============================================================

    private fun worksheetXml(
        rows: String,
        dimension: String,
        widths: List<Int>
    ): String {

        val columns =
            StringBuilder()

        for (i in widths.indices) {

            val column =
                i + 1

            columns.append(
                "<col " +
                    "min=\"$column\" " +
                    "max=\"$column\" " +
                    "width=\"${widths[i]}\" " +
                    "customWidth=\"1\"/>"
            )
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">

                <dimension ref="$dimension"/>

                <sheetViews>
                    <sheetView workbookViewId="0"/>
                </sheetViews>

                <sheetFormatPr defaultRowHeight="18"/>

                <cols>
                    $columns
                </cols>

                <sheetData>
                    $rows
                </sheetData>

                <pageMargins
                    left="0.7"
                    right="0.7"
                    top="0.75"
                    bottom="0.75"
                    header="0.3"
                    footer="0.3"/>

            </worksheet>
        """.trimIndent()
    }

    // ============================================================
    // Row
    // ============================================================

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

    // ============================================================
    // 文本单元格
    // ============================================================

    private fun textCell(
        reference: String,
        value: String,
        style: Int = 2
    ): String {

        val safeValue =
            escapeXml(value)

        return """
            <c
                r="$reference"
                t="inlineStr"
                s="$style">

                <is>
                    <t xml:space="preserve">$safeValue</t>
                </is>

            </c>
        """.trimIndent()
    }

    // ============================================================
    // 数字单元格
    // ============================================================

    private fun numberCell(
        reference: String,
        value: Double,
        style: Int = 5
    ): String {

        val formatted =
            String.format(
                Locale.US,
                "%.2f",
                value
            )

        return """
            <c
                r="$reference"
                s="$style">

                <v>$formatted</v>

            </c>
        """.trimIndent()
    }

    // ============================================================
    // Workbook
    // ============================================================

    private fun workbookXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>

            <workbook
                xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">

                <bookViews>
                    <workbookView
                        xWindow="0"
                        yWindow="0"
                        windowWidth="16000"
                        windowHeight="9000"/>
                </bookViews>

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

    // ============================================================
    // Workbook Relationships
    // ============================================================

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

                <Relationship
                    Id="rId4"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"
                    Target="styles.xml"/>

            </Relationships>
        """.trimIndent()
    }

    // ============================================================
    // Root Relationships
    // ============================================================

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

    // ============================================================
    // Content Types
    // ============================================================

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

                <Override
                    PartName="/xl/styles.xml"
                    ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>

            </Types>
        """.trimIndent()
    }

    // ============================================================
    // Styles
    // ============================================================

    private fun stylesXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>

            <styleSheet
                xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">

                <numFmts count="1">

                    <numFmt
                        numFmtId="164"
                        formatCode="0.00"/>

                </numFmts>

                <fonts count="2">

                    <font>
                        <sz val="11"/>
                        <name val="Microsoft YaHei"/>
                    </font>

                    <font>
                        <b/>
                        <sz val="11"/>
                        <name val="Microsoft YaHei"/>
                    </font>

                </fonts>

                <fills count="2">

                    <fill>
                        <patternFill patternType="none"/>
                    </fill>

                    <fill>
                        <patternFill patternType="gray125"/>
                    </fill>

                </fills>

                <borders count="1">

                    <border>
                        <left/>
                        <right/>
                        <top/>
                        <bottom/>
                        <diagonal/>
                    </border>

                </borders>

                <cellStyleXfs count="1">

                    <xf
                        numFmtId="0"
                        fontId="0"
                        fillId="0"
                        borderId="0"/>

                </cellStyleXfs>

                <cellXfs count="6">

                    <xf
                        numFmtId="0"
                        fontId="0"
                        fillId="0"
                        borderId="0"/>

                    <xf
                        numFmtId="0"
                        fontId="1"
                        fillId="0"
                        borderId="0"
                        applyFont="1"/>

                    <xf
                        numFmtId="0"
                        fontId="0"
                        fillId="0"
                        borderId="0"
                        applyAlignment="1">

                        <alignment
                            horizontal="left"
                            vertical="center"/>

                    </xf>

                    <xf
                        numFmtId="0"
                        fontId="0"
                        fillId="0"
                        borderId="0"
                        applyAlignment="1">

                        <alignment
                            horizontal="center"
                            vertical="center"/>

                    </xf>

                    <xf
                        numFmtId="0"
                        fontId="0"
                        fillId="0"
                        borderId="0"
                        applyAlignment="1">

                        <alignment
                            horizontal="right"
                            vertical="center"/>

                    </xf>

                    <xf
                        numFmtId="164"
                        fontId="0"
                        fillId="0"
                        borderId="0"
                        applyNumberFormat="1"
                        applyAlignment="1">

                        <alignment
                            horizontal="right"
                            vertical="center"/>

                    </xf>

                </cellXfs>

            </styleSheet>
        """.trimIndent()
    }

    // ============================================================
    // XML Escape
    // ============================================================

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