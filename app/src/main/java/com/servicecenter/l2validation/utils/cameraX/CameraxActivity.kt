package com.servicecenter.l2validation.utils.cameraX

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.activity.rvpShipment.CommitActivity
import com.servicecenter.l2validation.app.ui.viewmodel.CameraViewModel
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.ActivityCameraxBinding
import com.servicecenter.l2validation.databinding.ImageCheckBottomsheetBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutionException


@AndroidEntryPoint
class CameraxActivity : BaseActivity<ActivityCameraxBinding, CameraViewModel>(), View.OnClickListener {

    private var imageCapture: ImageCapture? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var camera: Camera? = null
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var preview: Preview
    var AWB_no = ""
    var image_type = Constants.FRONT_IMAGE
    var frontImageID: Long = 0L
    var backImageID: Long = 0L
    var frontRtsKey: String = ""
    var backRtsKey: String = ""
    var shipmentLabelImageID: Long = 0L
    private lateinit var bottomsheetQcPassed: BottomSheetDialog
    private lateinit var alternateBottomsheetDialogBinding: ImageCheckBottomsheetBinding
    private var isFromLinkFlyer = false
    private var isFeImageRequiredData = false
    private var isRts: Boolean = false

    override fun getLayout(): Int {
        return R.layout.activity_camerax
    }

