package com.example.myno.jz.data.imports

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipInputStream

object XlsxBillParser {

    fun parse(inputStream: InputStream): List<ImportBillItem> {

        val entries = mutableMapOf<String, ByteArray>()

        ZipInputStream(inputStream).use { zip ->

            var entry = zip.nextEntry

            while (entry != null) {

                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (entries.isEmpty()) {
            return emptyList()
        }

        val sharedStrings =
            parseSharedStrings(
                entries["xl/sharedStrings.xml"]
            )

        val worksheetEntry =
            entries.keys
                .filter {
                    it.startsWith("xl/worksheets/sheet") &&
                            it.endsWith(".xml")
                }
                .sorted()
                .firstOrNull()
                ?: return emptyList()

        val worksheetBytes =
            entries[worksheetEntry]
                ?: return emptyList()

        val rows =
            parseWorksheet(
                worksheetBytes,
                sharedStrings
            )

        return convertWechatRows(rows)
    }

    private fun parseSharedStrings(
        bytes: ByteArray?
    ): List<String> {

        if (bytes == null || bytes.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<String>()

        val parser =
            XmlPullParserFactory
                .newInstance()
                .newPullParser()

        parser.setInput(
            bytes.inputStream(),
            "UTF-8"
        )

        var event = parser.eventType

        var insideSi = false

        var currentText =
            StringBuilder()

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            when (event) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "si" -> {
                            insideSi = true
                            currentText = StringBuilder()
                        }

                        "t" -> {

                            if (insideSi) {
                                currentText.append(
                                    parser.nextText()
                                )
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {

                    if (parser.name == "si") {

                        result.add(
                            currentText.toString()
                        )

                        insideSi = false
                    }
                }
            }

            event = parser.next()
        }

        return result
    }

    private fun parseWorksheet(
        bytes: ByteArray,
        sharedStrings: List<String>
    ): List<List<String>> {

        val rows =
            mutableListOf<List<String>>()

        val parser =
            XmlPullParserFactory
                .newInstance()
                .newPullParser()

        parser.setInput(
            bytes.inputStream(),
            "UTF-8"
        )

        var event = parser.eventType

        var currentRow:
            MutableList<String>? = null

        var currentColumnIndex = 0

        var cellType = ""

        var cellValue = ""

        var cellColumnIndex = 0

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            when (event) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "row" -> {
                            currentRow =
                                mutableListOf()
                        }

                        "c" -> {

                            val reference =
                                parser.getAttributeValue(
                                    null,
                                    "r"
                                )

                            cellColumnIndex =
                                getColumnIndex(
                                    reference
                                )

                            while (
                                currentRow != null &&
                                currentRow!!.size <
                                cellColumnIndex
                            ) {

                                currentRow!!.add("")
                            }

                            cellType =
                                parser.getAttributeValue(
                                    null,
                                    "t"
                                ) ?: ""

                            cellValue = ""

                            currentColumnIndex =
                                cellColumnIndex
                        }

                        "v" -> {

                            cellValue =
                                parser.nextText()
                        }

                        "t" -> {

                            if (
                                cellType == "inlineStr" ||
                                cellType == "str"
                            ) {

                                cellValue =
                                    parser.nextText()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {

                    when (parser.name) {

                        "c" -> {

                            var value =
                                cellValue

                            if (
                                cellType == "s"
                            ) {

                                val index =
                                    cellValue.toIntOrNull()

                                if (
                                    index != null &&
                                    index in sharedStrings.indices
                                ) {

                                    value =
                                        sharedStrings[index]
                                }
                            }

                            if (
                                currentRow != null
                            ) {

                                while (
                                    currentRow!!.size <
                                    currentColumnIndex
                                ) {

                                    currentRow!!.add("")
                                }

                                currentRow!!.add(
                                    value
                                )
                            }
                        }

                        "row" -> {

                            currentRow?.let {
                                rows.add(it)
                            }

                            currentRow = null
                        }
                    }
                }
            }

            event = parser.next()
        }

        return rows
    }

    private fun getColumnIndex(
        reference: String?
    ): Int {

        if (reference.isNullOrBlank()) {
            return 0
        }

        val letters =
            reference
                .takeWhile {
                    it.isLetter()
                }
                .uppercase(
                    Locale.getDefault()
                )

        var result = 0

        for (char in letters) {

            result =
                result * 26 +
                        (char - 'A' + 1)
        }

        return result - 1
    }

    /**
     * 转换微信账单。
     *
     * 现在支持：
     *
     * 收入
     * 支出
     * 中性交易 / 转账
     */
    private fun convertWechatRows(
        rows: List<List<String>>
    ): List<ImportBillItem> {

        if (rows.isEmpty()) {
            return emptyList()
        }

        val headerIndex =
            rows.indexOfFirst { row ->

                val text =
                    row.joinToString("\t")

                text.contains("交易时间") &&
                        text.contains("交易类型") &&
                        text.contains("交易对方") &&
                        text.contains("收/支") &&
                        text.contains("金额")
            }

        if (headerIndex < 0) {
            return emptyList()
        }

        val header =
            rows[headerIndex]

        val timeIndex =
            findColumn(
                header,
                "交易时间"
            )

        val typeIndex =
            findColumn(
                header,
                "交易类型"
            )

        val merchantIndex =
            findColumn(
                header,
                "交易对方"
            )

        val productIndex =
            findColumn(
                header,
                "商品"
            )

        val incomeExpenseIndex =
            findColumn(
                header,
                "收/支"
            )

        val amountIndex =
            findColumn(
                header,
                "金额(元)"
            )

        val paymentMethodIndex =
            findColumn(
                header,
                "支付方式"
            )

        val statusIndex =
            findColumn(
                header,
                "当前状态"
            )

        val transactionIdIndex =
            findColumn(
                header,
                "交易单号"
            )

        val merchantIdIndex =
            findColumn(
                header,
                "商户单号"
            )

        val remarkIndex =
            findColumn(
                header,
                "备注"
            )

        if (
            timeIndex < 0 ||
            incomeExpenseIndex < 0 ||
            amountIndex < 0
        ) {
            return emptyList()
        }

        val result =
            mutableListOf<ImportBillItem>()

        var rowIndex = 0

        for (
            index in
            headerIndex + 1 until rows.size
        ) {

            val row =
                rows[index]

            if (row.isEmpty()) {
                continue
            }

            val timeText =
                getCell(
                    row,
                    timeIndex
                )

            val transactionType =
                getCell(
                    row,
                    typeIndex
                )

            val incomeExpense =
                getCell(
                    row,
                    incomeExpenseIndex
                )

            val amountText =
                getCell(
                    row,
                    amountIndex
                )

            if (timeText.isBlank()) {
                continue
            }

            val amount =
                parseAmount(
                    amountText
                )

            if (amount <= 0.0) {
                continue
            }

            /*
             * 关键修复：
             *
             * 微信账单中的：
             *
             * 收入 → INCOME
             * 支出 → EXPENSE
             * /    → 根据交易类型判断是否为 NEUTRAL
             *
             * 不再直接 continue。
             */

            val isNeutral =
                incomeExpense != "收入" &&
                        incomeExpense != "支出"

            val type =
                when {

                    incomeExpense == "收入" ->
                        ImportBillType.INCOME

                    incomeExpense == "支出" ->
                        ImportBillType.EXPENSE

                    isTransferTransaction(
                        transactionType
                    ) ->
                        ImportBillType.NEUTRAL

                    else ->
                        ImportBillType.UNKNOWN
                }

            /*
             * 真正无法识别的空交易不导入。
             *
             * 但微信的转账、提现等中性交易必须保留。
             */
            if (
                type == ImportBillType.UNKNOWN
            ) {
                continue
            }

            val merchant =
                getCell(
                    row,
                    merchantIndex
                )

            val product =
                getCell(
                    row,
                    productIndex
                )

            val paymentMethod =
                getCell(
                    row,
                    paymentMethodIndex
                )

            val status =
                getCell(
                    row,
                    statusIndex
                )

            val transactionId =
                getCell(
                    row,
                    transactionIdIndex
                )

            val merchantId =
                getCell(
                    row,
                    merchantIdIndex
                )

            val remark =
                getCell(
                    row,
                    remarkIndex
                )

            val finalMerchant =
                when {

                    merchant.isNotBlank() &&
                            merchant != "/" -> {
                        merchant
                    }

                    product.isNotBlank() &&
                            product != "/" -> {
                        product
                    }

                    transactionType.isNotBlank() -> {
                        transactionType
                    }

                    else -> {
                        if (
                            type ==
                            ImportBillType.NEUTRAL
                        ) {
                            "账户转账"
                        } else {
                            "未知交易"
                        }
                    }
                }

            val noteParts =
                mutableListOf<String>()

            if (
                transactionType.isNotBlank() &&
                transactionType != "/" &&
                transactionType != finalMerchant
            ) {

                noteParts.add(
                    transactionType
                )
            }

            if (
                product.isNotBlank() &&
                product != "/" &&
                product != finalMerchant
            ) {

                noteParts.add(
                    product
                )
            }

            if (
                remark.isNotBlank() &&
                remark != "/"
            ) {

                noteParts.add(
                    remark
                )
            }

            if (
                paymentMethod.isNotBlank() &&
                paymentMethod != "/"
            ) {

                noteParts.add(
                    "支付方式：$paymentMethod"
                )
            }

            if (
                status.isNotBlank() &&
                status != "支付成功" &&
                status != "/"
            ) {

                noteParts.add(
                    status
                )
            }

            if (
                transactionId.isNotBlank()
            ) {

                noteParts.add(
                    "交易单号：$transactionId"
                )
            }

            if (
                merchantId.isNotBlank() &&
                merchantId != "/"
            ) {

                noteParts.add(
                    "商户单号：$merchantId"
                )
            }

            val timestamp =
                parseTime(
                    timeText
                )

            result.add(
                ImportBillItem(
                    rowIndex = rowIndex++,

                    timestamp = timestamp,

                    type = type,

                    amount = amount,

                    merchant = finalMerchant,

                    note =
                        noteParts.joinToString(
                            " · "
                        ),

                    originalRow =
                        row.joinToString(
                            "\t"
                        ),

                    source = "WECHAT",

                    transactionId =
                        transactionId,

                    merchantOrderId =
                        merchantId,

                    paymentMethod =
                        paymentMethod,

                    status =
                        status,

                    transactionType =
                        transactionType,

                    duplicate = false,

                    selected = true
                )
            )
        }

        return result
    }

    /**
     * 判断微信中性交易是否属于
     * 转账 / 账户资金流转。
     */
    private fun isTransferTransaction(
        transactionType: String
    ): Boolean {

        val type =
            transactionType
                .replace(
                    "\uFEFF",
                    ""
                )
                .trim()

        if (type.isBlank()) {
            return false
        }

        return type.contains("转账") ||
                type.contains("转入") ||
                type.contains("转出") ||
                type.contains("提现") ||
                type.contains("充值") ||
                type.contains("零钱通") ||
                type.contains("经营账户")
    }

    private fun findColumn(
        headers: List<String>,
        name: String
    ): Int {

        for (
            index in headers.indices
        ) {

            val value =
                headers[index]
                    .replace(
                        "\uFEFF",
                        ""
                    )
                    .trim()

            if (value == name) {
                return index
            }
        }

        return -1
    }

    private fun getCell(
        row: List<String>,
        index: Int
    ): String {

        if (index < 0) {
            return ""
        }

        return row
            .getOrNull(index)
            ?.replace(
                "\uFEFF",
                ""
            )
            ?.trim()
            ?: ""
    }

    private fun parseAmount(
        text: String
    ): Double {

        if (text.isBlank()) {
            return 0.0
        }

        return text
            .replace("¥", "")
            .replace("￥", "")
            .replace(",", "")
            .trim()
            .toDoubleOrNull()
            ?: 0.0
    }

    private fun parseTime(
        text: String
    ): Long? {

        val value =
            text
                .replace(
                    "\uFEFF",
                    ""
                )
                .trim()

        if (value.isBlank()) {
            return null
        }

        val formats =
            listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm",
                "yyyy-MM-dd",
                "yyyy/MM/dd"
            )

        for (
            format in formats
        ) {

            try {

                val date =
                    SimpleDateFormat(
                        format,
                        Locale.CHINA
                    ).apply {
                        isLenient = false
                    }.parse(value)

                if (date != null) {
                    return date.time
                }

            } catch (_: Exception) {
            }
        }

        val excelNumber =
            value.toDoubleOrNull()

        if (
            excelNumber != null &&
            excelNumber > 1.0
        ) {

            return excelSerialToTimestamp(
                excelNumber
            )
        }

        return null
    }

    private fun excelSerialToTimestamp(
        serial: Double
    ): Long? {

        if (serial <= 0.0) {
            return null
        }

        return try {

            val wholeDays =
                serial.toLong()

            val fraction =
                serial - wholeDays

            val calendar =
                Calendar.getInstance(
                    Locale.CHINA
                )

            calendar.clear()

            calendar.set(
                1899,
                Calendar.DECEMBER,
                31,
                0,
                0,
                0
            )

            val correctedDays =
                if (
                    wholeDays >= 60
                ) {
                    wholeDays - 1
                } else {
                    wholeDays
                }

            calendar.add(
                Calendar.DAY_OF_YEAR,
                correctedDays.toInt()
            )

            val milliseconds =
                (
                    fraction *
                            24.0 *
                            60.0 *
                            60.0 *
                            1000.0
                    ).toLong()

            calendar.timeInMillis +=
                milliseconds

            calendar.timeInMillis

        } catch (_: Exception) {
            null
        }
    }
}