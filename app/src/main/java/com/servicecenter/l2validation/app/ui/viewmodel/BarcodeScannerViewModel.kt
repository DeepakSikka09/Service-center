package com.servicecenter.l2validation.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.data.local.entities.FilterResponse
import com.servicecenter.l2validation.data.local.entities.UDShipment
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.utils.APIResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val rvpScanUseCase: RVPScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper
) : ViewModel() {

    private var udShipment: ArrayList<UDShipment> = ArrayList()

    private val _UDShipmentList = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val udShipmentList: StateFlow<APIResultState> get() = _UDShipmentList


    private var MAX_Page_Count: Int = 0
    var currentPage = 1
    private var pagesize = 10


    fun callUDShipmentList(inputRequest: HashMap<String, Any>) {
        viewModelScope.launch {
            try {
                _UDShipmentList.value = APIResultState.Loading
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()
                val udShipmentListResult = withContext(Dispatchers.IO) {
                    inputRequest["page_no"] = currentPage
                    inputRequest["page_size"] = pagesize
                    inputRequest["dc_code"] = locationCode
                    rvpScanUseCase.executeUDShipment(commonRequest = inputRequest)
                }
                if (udShipmentListResult.status) {
                    withContext(Dispatchers.IO) {
                        MAX_Page_Count = udShipmentListResult.response?.page_count!!
                        udShipmentListResult.response?.ud_shipment?.let {
                            udShipment = ArrayList()
                            udShipment.addAll(it)
                        }
                    }
                    _UDShipmentList.value =
                        APIResultState.Success(udShipmentListResult.response)
                } else {
                        APIResultState.Failure(udShipmentListResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _UDShipmentList.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }


}