    override fun getViewModels(): Class<CameraViewModel> {
        return CameraViewModel::class.java
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = intent.extras
        AWB_no = bundle?.getString(Constants.flyerRelatedAirwillNo).toString()
        isFromLinkFlyer = bundle?.getBoolean(Constants.LINK_FLYER_ACTIVITY) ?: false
        isRts = bundle?.getBoolean(Constants.Is_RTS) ?: true

        startCamera()
        initlize()
        viewModel.QcRequiredDb(AWB_no)
        fetchResponse()
        fetchQCRequired()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()
            }
        })
    }

    private fun initlize() {
        binding.btnTakePicture.setOnClickListener(this)
        if (isRts) {
            binding.constAwb.visibility = View.VISIBLE
            binding.tvAwbNo.text = "AWB: " + AWB_no
        } else {
            binding.constAwb.visibility = View.GONE
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnTakePicture -> {
                if (::cameraProvider.isInitialized) {
                    cameraProvider.unbind(preview)
                }
                openUploadRetakeBottomSheet()
                updateCaptureAnalytics()
            }
        }
    }

    private fun openUploadRetakeBottomSheet() {
        try {
            alternateBottomsheetDialogBinding =
                ImageCheckBottomsheetBinding.inflate((layoutInflater))
            bottomsheetQcPassed = BottomSheetDialog(this, R.style.otpsheetDialogTheme)
            bottomsheetQcPassed.setContentView(alternateBottomsheetDialogBinding.root)
            bottomsheetQcPassed.setCancelable(true)
            alternateBottomsheetDialogBinding.btnRetake.setOnClickListener {
                startCamera()
                updateRetakeAnalytics()
                bottomsheetQcPassed.dismiss()
            }
            alternateBottomsheetDialogBinding.btnUpload.setOnClickListener {
                getCaptureImage()
                updateUploadAnalytics()
                bottomsheetQcPassed.dismiss()
            }
            if (bottomsheetQcPassed != null && !bottomsheetQcPassed.isShowing) {
                bottomsheetQcPassed.show()
            }
            bottomsheetQcPassed.setCancelable(false)
        } catch (e: java.lang.Exception) {
        }
    }

    private fun getCaptureImage() {
        val folder = File("${filesDir}")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, System.currentTimeMillis().toString() + ".png")

        imageCapture?.takePicture(ImageCapture.OutputFileOptions.Builder(
            file
        ).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    var imageName = AWB_no + "_RVP_" + image_type + ".png"
                    var imageString: String? =
                        CommonUtils.compressImages(file, this@CameraxActivity)
                    val bitmap: Bitmap = BitmapFactory.decodeFile(imageString)
                    if (isInternetAvailable(this@CameraxActivity)) {
                        viewModel.uploadImageServer(file, bitmap, imageName, AWB_no, image_type)
                    } else {
                        showToast(getString(R.string.no_internet), false)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                }
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            try {
                cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            } catch (e: ExecutionException) {
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    open fun bindPreview(cameraProvider: ProcessCameraProvider) {
        //Preview
        preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) //to manage image quality
            .setFlashMode(flashMode)
            .build()

        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
        } catch (exc: Exception) {
            Log.e("bindCameraUseCases", "Use case binding failed", exc)
        }
    }

    private fun fetchResponse() {
        lifecycleScope.launch {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.serverResponse.collect { allShipmentList ->
                when (allShipmentList) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        var data = allShipmentList.data as Response
                        data.description.let { showToast(it, true) }

                        if(::bottomsheetQcPassed.isInitialized && bottomsheetQcPassed.isShowing) {
                            bottomsheetQcPassed.dismiss()
                        }

                        if (isFromLinkFlyer) {
                            handleLinkFlyerCapture(data)
                        } else {
                            handleNormalCapture(data)
                        }
                    }

                    else -> {
                        startCamera()
                        manageApiFlowStatus(allShipmentList, true)
                    }
                }
            }
        }
    }

    private fun handleLinkFlyerCapture(data: Response) {
        when (image_type) {
            Constants.FRONT_IMAGE -> {
                frontImageID = data.image_id
                image_type = Constants.BACK_IMAGE
                binding.tvImageSide.text = getString(R.string.back_image)
                analyticsToAllScreen(Constants.CAPTURE_SHIPMENT_BACK_IMAGE)
                startCamera()
            }

            Constants.BACK_IMAGE -> {
                backImageID = data.image_id
                val resultIntent = Intent().apply {
                    putExtra(Constants.FRONT_IMAGE_ID, frontImageID)
                    putExtra(Constants.BACK_IMAGE_ID, backImageID)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun handleNormalCapture(data: Response) {
        when (image_type) {
            Constants.FRONT_IMAGE -> {
                frontRtsKey = data.file_name

                frontImageID = data.image_id
                image_type = Constants.BACK_IMAGE
                binding.tvImageSide.text = getString(R.string.back_image)
                analyticsToAllScreen(Constants.CAPTURE_SHIPMENT_BACK_IMAGE)
                startCamera()
            }

            Constants.BACK_IMAGE -> {
                backRtsKey = data.file_name

                backImageID = data.image_id
                val extras = Bundle().apply {
                    putString(Constants.flyerRelatedAirwillNo, data.airwaybill_number)
                    putLong(Constants.FRONT_IMAGE_ID, frontImageID)
                    putLong(Constants.BACK_IMAGE_ID, backImageID)
                    putString(Constants.FRONT_RTS_KEY, frontRtsKey)
                    putString(Constants.BACK_RTS_KEY, backRtsKey)
                    putBoolean(Constants.Is_RTS, isRts)
                }
                startScreen(CommitActivity(), extras)
                finish()
            }
        }
    }

    private fun fetchQCRequired() {
        lifecycleScope.launch {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.qc_required.collect { qc ->
                when (qc) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        isFeImageRequiredData = qc.data as Boolean
                        // getPendingList(qc.data as List<ShipmentDetail>)
                    }

                    else -> {
                        manageApiFlowStatus(qc, true)
                    }
                }
            }
        }
    }

     fun backPress() {
        showToast(getString(R.string.you_can_t_go_back), false)
    }

    private fun updateRetakeAnalytics() {
        when (image_type) {
            Constants.FRONT_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE_RETAKE,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE_RETAKE
            )

            Constants.BACK_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE_RETAKE,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE_RETAKE
            )
        }
    }

    private fun updateUploadAnalytics() {
        when (image_type) {
            Constants.FRONT_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE_UPLOAD,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE_UPLOAD
            )

            Constants.BACK_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE_UPLOAD,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE_UPLOAD
            )
        }
    }



    private fun updateCaptureAnalytics() {
        when (image_type) {

            Constants.FRONT_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE
            )

            Constants.BACK_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE
            )
        }
    }
}