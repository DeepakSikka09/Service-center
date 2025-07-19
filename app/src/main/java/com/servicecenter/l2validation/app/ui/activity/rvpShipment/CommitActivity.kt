package com.servicecenter.l2validation.app.ui.activity.rvpShipment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.isDateTomorrow
import com.servicecenter.l2validation.app.ui.activity.rtsScanning.RtsScanActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDCallingActivity
import com.servicecenter.l2validation.app.ui.viewmodel.CommitViewModel
import com.servicecenter.l2validation.application.L2Application
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.ActivityCommitBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.FE_IMAGE_CLEAR
import com.servicecenter.l2validation.utils.Constants.FE_IMAGE_MATCHED
import com.servicecenter.l2validation.utils.Constants.FLYER_PROPERLY_SEALED
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CommitActivity : BaseActivity<ActivityCommitBinding, CommitViewModel>(), View.OnClickListener {

    var answersList = ArrayList<Map<String, Any>>()
    private var flyerRltdAirwillNo: String = ""
    private var frontImageId: Long = 0L
    private var backImageId: Long = 0L
    private var feImageMatch: String = ""
    private var feImageClear: String = ""
    private var flyerProperlySealed: String = ""
    private var shipmentLabelImageID: Long = 0L
    private var imageMatch: String = ""
    private var isRts: Boolean = false

    private var frontRtsKey: String = ""
    private var backRtsKey: String = ""
    private var isButtonClicked = false
    private var isRtsFail = false
    private var isRtsDone = false

    private var awbNumber: String? = null
    private var isUdCalling=false
    private var drsId: String? = null
    private var remark: String? = ""
    private var udType: String? = null
    private var requestType: String? = null
    private var selectedDate: String = ""
    private  var clientCorrelationId:String?=null
    private  var rescheduleRemarks:String?=null
    private var serverRequest = CommonRequest()

    override fun getLayout(): Int {
        return R.layout.activity_commit
    }

    override fun getViewModels(): Class<CommitViewModel> {
        return CommitViewModel::class.java
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = intent.extras
        flyerRltdAirwillNo = bundle?.getString(Constants.flyerRelatedAirwillNo).toString()
        frontImageId = bundle?.getLong(Constants.FRONT_IMAGE_ID) ?: 0L
        backImageId = bundle?.getLong(Constants.BACK_IMAGE_ID) ?: 0L

        feImageMatch = bundle?.getString(FE_IMAGE_MATCHED).toString()
        feImageClear = bundle?.getString(FE_IMAGE_CLEAR).toString()
        flyerProperlySealed = bundle?.getString(FLYER_PROPERLY_SEALED).toString()
        answersList = intent.getSerializableExtra("answers") as? ArrayList<Map<String, Any>> ?: ArrayList()

        shipmentLabelImageID = bundle?.getLong(Constants.SHIPMENT_LABEL_IMAGE_ID) ?: 0L
        imageMatch = bundle?.getString(Constants.IMAGE_MATCH).toString()
        isRts = bundle?.getBoolean(Constants.Is_RTS) ?: false
        frontRtsKey = bundle?.getString(Constants.FRONT_RTS_KEY).toString()
        backRtsKey = bundle?.getString(Constants.BACK_RTS_KEY).toString()

        awbNumber = intent.getStringExtra(Constants.AWB_NUMBER)
        isUdCalling = bundle?.getBoolean(Constants.Is_UdCalling) ?: false
        drsId = intent.getStringExtra(Constants.DRS_ID)
        remark = intent.getStringExtra(Constants.REMARK)
        udType = intent.getStringExtra(Constants.UD_TYPE)
        requestType = intent.getStringExtra(Constants.REQUEST_TYPE)
        selectedDate = intent.getStringExtra(Constants.SELECTED_DATE).toString()
        clientCorrelationId = intent.getStringExtra(Constants.CLIENT_CORRELATION_ID)
        rescheduleRemarks = intent.getStringExtra(Constants.RESCHEDULE_REMARKS)

        intitlize()
        analyticsToAllScreen(Constants.L2_BARCODE_FINAL_SUCCESS)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()
            }
        })

    }

    private fun intitlize() {
        binding.qcPassedBtn.setOnClickListener(this)
        binding.productDetails.ivBackArrow.visibility = View.GONE
        val bundle = intent.extras
        flyerRltdAirwillNo = bundle?.getString(Constants.flyerRelatedAirwillNo).toString()
        binding.tvAwbNo.text = flyerRltdAirwillNo
        if (isRts) {
            binding.tvTrackStatus.visibility = View.VISIBLE
            binding.tvMessage.text = ""
            callRtsCreationApi()
            fetchRtsResponse()
        }else if(isUdCalling){
            binding.productDetails.root.visibility=View.GONE
            binding.tvMessage.text = ""
            binding.ivQcPassed.visibility=View.GONE
            binding.tvTrackStatus.visibility = View.GONE
            //binding.tvMessage.text = resources.getString(R.string.l2_validation_success)
            binding.qcPassedBtn.text=resources.getString(R.string.go_to_pending_list)
            binding.awbBar.visibility=View.GONE
            binding.view1.visibility=View.GONE
            callUdCallingApi()

        }
        else {
            binding.tvTrackStatus.visibility = View.GONE
            binding.tvMessage.text = resources.getString(R.string.l2_validation_success)
        }
        fetchResponse()
    }

    private fun fetchResponse() {
        lifecycleScope.launch {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.serverResponse.collect { allShipmentList ->
                when (allShipmentList) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        navigateToActivity(LinkFlyerActivity::class.java)
                    }

                    else -> {
                        manageApiFlowStatus(allShipmentList, true)
                    }
                }
            }
        }
    }

    private fun fetchRtsResponse() {
        lifecycleScope.launch {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.rtsPacketResponse.collect { allShipmentList ->
                when (allShipmentList) {
                    is APIResultState.Success -> {
                        binding.tvTrackStatus.visibility = View.VISIBLE
                        binding.ivQcPassed.visibility = View.VISIBLE
                        allShipmentList.data as Response
                        binding.tvMessage.text =
                            "${allShipmentList.data.description}\n Proceed for Bagging "
                        binding.tvMessage.setTextColor(Color.BLACK)

                        progressDialog().dismiss()
                        isRtsDone = true
                        isRtsFail = false

                    }

                    is APIResultState.Loading -> {
                        binding.tvTrackStatus.visibility = View.GONE
                        binding.ivQcPassed.visibility = View.GONE
                        // binding.tvMessage.text = "RTS status is Pending"
                        progressDialog().show()

                    }

                    is APIResultState.Failure -> {
                        // handling for error case
                        binding.tvTrackStatus.visibility = View.GONE
                        binding.ivQcPassed.visibility = View.GONE
                        binding.ivCross.visibility = View.VISIBLE
                        binding.qcPassedBtn.text = getString(R.string.ok)
                        L2Application.progressDialogCallbacks.hideProgress()
                        binding.tvMessage.text = allShipmentList.description
                        binding.tvMessage.setTextColor(Color.RED)
                    }

                    else -> {
                        manageApiFlowStatus(allShipmentList, true)
                    }
                }
            }
        }
    }


    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.qc_passed_btn -> {
                isButtonClicked = true
                analyticsToAllButton(
                    Constants.SCAN_NEXT_SHIPMENT_L2_SUCCESS,
                    Constants.BUTTON_KEY,
                    Constants.SCAN_NEXT_SHIPMENT_L2_SUCCESS
                )
                if (CommonUtils.isInternetAvailable(this)) {
                    val l2ValidationAdditionalInfo = mapOf(
                        FE_IMAGE_MATCHED to feImageMatch,
                        FE_IMAGE_CLEAR to feImageClear,
                        FLYER_PROPERLY_SEALED to flyerProperlySealed
                    )
                    if (isRts) {
                        /*if (isRtsDone) {
                            scanNextRtsShipment()
                        } else {
                            callRtsCreationApi()
                        }*/
                        scanNextRtsShipment()


                    }else if(isUdCalling){
                        scanNextUdCalling()
                    }else {
                        serverRequest = CommonRequest(
                            airwaybill_number = flyerRltdAirwillNo,
                            status = "success",
                            image_keys = arrayListOf(
                                frontImageId.toString(),
                                backImageId.toString()
                            ),
                            l2_validation_additional_info = l2ValidationAdditionalInfo,
                            qc_answer = answersList
                        )
                        viewModel.sendDataToServer(serverRequest, flyerRltdAirwillNo)

                    }

                } else {
                    showToast(getString(R.string.no_internet), false)
                }
            }
        }
    }

    private fun scanNextUdCalling() {
        navigateToActivity(UDCallingActivity::class.java)
    }


    private fun scanNextRtsShipment() {
        val intent = Intent(this@CommitActivity, RtsScanActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun callRtsCreationApi() {
        val imageKeyFront = mapOf("key" to frontRtsKey, "id" to frontImageId.toString())
        val imageKeyBack = mapOf("key" to backRtsKey, "id" to backImageId.toString())

        serverRequest = CommonRequest(
            ref_awb_no = flyerRltdAirwillNo,
            image_list = arrayListOf(imageKeyFront, imageKeyBack)
        )
        viewModel.sendRtsDataToServer(commonRequest = serverRequest)
    }

    private fun callUdCallingApi() {
        if (CommonUtils.isInternetAvailable(this)) {
        serverRequest = CommonRequest(
            airwaybill_number = awbNumber,
            drs_id = drsId,
            request_type = requestType,
            source = "SCA",
            remarks = remark,//otp page remark
            ud_type = udType,
            scheduled_delivery_date = selectedDate,
            reschedule_remarks = rescheduleRemarks
        )
        viewModel.sendUDDataToServer(commonRequest = serverRequest)
        fetchUDCallingResponse()
        } else {
            showToast(getString(R.string.no_internet), false)
        }
    }

    private fun fetchUDCallingResponse() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.udServerResponse.collect { allShipmentList ->
                when (allShipmentList) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        allShipmentList.data as Response
                        viewModel.preferenceDataStoreHelper.setData(
                            PreferenceDataStoreConstants.recentCorrelationId, ""
                        )
                        if(selectedDate.isDateTomorrow()){
                            binding.ivQcPassed.visibility = View.VISIBLE
                            binding.ivQcPassed.setImageResource(R.drawable.reattempt_alert)
                        }else{
                            binding.ivQcPassed.visibility = View.VISIBLE
                        }
                        binding.tvMessage.text =
                            allShipmentList.data.description
                    }

                    is APIResultState.Failure -> {
                        // handling for error case
                        progressDialog().dismiss()
                        binding.tvTrackStatus.visibility = View.GONE
                        binding.ivQcPassed.visibility = View.GONE
                        binding.ivCross.visibility = View.VISIBLE
                        binding.tvMessage.text = allShipmentList.description
                        binding.tvMessage.setTextColor(Color.RED)
                    }
                    else -> {
                        manageApiFlowStatus(allShipmentList, true)
                    }
                }
            }
        }
    }



    fun backPress() {
        showToast(getString(R.string.you_can_t_go_back), false)
    }
}