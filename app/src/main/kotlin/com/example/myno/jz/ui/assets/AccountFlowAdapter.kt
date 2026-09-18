package com.example.myno.jz.ui.assets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myno.jz.R
import com.example.myno.jz.data.model.BillType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountFlowAdapter(
    private val onBillClick: (String) -> Unit,
    private val onTransferClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<AccountFlowDisplayItem> = emptyList()

    companion object {
        private const val TYPE_DATE = 1
        private const val TYPE_FLOW = 2
    }

    fun submitList(newList: List<AccountFlowDisplayItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AccountFlowDisplayItem.DateHeader -> TYPE_DATE
            is AccountFlowDisplayItem.Flow -> TYPE_FLOW
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TYPE_DATE) {
            DateViewHolder(
                inflater.inflate(
                    R.layout.item_account_flow_date,
                    parent,
                    false
                )
            )
        } else {
            FlowViewHolder(
                inflater.inflate(
                    R.layout.item_account_flow,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        when (val item = items[position]) {

            is AccountFlowDisplayItem.DateHeader -> {
                (holder as DateViewHolder).bind(item.date)
            }

            is AccountFlowDisplayItem.Flow -> {
                (holder as FlowViewHolder).bind(
                    item.item,
                    onBillClick,
                    onTransferClick
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class DateViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvDate: TextView =
            itemView.findViewById(R.id.tvDate)

        fun bind(date: String) {
            tvDate.text = date
        }
    }

    class FlowViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvIcon: TextView =
            itemView.findViewById(R.id.tvIcon)

        private val tvTitle: TextView =
            itemView.findViewById(R.id.tvTitle)

        private val tvSubTitle: TextView =
            itemView.findViewById(R.id.tvSubTitle)

        private val tvTime: TextView =
            itemView.findViewById(R.id.tvTime)

        private val tvAmount: TextView =
            itemView.findViewById(R.id.tvAmount)

        fun bind(
            item: AccountFlowItem,
            onBillClick: (String) -> Unit,
            onTransferClick: (String) -> Unit
        ) {

            itemView.setOnClickListener(null)

            when (item) {

                is AccountFlowItem.BillItem -> {

                    val bill = item.bill

                    tvIcon.text =
                        if (bill.type == BillType.INCOME) {
                            "↓"
                        } else {
                            "↑"
                        }

                    tvTitle.text =
                        if (item.categoryName.isBlank()) {
                            if (bill.type == BillType.INCOME) {
                                "收入"
                            } else {
                                "支出"
                            }
                        } else {
                            item.categoryName
                        }

                    tvSubTitle.text =
                        if (bill.note.isBlank()) {
                            if (bill.source.isBlank()) {
                                "账单"
                            } else {
                                bill.source
                            }
                        } else {
                            bill.note
                        }

                    tvAmount.text =
                        if (bill.type == BillType.INCOME) {
                            "+¥${formatMoney(bill.amount)}"
                        } else {
                            "-¥${formatMoney(bill.amount)}"
                        }

                    tvTime.text =
                        formatTime(bill.timestamp)

                    itemView.setOnClickListener {
                        onBillClick(bill.id)
                    }
                }

                is AccountFlowItem.TransferItem -> {

                    tvIcon.text = "↔"

                    if (item.isTransferIn) {

                        tvTitle.text = "转账转入"

                    } else {

                        tvTitle.text = "转账转出"
                    }

                    tvSubTitle.text =
                        "${item.fromAccountName} → ${item.toAccountName}"

                    tvAmount.text =
                        if (item.isTransferIn) {
                            "+¥${formatMoney(item.transfer.amount)}"
                        } else {
                            "-¥${formatMoney(item.transfer.amount)}"
                        }

                    tvTime.text =
                        formatTime(item.transfer.timestamp)

                    itemView.setOnClickListener {
                        onTransferClick(item.transfer.id)
                    }
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            return SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(Date(timestamp))
        }

        private fun formatMoney(value: Double): String {
            return String.format(
                Locale.getDefault(),
                "%.2f",
                value
            )
        }
    }
}