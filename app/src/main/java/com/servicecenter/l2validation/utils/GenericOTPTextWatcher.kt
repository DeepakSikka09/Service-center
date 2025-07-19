package com.servicecenter.l2validation.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.servicecenter.l2validation.R

class GenericOTPTextWatcher internal constructor(private val currentView: View, private val nextView: View?) : TextWatcher {
    override fun afterTextChanged(editable: Editable) { // TODO Auto-generated method stub
        val text = editable.toString()
        when (currentView.id) {
            R.id.ed_otp_1 -> if (text.length == 1) nextView!!.requestFocus()
            R.id.ed_otp_2 -> if (text.length == 1) nextView!!.requestFocus()
            R.id.ed_otp_3 -> if (text.length == 1) nextView!!.requestFocus()
            R.id.ed_otp_4 -> if (text.length == 1) nextView!!.requestFocus()
            R.id.ed_otp_5 -> if (text.length == 1) nextView!!.requestFocus()
        }
    }

    override fun beforeTextChanged(arg0: CharSequence?, arg1: Int, arg2: Int, arg3: Int) {
    }

    override fun onTextChanged(arg0: CharSequence?, arg1: Int, arg2: Int, arg3: Int) {
    }

}