package com.servicecenter.l2validation.data.local.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Entity
@Keep
@JsonIgnoreProperties(ignoreUnknown = true)
data class ShipmentDetail(
    @PrimaryKey
    var AWB_No:Long,
    var flyer_code:String,
    var date:String,
    var status:String,
    var name:String,
    var employee_code:String,
    var is_fe_image_validation_required:Boolean,
    var l1_qc_validation_required:Boolean
)