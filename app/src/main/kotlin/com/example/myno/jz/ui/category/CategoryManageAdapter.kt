package com.example.myno.jz.ui.category

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.databinding.ItemCategoryManageBinding

/**
 * 分类管理列表适配器
 *
 * 支持：
 * 1. 分类展示
 * 2. 点击编辑
 * 3. 长按拖动排序
 */
class CategoryManageAdapter(
    private var categories: List<Category> = emptyList(),
    private val onClick: (Category) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<CategoryManageAdapter.CategoryViewHolder>() {

    fun updateData(
        newCategories: List<Category>
    ) {
        categories = newCategories
        notifyDataSetChanged()
    }

    fun getCategories(): List<Category> {
        return categories
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {

        val binding =
            ItemCategoryManageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryManageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {

            binding.tvIcon.text =
                category.icon

            binding.tvName.text =
                category.name

            binding.tvState.text =
                when {
                    !category.visible ->
                        "已隐藏"

                    category.isSystem ->
                        "默认"

                    else ->
                        ""
                }

            binding.root.setOnClickListener {
                onClick(category)
            }

            /*
             * 长按整个分类项目开始拖动。
             */
            binding.root.setOnLongClickListener {

                onStartDrag(this)

                true
            }
        }
    }
}