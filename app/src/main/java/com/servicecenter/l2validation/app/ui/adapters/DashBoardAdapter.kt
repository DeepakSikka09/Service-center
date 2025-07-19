package com.servicecenter.l2validation.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.data.local.entities.DashboardEnum
import com.servicecenter.l2validation.data.local.entities.DashboardItem
import com.servicecenter.l2validation.databinding.DashboardItemBinding



class DashBoardAdapter(
    private val list: List<DashboardItem>,
    private val onClick: (DashboardEnum)->Unit
):RecyclerView.Adapter<DashBoardAdapter.DashboardViewHolder>() {
    inner class DashboardViewHolder(
        val binding: DashboardItemBinding
    ) :RecyclerView.ViewHolder(binding.root) {
        init {
            binding.cardView.setOnClickListener {
                onClick(list[adapterPosition].code)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = DashboardItemBinding.inflate(inflater,parent,false)
        return DashboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: DashboardViewHolder, position: Int) {
        holder.binding.icon.setImageResource(list[position].icon)
        holder.binding.title.text = list[position].title
    }

    override fun getItemCount(): Int {
        return list.size
    }
}