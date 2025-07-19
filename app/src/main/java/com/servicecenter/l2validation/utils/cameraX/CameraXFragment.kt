package com.servicecenter.l2validation.utils.cameraX

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.gone
import com.servicecenter.l2validation.app.extensions.visible
import com.servicecenter.l2validation.databinding.FragmentCameraXBinding
import com.servicecenter.l2validation.databinding.ImageCheckBottomsheetBinding
import com.servicecenter.l2validation.utils.CommonUtils
import com.servicecenter.l2validation.utils.CommonUtils.calculateImageBlurryOrNot
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.ExecutionException

@AndroidEntryPoint
class CameraXFragment : BaseFragment<FragmentCameraXBinding>() {

    private val cameraPermissionRequestCode = 100
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private var imageCapture: ImageCapture? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var camera: Camera? = null
    private var imageType = Constants.FRONT_IMAGE

    private lateinit var bottomSheetQcPassed: BottomSheetDialog
    private lateinit var alternateBottomSheetDialogBinding: ImageCheckBottomsheetBinding
    override fun getLayout(): Int {
        return R.layout.fragment_camera_x
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialiseImageType()
        requestCameraPermission()
        initListeners()
    }

    private fun initialiseImageType() {
        if (requireArguments().getString(Constants.BundleConstants.IMAGE_TYPE)!=null){
            imageType= requireArguments().getString(Constants.BundleConstants.IMAGE_TYPE).toString()
        }
        val awbValue = "AWB: ${requireArguments().getString(Constants.BundleConstants.AWB_NO,"")}"
        binding.tvAwbNo.text = awbValue
        binding.ilHeader.tvHeadingName.text = requireArguments().getString(Constants.BundleConstants.HEADER_NAME,"")
        val isOrderType = requireArguments().getBoolean(Constants.BundleConstants.IS_ORDER_TYPE,false)
        val orderType = requireArguments().getString(Constants.BundleConstants.ORDER_TYPE,"")
        if(isOrderType && orderType.isNotBlank()){
            binding.orderType.visible()
            binding.orderType.text = orderType
        }else{
            binding.constAwb.gone()
        }
    }

    private fun initListeners() {
        with(binding) {
            btnTakePicture.setOnClickListener {
                if (camera != null) {
                    binding.btnTakePicture.isEnabled = false
                    cameraProvider.unbind(preview)
                    openUploadRetakeBottomSheet()
                    updateCaptureAnalytics()
                }
            }
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            } catch (e: ExecutionException) {
                showToast(e.localizedMessage ?: "An unknown error occurred", false)
            } catch (e: InterruptedException) {
                showToast(e.localizedMessage ?: "An unknown error occurred", false)
            }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
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

    private fun openUploadRetakeBottomSheet(
        headlineText: String = getString(R.string.image_clear_or_not),
        hideUploadButton: Boolean = false,
        hideTextView: Boolean = false,
        showImageView: Boolean = false
    ) {
        try {

            alternateBottomSheetDialogBinding =
                ImageCheckBottomsheetBinding.inflate((layoutInflater))
            bottomSheetQcPassed = BottomSheetDialog(requireActivity(), R.style.otpsheetDialogTheme)
            bottomSheetQcPassed.setContentView(alternateBottomSheetDialogBinding.root)
            alternateBottomSheetDialogBinding.tvPrdctClear.text = headlineText

            setViewVisibility(alternateBottomSheetDialogBinding.tvHeadline, hideTextView)
            setViewVisibility(
                alternateBottomSheetDialogBinding.imageCross,
                !showImageView
            ) // Note: inverse flag
            setViewVisibility(alternateBottomSheetDialogBinding.btnUpload, hideUploadButton)
            alternateBottomSheetDialogBinding.btnRetake.setOnClickListener {
                binding.btnTakePicture.isEnabled = true
                startCamera()
                updateRetakeAnalytics()
                bottomSheetQcPassed.setCancelable(true)
                bottomSheetQcPassed.dismiss()
            }
            alternateBottomSheetDialogBinding.btnUpload.setOnClickListener {
                getCaptureImage()
                updateUploadAnalytics()
                bottomSheetQcPassed.dismiss()
            }
            if (!bottomSheetQcPassed.isShowing) {
                bottomSheetQcPassed.show()
            }
            bottomSheetQcPassed.setCancelable(false)
        } catch (e: java.lang.Exception) {
                showToast(e.localizedMessage?:"An unknown error occurred",false)
        }
    }

    private fun getCaptureImage() {
        val folder = File("${requireActivity().filesDir}")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, System.currentTimeMillis().toString() + ".png")
        imageCapture?.takePicture(ImageCapture.OutputFileOptions.Builder(
            file
        ).build(),
            ContextCompat.getMainExecutor(requireActivity()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val imageString: String? =
                        CommonUtils.compressImages(file, requireActivity())
                    val bitmap: Bitmap = BitmapFactory.decodeFile(imageString)
                    val isImageBlurry = calculateImageBlurryOrNot(bitmap, 150.00, 20.00)
                    /*if (isImageBlurry) {
                        openUploadRetakeBottomSheet(
                            headlineText = getString(R.string.please_upload_the_valid_image),
                            hideUploadButton = true,
                            hideTextView = true,
                            showImageView = true
                        )
                        return
                    }*/
                    val bundle=Bundle()
                    bundle.putString(Constants.BundleConstants.IMAGE_PATH,imageString)
                    bundle.putString(Constants.BundleConstants.IMAGE_TYPE,imageType)
                    popBackWithData(bundle)
                }

                override fun onError(exception: ImageCaptureException) {
                }
            })
    }

    private fun updateCaptureAnalytics() {
        when (imageType) {

            Constants.FRONT_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE,
            )

            Constants.BACK_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE,
            )
        }
    }

    private fun setViewVisibility(view: View, shouldHide: Boolean) {
        view.visibility = if (shouldHide) View.GONE else View.VISIBLE
    }

    private fun updateRetakeAnalytics() {
        when (imageType) {
            Constants.FRONT_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE_RETAKE,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE_RETAKE,
            )

            Constants.BACK_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE_RETAKE,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE_RETAKE,
            )
        }
    }

    private fun updateUploadAnalytics() {
        when (imageType) {
            Constants.FRONT_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE_UPLOAD,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_FRONT_IMAGE_UPLOAD,
            )

            Constants.BACK_IMAGE -> analyticsToAllButton(
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE_UPLOAD,
                Constants.BUTTON_KEY,
                Constants.CAPTURE_SHIPMENT_BACK_IMAGE_UPLOAD,
            )
        }
    }
}