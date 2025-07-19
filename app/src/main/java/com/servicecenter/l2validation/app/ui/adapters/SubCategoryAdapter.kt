package com.servicecenter.l2validation.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.data.local.entities.SubData
import com.servicecenter.l2validation.databinding.ItemSubcategoryBinding

class SubCategoryAdapter(
    private var categoryPosition: Int,
    private var subCategories: List<SubData>,
    private val onCategoryClicked: (SubData, Boolean,Int) -> Unit,
) : RecyclerView.Adapter<SubCategoryAdapter.SubCategoryViewHolder>() {

    inner class SubCategoryViewHolder(val binding: ItemSubcategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dateModel: SubData) {
            binding.tvSubCateName.isChecked = dateModel.filter_checked
            binding.tvSubCateName.text = dateModel.filter_value

            binding.tvSubCateName.setOnClickListener {
                if (binding.tvSubCateName.isChecked) onCategoryClicked(dateModel, true,categoryPosition)
                else onCategoryClicked(dateModel, false ,categoryPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubCategoryViewHolder {
        val binding =
            ItemSubcategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubCategoryViewHolder, position: Int) {
        holder.bind(subCategories[position])
    }

    override fun getItemCount(): Int = subCategories.size

    fun updateData(newSubCategories: List<SubData>,categoryPositions: Int) {
        subCategories = newSubCategories
        categoryPosition=categoryPositions
        notifyDataSetChanged()
    }
}
