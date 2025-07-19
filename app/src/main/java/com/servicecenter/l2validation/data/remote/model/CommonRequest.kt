package com.servicecenter.l2validation.data.remote.model
//Code Reviewed
import androidx.annotation.Keep
import com.servicecenter.l2validation.data.local.entities.FilterResponse
import com.servicecenter.l2validation.data.local.entities.QualityCheck
import java.io.File

@Keep
class CommonRequest(
    var user_name: String? = "",
    var username: String? = "",
    var password: String? = "",
    var new_password: String? = "",
    var otp: String? = "",
    var old_password: String? = "",
    var airwaybill_number: String? = "",
    var drs_id: String? = "",
    var dc_code: String? = "",
    var employee_code: String? = "",
    var status: String? = "",
    var image_keys: ArrayList<String>? = null,
    var app_code: String? = "",
    var image_name: String? = "",
    var by_time: String? = "ASC",
    var image: File?=null,
    //var qc_answer: HashMap<Int, QualityCheck>?=null
    //var qc_answer: ArrayList<QualityCheck>?=null, Not used

    //UD calling
    var request_flag:String?="",
    var reschedule_date:String?="",
    var scheduled_delivery_date:String?="",
    var source:String?="",
    var request_type:String?="",
    var ud_type:String?="",//(make another ud_type key )
    /* var pageCount:Int=0,
     var pageSize:Int=15,*/

    var page_no:Int?=1,
    var page_size:Int?=10,


    var event:String?="",
    var remarks:String?="",
    var reschedule_remarks:String?="",
    var client_correlation_id:String?="",
    /*
    var fe_name:ArrayList<String>?=null,
    var by_times:ArrayList<String>?=null,
    var reason_code:ArrayList<String>?=null,
    var shipper:ArrayList<String>?=null,
    var product_type:ArrayList<String>?=null,
    var ud_type:ArrayList<String>?=null,
    var action_type:ArrayList<String>?=null,*/

    var filters: ArrayList<FilterResponse> = ArrayList<FilterResponse>(),

    var fe_image_matched:String?="",
    val l2_validation_additional_info: Map<String, String?>?=null,
    var qc_answer: List<Map<String, Any>>? = null,

    //rtsRequestParameter
    val image_list: ArrayList<Map<String,String>>?=null,
    val ref_awb_no:String?="",
    //adding the employee Device Information
    var device_info: DeviceDetailsRequest? = null,

    //sal tally
    var emp_code:String="",
    var awb_no:Long = 0,
    var awb_list:List<Long> = listOf(),
    var exception_reason:String = "",
    var exception_image_url:String = "",
    var front_image_url:String="",
    var back_image_url:String=""
)


