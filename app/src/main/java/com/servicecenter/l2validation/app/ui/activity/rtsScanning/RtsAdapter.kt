package com.servicecenter.l2validation.app.ui.activity.rtsScanning

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.data.local.entities.PendingRtsData
import com.servicecenter.l2validation.databinding.RtsDetailItemBinding
import com.servicecenter.l2validation.utils.Constants
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RtsAdapter(
    val context: Context,
    private val listOfDetails: MutableList<PendingRtsData>,
) : RecyclerView.Adapter<RtsAdapter.RtsViewHolder>() {
    inner class RtsViewHolder(val binding: RtsDetailItemBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RtsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val rtsDetailItemBinding = RtsDetailItemBinding.inflate(inflater, parent ,false)
        return RtsViewHolder(rtsDetailItemBinding)
    }

    override fun getItemCount(): Int {
        return listOfDetails.size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RtsViewHolder, position: Int) {
        holder.binding.tvAwb.text = "AWB: " +listOfDetails[position].awb_number
        convertTimestamp(holder,listOfDetails[position].rto_lock_applied_timestamp)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertTimestamp(holder: RtsViewHolder, timestamp: String?) {
        val zonedDateTime = ZonedDateTime.parse(
            timestamp,
            DateTimeFormatter.ISO_DATE_TIME
        )// Convert to IST (Indian Standard Time)
        val istZoneId = ZoneId.of("Asia/Kolkata")
        val istDateTime = zonedDateTime.withZoneSameInstant(istZoneId)
        // Format the date-time in the desired pattern
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm a")
        val final = istDateTime.format(formatter)
        holder.binding.tvRtoLock.text = "RTO Lock: " + final.uppercase()
    }


    fun addItems(newItems: List<PendingRtsData>, filter: String) {
       if(filter.equals(Constants.FILTER_DATA)){
            listOfDetails.clear()
            listOfDetails.addAll(newItems)
            notifyDataSetChanged()
            return
        }else if(filter.equals(Constants.MANUAL_SEARCH)){
            listOfDetails.clear()
            listOfDetails.addAll(newItems)
            notifyDataSetChanged()
            return
        }
       else{
            val startPosition = listOfDetails.size
            listOfDetails.addAll(newItems)
            notifyItemRangeInserted(startPosition, newItems.size)
        }
    }
    fun clearItems() {
        listOfDetails.clear()  // Clear the list of items
        notifyDataSetChanged() // Notify the adapter that the data has changed
    }
}
