package com.servicecenter.l2validation.application

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.servicecenter.l2validation.app.ui.interfaces.ProgressDialogCallbacks
import com.servicecenter.l2validation.utils.ProgressDialogUtil
import com.servicecenter.l2validation.utils.StateManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class L2Application : Application(), Application.ActivityLifecycleCallbacks,
    ProgressDialogCallbacks {

    lateinit var activity: Activity

    companion object {
        lateinit var progressDialogCallbacks: ProgressDialogCallbacks

    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        FirebaseApp.initializeApp(this)
        FirebaseAnalytics.getInstance(this)
        progressDialogCallbacks = this

    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        ProgressDialogUtil.createDialog(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        this.activity = activity
    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {
        StateManager.removeObserver(activity as AppCompatActivity)
    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    override fun showProgress() {
        ProgressDialogUtil.showProgressDialog()
    }

    override fun hideProgress() {
        ProgressDialogUtil.hideProgressDialog()
    }
}