package com.example.myno.jz.ui.assets

import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.Transfer

sealed class AccountFlowItem {

    abstract val timestamp: Long

    data class BillItem(
        val bill: Bill,
        val categoryName: String
    ) : AccountFlowItem() {

        override val timestamp: Long
            get() = bill.timestamp
    }

    data class TransferItem(
        val transfer: Transfer,
        val fromAccountName: String,
        val toAccountName: String,
        val isTransferIn: Boolean
    ) : AccountFlowItem() {

        override val timestamp: Long
            get() = transfer.timestamp
    }
}

sealed class AccountFlowDisplayItem {

    data class DateHeader(
        val date: String
    ) : AccountFlowDisplayItem()

    data class Flow(
        val item: AccountFlowItem
    ) : AccountFlowDisplayItem()
}