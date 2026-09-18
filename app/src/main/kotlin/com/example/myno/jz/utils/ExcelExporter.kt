package com.example.myno.jz.utils

import android.content.Context
import com.example.myno.jz.data.model.Account
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.roundToLong

/**
 * Excel XLSX 导出器
 *
 * 不依赖第三方 Excel 库，完全离线生成 XLSX。
 *
 * 工作簿：
 * 1. 账单明细
 * 2. 统计汇总
 * 3. 账户汇总
 */
class ExcelExporter(
    private val context: Context
) {

    companion object {
        private const val TAG = "ExcelExporter"

        private const val SHEET1 = "账单明细"
        private const val SHEET2 = "统计汇总"
        private const val SHEET3 = "账户汇总"

        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }

    /**
     * 导出 XLSX
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

val sortedBills = bills.sortedByDescending { bill ->
    bill.timestamp
}

        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        AppLogger.i(TAG, "账单排序完成：${sortedBills.size} 条")
        AppLogger.i(TAG, "账户 Map 创建完成：${accountMap.size}")
        AppLogger.i(TAG, "分类 Map 创建完成：${categoryMap.size}")

        ZipOutputStream(outputStream).use { zip ->

            // 基础文件
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

            // 文档属性
            writeEntry(
                zip,
                "docProps/app.xml",
                appPropertiesXml()
            )

            writeEntry(
                zip,
                "docProps/core.xml",
                corePropertiesXml()
            )

            // Workbook
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

            // 样式和主题
            writeEntry(
                zip,
                "xl/styles.xml",
                stylesXml()
            )

            writeEntry(
                zip,
                "xl/theme/theme1.xml",
                themeXml()
            )

            // Sheet 1
            AppLogger.i(TAG, "开始生成 sheet1：$SHEET1")

            val sheet1 = detailSheetXml(
                sortedBills,
                accountMap,
                categoryMap
            )

            writeEntry(
                zip,
                "xl/worksheets/sheet1.xml",
                sheet1
            )

            AppLogger.i(
                TAG,
                "sheet1 XML 写入完成：${sheet1.length} 字符"
            )

            // Sheet 2
            AppLogger.i(TAG, "开始生成 sheet2：$SHEET2")

            val sheet2 = summarySheetXml(
                sortedBills,
                categoryMap
            )

            writeEntry(
                zip,
                "xl/worksheets/sheet2.xml",
                sheet2
            )

            AppLogger.i(
                TAG,
                "sheet2 XML 写入完成：${sheet2.length} 字符"
            )

            // Sheet 3
            AppLogger.i(TAG, "开始生成 sheet3：$SHEET3")

            val sheet3 = accountSheetXml(
                sortedBills,
                accounts
            )

            writeEntry(
                zip,
                "xl/worksheets/sheet3.xml",
                sheet3
            )

            AppLogger.i(
                TAG,
                "sheet3 XML 写入完成：${sheet3.length} 字符"
            )

            zip.finish()
        }

        AppLogger.i(TAG, "XLSX ZIP 已完成")
        AppLogger.i(TAG, "export() 完成")
    }

    /**
     * 写入 ZIP Entry
     */
    private fun writeEntry(
        zip: ZipOutputStream,
        name: String,
        content: String
    ) {
        val bytes = content.toByteArray(Charsets.UTF_8)

        AppLogger.i(
            TAG,
            "写入 ZIP Entry：$name，${bytes.size} bytes"
        )

        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()

        AppLogger.i(
            TAG,
            "ZIP Entry 写入完成：$name"
        )
    }

    /**
     * Sheet 1：账单明细
     */
    private fun detailSheetXml(
        bills: List<Bill>,
        accountMap: Map<String, Account>,
        categoryMap: Map<String, Category>
    ): String {

        AppLogger.i(
            TAG,
            "detailSheetXml() 开始，账单数量=${bills.size}"
        )

        val xml = StringBuilder()

        xml.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
        )

        xml.append(
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """
        )

        xml.append(
            """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">"""
        )

        xml.append("<dimension ref=\"A1:F${bills.size + 1}\"/>")

        xml.append("<sheetViews>")
        xml.append(
            """<sheetView workbookViewId="0">"""
        )
        xml.append(
            """<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>"""
        )
        xml.append(
            """<selection pane="bottomLeft" activeCell="A2" sqref="A2"/>"""
        )
        xml.append("</sheetView>")
        xml.append("</sheetViews>")

        xml.append("<sheetFormatPr defaultRowHeight=\"20\"/>")

        xml.append("<cols>")
        xml.append("""<col min="1" max="1" width="20" customWidth="1"/>""")
        xml.append("""<col min="2" max="2" width="10" customWidth="1"/>""")
        xml.append("""<col min="3" max="3" width="16" customWidth="1"/>""")
        xml.append("""<col min="4" max="4" width="14" customWidth="1"/>""")
        xml.append("""<col min="5" max="5" width="14" customWidth="1"/>""")
        xml.append("""<col min="6" max="6" width="36" customWidth="1"/>""")
        xml.append("</cols>")

        xml.append("<sheetData>")

        // 标题行
        xml.append("<row r=\"1\" ht=\"24\" customHeight=\"1\">")

        appendStringCell(xml, "A1", "日期", 1)
        appendStringCell(xml, "B1", "类型", 1)
        appendStringCell(xml, "C1", "分类", 1)
        appendNumberCell(xml, "D1", 0.0, 1)
        appendStringCell(xml, "E1", "账户", 1)
        appendStringCell(xml, "F1", "备注", 1)

        xml.append("</row>")

        bills.forEachIndexed { index, bill ->

            val rowNumber = index + 2

            val typeText = when (bill.type) {
                BillType.INCOME -> "收入"
                BillType.EXPENSE -> "支出"
                else -> bill.type.toString()
            }

            val categoryName =
                categoryMap[bill.categoryId]?.name
                    ?: "未分类"

            val accountName =
                accountMap[bill.accountId]?.name
                    ?: "未知账户"

        val dateText = formatDate(bill.timestamp)
            AppLogger.i(
                TAG,
                "生成账单行：第${rowNumber}行，" +
                    "billId=${bill.id}，" +
                    "类型=$typeText，" +
                    "金额=${bill.amount}，" +
                    "分类=$categoryName，" +
                    "账户=$accountName"
            )

            xml.append(
                "<row r=\"$rowNumber\">"
            )

            appendStringCell(
                xml,
                "A$rowNumber",
                dateText
            )

            appendStringCell(
                xml,
                "B$rowNumber",
                typeText
            )

            appendStringCell(
                xml,
                "C$rowNumber",
                categoryName
            )

            appendNumberCell(
                xml,
                "D$rowNumber",
                bill.amount
            )

            appendStringCell(
                xml,
                "E$rowNumber",
                accountName
            )

            appendStringCell(
                xml,
                "F$rowNumber",
                bill.note ?: ""
            )

            xml.append("</row>")
        }

        xml.append("</sheetData>")

        xml.append(
            """
            <autoFilter ref="A1:F${bills.size + 1}"/>
            """.trimIndent()
        )

        xml.append(
            """
            <pageMargins left="0.7" right="0.7" top="0.75" bottom="0.75" header="0.3" footer="0.3"/>
            """.trimIndent()
        )

        xml.append("</worksheet>")

        AppLogger.i(
            TAG,
            "detailSheetXml() 完成：XML长度=${xml.length}"
        )

        return xml.toString()
    }

    /**
     * Sheet 2：统计汇总
     */
    private fun summarySheetXml(
        bills: List<Bill>,
        categoryMap: Map<String, Category>
    ): String {

        var income = 0.0
        var expense = 0.0

        val incomeCategories = linkedMapOf<String, Double>()
        val expenseCategories = linkedMapOf<String, Double>()

        bills.forEach { bill ->

            val categoryName =
                categoryMap[bill.categoryId]?.name
                    ?: "未分类"

            when (bill.type) {

                BillType.INCOME -> {
                    income += bill.amount

                    incomeCategories[categoryName] =
                        (incomeCategories[categoryName] ?: 0.0) +
                            bill.amount
                }

                BillType.EXPENSE -> {
                    expense += bill.amount

                    expenseCategories[categoryName] =
                        (expenseCategories[categoryName] ?: 0.0) +
                            bill.amount
                }

                else -> Unit
            }
        }

        val balance = income - expense

        AppLogger.i(
            TAG,
            "统计计算：收入=$income，支出=$expense，结余=$balance"
        )

        val xml = StringBuilder()

        xml.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
        )

        xml.append(
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">"""
        )

        xml.append("<dimension ref=\"A1:C${10 + incomeCategories.size + expenseCategories.size}\"/>")

        xml.append("<sheetViews>")
        xml.append(
            """<sheetView workbookViewId="0">"""
        )
        xml.append("</sheetView>")
        xml.append("</sheetViews>")

        xml.append("<sheetFormatPr defaultRowHeight=\"20\"/>")

        xml.append("<cols>")
        xml.append("""<col min="1" max="1" width="24" customWidth="1"/>""")
        xml.append("""<col min="2" max="2" width="20" customWidth="1"/>""")
        xml.append("""<col min="3" max="3" width="20" customWidth="1"/>""")
        xml.append("</cols>")

        xml.append("<sheetData>")

        var row = 1

        xml.append("<row r=\"$row\">")
        appendStringCell(xml, "A$row", "统计项目", 1)
        appendStringCell(xml, "B$row", "金额", 1)
        appendStringCell(xml, "C$row", "说明", 1)
        xml.append("</row>")

        row++

        xml.append("<row r=\"$row\">")
        appendStringCell(xml, "A$row", "总收入")
        appendNumberCell(xml, "B$row", income)
        appendStringCell(xml, "C$row", "所有收入账单合计")
        xml.append("</row>")

        row++

        xml.append("<row r=\"$row\">")
        appendStringCell(xml, "A$row", "总支出")
        appendNumberCell(xml, "B$row", expense)
        appendStringCell(xml, "C$row", "所有支出账单合计")
        xml.append("</row>")

        row++

        xml.append("<row r=\"$row\">")
        appendStringCell(xml, "A$row", "结余")
        appendNumberCell(xml, "B$row", balance)
        appendStringCell(xml, "C$row", "总收入 - 总支出")
        xml.append("</row>")

        row++

        xml.append("<row r=\"$row\">")
        appendStringCell(xml, "A$row", "收入分类")
        appendStringCell(xml, "B$row", "金额", 1)
        appendStringCell(xml, "C$row", "分类名称", 1)
        xml.append("</row>")

        row++

        incomeCategories
            .toList()
            .sortedByDescending { it.second }
            .forEach { (name, amount) ->

                AppLogger.i(
                    TAG,
                    "收入分类：$name=$amount"
                )

                xml.append("<row r=\"$row\">")
                appendStringCell(xml, "A$row", "收入")
                appendNumberCell(xml, "B$row", amount)
                appendStringCell(xml, "C$row", name)
                xml.append("</row>")

                row++
            }

        xml.append("<row r=\"$row\">")
        appendStringCell(xml, "A$row", "支出分类")
        appendStringCell(xml, "B$row", "金额", 1)
        appendStringCell(xml, "C$row", "分类名称", 1)
        xml.append("</row>")

        row++

        expenseCategories
            .toList()
            .sortedByDescending { it.second }
            .forEach { (name, amount) ->

                AppLogger.i(
                    TAG,
                    "支出分类：$name=$amount"
                )

                xml.append("<row r=\"$row\">")
                appendStringCell(xml, "A$row", "支出")
                appendNumberCell(xml, "B$row", amount)
                appendStringCell(xml, "C$row", name)
                xml.append("</row>")

                row++
            }

        xml.append("</sheetData>")

        xml.append(
            """
            <pageMargins left="0.7" right="0.7" top="0.75" bottom="0.75" header="0.3" footer="0.3"/>
            """.trimIndent()
        )

        xml.append("</worksheet>")

        return xml.toString()
    }

    /**
     * Sheet 3：账户汇总
     */
    private fun accountSheetXml(
        bills: List<Bill>,
        accounts: List<Account>
    ): String {

        AppLogger.i(
            TAG,
            "accountSheetXml() 开始，账户数量=${accounts.size}"
        )

        val xml = StringBuilder()

        xml.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
        )

        xml.append(
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">"""
        )

        xml.append(
            """<dimension ref="A1:E${accounts.size + 1}"/>"""
        )

        xml.append("<sheetViews>")
        xml.append(
            """<sheetView workbookViewId="0">"""
        )
        xml.append(
            """<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>"""
        )
        xml.append(
            """<selection pane="bottomLeft" activeCell="A2" sqref="A2"/>"""
        )
        xml.append("</sheetView>")
        xml.append("</sheetViews>")

        xml.append("<sheetFormatPr defaultRowHeight=\"20\"/>")

        xml.append("<cols>")
        xml.append("""<col min="1" max="1" width="20" customWidth="1"/>""")
        xml.append("""<col min="2" max="5" width="16" customWidth="1"/>""")
        xml.append("</cols>")

        xml.append("<sheetData>")

        xml.append("<row r=\"1\">")
        appendStringCell(xml, "A1", "账户", 1)
        appendStringCell(xml, "B1", "初始余额", 1)
        appendStringCell(xml, "C1", "收入", 1)
        appendStringCell(xml, "D1", "支出", 1)
        appendStringCell(xml, "E1", "当前余额", 1)
        xml.append("</row>")

        accounts.forEachIndexed { index, account ->

            val row = index + 2

            val income =
                bills
                    .filter {
                        it.accountId == account.id &&
                            it.type == BillType.INCOME
                    }
                    .sumOf { it.amount }

            val expense =
                bills
                    .filter {
                        it.accountId == account.id &&
                            it.type == BillType.EXPENSE
                    }
                    .sumOf { it.amount }

            val currentBalance =
                account.balance + income - expense

            AppLogger.i(
                TAG,
                "生成账户行：第${row}行，" +
                    "账户=${account.name}，" +
                    "初始=${account.balance}，" +
                    "收入=$income，" +
                    "支出=$expense，" +
                    "当前=$currentBalance"
            )

            xml.append("<row r=\"$row\">")

            appendStringCell(
                xml,
                "A$row",
                account.name
            )

            appendNumberCell(
                xml,
                "B$row",
                account.balance
            )

            appendNumberCell(
                xml,
                "C$row",
                income
            )

            appendNumberCell(
                xml,
                "D$row",
                expense
            )

            appendNumberCell(
                xml,
                "E$row",
                currentBalance
            )

            xml.append("</row>")
        }

        xml.append("</sheetData>")

        xml.append(
            """
            <pageMargins left="0.7" right="0.7" top="0.75" bottom="0.75" header="0.3" footer="0.3"/>
            """.trimIndent()
        )

        xml.append("</worksheet>")

        return xml.toString()
    }

    /**
     * 普通字符串单元格
     *
     * 使用 inlineStr，避免依赖 sharedStrings.xml。
     */
    private fun appendStringCell(
        xml: StringBuilder,
        reference: String,
        value: String,
        styleIndex: Int = 0
    ) {
        xml.append(
            """<c r="$reference" t="inlineStr" s="$styleIndex"><is><t"""
        )

        if (
            value.startsWith(" ") ||
            value.endsWith(" ") ||
            value.contains("\n") ||
            value.contains("\r")
        ) {
            xml.append(""" xml:space="preserve"""")
        }

        xml.append(">")

        xml.append(escapeXml(value))

        xml.append("</t></is></c>")
    }

    /**
     * 数字单元格
     */
    private fun appendNumberCell(
        xml: StringBuilder,
        reference: String,
        value: Double,
        styleIndex: Int = 2
    ) {
        val safeValue =
            if (value.isFinite()) {
                value
            } else {
                0.0
            }

        xml.append(
            """<c r="$reference" s="$styleIndex"><v>"""
        )

        xml.append(
            formatNumber(safeValue)
        )

        xml.append("</v></c>")
    }

    /**
     * 数字格式化
     */
    private fun formatNumber(value: Double): String {
        val rounded =
            (value * 100.0).roundToLong() / 100.0

        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    /**
     * 日期格式化
     */
private fun formatDate(timestamp: Long): String {
    return try {
        SimpleDateFormat(
            DATE_FORMAT,
            Locale.getDefault()
        ).format(Date(timestamp))
    } catch (e: Exception) {
        AppLogger.w(
            TAG,
            "日期格式化失败：${e.message}"
        )
        timestamp.toString()
    }
}

    /**
     * XML 转义
     */
    private fun escapeXml(value: String): String {

        return buildString {

            value.forEach { char ->

                when (char) {

                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")

                    '\u0000',
                    '\u0001',
                    '\u0002',
                    '\u0003',
                    '\u0004',
                    '\u0005',
                    '\u0006',
                    '\u0007',
                    '\u0008',
                    '\u000B',
                    '\u000C',
                    '\u000E',
                    '\u000F',
                    '\u0010',
                    '\u0011',
                    '\u0012',
                    '\u0013',
                    '\u0014',
                    '\u0015',
                    '\u0016',
                    '\u0017',
                    '\u0018',
                    '\u0019',
                    '\u001A',
                    '\u001B',
                    '\u001C',
                    '\u001D',
                    '\u001E',
                    '\u001F' -> Unit

                    else -> append(char)
                }
            }
        }
    }

    /**
     * [Content_Types].xml
     */
    private fun contentTypesXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                <Override PartName="/xl/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
                <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
            </Types>
        """.trimIndent()
    }

    /**
     * 根关系
     */
    private fun rootRelsXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship
                    Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                    Target="xl/workbook.xml"/>
                <Relationship
                    Id="rId2"
                    Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"
                    Target="docProps/core.xml"/>
                <Relationship
                    Id="rId3"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties"
                    Target="docProps/app.xml"/>
            </Relationships>
        """.trimIndent()
    }

    /**
     * 文档扩展属性
     */
    private fun appPropertiesXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
                        xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                <Application>MoneyBook</Application>
                <DocSecurity>0</DocSecurity>
                <ScaleCrop>false</ScaleCrop>
                <HeadingPairs>
                    <vt:vector size="2" baseType="variant">
                        <vt:variant>
                            <vt:lpstr>工作表</vt:lpstr>
                        </vt:variant>
                        <vt:variant>
                            <vt:i4>3</vt:i4>
                        </vt:variant>
                    </vt:vector>
                </HeadingPairs>
                <TitlesOfParts>
                    <vt:vector size="3" baseType="lpstr">
                        <vt:lpstr>$SHEET1</vt:lpstr>
                        <vt:lpstr>$SHEET2</vt:lpstr>
                        <vt:lpstr>$SHEET3</vt:lpstr>
                    </vt:vector>
                </TitlesOfParts>
            </Properties>
        """.trimIndent()
    }

    /**
     * 核心属性
     */
    private fun corePropertiesXml(): String {

        val now =
            SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.US
            ).format(Date())

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cp:coreProperties
                xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:dcterms="http://purl.org/dc/terms/"
                xmlns:dcmitype="http://purl.org/dc/dcmitype/"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <dc:title>MoneyBook 账单</dc:title>
                <dc:creator>MoneyBook</dc:creator>
                <cp:lastModifiedBy>MoneyBook</cp:lastModifiedBy>
                <dcterms:created xsi:type="dcterms:W3CDTF">$now</dcterms:created>
                <dcterms:modified xsi:type="dcterms:W3CDTF">$now</dcterms:modified>
            </cp:coreProperties>
        """.trimIndent()
    }

    /**
     * Workbook
     */
    private fun workbookXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook
                xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <fileVersion appName="xl"/>
                <workbookPr defaultThemeVersion="164011"/>
                <bookViews>
                    <workbookView xWindow="0" yWindow="0" windowWidth="24000" windowHeight="12000"/>
                </bookViews>
                <sheets>
                    <sheet name="$SHEET1" sheetId="1" r:id="rId1"/>
                    <sheet name="$SHEET2" sheetId="2" r:id="rId2"/>
                    <sheet name="$SHEET3" sheetId="3" r:id="rId3"/>
                </sheets>
                <calcPr calcId="191029" fullCalcOnLoad="1" forceFullCalc="1"/>
            </workbook>
        """.trimIndent()
    }

    /**
     * Workbook Relationships
     */
    private fun workbookRelsXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
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
                <Relationship
                    Id="rId5"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme"
                    Target="theme/theme1.xml"/>
            </Relationships>
        """.trimIndent()
    }

    /**
     * Excel Styles
     */
    private fun stylesXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">

                <numFmts count="1">
                    <numFmt numFmtId="165" formatCode="0.00"/>
                </numFmts>

                <fonts count="2">
                    <font>
                        <sz val="11"/>
                        <color theme="1"/>
                        <name val="等线"/>
                        <family val="2"/>
                        <scheme val="minor"/>
                    </font>
                    <font>
                        <b/>
                        <sz val="11"/>
                        <color theme="1"/>
                        <name val="等线"/>
                        <family val="2"/>
                        <scheme val="minor"/>
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
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
                </cellStyleXfs>

                <cellXfs count="3">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0"/>
                </cellXfs>

                <cellStyles count="1">
                    <cellStyle name="Normal" xfId="0" builtinId="0"/>
                </cellStyles>

                <dxfs count="0"/>

                <tableStyles
                    count="0"
                    defaultTableStyle="TableStyleMedium2"
                    defaultPivotStyle="PivotStyleMedium9"/>

            </styleSheet>
        """.trimIndent()
    }

    /**
     * Excel Theme
     */
    private fun themeXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Office Theme">
                <a:themeElements>

                    <a:clrScheme name="Office">
                        <a:dk1>
                            <a:sysClr val="windowText" lastClr="000000"/>
                        </a:dk1>
                        <a:lt1>
                            <a:sysClr val="window" lastClr="FFFFFF"/>
                        </a:lt1>
                        <a:dk2>
                            <a:srgbClr val="44546A"/>
                        </a:dk2>
                        <a:lt2>
                            <a:srgbClr val="E7E6E6"/>
                        </a:lt2>
                        <a:accent1>
                            <a:srgbClr val="4472C4"/>
                        </a:accent1>
                        <a:accent2>
                            <a:srgbClr val="ED7D31"/>
                        </a:accent2>
                        <a:accent3>
                            <a:srgbClr val="A5A5A5"/>
                        </a:accent3>
                        <a:accent4>
                            <a:srgbClr val="FFC000"/>
                        </a:accent4>
                        <a:accent5>
                            <a:srgbClr val="5B9BD5"/>
                        </a:accent5>
                        <a:accent6>
                            <a:srgbClr val="70AD47"/>
                        </a:accent6>
                        <a:hlink>
                            <a:srgbClr val="0563C1"/>
                        </a:hlink>
                        <a:folHlink>
                            <a:srgbClr val="954F72"/>
                        </a:folHlink>
                    </a:clrScheme>

                    <a:fontScheme name="Office">
                        <a:majorFont>
                            <a:latin typeface="等线"/>
                            <a:ea typeface=""/>
                            <a:cs typeface=""/>
                        </a:majorFont>
                        <a:minorFont>
                            <a:latin typeface="等线"/>
                            <a:ea typeface=""/>
                            <a:cs typeface=""/>
                        </a:minorFont>
                    </a:fontScheme>

                    <a:fmtScheme name="Office">
                        <a:fillStyleLst>
                            <a:solidFill>
                                <a:schemeClr val="phClr"/>
                            </a:solidFill>
                        </a:fillStyleLst>
                        <a:lnStyleLst/>
                        <a:effectStyleLst/>
                        <a:bgFillStyleLst/>
                    </a:fmtScheme>

                </a:themeElements>
            </a:theme>
        """.trimIndent()
    }

    /**
     * XLSX 文件结构验证
     */
    fun validate(inputStream: InputStream): ValidationResult {

        AppLogger.i(
            TAG,
            "validate() 开始验证 XLSX"
        )

        val requiredEntries = setOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "docProps/app.xml",
            "docProps/core.xml",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/theme/theme1.xml",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml",
            "xl/worksheets/sheet3.xml"
        )

        val found = mutableSetOf<String>()

        var sheet1Xml = ""
        var sheet2Xml = ""
        var sheet3Xml = ""

        ZipInputStream(inputStream).use { zip ->

            while (true) {

                val entry = zip.nextEntry ?: break

                val name = entry.name

                AppLogger.i(
                    TAG,
                    "验证发现 ZIP Entry：$name"
                )

                found += name

                val bytes = zip.readBytes()

                when (name) {

                    "xl/worksheets/sheet1.xml" -> {
                        sheet1Xml =
                            bytes.toString(Charsets.UTF_8)
                    }

                    "xl/worksheets/sheet2.xml" -> {
                        sheet2Xml =
                            bytes.toString(Charsets.UTF_8)
                    }

                    "xl/worksheets/sheet3.xml" -> {
                        sheet3Xml =
                            bytes.toString(Charsets.UTF_8)
                    }
                }
            }
        }

        val missing =
            requiredEntries
                .filterNot { found.contains(it) }

        if (missing.isNotEmpty()) {

            val message =
                "XLSX 缺少文件：${missing.joinToString()}"

            AppLogger.e(
                TAG,
                message
            )

            return ValidationResult(
                valid = false,
                message = message,
                entryCount = found.size,
                sheet1HasData = false,
                sheet2Exists = found.contains(
                    "xl/worksheets/sheet2.xml"
                ),
                sheet3Exists = found.contains(
                    "xl/worksheets/sheet3.xml"
                )
            )
        }

        val sheet1HasData =
            sheet1Xml.contains("<row r=\"2\"") &&
                sheet1Xml.contains("A2")

        val sheet2Exists =
            sheet2Xml.isNotEmpty()

        val sheet3Exists =
            sheet3Xml.isNotEmpty()

        AppLogger.i(
            TAG,
            "XLSX ZIP 验证完成，共 ${found.size} 个 Entry"
        )

        AppLogger.i(
            TAG,
            "sheet1 是否存在数据：$sheet1HasData"
        )

        AppLogger.i(
            TAG,
            "sheet2 是否存在：$sheet2Exists"
        )

        AppLogger.i(
            TAG,
            "sheet3 是否存在：$sheet3Exists"
        )

        val valid =
            sheet1HasData &&
                sheet2Exists &&
                sheet3Exists

        val message =
            if (valid) {
                "XLSX 标准结构验证成功，数据存在"
            } else {
                "XLSX 结构验证失败或数据不存在"
            }

        AppLogger.i(
            TAG,
            message
        )

        return ValidationResult(
            valid = valid,
            message = message,
            entryCount = found.size,
            sheet1HasData = sheet1HasData,
            sheet2Exists = sheet2Exists,
            sheet3Exists = sheet3Exists
        )
    }

    data class ValidationResult(
        val valid: Boolean,
        val message: String,
        val entryCount: Int,
        val sheet1HasData: Boolean,
        val sheet2Exists: Boolean,
        val sheet3Exists: Boolean
    )
}