package com.servicecenter.l2validation.data.local.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Entity
@Keep
@JsonIgnoreProperties(ignoreUnknown = true)
data class PendingRtsData(
    @PrimaryKey
    var awb_number:Long,
    var child_awb_number:String?="",
    var order_number:String,
    var customer_name:String,
    var customer_code:String,
    var rto_lock_applied_timestamp:String?="",
    var image_count:Int)

