package com.servicecenter.l2validation.app.ui.viewmodel
// Code Reviewed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QcPassedViewModel @Inject constructor(
    private val rvpScanUseCase: RVPScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
) : ViewModel() {
    private val _serverResponse = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val serverResponse: StateFlow<APIResultState> get() = _serverResponse

    private val _imageListApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val ImageListApiflow: StateFlow<APIResultState> get() = _imageListApiflow


    fun sendDataToServer(commonRequest: CommonRequest, flyerRltdAirwillNo: String) {
        viewModelScope.launch {
            try {
                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                _serverResponse.value = APIResultState.Loading
                commonRequest.apply {
                    employee_code = empCode
                    dc_code = locationCode
                }
                val response = withContext(Dispatchers.IO) {
                    rvpScanUseCase.sendCommitPacketToServer(commonRequest = commonRequest)
                }
                // DB Insertion in shipment Dao
                if (response.status) {
                    withContext(Dispatchers.IO) {
                        flyerRltdAirwillNo?.toLong()?.let {
                            serviceCenterDatabase.shipmentDetailDao().updateStatusWiseShipmentList(
                                it, Constants.Failed
                            )
                        }
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


    fun callImageListApi(awb: String) {
        viewModelScope.launch {
            try {
                _imageListApiflow.value = APIResultState.Loading
                val imageListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executeProductImage(awb)
                }
                if (imageListResult.status) {
                    val response = imageListResult.response
                    val list = mapOf(
                        "fe_images" to (response?.fe_images ?: emptyList()),
                        "product_images" to (response?.product_images ?: emptyList()),
                        "flyer_images" to (response?.flyer_images ?: emptyList()),
                    )

                    _imageListApiflow.value = APIResultState.Success(list)
                } else {
                    _imageListApiflow.value =
                        APIResultState.Failure(imageListResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _imageListApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }
}