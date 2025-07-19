package com.servicecenter.l2validation.data.local.entities


data class UdRequest (
    var page_no: Int = 0,
    var page_size: Int = 0,
    var dc_code: String? = null,
    var request_flag: String? = null,
    var fe_emp_code: ArrayList<Any>? = null,
    var by_time: String? = null,
    var reason_code: ArrayList<Any>? = null,
    var shipper_code: ArrayList<Any>? = null,
    var product_type: ArrayList<Any>? = null,
    var ud_type: ArrayList<Any>? = null,
    var action_type: ArrayList<Any>? = null
)