package com.example.myno.jz.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myno.jz.R
import com.example.myno.jz.data.model.Bill
import com.example.myno.jz.data.model.BillType
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.databinding.ItemBillHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentBillAdapter(
    private val onItemClick: (Bill) -> Unit = {}
) : RecyclerView.Adapter<RecentBillAdapter.BillViewHolder>() {

    private var bills: List<Bill> = emptyList()
    private var categories: List<Category> = emptyList()

    fun submitData(bills: List<Bill>, categories: List<Category>) {
        this.bills = bills
        this.categories = categories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemBillHomeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        holder.bind(bills[position])
    }

    override fun getItemCount(): Int = bills.size

    inner class BillViewHolder(
        private val binding: ItemBillHomeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bill: Bill) {
            val category = categories.firstOrNull { it.id == bill.categoryId }
            val categoryName = category?.name ?: "其他"

            binding.tvCategoryName.text = categoryName
            binding.ivBillCategoryIcon.setImageResource(
                getCategoryIcon(categoryName)
            )

            binding.tvBillTime.text = SimpleDateFormat(
                "MM-dd HH:mm",
                Locale.CHINA
            ).format(Date(bill.timestamp))

            val amountText = String.format(Locale.CHINA, "¥%.2f", bill.amount)
            if (bill.type == BillType.EXPENSE) {
                binding.tvBillAmount.text = "-$amountText"
                binding.tvBillAmount.setTextColor(
                    binding.root.context.getColor(R.color.expense_color)
                )
                binding.tvBillType.text = "支出"
            } else {
                binding.tvBillAmount.text = "+$amountText"
                binding.tvBillAmount.setTextColor(
                    binding.root.context.getColor(R.color.income_color)
                )
                binding.tvBillType.text = "收入"
            }

            binding.root.setOnClickListener { onItemClick(bill) }
        }
    }

    private fun getCategoryIcon(categoryName: String): Int {
        return when (categoryName) {
            "餐饮", "吃饭", "美食" -> R.drawable.ic_category_food
            "购物" -> R.drawable.ic_category_shopping
            "交通" -> R.drawable.ic_category_transport
            "工资" -> R.drawable.ic_category_salary
            "居住", "住房", "房租" -> R.drawable.ic_category_house
            "医疗" -> R.drawable.ic_category_medical
            "教育" -> R.drawable.ic_category_education
            "娱乐" -> R.drawable.ic_category_entertainment
            "通讯" -> R.drawable.ic_category_communication
            "旅行", "旅游" -> R.drawable.ic_category_travel
            else -> R.drawable.ic_category_other
        }
    }
}
