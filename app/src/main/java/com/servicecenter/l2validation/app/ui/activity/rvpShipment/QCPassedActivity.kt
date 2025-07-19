package com.servicecenter.l2validation.app.ui.activity.rvpShipment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.viewmodel.QcPassedViewModel
import com.servicecenter.l2validation.application.L2Application
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.databinding.ActivityQcPassedBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.FE_IMAGES
import com.servicecenter.l2validation.utils.Constants.FLYER_IMAGES
import com.servicecenter.l2validation.utils.Constants.PRODUCT_IMAGES
import com.servicecenter.l2validation.utils.Constants.scanned_AWB
import com.servicecenter.l2validation.utils.cameraX.CameraxActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QCPassedActivity : BaseActivity<ActivityQcPassedBinding, QcPassedViewModel>(), View.OnClickListener {

    private var flyerRltdAirwillNo: String = ""
    private var scanAwb: String = ""
    var status: String = ""
    var frontImageID: Long = 0L
    var backImageID: Long = 0L
    private var issuccessLinkFlyer = false
    private var l1QcValidationRequired = false

    override fun getLayout(): Int {
        return R.layout.activity_qc_passed
    }

    override fun getViewModels(): Class<QcPassedViewModel> {
        return QcPassedViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initialize()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()
            }
        })
    }

    private fun initialize() {
        binding.tvMessage.text = intent.getStringExtra(Constants.MESSAGE)

        binding.qcPassedBtn.setOnClickListener(this)
        binding.productDetails.ivBackArrow.visibility = View.GONE
        val bundle = intent.extras
        flyerRltdAirwillNo = bundle?.getString(Constants.flyerRelatedAirwillNo).toString()
        issuccessLinkFlyer = bundle?.getBoolean(Constants.SUCCESS_LINK_FLYER_ACTIVITY) ?: false
        l1QcValidationRequired = bundle?.getBoolean(Constants.l1_qc_validation_required) ?: false
        status = bundle?.getString(Constants.shiment_label_status).toString()
        scanAwb = bundle?.getString(scanned_AWB).toString()
        frontImageID = bundle?.getLong(Constants.FRONT_IMAGE_ID) ?: 0L
        backImageID = bundle?.getLong(Constants.BACK_IMAGE_ID) ?: 0L
        binding.tvAwbNo.setText(scanAwb)


        fetchResponse()

        if (status.equals("failure", true)) {
            analyticsToAllScreen(Constants.L2_BARCODE_LABEL_MISMATCH)
            binding.ivQcPassed.setImageDrawable(getDrawable(R.drawable.shipment_failed))
            binding.qcPassedBtn.text = getString(R.string.scan_next_shipment)
            binding.tvMessage.text = getString(R.string.failure_barcode_description)
            binding.constInfo.visibility = View.VISIBLE
        } else if(status.equals("success",true) && !issuccessLinkFlyer){
            analyticsToAllScreen(Constants.L2_BARCODE_LABEL_MATCH)
            binding.ivQcPassed.setBackgroundResource(R.drawable.qc_passed)
            binding.qcPassedBtn.text = getString(R.string.click_product_images)
            binding.tvMessage.text = getString(R.string.successfully_verified_desciption)
            binding.constInfo.visibility = View.GONE

        }else if(status.equals("success",true) && issuccessLinkFlyer){
            binding.ivQcPassed.setBackgroundResource(R.drawable.qc_passed)
            binding.qcPassedBtn.text = getString(R.string.next)
            binding.tvMessage.text = getString(R.string.successfully_verified_desciption)
            binding.constInfo.visibility = View.GONE
        }

    }

    private fun fetchResponse() {
        lifecycleScope.launch {
            viewModel.serverResponse.collect { allShipmentList ->
                when (allShipmentList) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        val intent = Intent(this@QCPassedActivity, LinkFlyerActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
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
                if (binding.qcPassedBtn.text.equals(getString(R.string.click_product_images))) {
                    analyticsToAllButton(Constants.SCAN_NEXT_SHIPMENT_MISMATCH,Constants.BUTTON_KEY,Constants.SCAN_NEXT_SHIPMENT_MISMATCH)

                    val extras = Bundle().apply {
                        putString(
                            Constants.flyerRelatedAirwillNo,
                            flyerRltdAirwillNo
                        )
                    }
                    startScreen(CameraxActivity(), extras)
                    //Need to change the button name
                }else if(binding.qcPassedBtn.text.equals(getString(R.string.next))) {
                    if (isInternetAvailable(this)) {
                        viewModel.callImageListApi(flyerRltdAirwillNo)
                        fetchImageListApiStateAndNavigate()
                    } else {
                        showToast(getString(R.string.no_internet), false)
                    }
                }
                else {
                    val serverRequest = CommonRequest(
                        airwaybill_number = flyerRltdAirwillNo,
                        status = "failure"
                    )
                    if (isInternetAvailable(this)) {
                        viewModel.sendDataToServer(serverRequest, flyerRltdAirwillNo)
                    } else {
                        showToast(getString(R.string.no_internet), false)
                    }
                }

            }
        }
    }

    private fun fetchImageListApiStateAndNavigate() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.ImageListApiflow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        val list = it.data as Map<String, List<String>>
                        val feImages = list[FE_IMAGES] ?: emptyList()
                        val productImages = list[PRODUCT_IMAGES] ?: emptyList()
                        val flyerImage = list[FLYER_IMAGES] ?: emptyList()

                        // Condition for navigating to QCImageActivity
                        if(issuccessLinkFlyer && !l1QcValidationRequired) {
                            // Navigate to QCImageActivity
                            val extras = Bundle().apply {
                                putString(Constants.flyerRelatedAirwillNo, flyerRltdAirwillNo)
                                putLong(Constants.FRONT_IMAGE_ID, frontImageID)
                                putLong(Constants.BACK_IMAGE_ID, backImageID)
                                putStringArrayList(FE_IMAGES, ArrayList(feImages))
                                putStringArrayList(PRODUCT_IMAGES, ArrayList(productImages))
                                putStringArrayList(FLYER_IMAGES, ArrayList(flyerImage))
                            }
                            startScreen(QcImageActivity(), extras)

                        }else if(issuccessLinkFlyer && l1QcValidationRequired){
                            val extras = Bundle().apply {
                                putString(Constants.flyerRelatedAirwillNo, flyerRltdAirwillNo)
                                putLong(Constants.FRONT_IMAGE_ID, frontImageID)
                                putLong(Constants.BACK_IMAGE_ID, backImageID)}
                            startScreen(ImageQcActivity(),extras)
                        }
                        else {
                            // Condition for navigating to CommitActivity
                            val extras = Bundle().apply {
                                putString(Constants.flyerRelatedAirwillNo, flyerRltdAirwillNo)
                                putLong(Constants.FRONT_IMAGE_ID, frontImageID)
                                putLong(Constants.BACK_IMAGE_ID, backImageID)
                            }
                            startScreen(CommitActivity(), extras)
                        }
                    }
                    else -> {
                        manageApiFlowStatus(it, false)
                    }
                }
            }
        }
    }


    fun backPress() {
        showToast(getString(R.string.you_can_t_go_back), false)
    }
}