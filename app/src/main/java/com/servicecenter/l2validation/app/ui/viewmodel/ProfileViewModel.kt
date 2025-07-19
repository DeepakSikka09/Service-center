package com.servicecenter.l2validation.app.ui.viewmodel
// Code Reviewed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
    val serviceCenterDatabase: ServiceCenterDatabase
) : ViewModel() {
    var user_name: String = ""
    var first_name: String = ""
    var last_name: String = ""
    var contact_no: String = ""
    var location_code: String = ""
    var department_code: String = ""
    var designation_code: String = ""
    var service_center_type: String = ""
    var last_login: String = ""
    var group_code: String = ""

    fun fetchLoginDB() {
        viewModelScope.launch {
            user_name = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.user_name, ""
            ).first()
            first_name = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.first_name, ""
            ).first()
            last_name = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.last_name, ""
            ).first()
            contact_no = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.PHONE, ""
            ).first()

            location_code = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.Location_Code, ""
            ).first()
            department_code = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.department_code, ""
            ).first()
            designation_code = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.designation_code, ""
            ).first()
            last_login = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.last_login, ""
            ).first()
            group_code = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.group_code, ""
            ).first()
            service_center_type = preferenceDataStoreHelper.getData(
                PreferenceDataStoreConstants.service_center_type, ""
            ).first()
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                preferenceDataStoreHelper.clearAllPreference()
                serviceCenterDatabase.shipmentDetailDao().deleteShipmentDetail()
                //serviceCenterDatabase.UDShipmentDao().deleteShipmentDetail()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}