package com.servicecenter.l2validation.data.local.entities

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Keep
@JsonIgnoreProperties(ignoreUnknown = true)
data class CallBridgeDetails(
    var callbridge_number: String,
    var pin: Int,
    var vendor_name: String
)
