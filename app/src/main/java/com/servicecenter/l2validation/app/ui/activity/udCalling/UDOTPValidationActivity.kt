package com.servicecenter.l2validation.app.ui.activity.udCalling

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.extensions.maskMobileNumber
import com.servicecenter.l2validation.app.extensions.navigateToActivity
import com.servicecenter.l2validation.app.extensions.setOtpWatcher
import com.servicecenter.l2validation.app.ui.activity.rvpShipment.CommitActivity
import com.servicecenter.l2validation.app.ui.viewmodel.UDOTPValidationViewModel
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.databinding.FragmentUdotpvalidationBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.GenericTextWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class UDOTPValidationActivity : BaseActivity<FragmentUdotpvalidationBinding,UDOTPValidationViewModel>(), View.OnClickListener {
    override fun getLayout(): Int =R.layout.fragment_udotpvalidation

    override fun getViewModels(): Class<UDOTPValidationViewModel> {
       return UDOTPValidationViewModel::class.java
    }
    private var orderID: String? = null
    private var awbNumber: Long = 0L
    private var drsId: String? = null
    private var paymentType: String? = null
    private var isUdCalling: Boolean = true
    private var event: String = ""
    private var selectedDate: String = ""
    private var isTimer = false
    private var feNumber: String? = ""
    private var udType: String? = null
    private var requestType: String? = null
    private  var clientCorrelationId:String?=null
    private  var rescheduleRemarks:String?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
        setContentView(binding.root)
        if (isInternetAvailable(this)) {
            viewModel.callSendOtpApi(awbNumber, event, selectedDate)
            callSendReSendOtpApiState()
            viewModel.startOTPTimer(this)
        } else {
            showToast(getString(R.string.no_internet), false)
        }
        setOtpWatcher(
            binding.edOtp1, binding.edOtp2, binding.edOtp3,
            binding.edOtp4, binding.edOtp5, binding.edOtp6
        )
        otpInitially()
        fetchVerifyCodeApiState()
        setRemarkWatcher()


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()

            }
        })
    }

    private fun initialize() {
        orderID = intent.getStringExtra(Constants.ORDER_ID)
        val awbNumberString = intent.getStringExtra(Constants.AWB_NUMBER)
        awbNumber = awbNumberString?.toLongOrNull() ?: 0L
        drsId = intent.getStringExtra(Constants.DRS_ID)
        paymentType = intent.getStringExtra(Constants.PAYMENT_TYPE)
        selectedDate = intent.getStringExtra(Constants.SELECTED_DATE).toString()
        event = intent.getStringExtra(Constants.EVENT).toString()
        feNumber = intent.getStringExtra(Constants.FE_NUMBER)
        udType = intent.getStringExtra(Constants.UD_TYPE)
        requestType = intent.getStringExtra(Constants.REQUEST_TYPE)
        clientCorrelationId = intent.getStringExtra(Constants.CLIENT_CORRELATION_ID)
        rescheduleRemarks = intent.getStringExtra(Constants.RESCHEDULE_REMARKS)

        binding.btnVerifyCode.setOnClickListener(this)
        binding.buttonBack.setOnClickListener(this)
        binding.cbOtp.setOnClickListener(this)
        binding.tvResend.setOnClickListener(this)
        binding.awbDetails.tvAwbNo.text = awbNumber.toString()
        binding.awbDetails.tvOrderNo.text = orderID
        binding.awbDetails.orderType.text = paymentType
        val maskedMobileNumber = feNumber?.maskMobileNumber()


        binding.tvDescription.text =
            getString(
                R.string.please_ask_the_consignee_to_share_the_re_attempt_code_sent_to_mobile_number,
                maskedMobileNumber
            )
        Log.d("selectedDates", "$awbNumber")

    }


    private fun otpInitially() {
        binding.edOtp1.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validationOTP,
                binding.btnVerifyCode
            )
        )
        binding.edOtp2.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validationOTP,
                binding.btnVerifyCode
            )
        )
        binding.edOtp3.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validationOTP,
                binding.btnVerifyCode
            )
        )
        binding.edOtp4.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validationOTP,
                binding.btnVerifyCode
            )
        )
        binding.edOtp5.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validationOTP,
                binding.btnVerifyCode
            )
        )
        binding.edOtp6.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validationOTP,
                binding.btnVerifyCode
            )
        )
    }

    private fun validationOTP(): Boolean {
        return binding.edOtp1.text?.isNotBlank() == true && binding.edOtp2.text?.isNotBlank() == true
                && binding.edOtp3.text?.isNotBlank() == true && binding.edOtp4.text?.isNotBlank() == true
                && binding.edOtp5.text?.isNotBlank() == true && binding.edOtp6.text?.isNotBlank() == true
    }

    private fun callVerifyCodeApi(otpVerify: CommonRequest) {
        if (isInternetAvailable(this)) {
            viewModel.callVerifyCodeApi(otpVerify)
        } else {
            showToast(getString(R.string.no_internet), false)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_verify_code -> {
                if (binding.cbOtp.isChecked) {
                    if (binding.etRemarks.text.toString().isNotEmpty()) {
                        val extras = Bundle().apply {
                            putString(Constants.AWB_NUMBER, awbNumber.toString())
                            putString(Constants.DRS_ID, drsId.toString())
                            putString(Constants.REMARK, binding.etRemarks.text.toString())
                            putBoolean(Constants.Is_UdCalling, isUdCalling)
                            putString(Constants.UD_TYPE, udType)
                            putString(Constants.REQUEST_TYPE, requestType)
                            putString(Constants.SELECTED_DATE, selectedDate)
                            putString(Constants.CLIENT_CORRELATION_ID, clientCorrelationId)
                            putString(Constants.RESCHEDULE_REMARKS,rescheduleRemarks)

                        }
                        navigateToActivity(UDCallingActivity::class.java)
                    } else {
                        showToast(getString(R.string.remarks_is_mandatory), false)
                    }
                } else {
                    if (validationOTP()) {
                        val otp = binding.edOtp1.text.toString() + binding.edOtp2.text.toString() +
                                binding.edOtp3.text.toString() + binding.edOtp4.text.toString() +
                                binding.edOtp5.text.toString() + binding.edOtp6.text.toString()
                        val otpVerify = CommonRequest(
                            airwaybill_number = awbNumber.toString(),
                            app_code = "SCA",
                            otp = otp,
                            remarks = null //otp remarks
                        )
                        callVerifyCodeApi(otpVerify)
                    } else {
                        showToast(getString(R.string.please_enter_the_otp), false)
                    }
                }
            }
            R.id.buttonBack->{finish()}

            R.id.cb_otp -> {

                if (binding.cbOtp.isChecked) {
                    checkbox()
                } else {
                    resetOtpFields()
                    binding.tvResend.isEnabled = true
                }
            }

            R.id.tv_resend -> {
                if (isTimer) {
                    viewModel.startOTPTimer(this)
                    binding.tvResend.setTextColor(
                        resources.getColor(
                            R.color.black_1A,
                            this@UDOTPValidationActivity.theme
                        )
                    )
                    binding.tvResend.paintFlags = Paint.LINEAR_TEXT_FLAG
                    if (isInternetAvailable(this)) {
                        viewModel.callSendOtpApi(awbNumber, event, selectedDate)
                    } else {
                        showToast(getString(R.string.no_internet), false)
                    }
                }
            }
        }
    }

    private fun fetchTimerState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.OTPTimerflow.collectLatest {
                binding.tvResend.text = it
            }
        }
    }

    private fun setRemarkWatcher() {
        binding.etRemarks.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                val isRemarkFilled = s?.isNotBlank() == true
                val isCheckboxChecked = binding.cbOtp.isChecked

                binding.btnVerifyCode.isEnabled = if (isCheckboxChecked) {
                    isRemarkFilled
                } else {
                    validationOTP()
                }
                if (binding.btnVerifyCode.isEnabled) {
                    binding.btnVerifyCode.setBackgroundColor(ContextCompat.getColor(this@UDOTPValidationActivity, R.color.primary_color))
                } else {
                    binding.btnVerifyCode.setBackgroundColor(Color.GRAY)
                }
            }
        })
    }

    private fun checkbox() {
        binding.constrntOtp.visibility = View.GONE
        binding.remarkConstraint.visibility = View.VISIBLE
        binding.constraintResend.visibility = View.GONE
        binding.tvResend.isEnabled = false
        binding.btnVerifyCode.isEnabled = true
        binding.btnVerifyCode.text = getString(R.string.submit)
    }

    private fun resetOtpFields() {
        binding.constrntOtp.visibility = View.VISIBLE
        binding.constraintResend.visibility = View.VISIBLE
        binding.remarkConstraint.visibility = View.GONE
        binding.btnVerifyCode.text = getString(R.string.verify_code)
    }

    private fun callSendReSendOtpApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.sendOtpApiflow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        fetchTimerState()
                        showToast(message = it.data as String, true)
                    }

                    else -> {
                        manageApiFlowStatus(apiResultState = it, true)
                    }
                }
            }
        }
    }


    private fun fetchVerifyCodeApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.verifyCodeApiflow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        showToast(message = it.data as String, true)
                        val extras = Bundle().apply {
                            putString(Constants.AWB_NUMBER, awbNumber.toString())
                            putString(Constants.DRS_ID, drsId.toString())
                            putBoolean(Constants.Is_UdCalling, isUdCalling)
                            putString(Constants.UD_TYPE, udType)
                            putString(Constants.REQUEST_TYPE, requestType)
                            putString(Constants.SELECTED_DATE, selectedDate)
                            putString(Constants.CLIENT_CORRELATION_ID, clientCorrelationId)
                            putString(Constants.RESCHEDULE_REMARKS,rescheduleRemarks)
                        }
                        startScreen(CommitActivity(), extras)
                    }

                    else -> {
                        manageApiFlowStatus(apiResultState = it, true)
                    }
                }
            }
        }
    }

    private fun resendClickable() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.resendClickable.collectLatest {
                //  binding.setCancelable(it)
                if (it) {
                    binding.tvResend.setTextColor(
                        resources.getColor(
                            R.color.blue_18,
                            this@UDOTPValidationActivity.theme
                        )
                    )
                    binding.tvResend.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                } else {
                    binding.tvResend.setTextColor(
                        resources.getColor(
                            R.color.black_1A,
                            this@UDOTPValidationActivity.theme
                        )
                    )
                    binding.tvResend.paintFlags = Paint.LINEAR_TEXT_FLAG
                }
                isTimer = it
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resendClickable()
    }

     fun backPress() {
        finish()
    }

}