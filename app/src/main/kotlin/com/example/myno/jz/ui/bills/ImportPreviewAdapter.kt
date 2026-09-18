package com.example.myno.jz.ui.bills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myno.jz.R
import com.example.myno.jz.data.imports.ImportBillItem
import com.example.myno.jz.data.imports.ImportBillType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImportPreviewAdapter(
    private val items: MutableList<ImportBillItem>,
    private val listener: OnSelectionChangedListener
) : RecyclerView.Adapter<ImportPreviewAdapter.ViewHolder>() {

    interface OnSelectionChangedListener {
        fun onSelectionChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val checkBox: CheckBox =
            view.findViewById(R.id.cbSelected)

        val tvTime: TextView =
            view.findViewById(R.id.tvItemTime)

        val tvMerchant: TextView =
            view.findViewById(R.id.tvItemMerchant)

        val tvAmount: TextView =
            view.findViewById(R.id.tvItemAmount)

        val tvInfo: TextView =
            view.findViewById(R.id.tvItemInfo)

        val tvDuplicate: TextView =
            view.findViewById(R.id.tvDuplicate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_import_preview,
                    parent,
                    false
                )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = items[position]

        holder.checkBox.setOnCheckedChangeListener(null)

        val isNeutral =
            item.type == ImportBillType.NEUTRAL

        val canSelect =
            !item.duplicate &&
                    (!isNeutral || item.transferResolved)

        holder.checkBox.isEnabled =
            canSelect

        holder.checkBox.isChecked =
            item.selected && canSelect

        holder.tvTime.text =
            formatTime(item.timestamp)

        holder.tvMerchant.text =
            if (item.merchant.isNotBlank()) {

                item.merchant

            } else {

                when (item.type) {

                    ImportBillType.NEUTRAL ->
                        "账户转账"

                    else ->
                        "未知交易"
                }
            }

        val amountText =
            when (item.type) {

                ImportBillType.EXPENSE ->
                    "-¥%.2f".format(
                        Locale.getDefault(),
                        item.amount
                    )

                ImportBillType.INCOME ->
                    "+¥%.2f".format(
                        Locale.getDefault(),
                        item.amount
                    )

                ImportBillType.NEUTRAL ->
                    "¥%.2f".format(
                        Locale.getDefault(),
                        item.amount
                    )

                ImportBillType.UNKNOWN ->
                    "¥%.2f".format(
                        Locale.getDefault(),
                        item.amount
                    )
            }

        holder.tvAmount.text =
            amountText

        val infoParts =
            mutableListOf<String>()

        if (item.transactionType.isNotBlank()) {
            infoParts.add(item.transactionType)
        }

        if (
            item.note.isNotBlank() &&
            item.note != "/"
        ) {
            infoParts.add(item.note)
        }

        if (
            item.paymentMethod.isNotBlank() &&
            item.paymentMethod != "/"
        ) {
            infoParts.add(
                "支付方式:${item.paymentMethod}"
            )
        }

        if (
            item.status.isNotBlank() &&
            item.status != "/"
        ) {
            infoParts.add(item.status)
        }

        if (isNeutral) {

            if (item.transferResolved) {

                infoParts.add(
                    "转出：${item.transferFromAccountName}"
                )

                infoParts.add(
                    "转入：${item.transferToAccountName}"
                )

            } else {

                infoParts.add(
                    "无法自动匹配转账账户"
                )
            }
        }

        holder.tvInfo.text =
            infoParts.joinToString(" · ")

        when {

            item.duplicate -> {

                holder.tvDuplicate.visibility =
                    View.VISIBLE

                holder.tvDuplicate.text =
                    "重复"

                holder.checkBox.isChecked =
                    false
            }

            isNeutral &&
                    item.transferResolved -> {

                holder.tvDuplicate.visibility =
                    View.VISIBLE

                holder.tvDuplicate.text =
                    "转账"
            }

            isNeutral -> {

                holder.tvDuplicate.visibility =
                    View.VISIBLE

                holder.tvDuplicate.text =
                    "需匹配账户"

                holder.checkBox.isChecked =
                    false
            }

            else -> {

                holder.tvDuplicate.visibility =
                    View.GONE
            }
        }

        holder.checkBox.setOnCheckedChangeListener {
                _,
                checked ->

            if (canSelect) {

                item.selected =
                    checked

                listener.onSelectionChanged()
            }
        }

        holder.itemView.setOnClickListener {

            if (!canSelect) {
                return@setOnClickListener
            }

            item.selected =
                !item.selected

            holder.checkBox.setOnCheckedChangeListener(null)

            holder.checkBox.isChecked =
                item.selected

            holder.checkBox.setOnCheckedChangeListener {
                    _,
                    checked ->

                if (canSelect) {

                    item.selected =
                        checked

                    listener.onSelectionChanged()
                }
            }

            listener.onSelectionChanged()
        }
    }

    override fun getItemCount(): Int =
        items.size

    fun updateItems(
        newItems: List<ImportBillItem>
    ) {

        items.clear()

        items.addAll(newItems)

        notifyDataSetChanged()
    }

    private fun formatTime(
        timestamp: Long?
    ): String {

        if (timestamp == null) {
            return "未识别时间"
        }

        return try {

            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )

        } catch (e: Exception) {

            "未识别时间"
        }
    }
}