package com.servicecenter.l2validation.app.ui.activity.udCalling.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.activity.rvpShipment.CommitActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDOTPValidationActivity
import com.servicecenter.l2validation.app.ui.adapters.DateAdapter
import com.servicecenter.l2validation.app.ui.viewmodel.UDShipmentViewModel
import com.servicecenter.l2validation.data.local.entities.DateModel
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.FragmentMarkUdvalidationBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class MarkUDValidationFragment : BaseFragment<FragmentMarkUdvalidationBinding>(),
    View.OnClickListener {
    override fun getLayout(): Int = R.layout.fragment_mark_udvalidation
    private val viewModel: UDShipmentViewModel by activityViewModels()
    private lateinit var dateAdapter: DateAdapter
    private val dateList = mutableListOf<DateModel>()
    private var selectedDate: String? = null
    private var orderID: String? = null
    private var awbNumber: String? = null
    private var drsId: String? = null
    private var paymentType: String? = null
    private var isUdCalling: Boolean = true
    private var actionableDays: Int? = 0
    private var feNumber: String? = ""
    private var udType: String? = null
    private var clientCorrelationId: String? = null
    lateinit var context: Activity
    private var cutOffTime: Long = 0

    private lateinit var backPress: OnBackPressedCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Prevent back press, you can show a message or toast here
                    // Log.d("Fragment", "Back press is disabled here!")


                }
            })*/
        backPress = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                showToast(getString(R.string.you_can_t_go_back), false)
            }


        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().onBackPressedDispatcher.addCallback(this.viewLifecycleOwner, backPress)

        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isInternetAvailable(requireContext())) {
            viewModel.callCutOffTimeApi()
        } else {
            showToast(
                getString(R.string.no_internet), false
            )
        }
        fetchCutOffDetails()
        context = requireActivity()
        orderID = arguments?.getString(Constants.ORDER_ID)
        awbNumber = arguments?.getString(Constants.AWB_NUMBER)
        paymentType = arguments?.getString(Constants.PAYMENT_TYPE)
        drsId = arguments?.getString(Constants.DRS_ID)
        actionableDays = arguments?.getInt(Constants.ACTIONALBLE_DAYS, 0)
        feNumber = arguments?.getString(Constants.FE_NUMBER)
        clientCorrelationId = arguments?.getString(Constants.CLIENT_CORRELATION_ID)


        binding.dateRecyclerview.layoutManager = GridLayoutManager(requireContext(), 3)

        dateAdapter = DateAdapter(dateList, this)
        binding.dateRecyclerview.adapter = dateAdapter
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        initialize()

    }

    private fun initialize() {
        binding.awbDetails.tvAwbNo.text = awbNumber
        binding.awbDetails.tvOrderNo.text = orderID
        binding.awbDetails.orderType.text = paymentType
        binding.view2.visibility = View.GONE
        binding.consigneeRequestConstraint.visibility = View.GONE
        binding.view3.visibility = View.GONE
        binding.consigneeScheduleConstraint.visibility = View.GONE
        binding.view4.visibility = View.GONE
        binding.remarkConstraint.visibility = View.GONE

        binding.correctRadio.setOnClickListener(this)
        binding.FakeRadio.setOnClickListener(this)
        binding.reAttemptRadio.setOnClickListener(this)
        binding.CancelRadio.setOnClickListener(this)
        binding.btnSubmit.setOnClickListener(this)

    }

    private fun fetchCutOffDetails() {
        lifecycleScope.launch {
            viewModel.CutOffTimeApiflow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                       progressDialog().dismiss()
                        val shipmentList = result.data as Response
                        cutOffTime = shipmentList.cutoff_time
                    }

                    else -> {
                        manageApiFlowStatus(
                            apiResultState = result,
                            false
                        )
                    }
                }
            }
        }
    }

    private fun markedUdValidation() {
        if (binding.correctRadio.isChecked) {
            binding.correctRadio.setBackgroundResource(R.drawable.curved_rectangle_white_four_side)
            binding.FakeRadio.setBackgroundResource(0)
            binding.view2.visibility = View.VISIBLE
            udType = getString(R.string.correct)
            binding.consigneeRequestConstraint.visibility = View.VISIBLE
        } else if (binding.FakeRadio.isChecked) {
            binding.FakeRadio.setBackgroundResource(R.drawable.curved_rectangle_white_four_side)
            binding.correctRadio.setBackgroundResource(0)
            binding.view2.visibility = View.VISIBLE
            udType = getString(R.string.fake)
            binding.consigneeRequestConstraint.visibility = View.VISIBLE
        }
    }

    private fun consigneeRequestedTo() {
        if (binding.reAttemptRadio.isChecked) {
            binding.reAttemptRadio.setBackgroundResource(R.drawable.curved_rectangle_white_four_side)
            binding.CancelRadio.setBackgroundResource(0)
            binding.view3.visibility = View.VISIBLE
            binding.consigneeScheduleConstraint.visibility = View.VISIBLE
            if (selectedDate == null) {
                consigneeRescheduleShipment(false)
            } else consigneeRescheduleShipment(true)
        } else if (binding.CancelRadio.isChecked) {
            binding.CancelRadio.setBackgroundResource(R.drawable.curved_rectangle_white_four_side)
            binding.reAttemptRadio.setBackgroundResource(0)
            binding.view3.visibility = View.GONE
            binding.consigneeScheduleConstraint.visibility = View.GONE
            binding.view4.visibility = View.GONE
            binding.remarkConstraint.visibility = View.GONE
            setSubmitButtonState(true)

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun populateDates() {
        dateList.clear()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val daysCount = actionableDays ?: 0
        val currentTimeStamp = System.currentTimeMillis() / 1000
        for (i in 0 until daysCount) {
            val date = dateFormat.format(calendar.time)
            val day = when (i) {
                0 -> getString(R.string.today)
                1 -> getString(R.string.tomorrow)
                else -> dayFormat.format(calendar.time)
            }
            dateList.add(DateModel(date, day))
            if (i == 0 && currentTimeStamp > cutOffTime) {
                // Remove "Today"
                dateList.removeAt(i)
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        dateAdapter.notifyDataSetChanged()
    }


    private fun consigneeRescheduleShipment(enableSubmit: Boolean) {
        populateDates()
        binding.view4.visibility = View.VISIBLE
        binding.remarkConstraint.visibility = View.VISIBLE
        setSubmitButtonState(enableSubmit)

    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.correct_radio, R.id.Fake_radio -> {
                markedUdValidation()
            }

            R.id.re_attempt_radio, R.id.Cancel_radio -> {
                consigneeRequestedTo()
            }

            R.id.btn_submit -> {

                handleSubmission()
            }
        }
    }

    fun selectDate(dateModel: DateModel) {
        selectedDate = dateModel.date
        setSubmitButtonState(true)
    }

    private fun handleSubmission() {
        viewModel.cancelBroadCast(true)
      /*  if ((requireActivity() as UDShipmentActivity).isBroadCastRegistered) {
            (requireActivity() as UDShipmentActivity).isBroadCastRegistered = false
            (requireActivity() as UDShipmentActivity).unregisterReceiver((requireActivity() as UDShipmentActivity).callReceiver)

        }*/

        val extras = Bundle().apply {
            putString(Constants.AWB_NUMBER, awbNumber.toString())
            putString(Constants.DRS_ID, drsId.toString())
            putString(Constants.CLIENT_CORRELATION_ID, clientCorrelationId)
            putString(Constants.UD_TYPE, udType)
            putString(Constants.PAYMENT_TYPE, paymentType)
            putString(Constants.RESCHEDULE_REMARKS, binding.etRemarks.text.toString())
        }

        if (binding.CancelRadio.isChecked) {
            extras.apply {
                putString(Constants.ORDER_ID, orderID)
                putString(Constants.PAYMENT_TYPE, paymentType)
                putString(Constants.FE_NUMBER, feNumber)
                putString(Constants.EVENT, getString(R.string.cancellation))
                putString(Constants.REQUEST_TYPE, getString(R.string.cancel))
            }
            startScreen(UDOTPValidationActivity(), extras)
            return
        }

        selectedDate?.let { selectedDateString ->
            val formattedSelectedDate = formatDateWithYear(selectedDateString)
            val today = Calendar.getInstance()
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }

            extras.apply {
                putString(Constants.SELECTED_DATE, formattedSelectedDate)
                putString(Constants.REQUEST_TYPE, getString(R.string.reattempt_))
                putString(Constants.EVENT, getString(R.string.reschedule))
            }

            when (formattedSelectedDate) {
                today.toFormattedString(), tomorrow.toFormattedString() -> {
                    extras.putBoolean(Constants.Is_UdCalling, isUdCalling)
                    startScreen(CommitActivity(), extras)
                }

                else -> {
                    extras.apply {
                        putString(Constants.ORDER_ID, orderID)
                        putString(Constants.FE_NUMBER, feNumber)
                    }
                    startScreen(UDOTPValidationActivity(), extras)
                }
            }
        }
    }

    private fun formatDateWithYear(dateString: String): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val dateWithYear = "$dateString $currentYear"
        val inputDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = inputDateFormat.parse(dateWithYear)
        return parsedDate?.let { outputDateFormat.format(it) } ?: ""
    }

    private fun Calendar.toFormattedString(): String {
        val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return outputDateFormat.format(this.time)
    }

    private fun setSubmitButtonState(enable: Boolean) {
        val color = if (enable) R.color.blue else R.color.disable_btn
        val stateColor = ContextCompat.getColor(requireActivity(), color)
        binding.btnSubmit.backgroundTintList = ColorStateList.valueOf(stateColor)
        binding.btnSubmit.isEnabled = enable
    }
}