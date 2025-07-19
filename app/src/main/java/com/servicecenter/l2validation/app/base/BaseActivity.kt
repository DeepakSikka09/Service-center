package com.servicecenter.l2validation.app.base

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.databinding.ProgressbarLayoutBinding
import com.servicecenter.l2validation.utils.APIResultState

abstract class BaseActivity<VB:ViewBinding,VM:ViewModel>:AppCompatActivity() {

    lateinit var binding:VB
    lateinit var viewModel: VM
    private lateinit var progressDialogBar: Dialog

    @LayoutRes
    abstract fun getLayout():Int

    abstract fun getViewModels():Class<VM>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,getLayout())
        viewModel = ViewModelProvider(this)[getViewModels()]
        progressDialogBar = Dialog(this)
        adjustFontScale(getResources().getConfiguration());

    }

    fun progressDialog(): Dialog {
        val pBinding: ProgressbarLayoutBinding
        if (!progressDialogBar.isShowing) {
            val params = progressDialogBar.window!!.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT // set as full width
            params.height = WindowManager.LayoutParams.MATCH_PARENT//full height
            progressDialogBar.window?.setGravity(Gravity.CENTER_HORIZONTAL)
            pBinding = ProgressbarLayoutBinding.inflate(
                layoutInflater
            )
            progressDialogBar.setContentView(pBinding.root, params)
            progressDialogBar.window!!.setBackgroundDrawableResource(
                R.color.transparent
            )
            progressDialogBar.setCancelable(false)

        }
        return progressDialogBar
    }

    fun manageApiFlowStatus(apiResultState: APIResultState, isToastMessage: Boolean) {
        when (apiResultState) {
            is APIResultState.Loading -> {
                progressDialog().show()
            }

            is APIResultState.Failure -> {
                progressDialog().dismiss()
                if (isToastMessage) {
                    this.showToast(apiResultState.description, false)
                }
            }

            else -> {
                progressDialog().dismiss()
            }
        }
    }

    fun showToast(message: String, status: Boolean) {
        val customToastLayout = layoutInflater.inflate(R.layout.toast_layout, null)
        val customToast = Toast(this)
        customToast.view = customToastLayout
        customToastLayout.findViewById<TextView>(R.id.tv_status).text = message
        customToast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 10)
        customToast.duration = Toast.LENGTH_SHORT
        if (status) {
            customToastLayout.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.container)
                .setBackgroundResource(R.drawable.success_toast_bg)
            customToastLayout.findViewById<TextView>(R.id.tv_status)
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.success_toast_icon, 0, 0, 0)
        } else {
            customToastLayout.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.container)
                .setBackgroundResource(R.drawable.error_toast_bg)
            customToastLayout.findViewById<TextView>(R.id.tv_status)
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.error_toast_icon, 0, 0, 0)

        }
        customToast.show()
    }
    fun AppCompatActivity.navigateToActivity(targetActivity: Class<*>) {
        val intent = Intent(this, targetActivity)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    fun adjustFontScale(configuration: Configuration) {
        if (configuration.fontScale <= 0.9 || configuration.fontScale > 1.30) {
            configuration.fontScale = 1.15f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
            }
            val metrics = resources.displayMetrics
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getMetrics(metrics)
            metrics.scaledDensity = configuration.fontScale * metrics.density
            baseContext.resources.updateConfiguration(configuration, metrics)
        }
    }
}