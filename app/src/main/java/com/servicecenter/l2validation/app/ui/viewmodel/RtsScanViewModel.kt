package com.servicecenter.l2validation.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.data.local.entities.PendingRtsData
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.usecases.RtsScanUseCase
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
class RtsScanViewModel @Inject constructor(
    private val rtsScanUseCase: RtsScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
) : ViewModel() {
    private var pendingRtsShipmentList: ArrayList<PendingRtsData>? = ArrayList()

    private val _shipmentList = MutableSharedFlow<APIResultState>()
    val ShipmentList: SharedFlow<APIResultState> get() = _shipmentList


    private val _rtsPendingList = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val rtsPendingListFlow: StateFlow<APIResultState> get() = _rtsPendingList


    private val _rtsCheckAwb = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val rtsCheckAwb: StateFlow<APIResultState> get() = _rtsCheckAwb



    fun callRtsShipmentApi() {
        viewModelScope. launch {
            try {
                _rtsPendingList.value = APIResultState.Loading
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                val rtsListResult = withContext(Dispatchers.IO) {
                    rtsScanUseCase.executePendingRtsShipment(locationCode,1,20)
                }
                if (rtsListResult.status) {
                    withContext(Dispatchers.IO) {
                        serviceCenterDatabase.rtsPendingDataDao().deleteRtsShipment()
                        rtsListResult.response?.rts_list?.let {
                            pendingRtsShipmentList = ArrayList()

                            it.let { it1 ->
                                serviceCenterDatabase.rtsPendingDataDao().insertAll(
                                    it1
                                )
                            }
                        }

                    }
                    _rtsPendingList.value =
                        APIResultState.Success(rtsListResult.response)
                } else {

                    serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
                    _rtsPendingList.value =
                        APIResultState.Failure(rtsListResult.response?.description ?: "")
                }
            }
            catch (e: Exception) {
                _rtsPendingList.value = APIResultState.Failure(e.localizedMessage ?: "")
            }

        }

    }

    fun checkAwbApi(awbNo:String){
        viewModelScope.launch {
            try {
                _rtsCheckAwb.value = APIResultState.Loading
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                val rtsListResult = withContext(Dispatchers.IO) {
                    rtsScanUseCase.executePendingCheckAwb(locationCode, awbNo)
                }
                if (rtsListResult.status) {
                    _rtsCheckAwb.value= APIResultState.Success(rtsListResult.response?.rts_list?.get(0)?.awb_number)
                   }else{
                    _rtsCheckAwb.value =
                        APIResultState.Failure(rtsListResult.response?.description ?: "")

                }

            }catch (e:Exception)
            {

                APIResultState.Failure(e?.localizedMessage?:"")

            }
        }

    }




}