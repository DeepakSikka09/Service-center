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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
class MarkExceptionViewModel @Inject constructor(
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val salTallyUseCase: SalTallyUseCase
) : ViewModel() {
    private val _uploadImageFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val uploadImageFlow: StateFlow<APIResultState> get() = _uploadImageFlow

    private val _scanShipmentsFlow = MutableSharedFlow<APIResultState>(replay = 0)
    val scanShipmentsFlow: SharedFlow<APIResultState> get() = _scanShipmentsFlow
    fun callImageUploadApi(
        bitmap: Bitmap?,
        imageName: String,
        awbNo: String,
    ) {
        try {
            viewModelScope.launch {
                val bytes = CommonUtils.convertToByteArray(bitmap!!)
                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val mFile: RequestBody =
                    bytes.toRequestBody(
                        "application/octet-stream".toMediaTypeOrNull(),
                        0,
                        bytes.size
                    )
                val fileToUpload = MultipartBody.Part.createFormData("image", imageName, mFile)
                val awbRequest = awbNo.toRequestBody(MultipartBody.FORM)

                val appCode = "SCA_SAL_TALLY_EXCEPTION_APP".toRequestBody(MultipartBody.FORM)

                val imagename = imageName.toRequestBody(MultipartBody.FORM)

                val map: MutableMap<String, RequestBody> = HashMap()
                map["image"] = mFile
                map["airwaybill_number"] = awbRequest
                map["app_code"] = appCode
                map["image_name"] = imagename
                map["employee_code"] = empCode.toRequestBody(MultipartBody.FORM)

                _uploadImageFlow.value = APIResultState.Loading
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.uploadTallyImage(map, fileToUpload)
                }
                if (result.status) {
                    _uploadImageFlow.value = APIResultState.Success(result.response)
                } else {
                    _uploadImageFlow.value =
                        APIResultState.Failure(result.response?.description ?: "")
                }

            }
        } catch (ex: Exception) {
            _uploadImageFlow.value =
                APIResultState.Failure(ex.localizedMessage ?: "Something went wrong")
        }
    }

    fun scanShipment(
        awbNo: Long,
        exceptionImageUrl: String,
        exceptionReason: String,
        remarks: String
    ) {
        viewModelScope.launch {
            _scanShipmentsFlow.emit(APIResultState.Loading)
            try {
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.markException(
                        CommonRequest(
                            dc_code = preferenceDataStoreHelper.getData(
                                PreferenceDataStoreConstants.Location_Code,
                                ""
                            ).first(),
                            emp_code = preferenceDataStoreHelper.getData(
                                PreferenceDataStoreConstants.EMP_CODE,
                                ""
                            ).first(),
                            awb_no = awbNo,
                            exception_image_url = exceptionImageUrl,
                            exception_reason = exceptionReason,
                            remarks = remarks
                        )
                    )
                }

                if (result.status) {
                    _scanShipmentsFlow.emit(APIResultState.Success(result.response))
                } else {
                    _scanShipmentsFlow.emit(APIResultState.Failure(result.response?.description ?: ""))
                }
            } catch (ex: Exception) {
                _scanShipmentsFlow.emit(APIResultState.Failure(ex.localizedMessage ?: ""))
            }
        }
    }

    fun updateTallyStatus(status: Int) {
        viewModelScope.launch {
            preferenceDataStoreHelper.setData(
                PreferenceDataStoreConstants.TALLY_STATUS,
                status
            )
        }
    }

    fun updateTallyMsg(description: String) {
        viewModelScope.launch {
            preferenceDataStoreHelper.setData(
                PreferenceDataStoreConstants.TALLY_MSG,
                description
            )
        }
    }


}