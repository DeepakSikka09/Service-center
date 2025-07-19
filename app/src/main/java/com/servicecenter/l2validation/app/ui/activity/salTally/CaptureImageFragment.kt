package com.servicecenter.l2validation.app.ui.activity.salTally

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.ui.viewmodel.CaptureImageViewModel
import com.servicecenter.l2validation.app.ui.viewmodel.TallyViewModel
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.FragmentCaptureImageBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.BundleConstants.AWB_NO
import com.servicecenter.l2validation.utils.Constants.BundleConstants.BTN_ACTION
import com.servicecenter.l2validation.utils.Constants.BundleConstants.BTN_TEXT
import com.servicecenter.l2validation.utils.Constants.BundleConstants.HEADER_NAME
import com.servicecenter.l2validation.utils.Constants.BundleConstants.IS_ORDER_TYPE
import com.servicecenter.l2validation.utils.Constants.BundleConstants.IS_SUB
import com.servicecenter.l2validation.utils.Constants.BundleConstants.MSG
import com.servicecenter.l2validation.utils.Constants.BundleConstants.MSG_BUNDLE
import com.servicecenter.l2validation.utils.Constants.BundleConstants.ORDER_TYPE
import com.servicecenter.l2validation.utils.Constants.BundleConstants.STATUS
import com.servicecenter.l2validation.utils.Constants.BundleConstants.SUB_MSG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CaptureImageFragment : BaseFragment<FragmentCaptureImageBinding>() {
    override fun getLayout(): Int {
        return R.layout.fragment_capture_image
    }

    val viewModel: CaptureImageViewModel by viewModels()
    private var awbNo = 0L
    private var imagePath = ""
    private var imageType = ""
    private var orderType = ""
    private val sharedViewModel: TallyViewModel by activityViewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callImageApi()
        setScreen()
        if(!viewModel.isFlowRegister) {
            observer()
            viewModel.isFlowRegister = true
        }
        observeImage()
        registerClick()
        awbNo = requireArguments().getLong(AWB_NO, 0L)
        orderType = requireArguments().getString(ORDER_TYPE, "")
    }

    private fun observer() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.imageCaptureFlow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        result.data as Response
                        progressDialog().dismiss()
                        setImage(result.data.file_name)
                    }

                    else -> {
                        manageApiFlowStatus(result, true)
                    }
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.scanShipmentsFlow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        result.data as Response
                        progressDialog().dismiss()
                        setMessageScreen(
                            status = true,
                            msg = "AWB: $awbNo\n${result.data.description}"
                        )
                        sharedViewModel.setShipmentDetails(Pair(awbNo,orderType))
                    }

                    is APIResultState.Failure->{
                        progressDialog().dismiss()
                        setMessageScreen(
                            status = false,
                            msg = result.description
                        )
                    }

                    else -> {
                        manageApiFlowStatus(result, true)
                    }
                }
            }
        }
    }

    private fun observeImage(){
        lifecycleScope.launch {
            sharedViewModel.imageFlow.collect { images ->
                if (images.frontImage.isEmpty()) {
                    binding.frontImgCl.captureShipmentCl.visibility = View.VISIBLE
                    binding.frontImgCl.capturedShipmentCl.visibility = View.GONE
                    binding.frontImgIv.setImageResource(R.drawable.front_capture_placeholder)
                } else {
                    binding.frontImgCl.captureShipmentCl.visibility = View.GONE
                    binding.frontImgCl.capturedShipmentCl.visibility = View.VISIBLE
                    binding.frontImgIv.setImageBitmap(BitmapFactory.decodeFile(images.frontImage))
                }
                if (images.backImage.isEmpty()) {
                    binding.backImgCl.captureShipmentCl.visibility = View.VISIBLE
                    binding.backImgCl.capturedShipmentCl.visibility = View.GONE
                    binding.backImgIv.setImageResource(R.drawable.back_capture_placeholder)
                } else {
                    binding.backImgCl.captureShipmentCl.visibility = View.GONE
                    binding.backImgCl.capturedShipmentCl.visibility = View.VISIBLE
                    binding.backImgIv.setImageBitmap(BitmapFactory.decodeFile(images.backImage))
                }
                if (images.backImage.isNotEmpty() && images.frontImage.isNotEmpty()) {
                    binding.submitBtn.isEnabled = true
                } else {
                    binding.submitBtn.isEnabled = false
                }
            }
        }
    }

    private fun callImageApi() {
        val bundle =
            findNavController().currentBackStackEntry?.savedStateHandle?.get<Bundle>("bundle")
        imageType = bundle?.getString(Constants.BundleConstants.IMAGE_TYPE) ?: ""
        imagePath = bundle?.getString(Constants.BundleConstants.IMAGE_PATH) ?: ""
        when (imageType) {
            Constants.FRONT_IMAGE -> {
                progressDialog().show()
                viewModel.callImageUploadApi(
                    BitmapFactory.decodeFile(imagePath),
                    "${awbNo}_SAL_TALLY_FRONT.png",
                    awbNo.toString()
                )
            }

            Constants.BACK_IMAGE -> {
                progressDialog().show()
                viewModel.callImageUploadApi(
                    BitmapFactory.decodeFile(imagePath),
                    "${awbNo}_SAL_TALLY_BACK.png",
                    awbNo.toString()
                )
            }
        }
    }

    private fun setImage(imageUrl: String) {
        when (imageType) {
            Constants.FRONT_IMAGE -> {
                sharedViewModel.updateFrontImage(imagePath, imageUrl)
            }

            Constants.BACK_IMAGE -> {
                sharedViewModel.updateBackImage(imagePath, imageUrl)
            }
        }
    }

    private fun setScreen() {
        binding.header.tvHeadingName.text = getString(R.string.capture_image)
        binding.frontImgCl.captureTv.text = getString(R.string.capture_front_image)
        binding.backImgCl.captureTv.text = getString(R.string.back_image)
    }

    private fun registerClick() {
        binding.frontImgCl.icCam.setOnClickListener {
            navigate(
                R.id.cameraFragment,
                bundleOf(
                    Constants.BundleConstants.IMAGE_TYPE to Constants.FRONT_IMAGE,
                    IS_ORDER_TYPE to true,
                    ORDER_TYPE to orderType,
                    AWB_NO to awbNo.toString(),
                    HEADER_NAME to "Capture shipment image"
                )
            )
        }
        binding.backImgCl.icCam.setOnClickListener {
            navigate(
                R.id.cameraFragment,
                bundleOf(
                    Constants.BundleConstants.IMAGE_TYPE to Constants.BACK_IMAGE,
                    IS_ORDER_TYPE to true,
                    ORDER_TYPE to orderType,
                    AWB_NO to awbNo.toString(),
                    HEADER_NAME to "Capture shipment image"
                )
            )
        }
        binding.frontImgCl.icDelete.setOnClickListener {
            sharedViewModel.updateFrontImage("", "")
        }
        binding.backImgCl.icDelete.setOnClickListener {
            sharedViewModel.updateBackImage("", "")
        }
        binding.submitBtn.setOnClickListener {
            sharedViewModel.imageFlow.value.let { image ->
                viewModel.scanShipment(awbNo, image.frontImageUrl, image.backImageUrl)
            }
        }
        binding.header.ivBackArrow.setOnClickListener {
            popBackStack()
        }
    }

    private fun setMessageScreen(status:Boolean,msg: String) {
        val bundle = Bundle()
        bundle.putBoolean(IS_SUB, true)
        bundle.putBoolean(STATUS, status)
        bundle.putString(
            SUB_MSG,
            "Check the status of the AWB from the Track Me screen"
        )
        bundle.putString(MSG, msg)
        bundle.putString(BTN_TEXT, "Scan Next Shipment")
        bundle.putInt(BTN_ACTION, 3)

        navigate(R.id.messageFragment, bundleOf(MSG_BUNDLE to bundle))
    }


}