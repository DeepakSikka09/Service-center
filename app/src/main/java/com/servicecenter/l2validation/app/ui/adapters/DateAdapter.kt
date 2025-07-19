package com.servicecenter.l2validation.app.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.ui.activity.udCalling.fragment.MarkUDValidationFragment
import com.servicecenter.l2validation.data.local.entities.DateModel
import com.servicecenter.l2validation.databinding.ListDateBinding

internal class DateAdapter(
    private val dateList: List<DateModel>,
    private val itemClickListener: MarkUDValidationFragment
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedItemPosition: Int = RecyclerView.NO_POSITION

    inner class DateViewHolder(val binding: ListDateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dateModel: DateModel) {
            binding.todayDateTv.text = dateModel.date
            binding.todayDayTv.text = dateModel.day


            if (adapterPosition == selectedItemPosition) {
                val color = ContextCompat.getColor(itemView.context, R.color.box_EF)
                ViewCompat.setBackgroundTintList(binding.todayDateConstraint, ColorStateList.valueOf(color))
            } else {
                // Set the default background
                ViewCompat.setBackgroundTintList(binding.todayDateConstraint, null)
                binding.todayDateConstraint.setBackgroundResource(R.drawable.curved_rectangle_date)
            }
            itemView.setOnClickListener {
                val previousItemPosition = selectedItemPosition
                selectedItemPosition = adapterPosition

                notifyItemChanged(previousItemPosition)
                notifyItemChanged(selectedItemPosition)

                itemClickListener.selectDate(dateModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ListDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(dateList[position])
    }

    override fun getItemCount(): Int = dateList.size
}
