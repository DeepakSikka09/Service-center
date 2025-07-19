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
class UDSharedViewModel @Inject constructor(
    private val rvpScanUseCase: RVPScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper
) : ViewModel() {
     var filterData: ArrayList<FilterResponse> = ArrayList()

    private var udShipment: ArrayList<UDShipment> = ArrayList()

    private val _UDShipmentList = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val udShipmentList: StateFlow<APIResultState> get() = _UDShipmentList

    private val _filterListApiFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val filterListApiFlow: StateFlow<APIResultState> get() = _filterListApiFlow

    private val _isLoadingBar = MutableStateFlow<Boolean>(false)
    val loadingBar: StateFlow<Boolean> get() = _isLoadingBar

    private val _shipment = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val shipment: StateFlow<APIResultState> get() = _shipment

    private var isLoading = false
    private var MAX_Page_Count: Int = 0

    var currentPage = 1
    private var pagesize = 10
    var isListScrolled = false
    var filterDataList = false

    private lateinit var commonRequest: CommonRequest

    val filteredData: HashMap<String, MutableSet<String>> = HashMap()

    fun addFilterData(filterResponses: List<FilterResponse>) {
        for (response in filterResponses) {
            val key_id = response.key_id
            val valueSet = mutableSetOf<String>()

            for (subData in response.value) {
                if (subData.filter_checked) {
                    valueSet.add(subData.filter_code)
                }
            }
            if (valueSet.isNotEmpty())
                filteredData.put(key_id, valueSet)
            else
                filteredData.remove(key_id)
        }
    }

    fun loadNextPage(udStatus: String) {
        if (!isLoading) {
            isLoading = true
            _isLoadingBar.value = true

            currentPage++
            Log.d("currentPagecountss", MAX_Page_Count.toString())
            if (currentPage <= MAX_Page_Count) {
                val requestSending = HashMap<String, Any>()
                for (data in filteredData) {
                    requestSending.put(data.key, data.value)
                }
                requestSending["page_no"] = currentPage
                requestSending["request_flag"] = udStatus

                callUDShipmentListApi(requestSending)
            } else {
                _isLoadingBar.value = false
            }

        } else {
            _isLoadingBar.value = false
        }
    }


    fun callUDShipmentListApi(inputRequest: HashMap<String, Any>) {
        // this.commonRequest = commonRequest
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
                    var size = 0
                    withContext(Dispatchers.IO) {
                        MAX_Page_Count = udShipmentListResult.response?.page_count!!
                        udShipmentListResult.response?.ud_shipment?.let {
                            udShipment = ArrayList()
                            udShipment?.addAll(it)
                            size = it.size
                        }
                    }
                    _UDShipmentList.value =
                        APIResultState.Success(udShipmentListResult.response)
                    isLoading = size < 10
                    _isLoadingBar.value = false
                } else {
                    isLoading = true
                    _UDShipmentList.value =
                        APIResultState.Failure(udShipmentListResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _UDShipmentList.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun callFilterApi(udStatus: String) {
        viewModelScope.launch {
            try {
                _filterListApiFlow.value = APIResultState.Loading
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                val shipmentListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executeFilterApi(locationCode, udStatus)
                }
                if (shipmentListResult.status) {

                    _filterListApiFlow.value = APIResultState.Success(shipmentListResult.response)
                } else {
                    _filterListApiFlow.value =
                        APIResultState.Failure(shipmentListResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _filterListApiFlow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }

        }

    }

    fun resetTheCurrentPage(currentpage: Int) {
        currentPage = currentpage
        //isLoading = false
    }


    fun clearFilteredList() {

        for (response in filteredData) {
            val key_id = response.key
            val valueSet = mutableSetOf<String>()
            if (valueSet.isNotEmpty())
                filteredData.put(key_id, valueSet)
            else
                filteredData.remove(key_id)

        }
    }
    fun clearFilterchecked() {

        for (i in 0 until filterData.size) {
            for (j in 0 until filterData[i].value.size) {
                filterData[i].value[j].filter_checked = false

            }
        }
        addFilterData(filterData)
    }

}