package com.servicecenter.l2validation.data.remote.model
import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

@Keep
@JsonIgnoreProperties(ignoreUnknown = true)
data class QCQuestion(
    var qc_parameter_id: Int,
    var qc_value: String,
    var qc_name: String,
    var instructions: String,
    var fe_image: String,
    var answer: String? = null
) : Serializable
