package com.servicecenter.l2validation.domain.usecases

import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.CommonResponse
import com.servicecenter.l2validation.domain.repository.ServiceCenterRepository
import okhttp3.MultipartBody
import okhttp3.RequestBody

class RtsScanUseCase(private val serviceCenterRepository: ServiceCenterRepository){

    suspend fun executePendingRtsShipment(location_code: String,pageNo:Int,pageSize:Int,fromDate:String?="",toDate:String?="",sorting:String?=""): CommonResponse {
        return serviceCenterRepository.callRtsShipmentApi(location_code,pageNo,pageSize,fromDate,toDate,sorting)
    }
    suspend fun executePendingCheckAwb(location_code: String,awbNo:String): CommonResponse {
        return serviceCenterRepository.callCheckAwbApi(location_code,awbNo)
    }

    suspend fun sendRtsPacketToServer(commonRequest: CommonRequest): CommonResponse {
        return serviceCenterRepository.sendRtsPacketToServer(commonRequest)
    }


    suspend fun upLoadRtsImage(

        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse {
        return serviceCenterRepository.upLoadRtsImage(request, file)
    }



}