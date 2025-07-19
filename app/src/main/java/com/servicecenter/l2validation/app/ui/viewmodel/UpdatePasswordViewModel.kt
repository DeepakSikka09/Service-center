package com.servicecenter.l2validation.app.ui.viewmodel
// Code Reviewed
import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.LoginUseCase
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants
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
class UpdatePasswordViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase, val preferenceDataStoreHelper: PreferenceDataStoreHelper
) : ViewModel() {

    private val _createPasswordApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val createPasswordApiflow: StateFlow<APIResultState> get() = _createPasswordApiflow

    private val _OTPTimerflow = MutableStateFlow<String>("")
    val OTPTimerflow: StateFlow<String> get() = _OTPTimerflow

    private val _resendCodeApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val resendCodeApiflow: StateFlow<APIResultState> get() = _resendCodeApiflow


    fun callCreatePasswordApi(commonRequest: CommonRequest) {
        viewModelScope.launch {
            try {
                _createPasswordApiflow.value = APIResultState.Loading
                val verifyOTPResult =
                    withContext(Dispatchers.IO) { loginUseCase.executePassword(commonRequest) }
                if (verifyOTPResult.status) {
                    _createPasswordApiflow.value =
                        APIResultState.Success(verifyOTPResult.response?.description)
                } else {
                    _createPasswordApiflow.value =
                        APIResultState.Failure(verifyOTPResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _createPasswordApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun setEmployeeCode(): String {
        var result = ""
        viewModelScope.launch {
            result = async {
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            }.await()
        }
        return result
    }

    fun callResendCodeApi() {
        viewModelScope.launch {
            try {
                _resendCodeApiflow.value = APIResultState.Loading
                val username =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val resendOTPResult = withContext(Dispatchers.IO) {

                    loginUseCase.executeForgetPasswordApi(
                        CommonRequest(username = username)
                    )
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


    fun startOTPTimer(context: Context) {
        val otpTimer = object : CountDownTimer(Constants.OTP_TIMER, Constants.OTP_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                viewModelScope.launch {
                    val formattedTime = String.format("%02d:%02d", minutes, seconds)
                    _OTPTimerflow.emit("($formattedTime)")
                }
            }

            override fun onFinish() {
                viewModelScope.launch {
                    _OTPTimerflow.emit(context.getString(R.string.resend_code))
                }
            }
        }
        otpTimer.start()
    }
}