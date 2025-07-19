package com.servicecenter.l2validation.app.ui.viewmodel

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.usecases.LoginUseCase
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
@HiltViewModel
class UDOTPValidationViewModel@Inject constructor(
    private val loginUseCase: LoginUseCase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val serviceCenterDatabase: ServiceCenterDatabase
) : ViewModel()  {

    private val _sendOtpApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val sendOtpApiflow: StateFlow<APIResultState> get() = _sendOtpApiflow

    private val _resendClickable = MutableSharedFlow<Boolean>()
    val resendClickable: SharedFlow<Boolean> get() = _resendClickable

    private val _OTPTimerflow = MutableStateFlow<String>("")
    val OTPTimerflow: StateFlow<String> get() = _OTPTimerflow


    private val _verifyCodeApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val verifyCodeApiflow: StateFlow<APIResultState> get() = _verifyCodeApiflow

    fun callSendOtpApi(awb:Long,event:String,reschedule_date:String) {
        viewModelScope.launch {
            try {
                _sendOtpApiflow.value = APIResultState.Loading
                val verifyOTPResult =
                    withContext(Dispatchers.IO) { loginUseCase.executeSendReSendOtp(awb,event,reschedule_date) }
                if (verifyOTPResult.status) {
                    _sendOtpApiflow.value =
                        APIResultState.Success(verifyOTPResult.response?.description)
                } else {
                    _sendOtpApiflow.value =
                        APIResultState.Failure(verifyOTPResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _sendOtpApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun callVerifyCodeApi(commonRequest: CommonRequest) {
        viewModelScope.launch {
            try {
                _verifyCodeApiflow.value = APIResultState.Loading
                val verifyOTPResult =
                    withContext(Dispatchers.IO) { loginUseCase.executeUDOtpVerify(commonRequest) }
                if (verifyOTPResult.status) {
                    _verifyCodeApiflow.value =
                        APIResultState.Success(verifyOTPResult.response?.description)
                } else {
                    _verifyCodeApiflow.value =
                        APIResultState.Failure(verifyOTPResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _verifyCodeApiflow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun startOTPTimer(context: Context) {
        viewModelScope.launch { _resendClickable.emit(false) }
        val otpTimer = object : CountDownTimer(Constants.OTP_TIMER, Constants.OTP_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                viewModelScope.launch {
                    val formattedTime = String.format("%01d:%02d", minutes, seconds)
                    _OTPTimerflow.emit("($formattedTime)")
                }
            }

            override fun onFinish() {
                viewModelScope.launch {
                    _OTPTimerflow.emit(context.getString(R.string.resend_code))
                    _resendClickable.emit(true)
                }
            }
        }
        otpTimer.start()
    }
}