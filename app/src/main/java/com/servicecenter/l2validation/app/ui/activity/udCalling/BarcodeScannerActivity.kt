package com.servicecenter.l2validation.app.ui.activity.udCalling

import android.os.Bundle
import android.view.View
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.setBeepSound
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.viewmodel.BarcodeScannerViewModel
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.ActivityBarcodeScannerBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.request_flag
import com.servicecenter.l2validation.utils.CustomScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BarcodeScannerActivity : BaseActivity<ActivityBarcodeScannerBinding,BarcodeScannerViewModel>(), View.OnClickListener  {

    private lateinit var customScanner: CustomScanner

    override fun getLayout(): Int =R.layout.activity_barcode_scanner

    override fun getViewModels(): Class<BarcodeScannerViewModel> {
       return BarcodeScannerViewModel::class.java
    }


    private var udStatus: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initially()

        customScanner = CustomScanner(
            binding.ivBarcode,
            { sValue ->
                setBeepSound()
                customScanner.pauseScanner()
                val inputDataSearch=HashMap<String,Any>()
                inputDataSearch[request_flag]=udStatus
                inputDataSearch["awb_no"]=sValue
                viewModel.callUDShipmentList(inputDataSearch)
                fetchShipmentListApiState()


            }, {
                binding.ivBarcode.pause()
                binding.ivBarcode.visibility = View.GONE
                binding.ivCross.visibility = View.VISIBLE
                binding.tvIncorrect.visibility = View.VISIBLE
                "Incorrect Awb Number".also { binding.tvIncorrect.text = it }
            },
            binding.ivFlash,
            this
        )
        customScanner.initializeScanner()

    }


    private fun initially() {
        binding.ivScannerBack.setOnClickListener(this)
        udStatus = intent.getStringExtra(Constants.UD_STATUS).toString()
    }

    override fun onResume() {
        super.onResume()
        if (::customScanner.isInitialized) {
            customScanner.startScanner()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::customScanner.isInitialized) {
            customScanner.unRegisterScanner()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::customScanner.isInitialized) {
            customScanner.pauseScanner()
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.iv_scannerBack -> {
                finish()
            }
        }
    }

    private fun fetchShipmentListApiState() {
        CoroutineScope(Dispatchers.Main).launch {String
            viewModel.udShipmentList.collect {
                when (it) {
                    is APIResultState.Success -> {
                      progressDialog().dismiss()
                        val shipmentList = it.data as Response

                        if (shipmentList.ud_shipment.isEmpty()) {
                            binding.ivBarcode.visibility = View.GONE
                            binding.ivCross.visibility = View.VISIBLE
                            binding.tvIncorrect.visibility = View.VISIBLE
                            binding.tvIncorrect.text=shipmentList.description

                        } else {
                            val awb=shipmentList.ud_shipment[0].awb_no
                            val drsId=shipmentList.ud_shipment[0].drs_id
                            val extras = Bundle().apply {
                                putLong(Constants.AWB_NUMBER,awb
                                )
                                putLong(Constants.DRS_ID, drsId)
                                putString(Constants.UD_STATUS, udStatus)
                            }
                            startScreen(UDShipmentActivity(), extras)
                            finish()

                        }
                    }
                    else -> {
                        manageApiFlowStatus(it, false)
                    }
                }
            }
        }
    }
}