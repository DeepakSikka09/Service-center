package com.servicecenter.l2validation.data.remote.model

import com.google.errorprone.annotations.Keep

@Keep
data class UpdateRequiredModel(
    val updateUrl: String? = null,
    val description: String?,
    val versionName: String?
)
