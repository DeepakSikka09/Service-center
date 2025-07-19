package com.servicecenter.l2validation.data.local.entities
//Code Reviewed
import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
@Keep
data class ConsigneeDetails(
    var consignee_name: String,
    var consignee_address: String,
    var consignee_mobile: String,
    var city: String,
    var pincode: String
)