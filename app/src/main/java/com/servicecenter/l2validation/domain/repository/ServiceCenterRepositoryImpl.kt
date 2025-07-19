package com.servicecenter.l2validation.domain.repository
// Code Reviewed

import com.servicecenter.l2validation.data.remote.ApiService
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.CommonResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Query


class ServiceCenterRepositoryImpl(val apiService: ApiService) : ServiceCenterRepository {
    override suspend fun callLoginApi(commonRequest: CommonRequest): CommonResponse {
        return apiService.callLoginApi(commonRequest)
    }
    override suspend fun callForgetPasswordApi(commonRequest: CommonRequest): CommonResponse {
        return apiService.callForgetPasswordApi(commonRequest)
    }
    override suspend fun callResetPasswordWithOtpApi(commonRequest: CommonRequest):CommonResponse{
        return apiService.callResetPasswordWithOtpApi(commonRequest)
    }

    override suspend fun callchangePasswordByUserNameApi(commonRequest: CommonRequest): CommonResponse {
       return apiService.callchangePasswordByUserNameApi(commonRequest)
    }

    override suspend fun callPendingShipmentApi(location_code: String): CommonResponse {
        return apiService.callPendingShipmentApi(location_code)
    }

    override suspend fun callVerifyOtp(commonRequest: CommonRequest):CommonResponse{
        return apiService.callVerifyOtp(commonRequest)
    }

    override suspend fun callresendLogin_Otp(commonRequest: CommonRequest):CommonResponse{
        return apiService.callresendLogin_Otp(commonRequest)
    }
    override suspend fun sendCommitPacketToServer(commonRequest: CommonRequest): CommonResponse {
        return apiService.sendCommitPacketToServer(commonRequest)
    }

    override suspend fun upLoadImage(
        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse {
        return  apiService.upLoadImage(request,file)
    }

    override suspend fun upLoadRtsImage(
        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse {
        return  apiService.upLoadRtsImage(request,file)
    }

    override suspend fun callProductImageApi(airwaybill_number:String): CommonResponse {
        return apiService.callProductImageApi(airwaybill_number)
    }

    //RtsApis
    override suspend fun callRtsShipmentApi(
        location_code: String,
        pageNo: Int,
        pageSize: Int,
        fromDate:String?, toDate:String?,sorting:String?
    ): CommonResponse {
        return apiService.callRtsPendingShipmentApi(location_code,pageNo,pageSize,fromDate,toDate,sorting)
    }

    override suspend fun callCheckAwbApi(location_code: String, awbNo: String): CommonResponse {
        return apiService.callRtsAwbCheckApi(location_code,awbNo)
    }

    override suspend fun sendRtsPacketToServer(commonRequest: CommonRequest): CommonResponse {
        return apiService.sendRtsPacketToServer(commonRequest)
    }

    override suspend fun callShipmentApi(commonRequest: HashMap<String,Any>): CommonResponse {
        return apiService.callShipmentApi(commonRequest)
    }
    override suspend fun callUdDetailApi(awb:Long,drs_id:Long,ud_status:String,dc_code:String): CommonResponse {
        return apiService.callUdAwbDetailApi(awb,drs_id,ud_status,dc_code)
    }

    override suspend fun getCallStatus(awb:Long,drs_id:Long): CommonResponse {
        return apiService.getCallStatus(awb,drs_id)
    }

    override suspend fun getCallEvent(commonRequest: CommonRequest): CommonResponse {
        return apiService.getCallEvent(commonRequest)
    }


    override suspend fun callSendReSendOtp(awb:Long, event:String, reschedule_date:String):CommonResponse{
        return apiService.callSendReSendOtp(awb,event,reschedule_date)
    }

    override suspend fun callUDVerifyOtp(commonRequest: CommonRequest):CommonResponse{
        return apiService.callUDVerifyOtp(commonRequest)
    }

    override suspend fun callCutOffTimeApi(dcCode: String): CommonResponse {
        return apiService.callCutOffTimeApi(dcCode)
    }


    override suspend fun callFilterApi(location_code: String,ud_status: String): CommonResponse {
        return apiService.callFilterApi(location_code,ud_status)
    }

    override suspend fun sendUdPacketToServer(commonRequest: CommonRequest): CommonResponse {
        return apiService.sendUdPacketToServer(commonRequest)
    }
    //SAL Tally api
    override suspend fun salTallyShipment(location_code: String,
                                          employee_code: String): CommonResponse {
        return apiService.salTallyShipment(location_code,employee_code)
    }

    override suspend fun updateShipment(request: CommonRequest): CommonResponse {
        return apiService.updateShipment(request)
    }

    override suspend fun reconStart(request: CommonRequest): CommonResponse {
        return apiService.reconStart(request)
    }

    override suspend fun reconStatus(location_code: String, employee_code: String): CommonResponse {
        return apiService.reconStatus(location_code,employee_code)
    }

    override suspend fun reconCancel(request: CommonRequest): CommonResponse {
        return apiService.reconCancel(request)
    }

    override suspend fun reconDone(request: CommonRequest): CommonResponse {
        return apiService.reconDone(request)
    }

    override suspend fun uploadTallyImage(
        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse {
        return apiService.uploadTallyImage(request,file)
    }

    override suspend fun updateShortage(request: CommonRequest): CommonResponse {
        return apiService.updateShortage(request)
    }

    override suspend fun updateShipmentWithImage(request: CommonRequest): CommonResponse {
        return apiService.updateShipmentWithImage(request)
    }

    override suspend fun markException(request: CommonRequest): CommonResponse {
        return apiService.markException(request)
    }


}
