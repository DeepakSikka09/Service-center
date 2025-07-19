package com.servicecenter.l2validation.app.ui.activity
// Code Reviewed
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDCallingActivity
import com.servicecenter.l2validation.app.ui.activity.rtsScanning.RtsScanActivity
import com.servicecenter.l2validation.app.ui.activity.auth.ProfileActivity
import com.servicecenter.l2validation.app.ui.activity.rvpShipment.LinkFlyerActivity
import com.servicecenter.l2validation.app.ui.activity.salTally.SalTallyActivity
import com.servicecenter.l2validation.app.ui.adapters.DashBoardAdapter
import com.servicecenter.l2validation.app.ui.viewmodel.DashboardViewModel
import com.servicecenter.l2validation.application.ForceUpdateChecker
import com.servicecenter.l2validation.data.local.entities.DashboardEnum
import com.servicecenter.l2validation.data.local.entities.DashboardItem
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.data.remote.model.UpdateRequiredModel
import com.servicecenter.l2validation.databinding.ActivityDashboardBinding
import com.servicecenter.l2validation.databinding.ReconInProgressDialogBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.ESPER_TOKEN
import com.servicecenter.l2validation.utils.Constants.TALLY_COMPLETED
import com.servicecenter.l2validation.utils.Constants.TALLY_MARKED_COMPLETED
import com.servicecenter.l2validation.utils.Constants.TALLY_NOT_STARTED
import com.servicecenter.l2validation.utils.Constants.TALLY_STARTED
import com.servicecenter.l2validation.utils.Constants.dashboardItemList
import dagger.hilt.android.AndroidEntryPoint
import io.esper.devicesdk.EsperDeviceSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : BaseActivity<ActivityDashboardBinding, DashboardViewModel>(), View.OnClickListener {
    override fun getLayout(): Int {
        return R.layout.activity_dashboard
    }

    override fun getViewModels(): Class<DashboardViewModel> {
        return DashboardViewModel::class.java
    }

    private var flyerAllow: Boolean = false
    private var sdk: EsperDeviceSDK? = null
    private var esperSDKActivated: Boolean? = null
    private var isSalCompleted = false
    private var completedMsg = ""

    @Inject
    lateinit var forceUpdateChecker: ForceUpdateChecker
    private var isUpdated: Boolean = false

    private lateinit var dashAdapter:DashBoardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initially()
        sdk = EsperDeviceSDK.getInstance(applicationContext)
        initEsperSDKActivationCheck()

        viewModel.tallyStatus()
        collectTallyStatus()
        sdk?.activateSDK(ESPER_TOKEN, object : EsperDeviceSDK.Callback<Void?> {
            override fun onResponse(response: Void?) {
                //Activation was successful
                esperSDKActivated = true
            }

            override fun onFailure(t: Throwable) {
                esperSDKActivated = false
            }
        })
        analyticsToAllScreen(Constants.APP_DASHBOARD_EVENT)

        dashAdapter = DashBoardAdapter(dashboardItemList) {type->
            when(type) {
                DashboardEnum.RVP_VALIDATION -> {
                    if (flyerAllow) {
                        analyticsToAllButton(
                            Constants.TILES,
                            Constants.BUTTON_KEY,
                            Constants.RVP_SHIPMENT_VALIDATION_VALUE
                        )
                        startScreen(LinkFlyerActivity())
                    } else {
                        showToast(
                            message = getString(R.string.access_denied),
                            status = false
                        )
                    }
                }

                DashboardEnum.RVP_HANDOVER -> {
                    showToast(getString(R.string.under_development), false)
                }

                DashboardEnum.UD_CALLING -> {
                    startScreen(UDCallingActivity())
                }

                DashboardEnum.RTS -> {
                    startScreen(RtsScanActivity())
                }

                DashboardEnum.SAL_TALLY -> {
                    if(isSalCompleted){
                        showTallyOngoingDialog()
                    }else {
                        startScreen(SalTallyActivity())
                    }
                }
            }
        }

        val grid = GridLayoutManager(this,2,GridLayoutManager.VERTICAL,false)
        binding.dashboardRecycler.adapter = dashAdapter
        binding.dashboardRecycler.layoutManager = grid
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()

            }
        })
    }

    private fun initially() {
        binding.headerLayout.ivProfileCircle.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.iv_profile_circle -> {
                analyticsToAllButton(
                    Constants.PROFILE,
                    Constants.BUTTON_KEY,
                    Constants.EMPLOYEE_PROFILE_VALUE
                )
                startScreen(ProfileActivity())
            }
        }
    }

    private fun fetchProfilePreferenceState() {
        binding.headerLayout.tvProfileCircle.text =
            (viewModel.firstName.firstOrNull() ?: "").toString()
                .plus((viewModel.lastName.firstOrNull() ?: "").toString())
        flyerAllow = viewModel.groupCode.equals("OPS Supervisor LM", ignoreCase = true)
    }

    private fun collectTallyStatus(){
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.tallyStatusFlow.collect{result->
                when(result){
                    is APIResultState.Success->{
                        progressDialog().dismiss()
                        result.data as Response
                        result.data.toggle_exception?.let {toggle->
                            viewModel.setExceptionToggle(toggle)
                        }
                        result.data.recon_status?.let {status->
                            viewModel.updateTallyStatus(status)
                            when (status) {
                                TALLY_NOT_STARTED -> {}
                                TALLY_STARTED -> showTallyOngoingDialog()
                                TALLY_MARKED_COMPLETED -> showTallyOngoingDialog()
                                TALLY_COMPLETED -> {
                                    isSalCompleted = true
                                    completedMsg = result.data.description
                                }
                                else->{}
                            }
                        }

                    }
                    else->{
                        manageApiFlowStatus(result,true)
                    }
                }
            }
        }
    }
    private fun showTallyOngoingDialog() {
        val  dBinding = ReconInProgressDialogBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        if(isSalCompleted){
            dBinding.alertTv.text = completedMsg
        }
        dialog.apply {
            setContentView(dBinding.root)
            window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
            }
            setCancelable(false)
        }
        dBinding.okBtn.setOnClickListener {
            dialog.dismiss()
            if (!isSalCompleted)
                startScreen(SalTallyActivity())
        }
        dialog.show()

    }


    override fun onResume() {
        super.onResume()
        viewModel.fetchLoginDB()
        lifecycleScope.launch{
            checkDashboard()
        }
        if (viewModel.firstName.isNullOrEmpty()) {
            viewModel.logout(this)
        }
        if(viewModel.getTallyStatus() == 4){
            isSalCompleted = true
            completedMsg = viewModel.getTallyMsg()
        }
        fetchProfilePreferenceState()
        if (!isUpdated) {
            if (isInternetAvailable(this)) {
                updateChecker()
                isUpdated = true
            } else {
                showToast(getString(R.string.no_internet), false)
            }
        }
    }

    fun backPress() {
        viewModel.exitApp(this)
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

    private fun showUpdateDialog(updateRequiredModel: UpdateRequiredModel) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_required))
            .setMessage(updateRequiredModel.description)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.update)) { _, _ ->
                startDownload(updateRequiredModel)
            }
            .show()
    }

    private fun startDownload(updateRequiredModel: UpdateRequiredModel) {
        viewModel.downloadAPK(
            updateRequiredModel.updateUrl, sdk,
            this@DashboardActivity, esperSDKActivated
        )
    }
    private suspend fun checkDashboard(){
        if (viewModel.setDashboardEventManagement()=="true") {

            if (!dashboardItemList.any { it.code == DashboardEnum.UD_CALLING }) {
                dashboardItemList.add(
                    DashboardItem(R.drawable.ud_calling, "UD Calling", DashboardEnum.UD_CALLING)
                )
            }
        } else {
            dashboardItemList.removeAll { it.code == DashboardEnum.UD_CALLING }
        }
        dashAdapter.notifyDataSetChanged()
    }


}