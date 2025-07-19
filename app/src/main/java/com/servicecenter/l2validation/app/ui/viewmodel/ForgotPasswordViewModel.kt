package com.servicecenter.l2validation.app.ui.viewmodel
// Code Reviewed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.LoginUseCase
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.utils.APIResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
) : ViewModel() {

    private val _resendCodeApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val resendCodeApiflow: StateFlow<APIResultState> get() = _resendCodeApiflow

    fun callResendCodeApi(commonRequest: CommonRequest) {
        viewModelScope.launch {
            preferenceDataStoreHelper.setData(
                PreferenceDataStoreConstants.EMP_CODE, commonRequest.username ?: ""
            )

            try {
                _resendCodeApiflow.value = APIResultState.Loading
                val resendOTPResult = withContext(Dispatchers.IO) {
                    loginUseCase.executeForgetPasswordApi(commonRequest)
                }
                if (resendOTPResult.status) {
                    _resendCodeApiflow.value =
                        APIResultState.Success(resendOTPResult.response?.description)
                } else {
                    _resendCodeApiflow.value =
                        APIResultState.Failure(resendOTPResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _resendCodeApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }
}