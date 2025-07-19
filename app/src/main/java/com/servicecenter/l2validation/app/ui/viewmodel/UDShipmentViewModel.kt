package com.servicecenter.l2validation.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.utils.APIResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UDShipmentViewModel @Inject constructor(
    private val rvpScanUseCase: RVPScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper
) : ViewModel() {
    private var job: Job? = null
    private val _isBroadcastCancel = MutableStateFlow<Boolean>(false)
    val broacastCancel: StateFlow<Boolean> get() = _isBroadcastCancel

    private val _udServerResponse = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val udServerResponse: StateFlow<APIResultState> get() = _udServerResponse

    private val _shipment = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val shipment: StateFlow<APIResultState> get() = _shipment


    private val _udCallingStatus = MutableSharedFlow<APIResultState>()
    val udCallingStatus: SharedFlow<APIResultState> get() = _udCallingStatus

    private val _CutOffTimeApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val CutOffTimeApiflow: StateFlow<APIResultState> get() = _CutOffTimeApiflow

    fun fetchShipmentByAwbNumber(awbNumber: Long, drs_Id: Long, udStatus: String) {
        viewModelScope.launch {
            try {
                _shipment.value = APIResultState.Loading
                //TODO for clear recentCorrelationId
                preferenceDataStoreHelper.setData(
                    PreferenceDataStoreConstants.recentCorrelationId, ""
                )

                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()
                val shipmentListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executeUdDetail(awbNumber, drs_Id, udStatus, locationCode)
                }
                if (shipmentListResult.status) {
                    _shipment.value =
                        APIResultState.Success(shipmentListResult.response?.awb_details)

                } else {
                    _shipment.value =
                        APIResultState.Failure(shipmentListResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _shipment.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }


    fun getUDCallingStatus(awbNumber: Long, drs_Id: Long) {
        viewModelScope.launch {
            try {
                // _udCallingStatus.value = APIResultState.Loading
                val shipmentListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.getCallStatus(awbNumber, drs_Id)
                }
                if (shipmentListResult.status) {
                    _udCallingStatus.emit(APIResultState.Success(shipmentListResult.response))


                } else {
                    _udCallingStatus.emit(
                        APIResultState.Failure(
                            shipmentListResult.response?.description ?: ""
                        )
                    )

                }
            } catch (e: Exception) {
                _udCallingStatus.emit(APIResultState.Failure(e.localizedMessage ?: ""))
            }
        }
    }


    fun sendUDDataToServer(commonRequest: CommonRequest) {
        viewModelScope.launch {
            try {
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()


                val CorrelationId = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.recentCorrelationId, ""
                ).first()




                _udServerResponse.value = APIResultState.Loading
                commonRequest.apply {
                    dc_code = locationCode
                    client_correlation_id = CorrelationId
                }
                val response = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executeUdPacketToServer(commonRequest = commonRequest)
                }

                if (response.status) {
                    withContext(Dispatchers.IO) {
                        _udServerResponse.value = APIResultState.Success(response.response)
                    }
                } else {
                    _udServerResponse.value =
                        APIResultState.Failure(response.response?.description ?: "")
                }
            } catch (e: Exception) {
                _udServerResponse.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun startJob(awbNumber: Long, drsId: Long) {
        job = viewModelScope.launch {
            while (isActive) {
                Log.d("check_Active", "ok")
                getUDCallingStatus(awbNumber, drsId)
                delay(5000)

            }
        }

    }

    fun cancelJob() {

        job?.cancel()
    }


    fun callCutOffTimeApi() {
        viewModelScope.launch {
            try {
                _CutOffTimeApiflow.value = APIResultState.Loading
                val dcCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                val shipmentListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executeCutOffTime(dcCode)
                }
                if (shipmentListResult.status) {
                    _CutOffTimeApiflow.value =
                        APIResultState.Success(shipmentListResult.response)
                } else {
                    serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
                    _CutOffTimeApiflow.value =
                        APIResultState.Failure(shipmentListResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _CutOffTimeApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }

    }

    fun cancelBroadCast(boolean: Boolean) {
        _isBroadcastCancel.value =boolean
    }
}