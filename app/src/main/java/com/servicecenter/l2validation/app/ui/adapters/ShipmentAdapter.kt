package com.servicecenter.l2validation.app.ui.adapters
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail
import com.servicecenter.l2validation.databinding.ShipmentDetailsBinding

class ShipmentAdapter(
    val context: Context,
    private val listOfDetails: ArrayList<ShipmentDetail>,
) : RecyclerView.Adapter<ShipmentAdapter.ShipmentViewHolder>() {
    inner class ShipmentViewHolder(val binding: ShipmentDetailsBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShipmentViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val shipmentDetailBinding = ShipmentDetailsBinding.inflate(inflater, parent ,false)
        return ShipmentViewHolder(shipmentDetailBinding)
    }

    override fun getItemCount(): Int {
       return listOfDetails.size
    }

    override fun onBindViewHolder(holder: ShipmentViewHolder, position: Int) {
        holder.binding.tvAwb.text = "AWB: " +listOfDetails[position].AWB_No.toString()
        holder.binding.tvNameHeading.text= listOfDetails[position].name
        holder.binding.tvEmpId.text="- "+ listOfDetails[position].employee_code
        holder.binding.tvDate.text= listOfDetails[position].date
    }
}