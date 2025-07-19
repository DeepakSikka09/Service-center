package com.servicecenter.l2validation.domain.usecases

import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.CommonResponse
import com.servicecenter.l2validation.domain.repository.ServiceCenterRepository
import okhttp3.MultipartBody
import okhttp3.RequestBody

class SalTallyUseCase(private val serviceCenterRepository: ServiceCenterRepository) {
    suspend fun getTallyShipment(location_code: String,employee_code: String):CommonResponse{
        return serviceCenterRepository.salTallyShipment(location_code,employee_code)
    }

    suspend fun updateShipment(commonRequest: CommonRequest):CommonResponse{
        return serviceCenterRepository.updateShipment(commonRequest)
    }

    suspend fun reconStart(request: CommonRequest): CommonResponse {
        return serviceCenterRepository.reconStart(request)
    }

    suspend fun reconStatus(location_code: String, employee_code: String): CommonResponse {
        return serviceCenterRepository.reconStatus(location_code,employee_code)
    }

    suspend fun reconCancel(request: CommonRequest): CommonResponse {
        return serviceCenterRepository.reconCancel(request)
    }

    suspend fun reconDone(request: CommonRequest): CommonResponse {
        return serviceCenterRepository.reconDone(request)
    }

    suspend fun uploadTallyImage(
        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse {
        return serviceCenterRepository.uploadTallyImage(request,file)
    }

    suspend fun updateShortage(request: CommonRequest):CommonResponse{
        return serviceCenterRepository.updateShortage(request)
    }

    suspend fun updateShipmentWithImage(request: CommonRequest):CommonResponse{
        return serviceCenterRepository.updateShipmentWithImage(request)
    }

    suspend fun markException(request: CommonRequest):CommonResponse{
        return serviceCenterRepository.markException(request)
    }
}