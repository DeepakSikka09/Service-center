package com.servicecenter.l2validation.data.local.entities

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@Keep
@JsonIgnoreProperties(ignoreUnknown = true)
data class AwbDetails(
    @PrimaryKey
    var awb_no: Long,
    var order_id: String = "",
    var drs_id: Long,
    var product_type: String = "",
    var payment_type: String = "",
    var ud_time_stamp: String ,
    var ud_type: String = "",
    var action_type: String = "",
    var ud_reason_code: String = "",
    var ud_reason_code_name: String = "",
    var shipper_name: String = "",
    var shipper_code: String = "",
    var product_description: String = "",
    var fe_name: String,
    var fe_emp_code: String,
    var fe_number: Long ,
    var actionalble_days_max_limit: Int = 0,
    var ud_call_remark: String = "",
    var reschedule_remarks:String?="",
    var disconnected_by:String?="",
    var consignee_details: ConsigneeDetails,
    var ud_location: UDLocation,
    var callbridge_details: List<CallBridgeDetails>?,
    var call_recording: String?="",
    var last_call_start_time: Long?,
    var dc_location:DcLocation,
    var dc_range:Int

)


