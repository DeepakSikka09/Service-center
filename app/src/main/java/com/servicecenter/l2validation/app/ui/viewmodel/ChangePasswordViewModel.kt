package com.servicecenter.l2validation.app.ui.viewmodel
// Code Reviewed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.LoginUseCase
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.utils.APIResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    val loginUseCase: LoginUseCase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val serviceCenterDatabase: ServiceCenterDatabase
) : ViewModel() {

    private val _changePasswordApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val  changePasswordApiflow : StateFlow<APIResultState> get()  = _changePasswordApiflow
    fun callChangePasswordApi(commonRequest: CommonRequest) {
        viewModelScope.launch {
            try {
                _changePasswordApiflow.value = APIResultState.Loading
                val changePasswordResult = withContext(Dispatchers.IO) { loginUseCase.executeChangePassword(commonRequest) }
                if (changePasswordResult.status) {
                    _changePasswordApiflow.value = APIResultState.Success(changePasswordResult.response?.description)
                } else {
                    _changePasswordApiflow.value = APIResultState.Failure(changePasswordResult.response?.description?:"")
                }
            } catch (e: Exception) {
                _changePasswordApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun setEmployeeCode():String
    {
        var result=""
        viewModelScope.launch {
            result = async {
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            }.await()
        }
        return result
    }

    fun logout() {
        viewModelScope.launch {
            try {
                preferenceDataStoreHelper.clearAllPreference()
                serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}