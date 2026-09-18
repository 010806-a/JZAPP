package com.example.myno.jz.data.imports

import com.example.myno.jz.data.model.Bill
import kotlin.math.abs

object ImportDuplicateChecker {

fun markDuplicates(
    importedItems: List<ImportBillItem>,
    existingBills: List<Bill>
): List<ImportBillItem> {

    return importedItems.map { imported ->

        val duplicate =
            existingBills.any { existing ->
                isSameBill(
                    imported,
                    existing
                )
            }

        imported.copy(
            duplicate = duplicate,
            selected = !duplicate
        )
    }
}

private fun isSameBill(
    imported: ImportBillItem,
    existing: Bill
): Boolean {

    /*
     * 中性交易目前保存到 transfers.json，
     * 不应该拿它和普通 Bill 比较。
     */
    if (imported.type == ImportBillType.NEUTRAL) {
        return false
    }

    /*
     * 第一优先级：
     * 来源 + 交易单号
     */
    if (
        imported.source.isNotBlank() &&
        imported.transactionId.isNotBlank() &&
        existing.source.isNotBlank() &&
        existing.sourceTransactionId.isNotBlank()
    ) {

        return imported.source == existing.source &&
                imported.transactionId ==
                existing.sourceTransactionId
    }

    /*
     * 第二优先级：
     * 类型 + 金额 + 时间 + 商户
     */
    val typeSame =
        when (imported.type) {

            ImportBillType.EXPENSE ->
                existing.type.name == "EXPENSE"

            ImportBillType.INCOME ->
                existing.type.name == "INCOME"

            ImportBillType.NEUTRAL ->
                false

            ImportBillType.UNKNOWN ->
                false
        }

    if (!typeSame) {
        return false
    }

    if (
        abs(
            imported.amount -
                    existing.amount
        ) > 0.01
    ) {
        return false
    }

    val importedTime =
        imported.timestamp
            ?: return false

    if (
        abs(
            importedTime -
                    existing.timestamp
        ) > 60_000L
    ) {
        return false
    }

    if (imported.merchant.isBlank()) {
        return true
    }

    val existingMerchant =
        extractMerchantFromBill(
            existing
        )

    return imported.merchant ==
            existingMerchant
}

private fun extractMerchantFromBill(
    bill: Bill
): String {

    val note =
        bill.note.trim()

    if (note.isBlank()) {
        return ""
    }

    return note
        .substringBefore(" · ")
        .trim()
}

}