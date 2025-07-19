package com.servicecenter.l2validation.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.servicecenter.l2validation.application.L2Application

object StateManager {

    val _livedata = MutableLiveData<APIResultState>()
    val livedata: LiveData<APIResultState> get() = _livedata

    val _loginLivedata = MutableLiveData<APIResultState>()
    val loginLivedata: LiveData<APIResultState> get() = _loginLivedata

    fun setState(newState: APIResultState) {
        if (newState.equals(APIResultState.Loading)) {
            L2Application.progressDialogCallbacks.showProgress()
        } else {
            L2Application.progressDialogCallbacks.hideProgress()
        }
        _livedata.setValue(newState)
    }

    fun getStateFlow(): LiveData<APIResultState> {
        return livedata
    }

    fun setLoginState(newState: APIResultState) {
        if (newState.equals(APIResultState.Loading)) {
            L2Application.progressDialogCallbacks.showProgress()
        } else {
            L2Application.progressDialogCallbacks.hideProgress()
        }
        _loginLivedata.setValue(newState)
    }

    fun getLoginStateFlow(): LiveData<APIResultState> {
        return loginLivedata
    }

    fun removeObserver(activity: AppCompatActivity) {
        getLoginStateFlow().removeObservers(activity)
        setLoginState(APIResultState.Empty)
    }

}
