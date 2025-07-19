package com.servicecenter.l2validation.domain.usecases
// code Reviewed
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.CommonResponse

import com.servicecenter.l2validation.domain.repository.ServiceCenterRepository

class LoginUseCase(private val serviceCenterRepository: ServiceCenterRepository) {

    suspend fun execute(commonRequest: CommonRequest): CommonResponse {
        return serviceCenterRepository.callLoginApi(commonRequest)
    }

    suspend fun executeForgetPasswordApi(commonRequest: CommonRequest): CommonResponse {
        return serviceCenterRepository.callForgetPasswordApi(commonRequest)
    }

    suspend fun executeOtpVerify(commonRequest: CommonRequest): CommonResponse{
        return serviceCenterRepository.callVerifyOtp(commonRequest)
    }
    suspend fun executeresendLogin_Otp(commonRequest: CommonRequest): CommonResponse{
        return serviceCenterRepository.callresendLogin_Otp(commonRequest)
    }

    suspend fun executePassword(commonRequest: CommonRequest):CommonResponse{
        return serviceCenterRepository.callResetPasswordWithOtpApi(commonRequest)
    }
    suspend fun executeChangePassword(commonRequest: CommonRequest):CommonResponse{
        return serviceCenterRepository.callchangePasswordByUserNameApi(commonRequest)
    }

    suspend fun executeSendReSendOtp(awb:Long, event:String, reschedule_date:String): CommonResponse{
        return serviceCenterRepository.callSendReSendOtp(awb,event,reschedule_date)
    }

    suspend fun executeUDOtpVerify(commonRequest: CommonRequest): CommonResponse{
        return serviceCenterRepository.callUDVerifyOtp(commonRequest)
    }

}
