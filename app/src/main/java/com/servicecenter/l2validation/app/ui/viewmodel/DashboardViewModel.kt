package com.servicecenter.l2validation.app.ui.viewmodel
// Code Reviewed
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.app.ui.activity.auth.LoginActivity
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.domain.usecases.SalTallyUseCase
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.UpdateAPKInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import io.esper.devicesdk.EsperDeviceSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class DashboardViewModel @Inject constructor(
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val salTallyUseCase: SalTallyUseCase
) : ViewModel() {
    // progress dialog deprecated
    private var pd: ProgressDialog? = null

    private val _profileDBflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val profileDBflow: StateFlow<APIResultState> get() = _profileDBflow

    var firstName:String=""
    var lastName:String=""
    var groupCode:String=""

    private val _tallyStatusFlow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val tallyStatusFlow: StateFlow<APIResultState> get() = _tallyStatusFlow
    fun fetchLoginDB() {

        viewModelScope.async {
                //firstName = withContext(Dispatchers.IO){
                  firstName=  preferenceDataStoreHelper.getData(
                        PreferenceDataStoreConstants.first_name, ""
                    ).first()
              //  }
                lastName = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.last_name, ""
                ).first()
                groupCode = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.group_code, ""
                ).first()
        }
    }

        fun exitApp(context: Context) {
            val a = Intent(Intent.ACTION_MAIN)
            a.addCategory(Intent.CATEGORY_HOME)
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(a)
        }

        fun logout(context: Context) {
            viewModelScope.launch {
                try {
                    preferenceDataStoreHelper.clearAllPreference()
                    serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val i = Intent(context, LoginActivity::class.java)
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.putExtra("EXIT", true)
            context.startActivity(i)
        }

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

    fun tallyStatus(){
        viewModelScope.launch {
            _tallyStatusFlow.value = APIResultState.Loading
            val dcCode =
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.Location_Code,
                    ""
                )
                    .first()

            val empCode =
                preferenceDataStoreHelper.getData(PreferenceDataStoreConstants.EMP_CODE, "").first()
            try {
            val result = withContext(Dispatchers.IO) {
                salTallyUseCase.reconStatus(dcCode,empCode)
            }

                if (result.status) {
                    _tallyStatusFlow.value = APIResultState.Success(result.response)
                } else {
                    _tallyStatusFlow.value =
                        APIResultState.Failure(result.response?.description ?: "")
                }
            } catch (ex: Exception) {
                _tallyStatusFlow.value =
                    APIResultState.Failure(ex.localizedMessage ?: "")
            }
        }
    }

    fun updateTallyStatus(status: Int) {
        viewModelScope.launch {
            preferenceDataStoreHelper.setData(
                PreferenceDataStoreConstants.TALLY_STATUS,
                status
            )
        }
    }

    fun setExceptionToggle(toggleException: Boolean) {
        viewModelScope.launch {
            preferenceDataStoreHelper.setData(
                PreferenceDataStoreConstants.IS_EXCEPTION_MARK,
                toggleException
            )
        }
    }

    fun getTallyStatus():Int{
        var tallyStatus = 0
        viewModelScope.launch {
            tallyStatus = withContext(Dispatchers.IO){
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.TALLY_STATUS,
                    0
                ).first()
            }
        }
        return tallyStatus
    }

    fun getTallyMsg(): String {
        var msg = ""
        viewModelScope.launch {
            msg = withContext(Dispatchers.IO) {
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.TALLY_MSG,
                    ""
                ).first()
            }
        }
        return msg
    }

    suspend fun setDashboardEventManagement(): String {
        return withContext(Dispatchers.IO) {
            delay(200)
            preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.Dashboard_event_management, "false"
            ).first()
        }
    }
}