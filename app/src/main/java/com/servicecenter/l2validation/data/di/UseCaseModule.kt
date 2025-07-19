package com.servicecenter.l2validation.data.di
//Code Reviewed
import com.servicecenter.l2validation.domain.usecases.LoginUseCase
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.domain.repository.ServiceCenterRepository
import com.servicecenter.l2validation.domain.usecases.RtsScanUseCase
import com.servicecenter.l2validation.domain.usecases.SalTallyUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun provideGetLoginUseCase(serviceCenterRepository: ServiceCenterRepository): LoginUseCase {
        return LoginUseCase(serviceCenterRepository)
    }

    @Provides
    fun provideRVPScanUseCase(serviceCenterRepository: ServiceCenterRepository): RVPScanUseCase {
        return RVPScanUseCase(serviceCenterRepository)
    }

    @Provides
    fun provideRtsScanUseCase(serviceCenterRepository: ServiceCenterRepository): RtsScanUseCase {
        return RtsScanUseCase(serviceCenterRepository)
    }

    @Provides
    fun provideSalTallyUseCase(serviceCenterRepository: ServiceCenterRepository): SalTallyUseCase {
        return SalTallyUseCase(serviceCenterRepository)
    }
}
