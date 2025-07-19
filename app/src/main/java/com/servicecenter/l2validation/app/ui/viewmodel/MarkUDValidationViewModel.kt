package com.servicecenter.l2validation.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.usecases.LoginUseCase
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
class MarkUDValidationViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val rvpScanUseCase: RVPScanUseCase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val serviceCenterDatabase: ServiceCenterDatabase
) : ViewModel() {


    private val _CutOffTimeApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val CutOffTimeApiflow: StateFlow<APIResultState> get() = _CutOffTimeApiflow






    //Cut Of time Api but there response is mismatch
   /* Response:
    {
        "status": 1,
        "message": "04:00 PM"
    }
    giving like this
    */

    fun callCutOffTimeApi() {
        viewModelScope. launch {
            try {
                _CutOffTimeApiflow.value = APIResultState.Loading
                val locationCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code, ""
                ).first()

                val shipmentListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executeCutOffTime(locationCode)
                }
                if (shipmentListResult.status) {
                    _CutOffTimeApiflow.value =
                        APIResultState.Success(shipmentListResult.response)
                } else {
                    serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
                    _CutOffTimeApiflow.value =
                        APIResultState.Failure(shipmentListResult.response?.description ?: "")
                }
            }
            catch (e: Exception) {
                _CutOffTimeApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }

    }
}