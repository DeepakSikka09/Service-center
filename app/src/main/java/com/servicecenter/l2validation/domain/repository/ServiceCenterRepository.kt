package com.servicecenter.l2validation.domain.repository

import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.CommonResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Query


interface ServiceCenterRepository {
    suspend fun callLoginApi(commonRequest: CommonRequest): CommonResponse
    suspend fun callForgetPasswordApi(commonRequest: CommonRequest): CommonResponse
    suspend fun callVerifyOtp(commonRequest: CommonRequest): CommonResponse
    suspend fun callresendLogin_Otp(commonRequest: CommonRequest): CommonResponse
    suspend fun callResetPasswordWithOtpApi(commonRequest: CommonRequest): CommonResponse
    suspend fun callchangePasswordByUserNameApi(commonRequest: CommonRequest): CommonResponse
    suspend fun callPendingShipmentApi(location_code: String): CommonResponse
    suspend fun sendCommitPacketToServer(commonRequest: CommonRequest): CommonResponse
    suspend fun upLoadImage(
        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse


    //rtsApis
    suspend fun callRtsShipmentApi(location_code: String,pageNo:Int,pageSize:Int,fromDate:String?="",toDate:String?="",sorting:String?=""): CommonResponse

    suspend fun callCheckAwbApi(location_code: String,awbNo:String):CommonResponse

    suspend fun sendRtsPacketToServer(request: CommonRequest):CommonResponse

    suspend fun upLoadRtsImage(
        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse



    suspend fun callProductImageApi(airwaybill_number:String): CommonResponse

    suspend fun callFilterApi(location_code: String,udString: String): CommonResponse
    suspend fun callShipmentApi(commonRequest: HashMap<String,Any>): CommonResponse

    suspend fun callSendReSendOtp(awb:Long, event:String, reschedule_date:String): CommonResponse

    suspend fun callUDVerifyOtp(commonRequest: CommonRequest): CommonResponse

    suspend fun callCutOffTimeApi(dcCode: String): CommonResponse

    suspend fun callUdDetailApi(awb: Long,drs_id:Long,ud_status:String,dc_code:String): CommonResponse


    suspend fun getCallStatus(awb: Long,drs_id:Long): CommonResponse

    suspend fun getCallEvent(request: CommonRequest): CommonResponse



    suspend fun sendUdPacketToServer(request: CommonRequest):CommonResponse





    //SAL TALLY
    suspend fun salTallyShipment(location_code: String,employee_code: String):CommonResponse
    suspend fun updateShipment(request: CommonRequest):CommonResponse
    suspend fun reconStart(request: CommonRequest):CommonResponse
    suspend fun reconStatus(location_code: String,employee_code: String):CommonResponse
    suspend fun reconCancel(request: CommonRequest):CommonResponse
    suspend fun reconDone(request: CommonRequest):CommonResponse
    suspend fun uploadTallyImage(
        request: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): CommonResponse
    suspend fun updateShortage(request: CommonRequest):CommonResponse
    suspend fun updateShipmentWithImage(request: CommonRequest):CommonResponse
    suspend fun markException(request: CommonRequest):CommonResponse
}