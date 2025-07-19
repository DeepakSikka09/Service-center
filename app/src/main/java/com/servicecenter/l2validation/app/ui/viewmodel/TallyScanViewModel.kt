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
class TallyScanViewModel @Inject constructor(
    val salTallyUseCase: SalTallyUseCase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper
) : ViewModel() {
    var isFlowRegister: Boolean = false
    private val _shipmentDetailApiFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val shipmentDetailApiFlow: StateFlow<APIResultState> get() = _shipmentDetailApiFlow

    private val _startReconFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val startReconFlow: StateFlow<APIResultState> get() = _startReconFlow

    private val _scanShipmentFlow = MutableSharedFlow<APIResultState>(replay = 0)
    val scanShipmentFlow: SharedFlow<APIResultState> get() = _scanShipmentFlow

    private val _cancelReckonFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val cancelReconFlow: StateFlow<APIResultState> get() = _cancelReckonFlow

    private val _shipmentCount = MutableStateFlow(Pair(0, 0))
    val shipmentCount: StateFlow<Pair<Int, Int>> get() = _shipmentCount

    private val _doneReconFlow = MutableSharedFlow<APIResultState>(replay = 0)
    val doneReconFlow: SharedFlow<APIResultState> get() = _doneReconFlow

    fun shipmentDetailApi() {
        viewModelScope.launch {
            val dcCode =
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code,
                    ""
                )
                    .first()

            val empCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            _shipmentDetailApiFlow.value = APIResultState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.getTallyShipment(
                        dcCode,
                        empCode
                    )
                }

                if (result.status) {
                    _shipmentDetailApiFlow.value = APIResultState.Success(result.response)
                } else {
                    _shipmentDetailApiFlow.value =
                        APIResultState.Failure(result.response?.description ?: "")

                }
            } catch (ex: Exception) {
                _shipmentDetailApiFlow.value =
                    APIResultState.Failure(ex.localizedMessage ?: "")
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

    fun startRecon() {
        viewModelScope.launch {
            try {
                _startReconFlow.value = APIResultState.Loading
                val dcCode =
                    preferenceDataStoreHelper.getData(
                        PreferenceDataStoreConstants.Location_Code,
                        ""
                    )
                        .first()

                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()

                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.reconStart(
                        CommonRequest(
                            dc_code = dcCode,
                            emp_code = empCode
                        )
                    )
                }

                if (result.status) {
                    _startReconFlow.value = APIResultState.Success(result.response)
                } else {
                    _startReconFlow.value =
                        APIResultState.Failure(result.response?.description ?: "")
                }
            } catch (ex: Exception) {
                _startReconFlow.value =
                    APIResultState.Failure(ex.localizedMessage ?: "")
            }
        }
    }

    fun scanShipment(awbNo: Long) {
        viewModelScope.launch {
            _scanShipmentFlow.emit(APIResultState.Loading)
            val dcCode =
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code,
                    ""
                )
                    .first()

            val empCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            try {
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.updateShipment(
                        CommonRequest(
                            dc_code = dcCode,
                            emp_code = empCode,
                            awb_no = awbNo
                        )
                    )
                }

                if (result.status) {
                    _scanShipmentFlow.emit(APIResultState.Success(result.response))
                } else {
                    result.response?.let { response ->
                        _scanShipmentFlow.emit(
                            APIResultState.Failure(data = response, description = "")
                        )
                    }

                }
            } catch (ex: Exception) {
                _scanShipmentFlow.emit(
                    APIResultState.Failure(awbNo.toString())
                )
            }
        }

    }

    fun callCancelRecon() {
        viewModelScope.launch {
            _cancelReckonFlow.value = APIResultState.Loading
            val dcCode =
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code,
                    ""
                )
                    .first()

            val empCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            try {
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.reconCancel(
                        CommonRequest(
                            dc_code = dcCode,
                            emp_code = empCode
                        )
                    )
                }

                if (result.status) {
                    _cancelReckonFlow.value = APIResultState.Success(result.response)
                } else {
                    _cancelReckonFlow.value =
                        APIResultState.Failure(result.response?.description ?: "")
                }
            } catch (ex: Exception) {
                _cancelReckonFlow.value =
                    APIResultState.Failure(ex.localizedMessage ?: "")
            }
        }
    }


    fun setShipmentCount(scannedShipment: Int, totalShipment: Int) {
        _shipmentCount.value = Pair(scannedShipment, totalShipment)
    }

    fun callCompleteRecon() {
        viewModelScope.launch {
            _doneReconFlow.emit(APIResultState.Loading)
            val dcCode =
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code,
                    ""
                )
                    .first()

            val empCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            try {
                val result = withContext(Dispatchers.IO) {
                    salTallyUseCase.reconDone(
                        CommonRequest(
                            dc_code = dcCode,
                            emp_code = empCode
                        )
                    )
                }

                if (result.status) {
                    _doneReconFlow.emit(APIResultState.Success(result.response))
                } else {
                    _doneReconFlow.emit(APIResultState.Failure(result.response?.description ?: ""))
                }
            } catch (ex: Exception) {
                _doneReconFlow.emit(
                    APIResultState.Failure(ex.localizedMessage ?: "")
                )
            }
        }
    }

    fun isExceptionMarkAvailable(): Boolean {
        var result = false
        viewModelScope.launch {
            result = withContext(Dispatchers.IO) {
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.IS_EXCEPTION_MARK,
                    false
                ).first()
            }
        }
        return result
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