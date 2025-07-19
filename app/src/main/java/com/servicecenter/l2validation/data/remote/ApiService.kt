package com.servicecenter.l2validation.data.remote
//Code Reviewed
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.CommonResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

interface ApiService {
    @POST("mdm/loginUser")
    suspend fun callLoginApi(@Body commonRequest: CommonRequest): CommonResponse

    @POST("mdm/forgetPassword")
    suspend fun callForgetPasswordApi(@Body commonRequest: CommonRequest): CommonResponse

    @POST("mdm/resetPasswordWithOtp")
    suspend fun callResetPasswordWithOtpApi(@Body commonRequest: CommonRequest): CommonResponse

    @POST("mdm/changePasswordByUserName")
    suspend fun callchangePasswordByUserNameApi(@Body commonRequest: CommonRequest): CommonResponse

    @POST("mdm/verify-otp")
    suspend fun callVerifyOtp(@Body commonRequest: CommonRequest): CommonResponse

    @POST("mdm/resendLogin-Otp")
    suspend fun callresendLogin_Otp(@Body commonRequest: CommonRequest): CommonResponse

    @GET("last_mile/service/sca/get_rvp_qc_summary")
    suspend fun callPendingShipmentApi(@Query("location_code") location_code: String): CommonResponse

    @PUT("last_mile/service/rvp/l2/validation")
    suspend fun sendCommitPacketToServer(@Body commonRequest: CommonRequest): CommonResponse

    @POST("image/services/last_mile/image/v1/service-center-rvp/postImage/")
    @Multipart
    suspend fun upLoadImage(
        @PartMap map: @JvmSuppressWildcards Map<String, RequestBody>,
        @Part image: MultipartBody.Part
    ): CommonResponse


    //Product Image

    @GET("last_mile/service/rvp/l2/validation/details")
    suspend fun callProductImageApi(@Query("airwaybill_number") airwaybill_number: String): CommonResponse

    //UD Calling

    @POST("last_mile/ud_calling/get_ud_calling_summary")
    suspend fun callShipmentApi(@Body inputData :HashMap<String,Any>): CommonResponse

    @GET("last_mile/ud_calling/get_ud_calling_filters")
    suspend fun callFilterApi(@Query("dcCode") location_code: String,@Query("requestType") request_type: String): CommonResponse

    @GET("last_mile/ud_calling/get_ud_awb_details")
    suspend fun callUdAwbDetailApi(@Query("awb") awb: Long,@Query("drs_id")drs_id: Long,@Query("ud_status")ud_status:String,@Query("dc_code")dc_code:String): CommonResponse


    @GET("last_mile/ud_calling/get_call_details")
    suspend fun getCallStatus(@Query("awb") awb: Long,@Query("drs_id")drs_id: Long): CommonResponse


    @POST("last_mile/ud_calling/get_call_event")
    suspend fun getCallEvent(@Body commonRequest: CommonRequest): CommonResponse




    @PUT("otp/validate_otp")
    suspend fun callUDVerifyOtp(@Body commonRequest: CommonRequest): CommonResponse

    @POST("last_mile/service/util/trigger_sms")
    suspend fun callSendReSendOtp(@Query("awb") awb: Long,@Query("event") event: String,@Query("reschedule_date") reschedule_date: String): CommonResponse

    @POST("last_mile/ud_calling/ud_calling_commit_api")
    suspend fun sendUdPacketToServer(@Body commonRequest: CommonRequest): CommonResponse



    //RtsApis
    @GET("last_mile/service/sca/rts")

    suspend fun callRtsPendingShipmentApi(
        @Query("dc_code") location_code: String,
        @Query("page_no") page_no: Int,
        @Query("page_size") page_size: Int,
        @Query("from_date") from_date: String?,
        @Query("to_date") to_date: String?,
        @Query("sort_order") sorting: String?
    ): CommonResponse

    @GET("last_mile/service/sca/rts/fetch_rts")
    suspend fun callRtsAwbCheckApi(
        @Query("dc_code") location_code: String,
        @Query("awb") awbNo: String
    ): CommonResponse

    @POST("last_mile/service/sca/rts/create_return")
    suspend fun sendRtsPacketToServer(@Body commonRequest: CommonRequest): CommonResponse


    @POST("image/services/last_mile/image/v1/rts/postImage/")
    @Multipart
    suspend fun upLoadRtsImage(
        @PartMap map: @JvmSuppressWildcards Map<String, RequestBody>,
        @Part image: MultipartBody.Part
    ): CommonResponse

    @GET("last_mile/service/sca/activity/saltally/details")
    suspend fun salTallyShipment(
        @Query("dc_code") location_code: String,
        @Query("employee_code") employee_code: String
    ):CommonResponse
    @POST("last_mile/service/sca/activity/saltally/update_shipment_scan")
    suspend fun updateShipment(@Body request: CommonRequest):CommonResponse
    @POST("last_mile/service/sca/activity/saltally/start")
    suspend fun reconStart(@Body request: CommonRequest):CommonResponse
    @GET("last_mile/service/sca/activity/saltally/status")
    suspend fun reconStatus(
        @Query("dc_code") location_code: String,
        @Query("employee_code") employee_code: String
    ):CommonResponse
    @PUT("last_mile/service/sca/activity/saltally/cancel")
    suspend fun reconCancel(@Body request: CommonRequest):CommonResponse
    @PUT("last_mile/service/sca/activity/saltally/done")
    suspend fun reconDone(@Body request: CommonRequest):CommonResponse

    @POST("image/services/last_mile/image/v1/salTally/postImage/")
    @Multipart
    suspend fun uploadTallyImage(
        @PartMap map: @JvmSuppressWildcards Map<String, RequestBody>,
        @Part image: MultipartBody.Part
    ): CommonResponse

    @POST("last_mile/service/sca/activity/saltally/update_shortage")
    suspend fun updateShortage(@Body request: CommonRequest):CommonResponse
    @POST("last_mile/service/sca/activity/saltally/update_recon_with_image")
    suspend fun updateShipmentWithImage(@Body request: CommonRequest):CommonResponse

    @POST("last_mile/service/sca/activity/saltally/update_exception")
    suspend fun markException(@Body request: CommonRequest):CommonResponse

    // DC CutOFF time
    @GET("last_mile/service/util/delivery_cutoff_time")
    suspend fun callCutOffTimeApi(@Query("dcCode") dcCode: String): CommonResponse

}

