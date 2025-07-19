package com.servicecenter.l2validation.app.ui.activity.auth
// Code Reviewed
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.servicecenter.l2validation.BuildConfig
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.databinding.ActivityProfileBinding
import com.servicecenter.l2validation.databinding.LogoutDialogBoxBinding
import com.servicecenter.l2validation.app.ui.viewmodel.ProfileViewModel
import com.servicecenter.l2validation.app.ui.viewmodel.UpdatePasswordViewModel
import com.servicecenter.l2validation.databinding.ActivityUpdatePasswordBinding
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : BaseActivity<ActivityProfileBinding, ProfileViewModel>(), View.OnClickListener {

    private lateinit var logoutDialogBinding: LogoutDialogBoxBinding
    override fun getLayout(): Int {
        return R.layout.activity_profile
    }

    override fun getViewModels(): Class<ProfileViewModel> {
        return ProfileViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.responseData=viewModel
        initialize()
        viewModel.fetchLoginDB()
        fetchProfilePreferenceData()

        analyticsToAllScreen(Constants.PROFILE_EVENT)

    }

    private fun initialize() {
        binding.tvVersionName.text = "Version " + BuildConfig.VERSION_NAME
        binding.lifecycleOwner = this
        binding.profile.ivBackArrow.setOnClickListener(this)
        binding.tvPasswordNext.setOnClickListener(this)
        binding.constLogout.setOnClickListener(this)
        binding.ivProfileCircle.setOnClickListener(this)
        binding.constChangePassword.setOnClickListener(this)
        binding.profile.tvHeadingName.text= getString(R.string.profile)
    }


    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.iv_back_arrow -> finish()

            R.id.tv_password_next->{
                startScreen(ChangePasswordActivity())
            }

            R.id.const_change_password -> {
                analyticsToAllButton(
                    Constants.CHANGE_PASSWORD,
                    Constants.BUTTON_KEY,
                    Constants.CHANGE_PASSWORD_VALUE
                )

                startScreen(ChangePasswordActivity())
            }

            R.id.const_logout -> {
                showCustomDialogBox()
            }
        }
    }

    private fun fetchProfilePreferenceData() {
        if (viewModel.group_code.isBlank()) {
            binding.dcHeadBorder.visibility = View.GONE
            binding.tvRole.visibility = View.GONE
            binding.tvDcHead.visibility = View.GONE
            binding.ivDc.visibility = View.GONE
        }
    }

    private fun showCustomDialogBox() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        logoutDialogBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this),
            R.layout.logout_dialog_box,
            binding.root as ViewGroup,
            false
        )
        dialog.setContentView(logoutDialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        logoutDialogBinding.btnNo.setOnClickListener {
            dialog.dismiss()
        }
        logoutDialogBinding.btnYes.setOnClickListener {
            analyticsToAllButton(Constants.LOGOUT, Constants.BUTTON_KEY, Constants.LOGOUT_YES_VALUE)

            dialog.dismiss()
            viewModel.logout()
            startScreen(LoginActivity())
        }

        dialog.show()
    }
}