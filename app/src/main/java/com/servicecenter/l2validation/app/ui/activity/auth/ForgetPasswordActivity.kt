package com.servicecenter.l2validation.app.ui.activity.auth
// Code Reviewed
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.hideKeyboard
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.viewmodel.ForgotPasswordViewModel
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.databinding.ActivityForgotPasswordBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.GenericTextWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgetPasswordActivity : BaseActivity<ActivityForgotPasswordBinding, ForgotPasswordViewModel>(), View.OnClickListener {
    override fun getLayout(): Int {
        return R.layout.activity_forgot_password
    }

    override fun getViewModels(): Class<ForgotPasswordViewModel> {
        return ForgotPasswordViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
        fetchResendCodeApiState()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()

            }
        })

        analyticsToAllScreen(Constants.FORGOT_PASSWORD_EVENT)
    }
    private fun initialize() {
        binding.etEmployeeCode.addTextChangedListener(
            GenericTextWatcher(
                this,
                ::validation,
                binding.btnSendCode
            )
        )
        binding.btnBack.setOnClickListener(this)
        binding.btnSendCode.setOnClickListener(this)
    }

    private fun fetchResendCodeApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.resendCodeApiflow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        showToast(message = it.data as String, true)
                        startScreen(UpdatePasswordActivity())
                    }

                    else -> {
                        manageApiFlowStatus(apiResultState = it, true)
                    }
                }
            }
        }
    }
    private fun validation(): Boolean {
        return binding.etEmployeeCode.text.toString().isNotBlank()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_back -> finish()
            R.id.btn_send_code -> {
                analyticsToAllButton(Constants.SEND_CODE,Constants.BUTTON_KEY,Constants.SEND_CODE)

                val commonRequest = CommonRequest(
                    username = binding.etEmployeeCode.text.toString()
                )
                hideKeyboard(this)
                if (validation()) {
                    if (isInternetAvailable(this)) {
                        viewModel.callResendCodeApi(commonRequest)
                    } else {
                        showToast(getString(R.string.no_internet), false)
                    }
                }
            }
        }
    }

     fun backPress() {
        finish()
    }
}