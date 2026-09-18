package com.example.myno.jz.ui.bills

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.databinding.ItemBillBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillAdapter(
    private var bills: List<Bill>,
    private var categories: List<Category>,
    private val onClick: (Bill) -> Unit
) : RecyclerView.Adapter<BillAdapter.BillViewHolder>() {

    private val timeFormat =
        SimpleDateFormat(
            "MM-dd HH:mm",
            Locale.getDefault()
        )

    fun updateData(
        newBills: List<Bill>,
        newCategories: List<Category>
    ) {
        bills = newBills
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BillViewHolder {

        val binding =
            ItemBillBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: BillViewHolder,
        position: Int
    ) {
        holder.bind(
            bills[position]
        )
    }

    override fun getItemCount(): Int {
        return bills.size
    }

    /**
     * 根据分类名称返回项目现有的分类图标。
     *
     * 不使用 Emoji，
     * 不对原始图标进行 tint。
     */
    private fun getCategoryIcon(
        categoryName: String?
    ): Int {

        return when (
            categoryName
                ?.trim()
                ?.lowercase(Locale.getDefault())
        ) {

            "餐饮",
            "吃饭",
            "美食" ->
                R.drawable.ic_category_food

            "购物" ->
                R.drawable.ic_category_shopping

            "交通" ->
                R.drawable.ic_category_transport

            "工资" ->
                R.drawable.ic_category_salary

            "居住",
            "住房",
            "房租" ->
                R.drawable.ic_category_house

            "医疗" ->
                R.drawable.ic_category_medical

            "教育" ->
                R.drawable.ic_category_education

            "娱乐" ->
                R.drawable.ic_category_entertainment

            "通讯" ->
                R.drawable.ic_category_communication

            "旅行",
            "旅游" ->
                R.drawable.ic_category_travel

            else ->
                R.drawable.ic_category_other
        }
    }

    inner class BillViewHolder(
        private val binding: ItemBillBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            bill: Bill
        ) {

            val category =
                categories.firstOrNull {
                    it.id == bill.categoryId
                }

            val categoryName =
                category?.name
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: "其他"

            binding.tvCategory.text =
                categoryName

            /*
             * 使用项目已有的分类图片资源。
             *
             * 不设置 imageTint，
             * 保留原图标自身颜色。
             */
            binding.ivCategoryIcon.setImageResource(
                getCategoryIcon(
                    categoryName
                )
            )

            binding.tvNote.text =
                if (bill.note.isBlank()) {
                    "无备注"
                } else {
                    bill.note
                }

            binding.tvTime.text =
                timeFormat.format(
                    Date(bill.timestamp)
                )

            val amountText =
                String.format(
                    Locale.getDefault(),
                    "¥%.2f",
                    bill.amount
                )

            if (bill.type ==
                BillType.EXPENSE
            ) {

                binding.tvAmount.text =
                    "-$amountText"

                binding.tvAmount.setTextColor(
                    binding.root.context
                        .getColor(
                            R.color.expense_color
                        )
                )

            } else {

                binding.tvAmount.text =
                    "+$amountText"

                binding.tvAmount.setTextColor(
                    binding.root.context
                        .getColor(
                            R.color.income_color
                        )
                )
            }

            binding.root.setOnClickListener {
                onClick(bill)
            }
        }
    }
}