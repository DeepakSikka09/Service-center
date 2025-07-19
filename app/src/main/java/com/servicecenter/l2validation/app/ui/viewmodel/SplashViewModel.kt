package com.servicecenter.l2validation.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.app.ui.activity.auth.LoginActivity
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val serviceCenterDatabase: ServiceCenterDatabase,

    ) : ViewModel() {

    private val _loginSessionFlow = MutableStateFlow(false)
    val loginSessionFlow: StateFlow<Boolean> get() = _loginSessionFlow
    fun isLoggedIn() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                val isLoggedIn = preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.IS_LOGGED_IN, false
                ).first()
                _loginSessionFlow.emit(isLoggedIn)
            }
        }
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

}