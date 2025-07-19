package com.servicecenter.l2validation.data.remote.interceptors
//Code Reviewed
import androidx.datastore.preferences.core.Preferences
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants.aUTH_TOKEN
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.utils.Constants.AUTH_TOKEN
import com.servicecenter.l2validation.utils.Constants.DEVICE
import com.servicecenter.l2validation.utils.Constants.app_code
import com.servicecenter.l2validation.utils.Constants.auth_required
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class HeaderInterceptor @Inject constructor(val preferenceDataStoreHelper: PreferenceDataStoreHelper) :
    Interceptor {

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val originalRequest: Request = chain.request()
        val modifiedRequest: Request = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
            .header(AUTH_TOKEN,getToken(aUTH_TOKEN,"").await())
            .header(app_code, "SCA")
            .header(auth_required, "YES")
            .header(DEVICE, "Android")
            .build()
        chain.proceed(modifiedRequest)
    }

    private fun getToken(key: Preferences.Key<String>, defaultValue: String) =
        coroutineScope.async {
            preferenceDataStoreHelper.getData(key, defaultValue).first().toString()
        }
}
