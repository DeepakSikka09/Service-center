package com.servicecenter.l2validation.utils
// Code reviewed
import android.app.Activity
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar

object ProgressDialogUtil {

    private var progressDialog: Dialog? = null

    fun createDialog(context: Activity){
        progressDialog = Dialog(context)
        progressDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog?.setContentView(ProgressBar(context))
        progressDialog?.setCancelable(false)
        progressDialog?.setCanceledOnTouchOutside(false)
    }
    fun showProgressDialog() {
        try
        {
           /* if (progressDialog == null) {*/

     /*       }*/
            if (!progressDialog?.isShowing!!) {
                progressDialog?.show()
            }
        }
        catch (e :Exception)
        {
            e.printStackTrace()
        } }

    fun hideProgressDialog() {
       // progressDialog?.dismiss()
        try {
            if (progressDialog?.isShowing == true) {
                progressDialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
