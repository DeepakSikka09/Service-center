package com.servicecenter.l2validation.app.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.data.local.entities.UDShipment
import com.servicecenter.l2validation.databinding.ListItemBinding
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.VALIDATED

class UDListAdapter(
    private var originalItems: List<UDShipment>,
    private val udStatus:String,
    private val onItemClick: (UDShipment) -> Unit

) : RecyclerView.Adapter<UDListAdapter.ViewHolder>() {

   // private var filteredItems: List<UDShipment> = originalItems

    inner class ViewHolder(private val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: UDShipment) {
            binding.tvAwbNo.text = item.awb_no.toString()
            if(udStatus==VALIDATED){
                binding.tvNameHeading.text= "UD :"
                binding.tvFeName.text = listOfNotNull(item.ud_type, item.action_type)
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(" | ") ?: ""
            }else {
            binding.tvFeName.text = item.fe_name
            }
            val timePart = item.ud_time_stamp?.substringAfter(' ') ?: ""
            binding.tvDate.text = timePart
            binding.shipmentType.text = listOfNotNull(item.product_type, item.payment_type)
                .takeIf { it.isNotEmpty() }
                ?.joinToString("/") ?: ""
            binding.itemConst.setOnClickListener {
                onItemClick(item)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(originalItems[position])
    }

    override fun getItemCount() = originalItems.size
    fun updateList(updatedList: List<UDShipment>) {
        originalItems = updatedList
        notifyDataSetChanged()
    }


}
