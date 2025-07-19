package com.servicecenter.l2validation.data.remote.model
//Code Reviewed
import ErrorResponse
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.servicecenter.l2validation.data.local.entities.PendingRtsData

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommonResponse (
    var status: Boolean = false,
    var response:Response?,
    var error_response:ErrorResponse?
    //rts entries
    )