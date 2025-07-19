package com.servicecenter.l2validation.data.local.entities
//Code Reviewed
import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
@Keep
data class DcLocation(
    var latitude: Double?,
    var longitude: Double?,
)