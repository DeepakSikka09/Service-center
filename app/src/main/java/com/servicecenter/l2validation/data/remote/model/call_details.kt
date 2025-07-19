package com.servicecenter.l2validation.data.remote.model

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Keep
@JsonIgnoreProperties(ignoreUnknown = true)
class call_details (

    var call_status: String,
    var call_status_flag:Int,
    val call_attempt_exceed:Boolean,

    var is_final_status: String,
    var disconnected_by: String?,
    var recording_url: String,
    var client_correlation_id: String=""
)