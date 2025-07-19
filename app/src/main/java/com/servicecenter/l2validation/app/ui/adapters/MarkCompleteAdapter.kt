package com.servicecenter.l2validation.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.extensions.gone
import com.servicecenter.l2validation.app.extensions.visible
import com.servicecenter.l2validation.data.remote.model.TallyShipmentDetail
import com.servicecenter.l2validation.databinding.TallyPendingItemBinding

class MarkCompleteAdapter(
    val selectAll: () -> Unit,
    val unSelectAll: () -> Unit,
    val toggleSelectAll:(Boolean)->Unit,
    val selectedAwb:MutableSet<Long>) :RecyclerView.Adapter<MarkCompleteAdapter.MarkCompleteViewHolder>() {
    private var pendingList:List<TallyShipmentDetail> = listOf()

    fun setList(list:List<TallyShipmentDetail>){
        pendingList = list
        if(selectedAwb.isNotEmpty()){
            selectAll()
        }
        if(selectedAwb.containsAll(pendingList.map { it.awb_no })){
            toggleSelectAll(true)
        }else{
            toggleSelectAll(false)
        }
        notifyDataSetChanged()
    }

    fun hideSelectAll(){
        selectedAwb.removeAll(pendingList.map { it.awb_no }.toSet())
        if(selectedAwb.isEmpty()){
            unSelectAll()
        }else{
            selectAll()
        }
        notifyDataSetChanged()
    }

    fun selectAllItem(){
        pendingList.forEach {
            selectedAwb.add(it.awb_no)
        }
        notifyDataSetChanged()
    }
    inner class MarkCompleteViewHolder(val binding:TallyPendingItemBinding):RecyclerView.ViewHolder(binding.root){
        init {
            binding.itemCl.setOnLongClickListener {
                binding.selectCheckbox.visible()
                selectAll()
                selectedAwb.add(pendingList[adapterPosition].awb_no)
                if(selectedAwb.containsAll(pendingList.map { it.awb_no })){
                    toggleSelectAll(true)
                }
                notifyDataSetChanged()
                return@setOnLongClickListener true
            }

            binding.selectCheckbox.setOnClickListener {
                if(selectedAwb.contains(pendingList[adapterPosition].awb_no)){
                    selectedAwb.remove(pendingList[adapterPosition].awb_no)
                }else{
                    selectedAwb.add(pendingList[adapterPosition].awb_no)
                }
                if(selectedAwb.containsAll(pendingList.map { it.awb_no })){
                    toggleSelectAll(true)
                }else{
                    toggleSelectAll(false)
                }
                if(selectedAwb.isEmpty()){
                    hideSelectAll()
                }else{
                    notifyItemChanged(adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkCompleteViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = TallyPendingItemBinding.inflate(inflater,parent,false)
        return MarkCompleteViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return pendingList.size
    }

    override fun onBindViewHolder(holder: MarkCompleteViewHolder, position: Int) {
        holder.binding.awb.text = "AWB: ${pendingList[position].awb_no}"
        holder.binding.orderType.text = pendingList[position].product_type
        if(selectedAwb.isNotEmpty()){
            holder.binding.selectCheckbox.visible()
        }else{
            holder.binding.selectCheckbox.gone()
        }
        if (selectedAwb.contains(pendingList[position].awb_no)) {
            holder.binding.itemCl.setBackgroundColor(
                ContextCompat.getColor(
                    holder.binding.itemCl.context,
                    R.color.blue_7FE
                )
            )
            holder.binding.selectCheckbox.isChecked = true
        } else{
            holder.binding.itemCl.setBackgroundColor(
                ContextCompat.getColor(
                    holder.binding.itemCl.context,
                    R.color.white
                )
            )
            holder.binding.selectCheckbox.isChecked = false
        }
    }
}