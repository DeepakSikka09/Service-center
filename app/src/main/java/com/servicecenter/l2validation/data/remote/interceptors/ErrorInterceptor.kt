package com.servicecenter.l2validation.data.remote.interceptors
//Code Reviewed
import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.app.ui.activity.auth.LoginActivity
import com.servicecenter.l2validation.app.ui.viewmodel.DashboardViewModel
import com.servicecenter.l2validation.app.ui.viewmodel.SplashViewModel
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Response
import java.nio.charset.Charset
import javax.inject.Inject

class ErrorInterceptor @Inject constructor(
    var context: Context,
    var viewModel: SplashViewModel
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)


        if (!response.isSuccessful) {
            response.body?.contentType()?.charset(
                Charset.forName("UTF-8")
            )?.let {charSet->
                val isTokenNotValid = response.body?.source()?.buffer?.clone()?.readString(
                    charSet
                )?.contains(" Invalid Token found") ?: false
                if (response.code == 400) {
                    if (isTokenNotValid) {
                        viewModel.logout(context)
                    }
                    return Response
                        .Builder()
                        .code(200)
                        .message(response.message)
                        .protocol(response.protocol)
                        .request(request)
                        .body(response.body)
                        .build()
                }
            }
        }

        return response
    }
}
