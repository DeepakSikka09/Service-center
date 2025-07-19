package com.servicecenter.l2validation.app.ui.activity.rtsScanning

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.ui.viewmodel.RtsPendingListViewModel
import com.servicecenter.l2validation.databinding.RtsFilterBottomsheetBinding
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class RtsFilterBottomSheet(
    val viewModel: RtsPendingListViewModel,
    private val rtsPendingListActivity: RtsPendingListActivity
) : BottomSheetDialogFragment(),
    View.OnClickListener {
    private lateinit var binding: RtsFilterBottomsheetBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private var context: Context? = null
    private var fromDate: String = ""
    private var toDate: String = ""
    private var sorting: String = ""
    private var customDates: Boolean = false
    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = RtsFilterBottomsheetBinding.inflate(inflater, container, false)

        inIt()
        rtsPendingListActivity.analyticsToAllScreen(Constants.FILTER_RTS_EVENT)

        return binding.root
    }

    private fun inIt() {
        binding.sort.setTextColor(ContextCompat.getColor(requireContext(),R.color.light_blue_3))
        binding.close.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
        binding.btnApply.setOnClickListener(this)
        binding.customDate.setOnClickListener(this)
        binding.sort.setOnClickListener(this)
        binding.dateRange.setOnClickListener(this)
        binding.toDate.setOnClickListener(this)
        binding.fromDate.setOnClickListener(this)
        binding.etToDate.setOnClickListener(this)
        binding.etFromDate.setOnClickListener(this)
        binding.last7Dates.setOnClickListener(this)
        binding.last15Days.setOnClickListener(this)
        binding.newToOld.setOnClickListener(this)
        binding.oldToNew.setOnClickListener(this)
        binding.today.setOnClickListener(this)
        context = rtsPendingListActivity
        if (fromDate.isNotEmpty()){
            binding.etToDate.isEnabled = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.close -> dismiss()

            R.id.btnApply -> {
                if (customDates) {
                    if (toDate.isNotEmpty()) {
                        viewModel.setDate(toDate, fromDate, sorting)
                        rtsPendingListActivity.analyticsToAllButton(
                            Constants.FILTER,
                            Constants.BUTTON_KEY,
                            Constants.FILTER
                        )
                        dismiss()
                    } else {
                        rtsPendingListActivity.showToast(getString(R.string.please_select_from_and_to_date), false)
                    }
                } else {
                    viewModel.setDate(toDate, fromDate, sorting)
                    rtsPendingListActivity.analyticsToAllButton(
                        Constants.FILTER,
                        Constants.BUTTON_KEY,
                        Constants.FILTER
                    )
                    dismiss()
                }
            }

            R.id.btnCancel -> {
                binding.groupSortBy.clearCheck()
                binding.groupDateRange.clearCheck()
                toDate=""
                fromDate=""
                sorting=""
                viewModel.setDate("", "", "")
                customDates=false
                clearCustomDates()
                dismiss()

            }

            R.id.sort -> {
                binding.sort.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue_18))
                binding.dateRange.setTextColor(ContextCompat.getColor(requireContext(),R.color.black_1A))
                binding.groupSortBy.visibility = View.VISIBLE
                binding.groupDateRange.visibility = View.GONE
                binding.constraintCustomDate.visibility = View.GONE
                binding.viewSort.visibility = View.VISIBLE
                binding.viewDateRange.visibility = View.GONE
            }

            R.id.dateRange -> {
                binding.dateRange.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue_18))
                binding.sort.setTextColor(ContextCompat.getColor(requireContext(),R.color.black_1A))
                binding.groupSortBy.visibility = View.GONE
                binding.groupDateRange.visibility = View.VISIBLE
                binding.viewSort.visibility = View.GONE
                binding.viewDateRange.visibility = View.VISIBLE
                if(customDates){
                    binding.constraintCustomDate.visibility = View.VISIBLE
                }
            }

            R.id.custom_date -> {
                customDates=true
                toDate=""
                binding.constraintCustomDate.visibility = View.VISIBLE
            }
            R.id.etFromDate -> {
                showDatePicker(true)
            }

            R.id.etToDate -> {
                showDatePicker(false)
            }

            R.id.last_7_dates -> {
                customDates=false
                calculateLastDays(7)
                binding.constraintCustomDate.visibility = View.GONE
                clearCustomDates()
            }

            R.id.last_15_days -> {
                customDates=false
                calculateLastDays(days = 15)
                binding.constraintCustomDate.visibility = View.GONE
                clearCustomDates()

            }

            R.id.new_to_old -> {
                customDates=false
                sorting = "DESC"
            }

            R.id.old_to_new -> {
                customDates=false
                sorting = "ASC"
            }

            R.id.today -> {
                customDates=false
                calculateLastDays(days = 0)
                binding.constraintCustomDate.visibility = View.GONE
                clearCustomDates()
            }
        }
    }
    private fun clearCustomDates() {
        binding.etFromDate.setText("")
        binding.etToDate.setText("")
    }


    private fun showDatePicker(isFromDate: Boolean) {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                // Get the selected date in milliseconds
                val selectedDate = calendar.timeInMillis
                // Format the selected date
                val formattedDate = dateFormat.format(selectedDate)

                if (isFromDate) {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    fromDate = calendar.timeInMillis.toString()
                    binding.etFromDate.text = formattedDate.toEditable()
                    binding.etToDate.text = "".toEditable()
                    binding.etToDate.isEnabled = true
                } else {
                    // For To Date, set the time to midnight of the next day (00:00:00 of the next day)
                    calendar.add(Calendar.DAY_OF_YEAR, 1) // Move to the next day
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    // Update toDate with the midnight time of the next day
                    toDate = calendar.timeInMillis.toString()
                    binding.etToDate.text = formattedDate.toEditable()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        if (isFromDate) {
            datePicker.datePicker.maxDate = System.currentTimeMillis()
        }else if (!isFromDate && fromDate.isNotEmpty()) {
            val parsedFromDate = dateFormat.parse(binding.etFromDate.text.toString())
            if (parsedFromDate != null) {
                datePicker.datePicker.minDate = parsedFromDate.time //+ 24 * 60 * 60 * 1000 // Next day
            }
        }
        datePicker.show()
    }

    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    private fun calculateLastDays(days: Int) {
        // Current date (toDate)
        val calendar = Calendar.getInstance()
        // Get the current timestamp (toDate)
        toDate = (calendar.timeInMillis).toString()

        // Set the time to 12:00 AM (start of the current day)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        // Subtract 'days' days from today 12:00 AM (fromDate)
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        fromDate = (calendar.timeInMillis).toString()
    }

}

