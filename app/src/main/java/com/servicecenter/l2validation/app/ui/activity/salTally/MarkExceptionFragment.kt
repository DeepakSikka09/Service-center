package com.servicecenter.l2validation.app.ui.activity.salTally

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.gone
import com.servicecenter.l2validation.app.extensions.visible
import com.servicecenter.l2validation.app.ui.viewmodel.MarkExceptionViewModel
import com.servicecenter.l2validation.app.ui.viewmodel.TallyViewModel
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.FragmentMarkExceptionBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.BundleConstants.AWB_NO
import com.servicecenter.l2validation.utils.Constants.BundleConstants.ORDER_TYPE
import com.servicecenter.l2validation.utils.Constants.TALLY_COMPLETED
import com.servicecenter.l2validation.utils.Constants.TALLY_MARKED_COMPLETED
import com.servicecenter.l2validation.utils.Constants.TALLY_NOT_STARTED
import com.servicecenter.l2validation.utils.Constants.TALLY_STARTED
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MarkExceptionFragment : BaseFragment<FragmentMarkExceptionBinding>() {
    private val sharedViewModel : TallyViewModel by activityViewModels()
    val viewModel : MarkExceptionViewModel by viewModels()
    var awbNo: Long = 0
    private var imagePath: String=""
    private var exceptionReason = ""
    override fun getLayout(): Int {
        return R.layout.fragment_mark_exception
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.tvHeadingName.text = ContextCompat.getString(requireContext(),R.string.mark_exception)

        awbNo = requireArguments().getLong(AWB_NO,0L)
        val orderType = requireArguments().getString(ORDER_TYPE,"")
        "AWB: $awbNo".also { binding.awb.text = it }
        binding.orderType.text = orderType
        exceptionReason = binding.damageRadioBtn.text.toString()
        manageClick()
        observe()
        callImageApi()

    }

    private fun manageClick() {
        binding.icCam.setOnClickListener {
            navigate(
                R.id.cameraFragment,
                bundleOf(
                    Constants.BundleConstants.IMAGE_TYPE to Constants.FRONT_IMAGE,
                    Constants.BundleConstants.HEADER_NAME to "Capture shipment image"
                )
            )
        }
        binding.icDelete.setOnClickListener {
            sharedViewModel.updateFrontImage("", "")
        }
        binding.submitBtn.setOnClickListener {
            viewModel.scanShipment(
                awbNo = awbNo,
                remarks = (binding.remarksEt.text?:"").toString(),
                exceptionReason = exceptionReason,
                exceptionImageUrl = sharedViewModel.imageFlow.value.frontImageUrl
            )
        }
        binding.damageRadioBtn.setOnClickListener {
            exceptionReason = binding.damageRadioBtn.text.toString()
        }
        binding.missingRadioBtn.setOnClickListener {
            exceptionReason = binding.missingRadioBtn.text.toString()
        }
        binding.toolbar.ivBackArrow.setOnClickListener {
            popBackStack()
        }
    }

    private fun observe() {
        lifecycleScope.launch(Dispatchers.Main) {
            sharedViewModel.imageFlow.collect{images->
                if(images.frontImage.isEmpty()){
                    binding.captureShipmentCl.visible()
                    binding.capturedImageCl.gone()
                    binding.capturedImage.setImageResource(R.drawable.front_capture_placeholder)
                    binding.submitBtn.isEnabled = false
                }else{
                    binding.captureShipmentCl.gone()
                    binding.capturedImageCl.visible()
                    binding.capturedImage.setImageBitmap(BitmapFactory.decodeFile(images.frontImage))
                    binding.submitBtn.isEnabled = true
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.uploadImageFlow.collect{result->
                when(result){
                    is APIResultState.Success->{
                        result.data as Response
                        progressDialog().dismiss()
                        sharedViewModel.updateFrontImage(imagePath,result.data.file_name)
                    }
                    else->{
                        manageApiFlowStatus(result,true)
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.scanShipmentsFlow.collect { result ->
                when(result){
                    is APIResultState.Success->{
                        progressDialog().dismiss()
                        result.data as Response
                        setMessageScreen("AWB: ${awbNo}\n${result.data.description}")
                        result.data.recon_status?.let {status->
                            viewModel.updateTallyStatus(status)
                            when (status) {
                                TALLY_NOT_STARTED -> {
                                    setMessageScreen(
                                        msg = result.data.description,
                                        btnAction = 4,
                                        status = false
                                    )
                                }
                                TALLY_STARTED -> {}
                                TALLY_MARKED_COMPLETED -> {

                                }
                                TALLY_COMPLETED -> {
                                    setMessageScreen(
                                        msg = result.data.description,
                                        btnAction = 4,
                                        status = false
                                    )
                                    viewModel.updateTallyMsg(result.data.description)
                                }
                                else->{}
                            }
                        }
                    }
                    is APIResultState.Failure->{
                        progressDialog().dismiss()
                        setMessageScreen(result.description,false)
                    }
                    else->{
                        manageApiFlowStatus(result,true)
                    }
                }
            }
        }
    }

    private fun callImageApi(){
        val bundle = findNavController().currentBackStackEntry?.savedStateHandle?.get<Bundle>("bundle")
        val imageType = bundle?.getString(Constants.BundleConstants.IMAGE_TYPE)?:""
        imagePath = bundle?.getString(Constants.BundleConstants.IMAGE_PATH)?:""
        when(imageType){
            Constants.FRONT_IMAGE->{
                viewModel.callImageUploadApi(
                    BitmapFactory.decodeFile(imagePath),
                    "${awbNo}_SAL_TALLY_FRONT.png",
                    awbNo.toString()
                )
            }
        }
    }

    private fun setMessageScreen(msg: String,status: Boolean = true,btnAction:Int = 3) {
        val bundle = Bundle()
        bundle.putBoolean(Constants.BundleConstants.IS_SUB, true)
        bundle.putBoolean(Constants.BundleConstants.STATUS, status)
        bundle.putString(
            Constants.BundleConstants.SUB_MSG,
            "Check Ticket Status at SLP Kavach"
        )
        bundle.putString(Constants.BundleConstants.MSG, msg)
        bundle.putString(Constants.BundleConstants.BTN_TEXT, "Scan Next Shipment")
        bundle.putInt(Constants.BundleConstants.BTN_ACTION, btnAction)

        navigate(R.id.messageFragment, bundleOf(Constants.BundleConstants.MSG_BUNDLE to bundle))
    }
}