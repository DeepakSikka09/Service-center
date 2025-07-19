package com.servicecenter.l2validation.data.di
//Code Reviewed
import android.content.Context
import com.servicecenter.l2validation.BuildConfig.BASE_URL
import com.servicecenter.l2validation.app.ui.viewmodel.DashboardViewModel
import com.servicecenter.l2validation.app.ui.viewmodel.SplashViewModel
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.di.DatabaseModule.provideDatabase
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.data.remote.interceptors.ErrorInterceptor
import com.servicecenter.l2validation.data.remote.interceptors.HeaderInterceptor
import com.servicecenter.l2validation.data.remote.interceptors.MockRequestInterceptor
import com.servicecenter.l2validation.data.remote.ApiService
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.domain.repository.ServiceCenterRepository
import com.servicecenter.l2validation.domain.repository.ServiceCenterRepositoryImpl
import com.servicecenter.l2validation.domain.usecases.SalTallyUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext

import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideApplicationContext(@ApplicationContext application: Context): Context {
        return application
    }

    @Singleton
    @Provides
    fun provideApiRequest(): CommonRequest {
        return CommonRequest()
    }

    @Provides
    fun provideDataStoreHelper(context: Context): PreferenceDataStoreHelper {
        return PreferenceDataStoreHelper(context)
    }

    @Provides
    @Singleton
    fun provideApiService(
        context: Context,
        preferenceDataStoreHelper: PreferenceDataStoreHelper,
    ): ApiService {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)

            .addInterceptor(HeaderInterceptor(preferenceDataStoreHelper))
            .addInterceptor(ErrorInterceptor(context, splashViewModel(preferenceDataStoreHelper,provideDatabase(context))))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(MockRequestInterceptor(context))
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    fun provideUserRepository(apiService: ApiService): ServiceCenterRepository{
        return ServiceCenterRepositoryImpl(apiService)
    }


    @Provides
    fun splashViewModel(dataStoreHelper: PreferenceDataStoreHelper,serviceCenterDatabase: ServiceCenterDatabase):SplashViewModel{
        return SplashViewModel(dataStoreHelper,serviceCenterDatabase)
    }

}
