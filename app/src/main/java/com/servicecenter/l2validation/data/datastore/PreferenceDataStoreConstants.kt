package com.servicecenter.l2validation.data.datastore
//Code Reviewed
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceDataStoreConstants {
    val IS_LOGGED_IN = booleanPreferencesKey("IS_LOGGED_IN")
    val EMP_CODE = stringPreferencesKey("EMP_CODE")
    val AWB_NO = stringPreferencesKey("AWB_NO")
    val aUTH_TOKEN= stringPreferencesKey("aUTH_TOKEN")
    val Location_Code= stringPreferencesKey("Location_Code")
    val PHONE= stringPreferencesKey("PHONE")
    val FRONT_IMAGE= longPreferencesKey("FRONT_IMAGE")
    val BACK_IMAGE= longPreferencesKey("BACK_IMAGE")




    val user_name= stringPreferencesKey("user_name")
    val first_name= stringPreferencesKey("first_name")
    val last_name= stringPreferencesKey("last_name")
    val department_code= stringPreferencesKey("department_code")
    val designation_code= stringPreferencesKey("designation_code")
    val service_center_type= stringPreferencesKey("service_center_type")
    val last_login= stringPreferencesKey("last_login")
    val group_code= stringPreferencesKey("group_code")
    val recentCorrelationId= stringPreferencesKey("clientCorrelationId")
    val callState= stringPreferencesKey("callState")

    //1->In Progress2, ->completed
    val TALLY_STATUS = intPreferencesKey("TALLY_STATUS")
    val IS_EXCEPTION_MARK = booleanPreferencesKey("is_exception_mark")
    val TALLY_MSG = stringPreferencesKey("tally_msg")
    val Dashboard_event_management = stringPreferencesKey("dashboard_event_management")

}