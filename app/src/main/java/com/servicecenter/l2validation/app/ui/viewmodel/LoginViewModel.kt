package com.servicecenter.l2validation.app.ui.viewmodel
// Code Reviewed
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
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
import com.servicecenter.l2validation.utils.UpdateAPKInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import io.esper.devicesdk.EsperDeviceSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val serviceCenterDatabase: ServiceCenterDatabase
) : ViewModel() {
    private var pd: ProgressDialog? = null

    private val _loginApiFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val loginApiFlow: StateFlow<APIResultState> get() = _loginApiFlow

    private val _verifyCodeApiFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val verifyCodeApiFlow: StateFlow<APIResultState> get() = _verifyCodeApiFlow

    private val _resendCodeApiFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val resendCodeApiFlow: StateFlow<APIResultState> get() = _resendCodeApiFlow

    private val _loginSessionFlow = MutableStateFlow(false)
    val loginSessionFlow: StateFlow<Boolean> get() = _loginSessionFlow

    private val _bottomClickable = MutableSharedFlow<Boolean>()
    val bottomClickable: SharedFlow<Boolean> get() = _bottomClickable

    private val _otpTimerFlow = MutableStateFlow<String>("")
    val otpTimerFlow: StateFlow<String> get() = _otpTimerFlow

    fun callLoginApi(commonRequest: CommonRequest) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        val outSdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)
        viewModelScope.launch {
            try {
                _loginApiFlow.value = APIResultState.Loading
                val loginResult =
                    withContext(Dispatchers.IO) { loginUseCase.execute(commonRequest) }
                if (loginResult.status) {
                    withContext(Dispatchers.IO) {
                        loginResult.response?.let { response ->

                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.user_name,
                                loginResult.response?.user_name ?: ""
                            )
                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.first_name,
                                loginResult.response?.first_name ?: ""
                            )


                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.last_name,
                                loginResult.response?.last_name ?: ""
                            )

                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.department_code,
                                loginResult.response?.department_code ?: ""
                            )

                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.service_center_type,
                                loginResult.response?.service_center_type ?: ""
                            )
                           var lastLogin = (sdf.parse(response.last_login)
                                ?.let { outSdf.format(it) }
                                ?: "").toString()
                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.last_login,
                                lastLogin
                            )
                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.group_code,
                                loginResult.response?.group_code?.replace("_", " ") ?: ""
                            )
                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.aUTH_TOKEN,
                                loginResult.response?.AUTH_TOKEN ?: ""
                            )
                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.EMP_CODE, loginResult.response?.user_name ?: ""
                            )
                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.Location_Code,
                                loginResult.response?.location_code ?: ""
                            )
                            preferenceDataStoreHelper.setData(
                                PreferenceDataStoreConstants.PHONE, loginResult.response?.contact_no ?: ""
                            )
                        }

                    }
                    _loginApiFlow.value = APIResultState.Success(loginResult.response?.description)

                } else {
                    _loginApiFlow.value =
                        APIResultState.Failure(loginResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _loginApiFlow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun callVerifyCodeApi(commonRequest: CommonRequest) {
        viewModelScope.launch {
            try {
                _verifyCodeApiFlow.value = APIResultState.Loading
                val verifyOTPResult =
                    withContext(Dispatchers.IO) { loginUseCase.executeOtpVerify(commonRequest) }
                if (verifyOTPResult.status) {
                    _verifyCodeApiFlow.value =
                        APIResultState.Success(verifyOTPResult.response?.description)
                    preferenceDataStoreHelper.setData(
                        PreferenceDataStoreConstants.IS_LOGGED_IN, true
                    )
                } else {
                    _verifyCodeApiFlow.value =
                        APIResultState.Failure(verifyOTPResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _verifyCodeApiFlow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun callResendCodeApi() {
        viewModelScope.launch {
            try {
                _resendCodeApiFlow.value = APIResultState.Loading
                val empCode =
                    preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "")
                        .first()
                val resendOTPResult = withContext(Dispatchers.IO) {
                    loginUseCase.executeresendLogin_Otp(
                        CommonRequest(username = empCode)
                    )
                }
                if (resendOTPResult.status) {
                    _resendCodeApiFlow.value =
                        APIResultState.Success(resendOTPResult.response?.description)
                } else {
                    _resendCodeApiFlow.value =
                        APIResultState.Failure(resendOTPResult.response?.description ?: "")
                }
            } catch (e: Exception) {
                _resendCodeApiFlow.value = APIResultState.Failure(e.localizedMessage ?: "")
            }
        }
    }

    fun exitApp(context: Context) {
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(a)
    }

    fun startOTPTimer(context: Context) {
        viewModelScope.launch { _bottomClickable.emit(false) }
        val otpTimer = object : CountDownTimer(Constants.OTP_TIMER, Constants.OTP_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                viewModelScope.launch {
                    val formattedTime = String.format("%01d:%02d", minutes, seconds)
                    _otpTimerFlow.emit("($formattedTime)")
                }
            }

            override fun onFinish() {
                viewModelScope.launch {
                    _otpTimerFlow.emit(context.getString(R.string.resend_code))
                    _bottomClickable.emit(true)
                }
            }
        }
        otpTimer.start()
    }

    fun setPhoneNumber(): String {
        var digit = ""
        viewModelScope.launch {
            digit = async {
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.PHONE, "").first()
            }.await()
        }
        return digit
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                preferenceDataStoreHelper.clearAllPreference()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Progress Dialog is deprecated
    fun downloadAPK(
        url: String?,
        sdk: EsperDeviceSDK?,
        context: Context?,
        esperSDKActivated: Boolean?
    ) {
        if (url != null) {
            pd = ProgressDialog(context)
            val downloadAndInstall: UpdateAPKInstaller = UpdateAPKInstaller()
            pd?.setCancelable(false)
            pd?.setMessage("Downloading APK File....")
            pd?.setMax(100)
            pd?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)

            downloadAndInstall.setContext(context, pd, sdk, esperSDKActivated)
            downloadAndInstall.execute(url.trim { it <= ' ' })
        }

    }
}