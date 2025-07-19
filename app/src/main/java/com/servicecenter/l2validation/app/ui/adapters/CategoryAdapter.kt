package com.servicecenter.l2validation.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.data.local.entities.FilterResponse
import com.servicecenter.l2validation.databinding.ItemCategoryBinding

class CategoryAdapter(
    private var categories: List<FilterResponse>,
    private val onCategoryClicked: (FilterResponse, Int) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dateModel: FilterResponse, position: Int) {
            binding.tvCateName.text = dateModel.key
            itemView.setOnClickListener {
                onCategoryClicked(dateModel, position)
                updateSelectedPosition(position)
            }

            if (position == selectedPosition) {
                binding.viewIndicator.visibility = View.VISIBLE
                binding.viewIndicator.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.blue_indicator
                    )
                )
                binding.tvCateName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.blue_indicator
                    )
                )
            } else {
                binding.viewIndicator.visibility = View.GONE
                binding.viewIndicator.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.white
                    )
                )
                binding.tvCateName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.black
                    )
                )

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<FilterResponse>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    private fun updateSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }
}
