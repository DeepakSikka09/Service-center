package com.servicecenter.l2validation.app.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.databinding.ProgressbarLayoutBinding
import com.servicecenter.l2validation.utils.APIResultState

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    lateinit var binding: VB
    private lateinit var progressDialogBar: Dialog

    @LayoutRes
    abstract fun getLayout(): Int

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, getLayout(), container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressDialogBar = Dialog(requireContext())
    }

    fun progressDialog(): Dialog {
        val params = progressDialogBar.window!!.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT // set as full width
            params.height = WindowManager.LayoutParams.MATCH_PARENT//full height
            progressDialogBar.window?.setGravity(Gravity.CENTER_HORIZONTAL)
        val pBinding: ProgressbarLayoutBinding = ProgressbarLayoutBinding.inflate(
                layoutInflater
            )
            progressDialogBar.setContentView(pBinding.root, params)
            progressDialogBar.window!!.setBackgroundDrawableResource(
                R.color.transparent
            )
            progressDialogBar.setCancelable(false)


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
        val customToast = Toast(this.requireContext())
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

    fun analyticsToAllButton(eventName: String, key: String, value: String) {
        val bundle = Bundle()
        bundle.putString(key, value)
        FirebaseAnalytics.getInstance(requireActivity()).logEvent(eventName, bundle)
    }

    fun navigate(destination: Int, bundle: Bundle?) {
        findNavController().navigate(destination, bundle)
    }

    fun navigate(destination: Int) {
        findNavController().navigate(destination)
    }

    fun popBackStack() {
        findNavController().popBackStack()

    }

    fun popBackStack(destination: Int, isInclusive: Boolean) {
        findNavController().popBackStack(destination, isInclusive)
    }

    fun popBackWithData(bundle: Bundle?) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set("bundle", bundle)
        popBackStack()
    }

    fun hideKeyboard() {
        val view = requireActivity().currentFocus
        val methodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (view != null) {
            methodManager.hideSoftInputFromWindow(
                view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

}