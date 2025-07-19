package com.servicecenter.l2validation.app.ui.activity.udCalling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiverBroadcast : BroadcastReceiver() {
    private  var callback: ()->Unit={}

    fun setCallback(callback: ()->Unit) {
        this.callback = callback
    }

    override fun onReceive(context: Context, intent: Intent) {

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                Log.d("CallReceiver", "Incoming call")
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.d("CallReceiver", "Call is active (off-hook)")

            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                callback()
                Log.d("CallReceiver", "Call ended or idle")
            }
        }
    }
}
