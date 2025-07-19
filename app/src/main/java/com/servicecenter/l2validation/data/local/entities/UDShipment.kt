package com.servicecenter.l2validation.data.local.entities

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Entity
@Keep
@JsonIgnoreProperties(ignoreUnknown = true)
data class UDShipment(
    @PrimaryKey
    var awb_no: Long,
    var drs_id: Long,
    var product_type: String,
    var payment_type: String,
    var ud_time_stamp: String?="",
    var fe_name: String,
    var fe_emp_code: String,
    var ud_type: String,
    var action_type: String,

)




