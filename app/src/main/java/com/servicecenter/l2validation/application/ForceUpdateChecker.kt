package com.servicecenter.l2validation.application
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.servicecenter.l2validation.BuildConfig
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants.Dashboard_event_management
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.remote.model.UpdateRequiredModel
import com.servicecenter.l2validation.utils.Constants.DASHBOARD_EVENT_MANAGEMENT
import com.servicecenter.l2validation.utils.Constants.KEY_DESCRIPTION
import com.servicecenter.l2validation.utils.Constants.KEY_REQUIRED_VERSION
import com.servicecenter.l2validation.utils.Constants.KEY_UPDATE_REQUIRED
import com.servicecenter.l2validation.utils.Constants.KEY_UPDATE_URL
import com.servicecenter.l2validation.utils.Constants.MINIMUM_FETCH_INTERVAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ForceUpdateChecker  @Inject constructor(val preferenceDataStoreHelper: PreferenceDataStoreHelper) {
    fun checkForceUpdateRequired(updateRequired: (result: UpdateRequiredModel?) -> Unit) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_UPDATE_REQUIRED to false,
                KEY_REQUIRED_VERSION to BuildConfig.VERSION_NAME,
                KEY_UPDATE_URL to "store_url",
                KEY_DESCRIPTION to "",
                DASHBOARD_EVENT_MANAGEMENT to false
            )
        ).addOnCompleteListener {
            remoteConfig.fetch(MINIMUM_FETCH_INTERVAL).addOnCompleteListener {
                if (it.isSuccessful) {
                    remoteConfig.activate().addOnCompleteListener {
                        if (remoteConfig.getBoolean(KEY_UPDATE_REQUIRED)) {
                            val currentVersion = (BuildConfig.VERSION_NAME).replace(".", "").toInt()

                            val requiredVersion =
                                remoteConfig.getString(KEY_REQUIRED_VERSION).replace(".", "")
                                    .toInt()
                            if(BuildConfig.BUILD_TYPE.equals("release")) {
                                if (currentVersion < requiredVersion) {
                                    updateRequired.invoke(
                                        UpdateRequiredModel(
                                            versionName = remoteConfig.getString(
                                                KEY_REQUIRED_VERSION
                                            ),
                                            updateUrl = remoteConfig.getString(KEY_UPDATE_URL),
                                            description = remoteConfig.getString(KEY_DESCRIPTION)
                                        )
                                    )
                                }
                            }else {
                                updateRequired.invoke(null)
                            }
                        }
                        if((remoteConfig.getString(DASHBOARD_EVENT_MANAGEMENT)=="true") && BuildConfig.BUILD_TYPE.equals("release")){
                            CoroutineScope(Dispatchers.IO).launch {
                                preferenceDataStoreHelper.setData(Dashboard_event_management,
                                    "true"
                                )
                            }
                        }else{
                            CoroutineScope(Dispatchers.IO).launch {
                                preferenceDataStoreHelper.setData(Dashboard_event_management,
                                    "false"
                                )
                            }
                        }
                    }
                } else {
                    updateRequired.invoke(null)
                }
            }
        }
    }

}
