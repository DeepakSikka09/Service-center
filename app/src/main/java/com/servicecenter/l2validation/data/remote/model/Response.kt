package com.servicecenter.l2validation.data.remote.model
//Code Reviewed
import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.servicecenter.l2validation.data.local.entities.AwbDetails
import com.servicecenter.l2validation.data.local.entities.PendingRtsData
import com.servicecenter.l2validation.data.local.entities.FilterResponse
import com.servicecenter.l2validation.data.local.entities.QualityCheck
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail
import com.servicecenter.l2validation.data.local.entities.UDShipment

@JsonIgnoreProperties(ignoreUnknown = true)
@Keep
data class Response(
    // MDC Response packet
    var description: String = "",
    var AUTH_TOKEN: String,
    var session_timeout: Int,
    var user_name: String,
    var first_name: String?=null,
    var last_name: String?=null,
    var contact_no: String?=null,
    var location_code: String?=null,
    var department_code: String?=null,
    var designation_code: String?=null,
    var Pending_shipment:ArrayList<ShipmentDetail>,
    var image_id:Long=0L,
    var airwaybill_number:String?=null,
    var service_center_type:String="",
    var last_login:String ="",
    var group_code:String ="",
    var is_qc_required:Boolean,

    //qc question
    //var AWB_No:String,
    var pageCount:Int=0,
    var product_images: List<String> = emptyList(),
    var quality_checks: List<QualityCheck> = emptyList(),

    //rtsData

    var page_count: Int,
    var total_count: Int,
    var rts_list_count:Int,
    var rts_list:ArrayList<PendingRtsData>,
    var awb_number:Long,
    var fe_images: List<String> = listOf(),
    //rtsfileName
    var file_name:String="",

    var flyer_images: List<String> = emptyList(),
    var qc_questions:List<QCQuestion> = emptyList(),


    var ud_shipment:ArrayList<UDShipment>,
    var awb_details:AwbDetails,
    var filters: ArrayList<FilterResponse> = ArrayList<FilterResponse>(),
    var call_details:call_details,
    var is_marked_no_response:Boolean=false,
    var cutoff_time:Long=0L,
    //tally response
    var tally_shipment:List<TallyShipmentDetail>? = listOf(),
    var total_shipments:Int? = 0,
    var scanned_shipments:Int? = 0,
    var recon_status:Int? = 0,
    var awb_no:Long?=0,
    var image_required:Boolean?=false,
    var is_priority_shipment:Boolean?= false,
    var is_sort_code:Boolean?=false,
    var sort_code:String?="",
    var product_type:String?="",
    var error_map:List<Long> = emptyList(),
    var toggle_exception:Boolean? = false,
    var is_rto:Boolean? = false,
    var is_ofd:Boolean? = false,
    var is_dmg:Boolean?=false
)
