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
class MapViewModel @Inject constructor(
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val serviceCenterDatabase: ServiceCenterDatabase,

    ) : ViewModel() {


}