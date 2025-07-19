package com.servicecenter.l2validation.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.utils.APIResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ShipmentListViewModel @Inject constructor(
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
) : ViewModel() {
    private val _shipmentList = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val shipmentList: StateFlow<APIResultState> get() = _shipmentList


    fun getShipmentList(s: String) {
        viewModelScope.launch() {
            try {
                _shipmentList.value = APIResultState.Loading
                val flyerlinkingresult = withContext(Dispatchers.IO) {
                    serviceCenterDatabase.shipmentDetailDao().getStatusWiseShipmentList(s)
                }
                _shipmentList.value = APIResultState.Success(flyerlinkingresult)
            } catch (e: Exception) {
                _shipmentList.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }
}