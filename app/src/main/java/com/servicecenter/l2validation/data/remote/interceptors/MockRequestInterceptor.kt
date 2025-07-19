package com.servicecenter.l2validation.data.remote.interceptors
//Code Reviewed
import android.content.Context
import com.servicecenter.l2validation.BuildConfig
import com.servicecenter.l2validation.app.extensions.readFileFromAssets
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import javax.inject.Inject

class MockRequestInterceptor @Inject constructor(private val context: Context) : Interceptor {

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaTypeOrNull()
        const val IS_MOCK = BuildConfig.IS_MOCK_API
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (IS_MOCK.contentEquals("true")) {
            val filename = request.url.pathSegments.last()
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("")
                .code(200)
                .body(ResponseBody.create(JSON_MEDIA_TYPE, context.readFileFromAssets("mockApiResponse/$filename.json")))
                .build()
        }

        return chain.proceed(request.newBuilder().build())
    }

}