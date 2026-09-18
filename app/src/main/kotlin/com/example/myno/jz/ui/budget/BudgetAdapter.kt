package com.example.myno.jz.ui.budget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myno.jz.data.model.Budget
import com.example.myno.jz.databinding.ItemBudgetBinding
import java.util.Locale

class BudgetAdapter(
    private var items: List<BudgetItem> = emptyList(),
    private val onClick: (BudgetItem) -> Unit,
    private val onDelete: (BudgetItem) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    fun updateData(
        newItems: List<BudgetItem>
    ) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BudgetViewHolder {

        val binding =
            ItemBudgetBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: BudgetViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int =
        items.size

    inner class BudgetViewHolder(
        private val binding: ItemBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BudgetItem) {

            val budget =
                item.budget

            binding.tvCategory.text =
                item.categoryName

            val percent =
                item.usedPercent

            binding.tvPercent.text =
                String.format(
                    Locale.getDefault(),
                    "%.0f%%",
                    percent
                )

            binding.tvAmount.text =
                String.format(
                    Locale.getDefault(),
                    "已用 ¥%.2f / ¥%.2f",
                    item.usedAmount,
                    budget.amount
                )

            binding.progressBudget.progress =
                percent
                    .coerceAtMost(100.0)
                    .toInt()

            binding.tvRemaining.text =
                if (item.remainingAmount >= 0) {

                    String.format(
                        Locale.getDefault(),
                        "剩余 ¥%.2f",
                        item.remainingAmount
                    )

                } else {

                    String.format(
                        Locale.getDefault(),
                        "已超支 ¥%.2f",
                        -item.remainingAmount
                    )
                }

            if (percent >= 100.0) {

                binding.tvWarning.visibility =
                    android.view.View.VISIBLE

                binding.tvWarning.text =
                    "⚠ 已超过预算"

            } else if (
                percent >= budget.warningPercent
            ) {

                binding.tvWarning.visibility =
                    android.view.View.VISIBLE

                binding.tvWarning.text =
                    "⚠ 已达到预算预警线"

            } else {

                binding.tvWarning.visibility =
                    android.view.View.GONE
            }

            binding.root.setOnClickListener {
    onClick(item)
}

binding.root.setOnLongClickListener {
    android.app.AlertDialog.Builder(
        binding.root.context
    )
        .setTitle("预算操作")
        .setItems(
            arrayOf(
                "编辑预算",
                "删除预算"
            )
        ) { _, which ->

            when (which) {

                0 -> {
                    onClick(item)
                }

                1 -> {
                    onDelete(item)
                }
            }
        }
        .show()

    true
}
        }
    }
}