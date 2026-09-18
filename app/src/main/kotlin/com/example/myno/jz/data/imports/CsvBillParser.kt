package com.example.myno.jz.data.imports

import java.text.SimpleDateFormat
import java.util.Locale

object CsvBillParser {

    fun parse(content: String): List<ImportBillItem> {

        if (content.isBlank()) {
            return emptyList()
        }

        val normalized =
            content
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace("\r", "\n")

        val lines =
            normalized.split("\n")

        // ==========================================
        // 1. 首先寻找真正的微信账单表头
        // ==========================================

        val wechatHeaderIndex =
            lines.indexOfFirst { line ->

                line.contains("交易时间") &&
                    line.contains("交易类型") &&
                    line.contains("交易对方") &&
                    line.contains("收/支") &&
                    line.contains("金额(元)")
            }

        if (wechatHeaderIndex >= 0) {

            return parseWechatBill(
                lines = lines,
                headerIndex = wechatHeaderIndex
            )
        }

        // ==========================================
        // 2. 如果不是微信格式，再使用普通 CSV
        // ==========================================

        return parseGenericCsv(
            lines
        )
    }

    /**
     * 微信账单解析。
     */
    private fun parseWechatBill(
        lines: List<String>,
        headerIndex: Int
    ): List<ImportBillItem> {

        val result =
            mutableListOf<ImportBillItem>()

        var rowIndex = 0

        for (i in headerIndex + 1 until lines.size) {

            val line =
                lines[i]

            if (line.isBlank()) {
                continue
            }

            // 微信账单是 TAB 分隔
            val columns =
                line.split("\t")

            /*
             * 微信正常账单至少应该有：
             *
             * 0 交易时间
             * 1 交易类型
             * 2 交易对方
             * 3 商品
             * 4 收/支
             * 5 金额(元)
             * 6 支付方式
             * 7 当前状态
             * 8 交易单号
             * 9 商户单号
             * 10 备注
             */

            if (columns.size < 6) {
                continue
            }

            val transactionTime =
                columns.getOrNull(0)?.trim() ?: ""

            val transactionType =
                columns.getOrNull(1)?.trim() ?: ""

            val merchant =
                columns.getOrNull(2)?.trim() ?: ""

            val product =
                columns.getOrNull(3)?.trim() ?: ""

            val incomeExpense =
                columns.getOrNull(4)?.trim() ?: ""

            val amountText =
                columns.getOrNull(5)?.trim() ?: ""

            val paymentMethod =
                columns.getOrNull(6)?.trim() ?: ""

            val status =
                columns.getOrNull(7)?.trim() ?: ""

            val transactionId =
                columns.getOrNull(8)?.trim() ?: ""

            val merchantId =
                columns.getOrNull(9)?.trim() ?: ""

            val remark =
                columns.getOrNull(10)?.trim() ?: ""

            // ==========================================
            // 忽略完全无效的行
            // ==========================================

            if (transactionTime.isBlank()) {
                continue
            }

            // ==========================================
            // 判断是否为中性交易
            // ==========================================

            /*
             * 微信官方账单中的：
             *
             * / 收/支
             *
             * 通常代表：
             * 充值
             * 提现
             * 零钱通转入
             * 零钱通转出
             * 信用卡还款
             * 账户之间资金转移
             *
             * 这些不能计入收入/支出。
             */

            if (
                incomeExpense != "收入" &&
                incomeExpense != "支出"
            ) {
                continue
            }

            // ==========================================
            // 金额
            // ==========================================

            val amount =
                parseWechatAmount(
                    amountText
                )

            if (amount <= 0.0) {
                continue
            }

            // ==========================================
            // 收入 / 支出
            // ==========================================

            val type =
                when (incomeExpense) {

                    "收入" ->
                        ImportBillType.INCOME

                    "支出" ->
                        ImportBillType.EXPENSE

                    else ->
                        ImportBillType.UNKNOWN
                }

            // ==========================================
            // 时间
            // ==========================================

            val timestamp =
                parseWechatTime(
                    transactionTime
                )

            // ==========================================
            // 商户名称
            // ==========================================

            val finalMerchant =
                when {

                    merchant.isNotBlank() &&
                        merchant != "/" ->
                        merchant

                    product.isNotBlank() &&
                        product != "/" ->
                        product

                    else ->
                        transactionType
                }

            // ==========================================
            // 备注
            // ==========================================

            val finalNote =
                buildWechatNote(
                    product = product,
                    status = status,
                    paymentMethod = paymentMethod,
                    remark = remark,
                    transactionId = transactionId
                )

            result.add(
                ImportBillItem(
                    rowIndex = rowIndex++,
                    timestamp = timestamp,
                    type = type,
                    amount = amount,
                    merchant = finalMerchant,
                    note = finalNote,
                    originalRow = line,
                    duplicate = false,
                    selected = true
                )
            )
        }

        return result
    }

    /**
     * 解析微信金额。
     *
     * 支持：
     *
     * ¥1.00
     * ¥50.37
     * 1.00
     * 50.37
     */
    private fun parseWechatAmount(
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

    /**
     * 解析微信时间。
     */
    private fun parseWechatTime(
        text: String
    ): Long? {

        val formats =
            listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm"
            )

        for (format in formats) {

            try {

                return SimpleDateFormat(
                    format,
                    Locale.CHINA
                ).parse(text)?.time

            } catch (_: Exception) {
            }
        }

        return null
    }

