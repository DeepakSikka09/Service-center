package com.servicecenter.l2validation.app.ui.activity.auth
// Code Reviewed
import android.os.Bundle
import android.view.View
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.databinding.ActivityChangePasswordBinding
import com.servicecenter.l2validation.app.ui.viewmodel.ChangePasswordViewModel
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.GenericTextWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangePasswordActivity : BaseActivity<ActivityChangePasswordBinding, ChangePasswordViewModel>(), View.OnClickListener {
    override fun getLayout(): Int {
        return R.layout.activity_change_password    }

    override fun getViewModels(): Class<ChangePasswordViewModel> {
        return ChangePasswordViewModel::class.java
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initially()
        fetchChangePasswordApiState()

        analyticsToAllScreen(Constants.CHANGE_PASSWORD_EVENT)
    }

    private fun initially() {
        binding.lifecycleOwner = this
        binding.etPassword.addTextChangedListener(
            GenericTextWatcher(
                this, ::validation, binding.btnResetPwd
            )
        )
        binding.etCnfPassword.addTextChangedListener(
            GenericTextWatcher(
                this, ::validation, binding.btnResetPwd
            )
        )
        binding.tvEmployeeCode.text = viewModel.setEmployeeCode()
        binding.btnBack.setOnClickListener(this)
        binding.btnResetPwd.setOnClickListener {
            analyticsToAllButton(Constants.RESET_PASSWORD,Constants.BUTTON_KEY,Constants.RESET_PASSWORD)

            val changePassword = CommonRequest(
                user_name = binding.tvEmployeeCode.text.toString(),
                new_password = binding.etPassword.text.toString(),
                old_password = binding.etOldPassword.text.toString()
            )
            if (validation()) {
                if (isInternetAvailable(this)) {
                    viewModel.callChangePasswordApi(changePassword)
                } else {
                    showToast(getString(R.string.no_internet), false)
                }
            }
        }
    }

    private fun validation(): Boolean {
        return binding.etOldPassword.text.toString()
            .isNotBlank() && binding.etPassword.text.toString().isNotBlank() &&
                binding.etPassword.text.toString().contentEquals(binding.etCnfPassword.text)
    }

    private fun fetchChangePasswordApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.changePasswordApiflow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        showToast(message = it.data as String, true)
                        viewModel.logout()
                        startScreen(LoginActivity())

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
        }
    }


}