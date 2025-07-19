package com.servicecenter.l2validation.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.usecases.RtsScanUseCase
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
class CommitViewModel @Inject constructor(
    private val rvpScanUseCase: RVPScanUseCase,
    private val rtsScanUseCase: RtsScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
) : ViewModel() {


    private val _serverResponse = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val serverResponse: StateFlow<APIResultState> get() = _serverResponse
    private val _rtsPacketResponse= MutableStateFlow<APIResultState>(APIResultState.Empty)
    val rtsPacketResponse:StateFlow<APIResultState> get() = _rtsPacketResponse

    private val _udServerResponse = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val udServerResponse: StateFlow<APIResultState> get() = _udServerResponse


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
                        if(commonRequest.status.equals("failure",true)){
                            flyerRltdAirwillNo?.toLong()?.let {
                                serviceCenterDatabase.shipmentDetailDao().updateStatusWiseShipmentList(
                                    it, Constants.Failed
                                )
                            }
                        }else {

                            flyerRltdAirwillNo.toLong().let {
                                serviceCenterDatabase.shipmentDetailDao()
                                    .updateStatusWiseShipmentList(
                                        it, Constants.Success
                                    )
                            }
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



    fun sendRtsDataToServer(commonRequest: CommonRequest) {
        viewModelScope.launch {
            try {
                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                _rtsPacketResponse.value = APIResultState.Loading
                commonRequest.apply {
                    employee_code = empCode
                    dc_code = locationCode
                }


                val response = withContext(Dispatchers.IO) {
                    rtsScanUseCase.sendRtsPacketToServer(commonRequest = commonRequest)
                }
                if (response.status) {
                    _rtsPacketResponse.value = APIResultState.Success(response.response)
                } else {
                    if (response.error_response!=null){
                        _rtsPacketResponse.value =
                            APIResultState.Failure(response.error_response?.message ?: "")
                    }else{
                        _rtsPacketResponse.value =
                            APIResultState.Failure(response.response?.description ?: "")
                    }

                     }
            } catch (e: Exception) {
                _rtsPacketResponse.value = APIResultState.Failure(e.localizedMessage ?: "")
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
                    client_correlation_id=CorrelationId
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


}