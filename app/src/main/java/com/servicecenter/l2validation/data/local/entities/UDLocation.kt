package com.servicecenter.l2validation.data.local.entities

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
@Keep
@JsonIgnoreProperties(ignoreUnknown = true)
data class UDLocation(
    var fe_lat: Double?,
    var fe_long: Double?,
    var consignee_lat: Double?,
    var consignee_long: Double?
)