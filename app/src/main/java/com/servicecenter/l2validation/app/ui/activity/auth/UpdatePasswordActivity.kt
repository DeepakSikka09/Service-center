package com.servicecenter.l2validation.app.ui.activity.auth
// Code Reviewed
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.viewmodel.UpdatePasswordViewModel
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.databinding.ActivityUpdatePasswordBinding
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
class UpdatePasswordActivity : BaseActivity<ActivityUpdatePasswordBinding, UpdatePasswordViewModel>(), View.OnClickListener {

    private var isTimer = false
    override fun getLayout(): Int {
        return R.layout.activity_update_password
    }

    override fun getViewModels(): Class<UpdatePasswordViewModel> {
        return UpdatePasswordViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
        viewModel.startOTPTimer(this)
        fetchCreatePasswordApiState()
        fetchTimerState()
        fetchResendCodeApiState()
    }

    private fun initialize() {
        binding.lifecycleOwner = this
        binding.etPassword.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validation,
                binding.btnResetPwd
            )
        )
        binding.etCnfPassword.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validation,
                binding.btnResetPwd
            )
        )
        binding.etOtp.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validation,
                binding.btnResetPwd
            )
        )
        binding.tvEmployeeCode.text = viewModel.setEmployeeCode()
        binding.btnBack.setOnClickListener(this)
        binding.tvResend.setOnClickListener(this)
        binding.btnResetPwd.setOnClickListener(this)
    }


    private fun validation(): Boolean {
        return binding.etPassword.text.toString()
            .isNotBlank() && binding.etCnfPassword.text.toString().isNotBlank() &&
                binding.etPassword.text.toString()
                    .contentEquals(binding.etCnfPassword.text) && binding.etOtp.text.toString().length == 6
    }

    private fun fetchCreatePasswordApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.createPasswordApiflow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        showToast(message = it.data as String, true)
                        startScreen(LoginActivity())
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
            viewModel.resendCodeApiflow.collect {
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


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_back -> finish()

            R.id.tv_resend -> {
                analyticsToAllButton(Constants.RESEND_OTP_FORGOT_PASSWORD, Constants.BUTTON_KEY, Constants.RESEND_OTP_FORGOT_PASSWORD)

                if (isTimer) {
                    binding.tvResend.setTextColor(
                        resources.getColor(
                            R.color.black_1A,
                            this@UpdatePasswordActivity.theme
                        )
                    )
                    binding.tvResend.paintFlags = Paint.LINEAR_TEXT_FLAG
                    viewModel.startOTPTimer(this)
                    isTimer = false
                    if(isInternetAvailable(this)){ viewModel.callResendCodeApi()}else{
                        showToast(getString(R.string.no_internet), false)

                    }
                }
            }

            R.id.btn_reset_pwd -> {
                val updatePassword = CommonRequest(
                    username = binding.tvEmployeeCode.text.toString(),
                    new_password = binding.etPassword.text.toString(),
                    otp = binding.etOtp.text.toString()
                )
                if (isInternetAvailable(this)){ viewModel.callCreatePasswordApi(updatePassword) }else{
                    showToast(getString(R.string.no_internet), false)
                }
            }
        }
    }

    private fun fetchTimerState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.OTPTimerflow.collectLatest {
                binding.tvResend.text = it
                if (it.contains(this@UpdatePasswordActivity.getString(R.string.resend_code))) {
                    binding.tvResend.setTextColor(
                        resources.getColor(
                            R.color.blue_18,
                            this@UpdatePasswordActivity.theme
                        )
                    )
                    binding.tvResend.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                    isTimer = true
                }
            }
        }
    }
}