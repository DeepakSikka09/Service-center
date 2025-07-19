package com.servicecenter.l2validation.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
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


@HiltViewModel
class FilterViewModel @Inject constructor(
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val rvpScanUseCase: RVPScanUseCase
): ViewModel() {

    private val _filterListApiFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val filterListApiFlow: StateFlow<APIResultState> get() = _filterListApiFlow


   /* fun callFilterApi() {
        viewModelScope. launch {
            try {
                _filterListApiFlow.value = APIResultState.Loading
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                val shipmentListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executeFilterApi(locationCode)
                }
                if (shipmentListResult.status) {

                    _filterListApiFlow.value = APIResultState.Success(shipmentListResult.response)
                } else {
                    _filterListApiFlow.value = APIResultState.Failure(shipmentListResult.response?.description ?: "")
                }
            }
            catch (e: Exception) {
                _filterListApiFlow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }

        }

    }*/
}
