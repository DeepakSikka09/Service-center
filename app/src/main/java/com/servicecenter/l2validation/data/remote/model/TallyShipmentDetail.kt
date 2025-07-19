package com.servicecenter.l2validation.data.remote.model

data class TallyShipmentDetail(
    var awb_no: Long = 0L,
    var product_type: String = "",
    var image_required: Boolean = false,
    var is_priority_shipment: Boolean = false,
    var is_sort_code: Boolean =false,
    var sort_code: String = "",
)