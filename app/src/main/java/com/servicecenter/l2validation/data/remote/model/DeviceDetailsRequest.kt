package com.servicecenter.l2validation.data.remote.model

import androidx.annotation.Keep

@Keep
class DeviceDetailsRequest (
    var sdk_version_code: String? = "",
    var model_number: String? = "",
    var manufacturer: String? = "",
    var sdk_version: String? = "",
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0
)