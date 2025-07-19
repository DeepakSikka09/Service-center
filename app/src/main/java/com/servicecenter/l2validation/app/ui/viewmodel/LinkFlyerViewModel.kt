package com.servicecenter.l2validation.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class LinkFlyerViewModel @Inject constructor(
    private val rvpScanUseCase: RVPScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
) : ViewModel() {
    private var pendingShipment: ArrayList<ShipmentDetail>? = ArrayList()
    private val _ShipmentListApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val ShipmentListApiflow: StateFlow<APIResultState> get() = _ShipmentListApiflow


    private val _AllShipmentList = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val allShipmentList: StateFlow<APIResultState> get() = _AllShipmentList
    private val _shipmentList = MutableSharedFlow<APIResultState>()
    val ShipmentList: SharedFlow<APIResultState> get() = _shipmentList

    private val _serverResponse = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val serverResponse: StateFlow<APIResultState> get() = _serverResponse


    fun getAllPendingShipment() {
        viewModelScope.launch() {
            try {
                _AllShipmentList.value = APIResultState.Loading
                val flyerLinkingResult = withContext(Dispatchers.IO) {
                    serviceCenterDatabase.shipmentDetailDao().getAllShipment()
                }
                _AllShipmentList.value = APIResultState.Success(flyerLinkingResult)
            } catch (e: Exception) {
                _AllShipmentList.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun callShipmentListApi() {
        viewModelScope.launch {
            try {
                _ShipmentListApiflow.value = APIResultState.Loading
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()
                val shipmentListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executePendingShipment(locationCode)
                }
                if (shipmentListResult.status) {
                    withContext(Dispatchers.IO) {
                        serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
                        shipmentListResult.response?.Pending_shipment?.let {
                            pendingShipment = ArrayList()
                            pendingShipment?.addAll(it)

                            pendingShipment?.let { it1 ->
                                serviceCenterDatabase.shipmentDetailDao().insertScanData(
                                    it1
                                )
                            }
                        }
                    }
                    _ShipmentListApiflow.value =
                        APIResultState.Success(shipmentListResult.response)
                } else {
                    serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
                    _ShipmentListApiflow.value =
                        APIResultState.Failure(shipmentListResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _ShipmentListApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }

    }

    fun checkForFlyerCode(lastTextFlyerCode: String?) {

        viewModelScope.launch() {
            try {
                _shipmentList.emit(APIResultState.Empty)
                _shipmentList.emit(APIResultState.Loading)
                val flyerlinkingResult = withContext(Dispatchers.IO) {
                    lastTextFlyerCode?.let {
                        serviceCenterDatabase.shipmentDetailDao().checkflyerCodeExistance(
                            it
                        )
                    }
                }
                _shipmentList.emit(APIResultState.Success(flyerlinkingResult))
            } catch (e: Exception) {
                _shipmentList.emit(APIResultState.Failure(e.localizedMessage ?: ""))
            }
        }
    }


    fun sendDataToServer(commonRequest: CommonRequest, flyerRelatedAirwayNo: Long) {
        viewModelScope.launch {
            try {
                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code,
                    ""
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
                        flyerRelatedAirwayNo.let {
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
}