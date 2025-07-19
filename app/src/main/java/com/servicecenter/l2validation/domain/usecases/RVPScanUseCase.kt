package com.servicecenter.l2validation.domain.usecases

import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.CommonResponse
// Code Reviewed
import com.servicecenter.l2validation.domain.repository.ServiceCenterRepository
import okhttp3.MultipartBody
import okhttp3.RequestBody

class RVPScanUseCase(private val serviceCenterRepository: ServiceCenterRepository) {
    suspend fun executePendingShipment(location_code: String): CommonResponse {
        return serviceCenterRepository.callPendingShipmentApi(location_code)
    }

    suspend fun sendCommitPacketToServer(commonRequest: CommonRequest): CommonResponse {
        return serviceCenterRepository.sendCommitPacketToServer(commonRequest)
    }


    suspend fun upLoadImage(
        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse {
        return serviceCenterRepository.upLoadImage(request, file)
    }

    /*suspend fun executeQcQuestion(dcCode: String, airwayBillNumber: String): CommonResponse {
        return serviceCenterRepository.callQcQuestionApi(dcCode, airwayBillNumber)
    }*/

    suspend fun executeFilterApi(locationCode: String,ud_status: String): CommonResponse {
        return serviceCenterRepository.callFilterApi(locationCode,ud_status)
    }

    suspend fun executeUDShipment(commonRequest: HashMap<String,Any>): CommonResponse {
        return serviceCenterRepository.callShipmentApi(commonRequest)
    }

    suspend fun executeUdDetail(awb:Long,drs_id:Long,ud_status:String,dc_code:String): CommonResponse {
        return serviceCenterRepository.callUdDetailApi(awb,drs_id,ud_status,dc_code)
    }


    suspend fun getCallStatus(awb:Long,drs_id:Long): CommonResponse {
        return serviceCenterRepository.getCallStatus(awb,drs_id)
    }

    suspend fun getCallEvent(commonRequest: CommonRequest): CommonResponse {
        return serviceCenterRepository.getCallEvent(commonRequest)
    }


    suspend fun executeCutOffTime(location_code: String): CommonResponse {
        return serviceCenterRepository.callCutOffTimeApi(location_code)
    }

    suspend fun executeUdPacketToServer(commonRequest: CommonRequest): CommonResponse {
        return serviceCenterRepository.sendUdPacketToServer(commonRequest)
    }


    //rtsApisUseCase



    suspend fun executeProductImage(airwayBillNumber: String): CommonResponse {
        return serviceCenterRepository.callProductImageApi(airwayBillNumber)
    }



}
