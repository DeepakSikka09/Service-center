package com.servicecenter.l2validation.app.ui.activity.udCalling

import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.fragment.CallDisconnectedFragment
import com.servicecenter.l2validation.app.ui.activity.udCalling.fragment.MarkUDValidationFragment
import com.servicecenter.l2validation.app.ui.activity.udCalling.fragment.UdShipmentDetailFragment
import com.servicecenter.l2validation.app.ui.viewmodel.UDShipmentViewModel
import com.servicecenter.l2validation.databinding.ActivityUdShipmentDetailsBinding
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.PERMISSION_REQUEST_CODE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class UDShipmentActivity : BaseActivity<ActivityUdShipmentDetailsBinding, UDShipmentViewModel>() {

    override fun getLayout(): Int = R.layout.activity_ud_shipment_details

    override fun getViewModels(): Class<UDShipmentViewModel> {
        return UDShipmentViewModel::class.java
    }

    private var awbNumber: Long = 0L
    private var drsId: Long = 0L
    private var udStatus: String = ""
    private var itemType: String? = ""
    private var isBroadCastRegistered = false
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var callReceiver: CallReceiverBroadcast
    private var fragmentCallback: () -> Unit = {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        itemType = intent.getStringExtra(Constants.UD_STATUS)
        awbNumber = intent.getLongExtra(Constants.AWB_NUMBER, 0L) // Get AWB number as Long
        drsId = intent.getLongExtra(Constants.DRS_ID, 0L)
        udStatus = intent.getStringExtra(Constants.UD_STATUS).toString()
        setupFragmentUI()

        // Initialize TelephonyManager to listen to call states
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Register the receiver to listen for changes in phone state
        registeredCall()
        cancelBroadCast()
    }


    private fun cancelBroadCast() {
        lifecycleScope.launch {
            viewModel.broacastCancel.collect {
                if (it) {
                    Log.d("check_broadCast", "cancel")
                    unregisterBroadcast()
                }
            }
        }
    }

    private fun registeredCall() {
        isBroadCastRegistered = true
        callReceiver = CallReceiverBroadcast()
        callReceiver.setCallback {
            if (isFragmentVisible() is MarkUDValidationFragment) {
                replaceFragment(CallDisconnectedFragment(), "CallDisconnectedFragment")
            } else if (isFragmentVisible() is UdShipmentDetailFragment) {

                fragmentCallback()

            }

        }
        val filter = IntentFilter()
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(callReceiver, filter)
    }

    fun setFragmentCallback(callback: () -> Unit) {
        fragmentCallback = callback
    }

    private fun setupFragmentUI() {
        binding.productDetails.ivBackArrow.visibility = View.GONE
        addFragment()
    }

    private fun addFragment() {
        if (supportFragmentManager.findFragmentById(R.id.ud_shipment_container) == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.ud_shipment_container, UdShipmentDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString(Constants.ITEM_TYPE, itemType)
                        putLong(Constants.AWB_NUMBER, awbNumber)
                        putLong(Constants.DRS_ID, drsId)
                        putString(Constants.UD_STATUS, udStatus)

                    }

                })
                .commit()
        }

    }

    fun replaceFragment(getFragment: Fragment, fragmentName: String) {
        if (!isFinishing && !isChangingConfigurations) {
            supportFragmentManager.beginTransaction()
                .add(R.id.ud_shipment_container, getFragment)
                .addToBackStack(fragmentName)
                .commit()
        }
    }


    override fun onDestroy() {
        unregisterBroadcast()
        super.onDestroy()
    }

    private fun unregisterBroadcast() {
        if (isBroadCastRegistered) {
            isBroadCastRegistered = false
            unregisterReceiver(callReceiver)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                val allPermissionsGranted =
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allPermissionsGranted) {
                    // Permissions granted
                    Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT)
                        .show()
                    //checkLocationPermissionAndFetch() // Call the location-fetching method
                } else {
                    // Permissions denied
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    private fun isFragmentVisible(): Fragment? {
        supportFragmentManager.executePendingTransactions()
        val fragment = supportFragmentManager.findFragmentById(R.id.ud_shipment_container)
        return fragment
    }

    companion object {
        var correlationId = ""
    }
}

