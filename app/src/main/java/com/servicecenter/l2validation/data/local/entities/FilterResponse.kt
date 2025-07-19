package com.servicecenter.l2validation.data.local.entities



    data class FilterResponse(
    val key_id:String,
    val key: String,
    val value: List<SubData>
)

data class SubData(val filter_value: String, val filter_code: String, var filter_checked: Boolean)



