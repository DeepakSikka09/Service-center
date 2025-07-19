package com.servicecenter.l2validation.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.usecases.SalTallyUseCase
import com.servicecenter.l2validation.utils.APIResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MarkCompleteViewModel @Inject constructor(
    val salTallyUseCase: SalTallyUseCase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper
) : ViewModel() {
    private val _pendingList = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val pendingList: StateFlow<APIResultState> get() = _pendingList
    var isFlowRegister = false
    private val _scanShipmentFlow =
        MutableSharedFlow<APIResultState>(replay = 0)
    val scanShipmentFlow: SharedFlow<APIResultState> get() = _scanShipmentFlow

    fun callPendingShipment() {
        viewModelScope.launch {
            _pendingList.value = APIResultState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.getTallyShipment(
                        preferenceDataStoreHelper.getData(
                            PreferenceDataStoreConstants.Location_Code,
                            ""
                        ).first(),
                        preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                            .first()
                    )
                }

                if (result.status) {
                    _pendingList.value = APIResultState.Success(result.response)
                } else {
                    _pendingList.value =
                        APIResultState.Failure(result.response?.description ?: "")
                }
            } catch (ex: Exception) {
                _pendingList.value =
                    APIResultState.Failure(ex.localizedMessage ?: "")
            }
        }
    }

    fun callMarkShortage(awbList: List<Long>) {
        viewModelScope.launch {
            _scanShipmentFlow.emit(APIResultState.Loading)
            val dcCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.Location_Code, "")
                    .first()
            val empCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            try {
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.updateShortage(
                        CommonRequest(
                            dc_code = dcCode,
                            emp_code = empCode,
                            awb_list = awbList
                        )
                    )
                }

                if (result.status) {
                    _scanShipmentFlow.emit(APIResultState.Success(result.response))
                } else {
                    if (result.response == null) {
                        _scanShipmentFlow.emit(APIResultState.Failure("Something went wrong"))
                    } else {
                        result.response?.let { res ->
                            if (res.error_map.isNotEmpty()) {
                                _scanShipmentFlow.emit(APIResultState.Failure("${res.error_map.size} shipments Failed to Marked Shortage"))
                            } else {
                                _scanShipmentFlow.emit(APIResultState.Failure(res.description))
                            }
                        }
                    }

                }
            } catch (ex: Exception) {
                _scanShipmentFlow.emit(
                    APIResultState.Failure(ex.localizedMessage ?: "")
                )
            }
        }
    }
    fun updateTallyStatus(status: Int) {
        viewModelScope.launch {
            preferenceDataStoreHelper.setData(
                PreferenceDataStoreConstants.TALLY_STATUS,
                status
            )
        }
    }

    fun updateTallyMsg(description: String) {
        viewModelScope.launch {
            preferenceDataStoreHelper.setData(
                PreferenceDataStoreConstants.TALLY_MSG,
                description
            )
        }
    }
}