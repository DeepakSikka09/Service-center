package com.servicecenter.l2validation.data.local.entities

data class DashboardItem(
    val icon: Int,
    val title: String,
    val code:DashboardEnum
)

enum class DashboardEnum {
    RVP_VALIDATION,
    RVP_HANDOVER,
    UD_CALLING,
    RTS,
    SAL_TALLY
}
