package com.servicecenter.l2validation.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.domain.usecases.RtsScanUseCase
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils
import com.servicecenter.l2validation.utils.Constants
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val rvpScanUseCase: RVPScanUseCase,
    private val rtsScanUseCase: RtsScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
) : ViewModel() {
    private val _serverResponse = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val serverResponse: StateFlow<APIResultState> get() = _serverResponse

    private val _qc_required = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val qc_required: StateFlow<APIResultState> get() = _qc_required

    fun uploadImageServer(
        file: File,
        bitmap: Bitmap?,
        imageName: String,
        awbNo: String,
        came_from: String,
    ) {
        viewModelScope.launch {
            try {
                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val bytes = CommonUtils.convertToByteArray(bitmap!!)
                val mFile: RequestBody =
                    RequestBody.create("application/octet-stream".toMediaTypeOrNull(), bytes)
                val fileToUpload = MultipartBody.Part.createFormData("image", file.name, mFile)
                val awb_no = awbNo.toString().toRequestBody(MultipartBody.FORM)

                val appCode = "SCA_L2_APP".toRequestBody(MultipartBody.FORM)

                val emp_code = empCode.toRequestBody(MultipartBody.FORM)
                val imagename = imageName.toRequestBody(MultipartBody.FORM)

                val map: MutableMap<String, RequestBody> = HashMap()
                map["image"] = mFile
                map["airwaybill_number"] = awb_no
                map["app_code"] = appCode
                map["employee_code"] = emp_code
                map["image_name"] = imagename

                _serverResponse.value = APIResultState.Loading

                val response = withContext(Dispatchers.IO) {
                    rvpScanUseCase.upLoadImage(map, fileToUpload)
                }
                if (response.status) {

                    if (came_from.equals(Constants.FRONT_IMAGE, ignoreCase = true)) {
                        preferenceDataStoreHelper.setData(
                            PreferenceDataStoreConstants.FRONT_IMAGE,
                            response.response?.image_id ?: 0L
                        )
                    } else {
                        preferenceDataStoreHelper.setData(
                            PreferenceDataStoreConstants.BACK_IMAGE,
                            response.response?.image_id ?: 0L
                        )
                    }
                    _serverResponse.value = APIResultState.Success(response.response)
                } else {
                    _serverResponse.value =
                        APIResultState.Failure(response.response?.description ?: "")
                }
            } catch (e: Exception) {
                _serverResponse.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }


    fun uploadRtsImageServer(
        file: File,
        bitmap: Bitmap?,
        imageName: String,
        awbNo: String,
        came_from: String,
    ) {
        viewModelScope.launch {
            try {
                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val bytes = CommonUtils.convertToByteArray(bitmap!!)
                val mFile: RequestBody =
                    RequestBody.create("application/octet-stream".toMediaTypeOrNull(), bytes)
                val fileToUpload = MultipartBody.Part.createFormData("rts_bitmap", file.name, mFile)
                val awb_no = awbNo.toString().toRequestBody(MultipartBody.FORM)

                val appCode = "SCA_RTS_APP".toRequestBody(MultipartBody.FORM)

                val emp_code = empCode.toRequestBody(MultipartBody.FORM)
                val imagename = imageName.toRequestBody(MultipartBody.FORM)

                val map: MutableMap<String, RequestBody> = HashMap()
                map["image"] = mFile
                map["airwaybill_number"] = awb_no
                map["app_code"] = appCode
                map["employee_code"] = emp_code
                map["image_name"] = imagename

                _serverResponse.value = APIResultState.Loading
                //change url in production --->

                val response = withContext(Dispatchers.IO) {
                    rtsScanUseCase.upLoadRtsImage(map, fileToUpload)
                }
                if (response.status) {

                    if (came_from.equals(Constants.FRONT_IMAGE, ignoreCase = true)) {
                        preferenceDataStoreHelper.setData(
                            PreferenceDataStoreConstants.FRONT_IMAGE,
                            response.response?.image_id ?: 0L
                        )
                    } else {
                        preferenceDataStoreHelper.setData(
                            PreferenceDataStoreConstants.BACK_IMAGE,
                            response.response?.image_id ?: 0L
                        )
                    }
                    _serverResponse.value = APIResultState.Success(response.response)
                } else {
                    _serverResponse.value =
                        APIResultState.Failure(response.response?.description ?: "")
                }
            } catch (e: Exception) {
                _serverResponse.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun QcRequiredDb(awbNo: String) {
        viewModelScope.launch() {
            try {
                _qc_required.value = APIResultState.Loading
                val flyerLinkingResult = withContext(Dispatchers.IO) {
                    serviceCenterDatabase.shipmentDetailDao().getQcStatus(awbNo)
                }
                _qc_required.value = APIResultState.Success(flyerLinkingResult)
            } catch (e: Exception) {
                _qc_required.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }
}



