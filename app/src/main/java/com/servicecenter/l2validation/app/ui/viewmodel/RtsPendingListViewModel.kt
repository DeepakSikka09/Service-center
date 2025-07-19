package com.servicecenter.l2validation.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.domain.usecases.RtsScanUseCase
import com.servicecenter.l2validation.utils.APIResultState
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
class RtsPendingListViewModel @Inject constructor(
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val rtsScanUseCase: RtsScanUseCase

) : ViewModel() {
    var came_from = ""
    var currentPage = 1
    private var isLoading = false
    private var MAX_Page_Count = 0
    var toDate = ""
    var fromDate = ""
    var sorting = ""

    private val _rtsPendingList = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val rtsPendingList: StateFlow<APIResultState> get() = _rtsPendingList

    private val _isLoadingBar = MutableStateFlow<Boolean>(false)
    val loadingBar: StateFlow<Boolean> get() = _isLoadingBar


    private val _rtsCheckAwb = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val rtsCheckAwb: StateFlow<APIResultState> get() = _rtsCheckAwb


    fun getRtsPendingListData(
        currentPage: Int,
        toDate: String? = this.toDate,
        fromDate: String? = this.fromDate,
        sorting: String? = this.sorting
    ) {
        viewModelScope.launch {
            try {
                _rtsPendingList.value = APIResultState.Loading
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                val rtsListResult = withContext(Dispatchers.IO) {
                    rtsScanUseCase.executePendingRtsShipment(
                        locationCode,
                        currentPage,
                        15,
                        toDate = toDate,
                        fromDate = fromDate,
                        sorting = sorting
                    )
                }
                if (rtsListResult.status) {
                    var size = 0
                    withContext(Dispatchers.IO) {
                        //  serviceCenterDatabase.rtsPendingDataDao().deleteRtsShipment()

                        MAX_Page_Count = rtsListResult.response?.page_count!!

                        rtsListResult.response?.rts_list?.let {

                            it.let { it1 ->
                                serviceCenterDatabase.rtsPendingDataDao().insertAll(
                                    it1
                                )
                            }
                            size = it.size
                        }

                    }
                    _rtsPendingList.value = APIResultState.Success(rtsListResult.response?.rts_list)
                    isLoading = size < 15
                    _isLoadingBar.value = false
                } else {
                    //_isLoadingBar.value=true
                    isLoading = true

                    serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
                    _rtsPendingList.value =
                        APIResultState.Failure(rtsListResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _rtsPendingList.value = APIResultState.Failure(e.localizedMessage ?: "")
            }

        }

    }

    fun loadNextPage() {
        if (!isLoading) {
            isLoading = true
            _isLoadingBar.value = true

            currentPage++
            if (currentPage <= MAX_Page_Count) {
                getRtsPendingListData(currentPage)

            } else {
                _isLoadingBar.value = false
            }

        } else {
            _isLoadingBar.value = false
        }
    }


    //for pagination
    fun checkAwbApi(awbNo: String) {
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
                    _rtsCheckAwb.value =
                        APIResultState.Success(rtsListResult.response)
                } else {
                    _rtsCheckAwb.value =
                        APIResultState.Failure(rtsListResult.response?.description ?: "")

                }

            } catch (e: Exception) {
                Log.d("failure---->", e.toString())
                APIResultState.Failure(e.localizedMessage ?: "")

            }
        }

    }

    fun setDate(toDate: String, fromDate: String, sorting: String) {
        this.toDate = toDate
        this.fromDate = fromDate
        this.sorting = sorting
        currentPage = 1
        came_from = Constants.FILTER
        getRtsPendingListData(currentPage, toDate = toDate, fromDate = fromDate, sorting = sorting)
        Log.d(
            "date---->",
            "toDate-->" + toDate + "__" + "fromDate-->" + fromDate + "--sorting--" + sorting
        )
    }

    fun resetCurrentPage() {
        currentPage = 1 // Reset ViewModel's current page tracker
    }


}


