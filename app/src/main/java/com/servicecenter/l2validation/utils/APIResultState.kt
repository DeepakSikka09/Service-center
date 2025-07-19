package com.servicecenter.l2validation.utils

// Code Reviewed

sealed class APIResultState {
    object Loading : APIResultState()
    data class Success(val data: Any?) : APIResultState()
    data class Failure(val description: String,val data: Any? = null) : APIResultState()
    object Empty : APIResultState()

}
