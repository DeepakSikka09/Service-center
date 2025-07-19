package com.servicecenter.l2validation.app.ui.activity.udCalling.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.navigateToActivity
import com.servicecenter.l2validation.app.extensions.showToast
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDCallingActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDShipmentActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDShipmentActivity.Companion.correlationId
import com.servicecenter.l2validation.app.ui.viewmodel.UDShipmentViewModel
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.local.entities.AwbDetails
import com.servicecenter.l2validation.data.local.entities.CallBridgeDetails
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.CallBottomsheetBinding
import com.servicecenter.l2validation.databinding.FragmentCallDisconnectedBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isCallActive
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CallDisconnectedFragment : BaseFragment<FragmentCallDisconnectedBinding>() {
    override fun getLayout(): Int=R.layout.fragment_call_disconnected
    private val viewModel: UDShipmentViewModel by activityViewModels()
    private var serverRequest = CommonRequest()
    private lateinit var shipmentDetail: AwbDetails
    private var clientCorrelationId: String = ""
    private var callStatusFlag: Int = 0
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var callBottomSheet: CallBottomsheetBinding

    private lateinit var backPress: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        setRemarkWatcher()
        fetchShipmentDetails()
        binding.btnSubmit.setOnClickListener {
            if (isInternetAvailable(requireContext())) {
                callCommitAPI()
            } else {
                showToast(getString(R.string.no_internet), false)
            }

        }
        binding.btnCall.setOnClickListener {
            if (isCallActive(requireContext())) {
                showToast(getString(R.string.your_call_is_already_active), false)
            } else {
                clientCorrelationId = ""
                val bridgeDetails = shipmentDetail.callbridge_details
                if (bridgeDetails != null && bridgeDetails.size > 1) {
                    openBottomSheet(bridgeDetails)
                } else {
                    val singlePrimaryNumber =
                        "${bridgeDetails?.get(0)?.callbridge_number},${bridgeDetails?.get(0)?.pin}#"
                    dialPhoneNumber(singlePrimaryNumber)
                }
            }

        }

        waitForStaus()
        fetchudCallingStatus()


    }

    private fun waitForStaus() {
        binding.ivCallDrop.visibility=View.VISIBLE
        Glide.with(this)
            .load(R.mipmap.loading)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder)
            .into(binding.ivCallDrop)
        binding.tvMessage.text = getString(R.string.please_wait_getting_previous_call_status)
        binding.tvMessage.setTextColor(ContextCompat.getColor(requireContext(),R.color.Yellow))

        binding.remarkHeadingTv.setTextColor(ContextCompat.getColor(requireContext(),R.color.disable_btn))
        binding.btnCall.text = getString(R.string.call_again)
        binding.btnCall.isEnabled = false
        binding.btnCall.setTextColor(ContextCompat.getColor(requireContext(),R.color.grey_7F))
        binding.btnCall.background = ContextCompat.getDrawable(
                requireContext(),R.drawable.disable_call)
        binding.etRemarks.isEnabled = false
    }


    private fun enableView(calldisconnectOwner: String) {
        binding.ivCallDrop.visibility=View.VISIBLE
        Glide.with(this)
            .load(R.drawable.calldrop)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder)
            .into(binding.ivCallDrop)

        binding.tvMessage.text = calldisconnectOwner
        binding.tvMessage.setTextColor(ContextCompat.getColor(requireContext(),R.color.black))

        binding.remarkHeadingTv.setTextColor(ContextCompat.getColor(requireContext(),R.color.black_1A))

        binding.btnCall.text = getString(R.string.call_again)
        binding.btnCall.isEnabled = true
        binding.btnCall.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue_FC))
        binding.btnCall.background = ContextCompat.getDrawable(
                requireContext(),R.drawable.clear_filter)

        binding.etRemarks.isEnabled = true
    }

    private fun openBottomSheet(shipmentDetails: List<CallBridgeDetails>) {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        callBottomSheet = CallBottomsheetBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(callBottomSheet.root)

        val primaryNumber = shipmentDetails[0].callbridge_number
        val primaryNumberPin = shipmentDetails[0].pin
        callBottomSheet.primaryContactNumber.text =
            String.format(getString(R.string._xxxxxx_s), primaryNumber.takeLast(4))

        val alternateNumber = shipmentDetails[1].callbridge_number
        val alternateNumberPin = shipmentDetails[1].pin
        callBottomSheet.alternateContactNumber.text =
            String.format(getString(R.string._xxxxxx_s), alternateNumber.takeLast(4))
        callBottomSheet.primaryCallCard.setOnClickListener {
            val primaryNumberWithPin = "${primaryNumber},${primaryNumberPin}#"
            dialPhoneNumber(primaryNumberWithPin)
            bottomSheetDialog.dismiss()
        }
        callBottomSheet.alternateCallCard.setOnClickListener {
            val alternateNumberWithPin = "${alternateNumber},${alternateNumberPin}#"
            dialPhoneNumber(alternateNumberWithPin)
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }


    private fun dialPhoneNumber(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(intent)
            binding.btnCall.text = getString(R.string.call_again)
            binding.btnCall.isEnabled = false
            binding.btnCall.setTextColor(ContextCompat.getColor(requireContext(),R.color.grey_7F))

            binding.btnCall.background = ContextCompat.getDrawable(
                requireContext(),R.drawable.disable_call)

        } else {
            // Handle permission request if not granted
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE
                ), 1
            )
        }
    }

    private fun fetchShipmentDetails() {

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.shipment.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        val shipmentDetails = result.data
                        shipmentDetail = shipmentDetails as AwbDetails
                        sendDetails(shipmentDetail)
                        progressDialog().dismiss()

                    }

                    is APIResultState.Failure -> {
                        progressDialog().dismiss()
                    }

                    else -> {
                        manageApiFlowStatus(
                            apiResultState = result,
                            true
                        )
                    }
                }
            }
        }
    }

    private fun sendDetails(shipmentDetails: AwbDetails) {
        binding.awbDetails.tvAwbNo.text = shipmentDetails.awb_no.toString()
        binding.awbDetails.tvOrderNo.text = shipmentDetails.order_id
        binding.awbDetails.orderType.text = shipmentDetails.payment_type
        if (isInternetAvailable(requireContext())) {


            viewModel.startJob(shipmentDetails.awb_no, shipmentDetails.drs_id)
        } else {
            showToast(getString(R.string.no_internet), false)
        }

    }



    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.cancelJob()
    }

    private fun callCommitAPI() {
        if (isInternetAvailable(requireActivity() as UDShipmentActivity)) {
            serverRequest = CommonRequest(
                airwaybill_number = shipmentDetail.awb_no.toString(),
                drs_id = shipmentDetail.drs_id.toString(),
                request_type = "remarks", //static pass as per backend
                source = "SCA",
                remarks = binding.etRemarks.text.toString(),
                client_correlation_id = clientCorrelationId

                // ud_type = "udType",

            )
            viewModel.sendUDDataToServer(commonRequest = serverRequest)
            fetchCommitAPIResponse()
        } else {
            showToast(
                getString(R.string.no_internet),
                false
            )
        }
    }

    private fun fetchCommitAPIResponse() {
        lifecycleScope.launch {
            viewModel.udServerResponse.collect { allShipmentList ->
                when (allShipmentList) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()

                        viewModel.preferenceDataStoreHelper.setData(
                            PreferenceDataStoreConstants.recentCorrelationId, ""
                        )
                        navigateToActivity(
                            UDCallingActivity::class.java
                        )
                    }


                    else -> {
                        manageApiFlowStatus(
                            allShipmentList,
                            true
                        )
                    }
                }
            }
        }
    }

    private fun setRemarkWatcher() {
        binding.etRemarks.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (s?.isNotEmpty() == true) {
                    binding.btnSubmit.isEnabled = true
                }
                if (binding.btnSubmit.isEnabled) {
                    binding.btnSubmit.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.custom_enable_button_10dp
                    )
                } else {
                    // binding.btnSubmit.setBackgroundColor(Color.GRAY)
                    binding.btnSubmit.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.custom_disable_button_10dp
                    )
                }
            }
        })
    }

    private fun fetchudCallingStatus() {
        lifecycleScope.launch {
            viewModel.udCallingStatus.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
//                            L2Application.progressDialogCallbacks.hideProgress()

                        val callStatusData = result.data as Response
                        Log.d("check_api", "ook")
                        clientCorrelationId = callStatusData.call_details.client_correlation_id
                         viewModel.preferenceDataStoreHelper.setData(
                            PreferenceDataStoreConstants.recentCorrelationId,
                            clientCorrelationId
                        )
                        callStatusFlag = callStatusData.call_details.call_status_flag
                        Log.d("previous_correlationId", correlationId)
                        Log.d("correlationId", clientCorrelationId)

                        if (callStatusData.is_marked_no_response) {
                            showToast(getString(R.string.max_reached), false, Toast.LENGTH_LONG)

                            navigateToActivity(
                                UDCallingActivity::class.java
                            )

                        } else if (correlationId == clientCorrelationId) {
                            Log.d("check_for_fragment", "1")

                            if (callStatusData.call_details.call_status_flag == 2) {
                                waitForStaus()

                            } else if (callStatusData.call_details.call_status_flag == 4) {
                                if (callStatusData.call_details.disconnected_by == null) {

                                    binding.ivCallDrop.visibility=View.GONE
                                    binding.tvMessage.text = getString(R.string.please_wait)
                                } else{
                                    callStatusData.call_details.disconnected_by?.let { enableView(it) }
                            }
                            }
                        } else {

                            if (callStatusData.call_details.disconnected_by == null) {

                                binding.ivCallDrop.visibility=View.GONE
                                binding.tvMessage.text = getString(R.string.please_wait)
                            }else {
                                binding.tvMessage.text = callStatusData.call_details.disconnected_by
                            }
//
                            Log.d("check_for_fragment", "2")

                            manageButton(callStatusData.call_details.call_status_flag)
                        }

//                        binding.responseData = shipmentDetails as AwbDetails?
//                        sendDetails(shipmentDetails)

                    }

                    is APIResultState.Failure -> {
                        //   L2Application.progressDialogCallbacks.hideProgress()
//                        showIncorrectAwbError(result.description)
                        Log.d("check_error", result.description)
                        //  manageButton(2)
                    }

                    else -> {
                        manageApiFlowStatus(apiResultState = result, true)
                    }
                }
            }

        }
    }

    private fun manageButton(callStatusFlag: Int) {


        //if call picked
        when (callStatusFlag) {
            2 -> {
                Log.d("check_for_fragment", "3")
                correlationId = clientCorrelationId
                parentFragmentManager.popBackStack()

              //  (activity as UDShipmentActivity).supportFragmentManager.popBackStack()


            }
            1 -> {
                //ringing
                binding.btnCall.text = getString(R.string.call_again)
                binding.btnCall.isEnabled = false
                binding.btnCall.setTextColor(ContextCompat.getColor(requireContext(),R.color.grey_7F))


                binding.btnCall.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.disable_call
                )
            }
            else -> {
                binding.btnCall.text = getString(R.string.call_again)
                binding.btnCall.isEnabled = true
                binding.btnCall.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue_FC))



                binding.btnCall.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.clear_filter
                )

            }
        }


    }
}