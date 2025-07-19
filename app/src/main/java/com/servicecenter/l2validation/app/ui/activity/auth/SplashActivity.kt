package com.servicecenter.l2validation.app.ui.activity.auth

import androidx.lifecycle.lifecycleScope
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.activity.DashboardActivity
import com.servicecenter.l2validation.app.ui.viewmodel.SplashViewModel
import com.servicecenter.l2validation.application.ForceUpdateChecker
import com.servicecenter.l2validation.databinding.ActivitySplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding,SplashViewModel>() {
    @Inject
    lateinit var forceUpdateChecker: ForceUpdateChecker
    override fun getLayout(): Int {
        return R.layout.activity_splash
    }

    override fun getViewModels(): Class<SplashViewModel> {
        return SplashViewModel::class.java
    }

    override fun onResume() {
        super.onResume()
        viewModel.isLoggedIn()
        fetchLoginSessionState()
        updateChecker()
    }

    private fun updateChecker() {
        forceUpdateChecker.checkForceUpdateRequired {
            lifecycleScope.launch {
                // Ensure data is saved before proceeding
            }
        }
    }
    private fun fetchLoginSessionState() {
        lifecycleScope.launch {
            delay(3000)
            viewModel.loginSessionFlow.collect { isLoggedIn ->
                if (isLoggedIn) {
                    startScreen(DashboardActivity())
                    this@SplashActivity.finish()
                }else{
                    startScreen(LoginActivity())
                    this@SplashActivity.finish()
                }
            }
        }
    }
}