    /**
     * 生成微信账单备注。
     */
    private fun buildWechatNote(
        product: String,
        status: String,
        paymentMethod: String,
        remark: String,
        transactionId: String
    ): String {

        val parts =
            mutableListOf<String>()

        if (
            product.isNotBlank() &&
            product != "/"
        ) {
            parts.add(product)
        }

        if (
            remark.isNotBlank() &&
            remark != "/"
        ) {
            parts.add(remark)
        }

        /*
         * 如果是退款等特殊状态，
         * 保存状态方便后续处理。
         */
        if (
            status.isNotBlank() &&
            status != "支付成功"
        ) {
            parts.add(status)
        }

        return parts.joinToString(
            separator = " · "
        )
    }

    /**
     * 普通 CSV / TXT 解析。
     */
    private fun parseGenericCsv(
        lines: List<String>
    ): List<ImportBillItem> {

        if (lines.isEmpty()) {
            return emptyList()
        }

        val headerIndex =
            lines.indexOfFirst { line ->

                line.contains("时间") ||
                    line.contains("日期") ||
                    line.contains("金额") ||
                    line.contains("amount", true)
            }

        if (headerIndex < 0) {
            return emptyList()
        }

        val header =
            lines[headerIndex]

        val delimiter =
            detectDelimiter(header)

        val headers =
            header.split(delimiter)

        val timeIndex =
            findColumn(
                headers,
                listOf(
                    "交易时间",
                    "时间",
                    "日期",
                    "交易日期",
                    "date",
                    "time"
                )
            )

        val typeIndex =
            findColumn(
                headers,
                listOf(
                    "收/支",
                    "类型",
                    "交易类型",
                    "type"
                )
            )

        val amountIndex =
            findColumn(
                headers,
                listOf(
                    "金额(元)",
                    "金额",
                    "amount"
                )
            )

        val merchantIndex =
            findColumn(
                headers,
                listOf(
                    "交易对方",
                    "商户",
                    "商户名称",
                    "merchant"
                )
            )

        val noteIndex =
            findColumn(
                headers,
                listOf(
                    "商品",
                    "备注",
                    "说明",
                    "note"
                )
            )

        if (
            timeIndex < 0 ||
            amountIndex < 0
        ) {
            return emptyList()
        }

        val result =
            mutableListOf<ImportBillItem>()

        var rowIndex = 0

        for (
            i in headerIndex + 1 until lines.size
        ) {

            val line =
                lines[i]

            if (line.isBlank()) {
                continue
            }

            val columns =
                line.split(delimiter)

            val timeText =
                columns.getOrNull(timeIndex)
                    ?.trim()
                    ?: ""

            val amountText =
                columns.getOrNull(amountIndex)
                    ?.trim()
                    ?: ""

            val amount =
                amountText
                    .replace("¥", "")
                    .replace("￥", "")
                    .replace(",", "")
                    .trim()
                    .toDoubleOrNull()
                    ?: 0.0

            if (
                timeText.isBlank() ||
                amount <= 0.0
            ) {
                continue
            }

            val typeText =
                if (typeIndex >= 0) {
                    columns.getOrNull(typeIndex)
                        ?.trim()
                        ?: ""
                } else {
                    ""
                }

            val type =
                detectGenericType(
                    typeText
                )

            if (
                type == ImportBillType.UNKNOWN
            ) {
                continue
            }

            val merchant =
                if (merchantIndex >= 0) {
                    columns.getOrNull(merchantIndex)
                        ?.trim()
                        ?: ""
                } else {
                    ""
                }

            val note =
                if (noteIndex >= 0) {
                    columns.getOrNull(noteIndex)
                        ?.trim()
                        ?: ""
                } else {
                    ""
                }

            result.add(
                ImportBillItem(
                    rowIndex = rowIndex++,
                    timestamp = parseGenericTime(
                        timeText
                    ),
                    type = type,
                    amount = amount,
                    merchant = merchant,
                    note = note,
                    originalRow = line,
                    duplicate = false,
                    selected = true
                )
            )
        }

        return result
    }

    private fun detectDelimiter(
        header: String
    ): String {

        return when {

            header.contains("\t") ->
                "\t"

            header.contains(",") ->
                ","

            header.contains(";") ->
                ";"

            else ->
                "\t"
        }
    }

    private fun findColumn(
        headers: List<String>,
        names: List<String>
    ): Int {

        for (i in headers.indices) {

            val header =
                headers[i]
                    .trim()
                    .lowercase(
                        Locale.getDefault()
                    )

            if (
                names.any {
                    header == it.lowercase(
                        Locale.getDefault()
                    )
                }
            ) {
                return i
            }
        }

        return -1
    }

    private fun detectGenericType(
        text: String
    ): ImportBillType {

        return when {

            text.contains("支出") ||
                text.contains("消费") ||
                text.equals(
                    "expense",
                    true
                ) ->
                ImportBillType.EXPENSE

            text.contains("收入") ||
                text.equals(
                    "income",
                    true
                ) ->
                ImportBillType.INCOME

            else ->
                ImportBillType.UNKNOWN
        }
    }

    private fun parseGenericTime(
        text: String
    ): Long? {

        val formats =
            listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd HH:mm",
                "yyyy-MM-dd",
                "yyyy/MM/dd"
            )

        for (format in formats) {

            try {

                return SimpleDateFormat(
                    format,
                    Locale.CHINA
                ).parse(text)?.time

            } catch (_: Exception) {
            }
        }

        return null
    }
}