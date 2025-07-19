package com.servicecenter.l2validation.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.usecases.SalTallyUseCase
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class CaptureImageViewModel @Inject constructor(
    val salTallyUseCase: SalTallyUseCase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper
) : ViewModel() {
    var isFlowRegister: Boolean = false
    private val _imageCaptureFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val imageCaptureFlow: StateFlow<APIResultState> get() = _imageCaptureFlow

    private val _scanShipmentsFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val scanShipmentsFlow: StateFlow<APIResultState> get() = _scanShipmentsFlow

    fun callImageUploadApi(
        bitmap: Bitmap?,
        imageName: String,
        awbNo: String,
    ) {
        try {
            viewModelScope.launch {
                _imageCaptureFlow.value = APIResultState.Loading
                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val bytes = CommonUtils.convertToByteArray(bitmap!!)
                val mFile: RequestBody =
                    bytes.toRequestBody(
                        "application/octet-stream".toMediaTypeOrNull(),
                        0,
                        bytes.size
                    )
                val fileToUpload = MultipartBody.Part.createFormData("image", imageName, mFile)
                val awbBody = awbNo.toRequestBody(MultipartBody.FORM)

                val appCode = "SCA_SAL_TALLY_APP".toRequestBody(MultipartBody.FORM)

                val imagename = imageName.toRequestBody(MultipartBody.FORM)
                val empBody = empCode.toRequestBody(MultipartBody.FORM)

                val map: MutableMap<String, RequestBody> = HashMap()
                map["image"] = mFile
                map["airwaybill_number"] = awbBody
                map["employee_code"] = empBody
                map["app_code"] = appCode
                map["image_name"] = imagename
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.uploadTallyImage(map, fileToUpload)
                }
                if (result.status) {
                    _imageCaptureFlow.value = APIResultState.Success(result.response)
                } else {
                    _imageCaptureFlow.value =
                        APIResultState.Failure(result.response?.description ?: "")
                }

            }
        } catch (ex: Exception) {
            _imageCaptureFlow.value =
                APIResultState.Failure(ex.localizedMessage ?: "Something went wrong")
        }
    }

    fun scanShipment(awbNo: Long, frontImageUrl: String, backImageUrl: String) {
        viewModelScope.launch {
            _scanShipmentsFlow.value = APIResultState.Loading
            val dcCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.Location_Code, "")
                    .first()
            val empCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            try {
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.updateShipmentWithImage(
                        CommonRequest(
                            dc_code = dcCode,
                            emp_code = empCode,
                            awb_no = awbNo,
                            front_image_url = frontImageUrl,
                            back_image_url = backImageUrl
                        )
                    )
                }

                if (result.status) {
                    _scanShipmentsFlow.value = APIResultState.Success(result.response)
                } else {
                    _scanShipmentsFlow.value =
                        APIResultState.Failure(result.response?.description ?: "")
                }
            } catch (ex: Exception) {
                _scanShipmentsFlow.value =
                    APIResultState.Failure(ex.localizedMessage ?: "")
            }
        }
    }
}