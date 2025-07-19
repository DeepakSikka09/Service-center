package com.servicecenter.l2validation.app.ui.activity.auth
// Code Reviewed

import android.content.Intent
import android.graphics.Paint
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.servicecenter.l2validation.BuildConfig
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.checkLocationSettingsAndRequestUpdates
import com.servicecenter.l2validation.app.extensions.handleLocationSettingsResult
import com.servicecenter.l2validation.app.extensions.hideKeyboard
import com.servicecenter.l2validation.app.extensions.requestLocationPermission
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.activity.DashboardActivity
import com.servicecenter.l2validation.app.ui.viewmodel.LoginViewModel
import com.servicecenter.l2validation.application.ForceUpdateChecker
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.DeviceDetailsRequest
import com.servicecenter.l2validation.data.remote.model.UpdateRequiredModel
import com.servicecenter.l2validation.databinding.ActivityLoginBinding
import com.servicecenter.l2validation.databinding.BottomLoginverficationcodeBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.checkGpsStatus
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.ESPER_TOKEN
import com.servicecenter.l2validation.utils.Constants.PERMISSION_REQUEST_ACCESS_LOCATION
import com.servicecenter.l2validation.utils.Constants.REQUEST_CHECK_SETTINGS
import com.servicecenter.l2validation.utils.GenericKeyEvent
import com.servicecenter.l2validation.utils.GenericOTPTextWatcher
import com.servicecenter.l2validation.utils.GenericTextWatcher
import dagger.hilt.android.AndroidEntryPoint
import io.esper.devicesdk.EsperDeviceSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding, LoginViewModel>(), View.OnClickListener {
    private lateinit var bottomSheetQc: BottomLoginverficationcodeBinding
    private lateinit var bottomSheetQcPassed: BottomSheetDialog
    private var isTimer = false
    private var sdk: EsperDeviceSDK? = null
    private var esperSDKActivated: Boolean? = null
    private var isUpdate: Boolean = false
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    @Inject
    lateinit var forceUpdateChecker: ForceUpdateChecker

    override fun getLayout(): Int {
        return R.layout.activity_login
    }

    override fun getViewModels(): Class<LoginViewModel> {
        return LoginViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocationPermission(PERMISSION_REQUEST_ACCESS_LOCATION)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L).build()
        getLatLong()
        initially()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()

            }
        })
        if (isInternetAvailable(this)) {
            updateChecker()
        } else {
            isUpdate = true
            showToast(getString(R.string.no_internet), false)
        }
        sdk = EsperDeviceSDK.getInstance(applicationContext)
        initEsperSDKActivationCheck()

        setOTPWatcher()
        fetchTimerState()
        fetchLoginApiState()
        fetchVerifyCodeApiState()
        fetchResendCodeApiState()

        sdk?.activateSDK(ESPER_TOKEN, object : EsperDeviceSDK.Callback<Void?> {
            override fun onResponse(response: Void?) {
                //Activation was successful
                esperSDKActivated = true
            }

            override fun onFailure(t: Throwable) {
                esperSDKActivated = false
            }
        })
        analyticsToAllScreen(Constants.LOGIN_EVENT)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleLocationSettingsResult(
            requestCode, resultCode, fusedLocationClient, locationRequest!!, locationCallback!!
        )
    }

    private fun initially() {
        binding.tvVersionName.text = getString(R.string.version_) + BuildConfig.VERSION_NAME
        bottomSheetQc = BottomLoginverficationcodeBinding.inflate((layoutInflater))
        binding.tvForgetPwd.setOnClickListener(this)
        binding.btnGetCode.setOnClickListener(this)
        bottomSheetQc.btnVerifyCode.setOnClickListener(this)
        bottomSheetQc.tvResend.setOnClickListener(this)
        binding.etEmployeeCode.addTextChangedListener(
            GenericTextWatcher(
                this, ::validation, binding.btnGetCode
            )
        )
        binding.etPassword.addTextChangedListener(
            GenericTextWatcher(
                this, ::validation, binding.btnGetCode
            )
        )
    }

    private fun validation(): Boolean {
        return binding.etEmployeeCode.text.toString()
            .isNotBlank() && binding.etPassword.text.toString().isNotBlank()
    }

    private fun validationOTP(): Boolean {
        return bottomSheetQc.edOtp1.text?.isNotBlank() == true && bottomSheetQc.edOtp2.text?.isNotBlank() == true && bottomSheetQc.edOtp3.text?.isNotBlank() == true && bottomSheetQc.edOtp4.text?.isNotBlank() == true && bottomSheetQc.edOtp5.text?.isNotBlank() == true && bottomSheetQc.edOtp6.text?.isNotBlank() == true
    }

    private fun fetchLoginApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.loginApiFlow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        if (!this@LoginActivity::bottomSheetQcPassed.isInitialized) {
                            showVerificationCodeDialog()
                        }
                        otpInitially()
                        bottomSheetQcPassed.setCancelable(false)
                        viewModel.startOTPTimer(this@LoginActivity)
                        setPhoneNumber()
                        bottomSheetQcPassed.show()

                    }

                    else -> {
                        manageApiFlowStatus(apiResultState = it, true)
                    }
                }
            }
        }
    }

    private fun setPhoneNumber() {
        val fullPhoneNumber = viewModel.setPhoneNumber()
        val last4Digits = fullPhoneNumber.takeLast(4)
        val maskedPhoneNumber = "*".repeat(fullPhoneNumber.length - 4) + last4Digits

        bottomSheetQc.tvDescription.text = buildString {
            append(getString(R.string.enter_the_6_digit))
            append(" ")
            append(maskedPhoneNumber)
        }
    }

    private fun fetchVerifyCodeApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.verifyCodeApiFlow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        showToast(message = it.data as String, true)
                        Log.e("crash ","flow collected again")
                        startScreen(DashboardActivity())
                        bottomSheetQcPassed.dismiss()
                    }

                    else -> {
                        manageApiFlowStatus(apiResultState = it, true)
                    }
                }
            }
        }
    }

    private fun fetchResendCodeApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.resendCodeApiFlow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        showToast(message = it.data as String, true)
                    }

                    else -> {
                        manageApiFlowStatus(apiResultState = it, true)
                    }
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.tv_forget_pwd -> {
                startScreen(ForgetPasswordActivity())
                analyticsToAllButton(
                    Constants.FORGOT_PASSWORD, Constants.BUTTON_KEY, Constants.FORGOT_PASSWORD
                )
            }

            R.id.btn_get_code -> {
                if (checkGpsStatus(this)) {
                    if (validation()) {
                        if (isInternetAvailable(this)) {
                            analyticsToAllButton(
                                Constants.GET_CODE, Constants.BUTTON_KEY, Constants.GET_CODE
                            )
                            hideKeyboard(this)
                            if (isUpdate) {
                                updateChecker()
                                return
                            }
                            val deviceDetails = DeviceDetailsRequest(
                                sdk_version_code = BuildConfig.VERSION_NAME,//Sending sdk_version_code -> APP version code
                                manufacturer = Build.MANUFACTURER,
                                sdk_version = Build.VERSION.RELEASE,//Sending sdk_version-> Android Version
                                model_number = Build.MODEL,
                                latitude = latitude,
                                longitude = longitude
                            )
                            val commonRequest = CommonRequest(
                                username = binding.etEmployeeCode.text.toString(),
                                password = binding.etPassword.text.toString(),
                                //adding the employee Device Information
                                device_info = deviceDetails
                            )
                            viewModel.clearAll()
                            viewModel.callLoginApi(commonRequest)
                        } else {
                            showToast(getString(R.string.no_internet), false)
                        }
                    }
                } else {
                    showToast("Please Turn On GPS Location", false)
                    checkLocationSettingsAndRequestUpdates(
                        fusedLocationClient,
                        locationRequest!!,
                        locationCallback!!,
                        REQUEST_CHECK_SETTINGS
                    )
                }
            }

            R.id.btn_verify_code -> {
                analyticsToAllButton(
                    Constants.VERIFY_CODE, Constants.BUTTON_KEY, Constants.VERIFY_CODE
                )
                val otp =
                    bottomSheetQc.edOtp1.text.toString() + "" + bottomSheetQc.edOtp2.text.toString() + "" + bottomSheetQc.edOtp3.text.toString() + "" + bottomSheetQc.edOtp4.text.toString() + "" + bottomSheetQc.edOtp5.text.toString() + "" + bottomSheetQc.edOtp6.text.toString()
                val otpVerify = CommonRequest(
                    username = binding.etEmployeeCode.text.toString(),
                    otp = otp,
                    password = binding.etPassword.text.toString()
                )
                if (validationOTP()) {
                    if (isInternetAvailable(this)) {
                        viewModel.callVerifyCodeApi(otpVerify)
                    } else {
                        showToast(getString(R.string.no_internet), false)
                    }
                }
            }

            R.id.tv_resend -> {
                analyticsToAllButton(
                    Constants.RESEND_OTP_LOGIN, Constants.BUTTON_KEY, Constants.RESEND_OTP_LOGIN
                )
                if (isTimer) {
                    viewModel.startOTPTimer(this)
                    bottomSheetQc.tvResend.setTextColor(
                        resources.getColor(
                            R.color.black_1A, this@LoginActivity.theme
                        )
                    )
                    bottomSheetQc.tvResend.paintFlags = Paint.LINEAR_TEXT_FLAG
                    if (isInternetAvailable(this)) {
                        viewModel.callResendCodeApi()
                    } else {
                        showToast(getString(R.string.no_internet), false)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomClickable()
        checkLocationSettingsAndRequestUpdates(
            fusedLocationClient, locationRequest!!, locationCallback!!, REQUEST_CHECK_SETTINGS
        )
    }

     fun backPress() {
        viewModel.exitApp(this)
    }


    private fun showVerificationCodeDialog() {
        bottomSheetQcPassed = BottomSheetDialog(this, R.style.otpsheetDialogTheme)
        bottomSheetQcPassed.setContentView(bottomSheetQc.root)
    }

    private fun setOTPWatcher() {
        bottomSheetQc.edOtp1.addTextChangedListener(
            GenericOTPTextWatcher(
                bottomSheetQc.edOtp1, bottomSheetQc.edOtp2
            )
        )
        bottomSheetQc.edOtp2.addTextChangedListener(
            GenericOTPTextWatcher(
                bottomSheetQc.edOtp2, bottomSheetQc.edOtp3
            )
        )
        bottomSheetQc.edOtp3.addTextChangedListener(
            GenericOTPTextWatcher(
                bottomSheetQc.edOtp3, bottomSheetQc.edOtp4
            )
        )
        bottomSheetQc.edOtp4.addTextChangedListener(
            GenericOTPTextWatcher(
                bottomSheetQc.edOtp4, bottomSheetQc.edOtp5
            )
        )
        bottomSheetQc.edOtp5.addTextChangedListener(
            GenericOTPTextWatcher(
                bottomSheetQc.edOtp5, bottomSheetQc.edOtp6
            )
        )
        bottomSheetQc.edOtp6.addTextChangedListener(
            GenericOTPTextWatcher(
                bottomSheetQc.edOtp6, null
            )
        )
        bottomSheetQc.edOtp1.setOnKeyListener(GenericKeyEvent(bottomSheetQc.edOtp1, null))
        bottomSheetQc.edOtp2.setOnKeyListener(
            GenericKeyEvent(
                bottomSheetQc.edOtp2, bottomSheetQc.edOtp1
            )
        )
        bottomSheetQc.edOtp3.setOnKeyListener(
            GenericKeyEvent(
                bottomSheetQc.edOtp3, bottomSheetQc.edOtp2
            )
        )
        bottomSheetQc.edOtp4.setOnKeyListener(
            GenericKeyEvent(
                bottomSheetQc.edOtp4, bottomSheetQc.edOtp3
            )
        )
        bottomSheetQc.edOtp5.setOnKeyListener(
            GenericKeyEvent(
                bottomSheetQc.edOtp5, bottomSheetQc.edOtp4
            )
        )
        bottomSheetQc.edOtp6.setOnKeyListener(
            GenericKeyEvent(
                bottomSheetQc.edOtp6, bottomSheetQc.edOtp5
            )
        )
    }

    private fun otpInitially() {
        bottomSheetQc.edOtp1.addTextChangedListener(
            GenericTextWatcher(
                this, ::validationOTP, bottomSheetQc.btnVerifyCode
            )
        )
        bottomSheetQc.edOtp2.addTextChangedListener(
            GenericTextWatcher(
                this, ::validationOTP, bottomSheetQc.btnVerifyCode
            )
        )
        bottomSheetQc.edOtp3.addTextChangedListener(
            GenericTextWatcher(
                this, ::validationOTP, bottomSheetQc.btnVerifyCode
            )
        )
        bottomSheetQc.edOtp4.addTextChangedListener(
            GenericTextWatcher(
                this, ::validationOTP, bottomSheetQc.btnVerifyCode
            )
        )
        bottomSheetQc.edOtp5.addTextChangedListener(
            GenericTextWatcher(
                this, ::validationOTP, bottomSheetQc.btnVerifyCode
            )
        )
        bottomSheetQc.edOtp6.addTextChangedListener(
            GenericTextWatcher(
                this, ::validationOTP, bottomSheetQc.btnVerifyCode
            )
        )

    }

    private fun fetchTimerState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.otpTimerFlow.collectLatest {
                bottomSheetQc.tvResend.text = it

            }
        }
    }

    private fun bottomClickable() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.bottomClickable.collectLatest {
                bottomSheetQcPassed.setCancelable(it)
                if (it) {
                    bottomSheetQc.tvResend.setTextColor(
                        resources.getColor(
                            R.color.blue_18, this@LoginActivity.theme
                        )
                    )
                    bottomSheetQc.tvResend.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                } else {
                    bottomSheetQc.tvResend.setTextColor(
                        resources.getColor(
                            R.color.black_1A, this@LoginActivity.theme
                        )
                    )
                    bottomSheetQc.tvResend.paintFlags = Paint.LINEAR_TEXT_FLAG
                }
                isTimer = it
            }
        }
    }

    // Need to be move in extension function
    private fun showUpdateDialog(updateRequiredModel: UpdateRequiredModel) {
        AlertDialog.Builder(this).setTitle(getString(R.string.update_required))
            .setMessage(updateRequiredModel.description).setCancelable(false)
            .setPositiveButton(getString(R.string.update)) { _, _ ->
                startDownload(updateRequiredModel)
            }.show()
    }

    // Migrate to Viewmodel
    private fun startDownload(updateRequiredModel: UpdateRequiredModel) {
        viewModel.downloadAPK(
            updateRequiredModel.updateUrl, sdk, this@LoginActivity, esperSDKActivated
        )
    }

    private fun initEsperSDKActivationCheck() {
        // Check whether sdk is activated or not
        sdk!!.isActivated(object : EsperDeviceSDK.Callback<Boolean?> {
            override fun onResponse(isActive: Boolean?) {
                if (isActive!!) {
                    Log.d("TAG", "isEsperSDKActivated: SDK is activated")
                } else {
                    Log.d("TAG", "isEsperSDKActivated: SDK is not activated")
                }
            }

            override fun onFailure(t: Throwable) {
                Log.d("check status", t.toString())
            }
        })
    }

    private fun updateChecker() {
        forceUpdateChecker.checkForceUpdateRequired { updateRequiredModel ->
            if (updateRequiredModel != null) {
                showUpdateDialog(updateRequiredModel)
            }
        }
    }

    private fun getLatLong() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location? = locationResult.lastLocation
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    getStopLocation(locationCallback)
                    Log.e("LocationData", "Latitude: $latitude, Longitude: $longitude")
                } else {
                    Log.e("LocationData", "Location is null")
                }
            }
        }
    }

    private fun getStopLocation(locationCallback: LocationCallback?) {
        try {
            val voidTask: Task<Void> = fusedLocationClient.removeLocationUpdates(locationCallback!!)
            voidTask.addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("Location", "stopMonitoring: removeLocationUpdates successful.")
                } else {
                    Log.d("Location", "stopMonitoring: removeLocationUpdates updates unsuccessful! " + voidTask.toString()
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.d("Location", "stopMonitoring: Security exception.")
        }
    }

}
