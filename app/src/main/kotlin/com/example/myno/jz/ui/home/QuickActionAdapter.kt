package com.example.myno.jz.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myno.jz.databinding.ItemQuickActionBinding

class QuickActionAdapter(
    private val onActionClick: (QuickAction) -> Unit,
    private val onDeleteClick: (QuickAction) -> Unit
) : RecyclerView.Adapter<QuickActionAdapter.QuickActionViewHolder>() {

    private val actions =
        mutableListOf<QuickAction>()

    private var managementMode =
        false

    fun submitList(
        newActions: List<QuickAction>
    ) {

        actions.clear()
        actions.addAll(newActions)

        notifyDataSetChanged()
    }

    fun getActions(): List<QuickAction> {
        return actions.toList()
    }

    fun setManagementMode(
        enabled: Boolean
    ) {

        managementMode = enabled

        notifyDataSetChanged()
    }

    fun moveItem(
        fromPosition: Int,
        toPosition: Int
    ) {

        if (fromPosition !in actions.indices) {
            return
        }

        if (toPosition !in actions.indices) {
            return
        }

        val item =
            actions.removeAt(fromPosition)

        actions.add(
            toPosition,
            item
        )

        notifyItemMoved(
            fromPosition,
            toPosition
        )
    }

    fun removeItem(
        position: Int
    ): QuickAction? {

        if (position !in actions.indices) {
            return null
        }

        val item =
            actions.removeAt(position)

        notifyItemRemoved(position)

        return item
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuickActionViewHolder {

        val binding =
            ItemQuickActionBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return QuickActionViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: QuickActionViewHolder,
        position: Int
    ) {

        holder.bind(
            actions[position]
        )
    }

    override fun getItemCount(): Int {
        return actions.size
    }

    inner class QuickActionViewHolder(
        private val binding: ItemQuickActionBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            action: QuickAction
        ) {

            binding.ivQuickActionIcon.setImageResource(
                action.iconRes
            )

            binding.tvQuickActionTitle.text =
                action.title

            binding.tvDeleteBadge.visibility =
                if (managementMode) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

            binding.cardQuickAction.setOnClickListener {

                if (!managementMode) {

                    onActionClick(action)
                }
            }

            binding.tvDeleteBadge.setOnClickListener {

                if (managementMode) {

                    onDeleteClick(action)
                }
            }
        }
    }
}