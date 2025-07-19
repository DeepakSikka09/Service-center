package com.servicecenter.l2validation.app.ui.activity.rtsScanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.nlscan.android.scan.ScanManager
import com.nlscan.android.scan.ScanSettings
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.viewmodel.RtsScanViewModel
import com.servicecenter.l2validation.data.local.entities.PendingRtsData
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.ActivityRtsScanBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.cameraX.CameraxActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

@AndroidEntryPoint
class RtsScanActivity : BaseActivity<ActivityRtsScanBinding, RtsScanViewModel>(), View.OnClickListener {

    private var device: String? = null
    private lateinit var mScanMgr: ScanManager
    var lastText: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var pendingRtsShipmentList: ArrayList<PendingRtsData>? = ArrayList()
    private var latestText:String?=""
    override fun getLayout(): Int {
        return R.layout.activity_rts_scan
    }

    override fun getViewModels(): Class<RtsScanViewModel> {
        return RtsScanViewModel::class.java
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initialize()
        initializeScanner()
        checkAwb()
        analyticsToAllScreen(Constants.RTS_LIST_EVENT_1)
    }

    private fun fetchRtsListPendingList() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.rtsPendingListFlow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        startScanner()
                        it.data as Response
                        binding.tvPendingNo.text = it.data.total_count.toString()
                        pendingRtsShipmentList?.clear()
                        pendingRtsShipmentList?.addAll(it.data.rts_list)
                    }
                    is APIResultState.Failure->{
                        progressDialog().dismiss()
                        binding.disableLocationLayout.tvHeadingName.text=it.description
                        binding.disableLocationLayout.clHeader.visibility = View.VISIBLE
                        binding.constraintChildContainer.visibility = View.GONE
                        binding.consBarcodeNotRedable.visibility = View.GONE
                    }

                    else -> {
                        manageApiFlowStatus(it, false)
                    }
                }
            }
        }
    }

    private fun checkAwb() {
        lifecycleScope.launch {
            viewModel.rtsCheckAwb.collect {
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        it.data as Long
                        openNextActivity(it.data.toString())
                    }
                    is APIResultState.Failure-> {
                        progressDialog().dismiss()
                        showIncorrectAwbError(it.description,"Ok")
                    }
                    else -> {
                        manageApiFlowStatus(it, false)

                    }
                }
            }
        }
    }

    private fun pauseScanner() {
        if (device.equals(
                Constants._NEWLAND, ignoreCase = true
            ) || device.equals(
                Constants._NEW_NEWLAND, ignoreCase = true
            ) || device.equals(Constants._NEWLAND_T90, ignoreCase = true)
        ) {
            mScanMgr.setScanEnable(false)
        } else {
            binding.ivBarcode.pause()
        }
    }

    private fun startScanner() {
        if (device.equals(
                Constants._NEWLAND, ignoreCase = true
            ) || device.equals(
                Constants._NEW_NEWLAND, ignoreCase = true
            ) || device.equals(Constants._NEWLAND_T90, ignoreCase = true)
        ) {
            mScanMgr.setScanEnable(true)
        } else {
            binding.ivBarcode.resume()
        }
    }


    private fun initializeScanner() {
        device = (Build.MANUFACTURER + ":" + Build.MODEL).uppercase()
        if (device.equals(
                Constants._NEWLAND, ignoreCase = true
            ) || device.equals(
                Constants._NEW_NEWLAND, ignoreCase = true
            ) || device.equals(Constants._NEWLAND_T90, ignoreCase = true)
        ) {
            binding.ivBarcode.visibility = View.GONE
            mScanMgr = ScanManager.getInstance()
            mScanMgr.startScan()
            mScanMgr.disableBeep()
            mScanMgr.setScanEnable(true)
            mScanMgr.setOutpuMode(ScanSettings.Global.VALUE_OUT_PUT_MODE_BROADCAST)
            val settings: Map<String, String> = mScanMgr.scanSettings
            val sOutputMode = settings[ScanSettings.Global.OUT_PUT_MODE] //Acquire
        } else {
            val formats: Collection<BarcodeFormat> =
                listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128)
            binding.ivBarcode.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
            binding.ivBarcode.initializeFromIntent(intent)
            binding.ivBarcode.decodeContinuous(callback)
        }
    }

    private fun initialize() {
        binding.ivFlash.setOnClickListener(this)
        binding.clPending.setOnClickListener(this)
        binding.btnConst.setOnClickListener(this)
        binding.scanNextShipmentBtn.setOnClickListener(this)
        binding.customHeader.ivBackArrow.setOnClickListener(this)
        binding.customHeader.tvHeadingName.text = getString(R.string.rts_creation)

    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.iv_flash -> {
                switchFlashlight()
            }

            R.id.iv_back_arrow -> {
                finish()
            }

            R.id.scan_next_shipment_btn -> {
                binding.constraintChildContainer.visibility = View.VISIBLE
                binding.consInvalidAwb.visibility = View.GONE
                binding.btnConst.visibility = View.GONE
                binding.customHeader.clHeader.visibility = View.VISIBLE
                binding.constInfo.visibility=View.VISIBLE
                startScanner()
            }

            R.id.cl_pending -> {
                startScreen(RtsPendingListActivity())
            }

            R.id.rvp_label_cb -> {

            }
        }
    }


    private val mResultReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ScanManager.ACTION_SEND_SCAN_RESULT == action) {
                val bValue = intent.getByteArrayExtra(ScanManager.EXTRA_SCAN_RESULT_ONE_BYTES)
                var sValue = intent.getStringExtra("SCAN_BARCODE1")
                try {
                    if (sValue == null && bValue != null) sValue = String(bValue, charset("GBK"))
                    sValue = sValue ?: ""
                    if (sValue !== "" && sValue != null) {

                        val pattern = Pattern.compile(Constants.REGEX)
                        val matcher = pattern.matcher(sValue)
                        if (matcher.matches()) {
                            checkAwbInRtsList(sValue)

                        } else {
                            setErrorSound()
                            showIncorrectAwbError("Incorrect Awb Number","Scan Again")
                        }

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val callback: BarcodeCallback = BarcodeCallback { result ->
        try {

            lastText = result.text
            val pattern = Pattern.compile(Constants.REGEX)
            val matcher = pattern.matcher(lastText)
            if (matcher.matches()) {
                    checkAwbInRtsList(lastText!!)
            } else {
                setErrorSound()
                showIncorrectAwbError("Incorrect Awb Number","Scan Again")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showIncorrectAwbError(message:String,btnText:String) {
        latestText=""
        binding.constraintChildContainer.visibility = View.GONE
        if(btnText == "Ok"){
            binding.constInfo.visibility=View.GONE
        }
        binding.consInvalidAwb.visibility = View.VISIBLE
        binding.btnConst.visibility = View.VISIBLE
        binding.customHeader.clHeader.visibility = View.GONE
        binding.tvInvalid.text=message
        binding.scanNextShipmentBtn.text=btnText
        binding.scanNextShipmentBtn.setBackgroundColor(
            ContextCompat.getColor(this, R.color.blue_93)
        )
        pauseScanner()
    }

    private fun checkAwbInRtsList(scannedAwb: String) {
        var found = false
        pendingRtsShipmentList?.forEach {
            if (it.awb_number == scannedAwb.toLong()) {
                found = true
                openNextActivity(scannedAwb)
                return@forEach

            }
        }
        if (!found) {
            if(!latestText.equals(scannedAwb)){
                viewModel.checkAwbApi(scannedAwb)
                latestText=scannedAwb
            }

        }
    }


    override fun onResume() {
        super.onResume()

        if (isInternetAvailable(this)) {
            viewModel.callRtsShipmentApi()
        } else {
            showToast(getString(R.string.no_internet), false)
        }
        fetchRtsListPendingList()

        if (device.equals(
                Constants._NEWLAND, ignoreCase = true
            ) || device.equals(
                Constants._NEW_NEWLAND, ignoreCase = true
            ) || device.equals(Constants._NEWLAND_T90, ignoreCase = true)
        ) {
            val intFilter = IntentFilter(ScanManager.ACTION_SEND_SCAN_RESULT)
            registerReceiver(mResultReceiver, intFilter)
        } else {
            binding.ivBarcode.isSelected = true
            switchFlashlight()
            binding.ivBarcode.resume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (device.equals(
                Constants._NEWLAND, ignoreCase = true
            ) || device.equals(
                Constants._NEW_NEWLAND, ignoreCase = true
            ) || device.equals(Constants._NEWLAND_T90, ignoreCase = true)
        ) {
            mScanMgr.stopScan()
            unregisterReceiver(mResultReceiver)
        }
    }

    private fun switchFlashlight() {
        if (binding.ivFlash.isSelected) {
            binding.ivBarcode.setTorchOff()
            binding.ivFlash.isSelected = false
            binding.ivFlash.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.flash_off, theme
                )
            )
        } else {
            binding.ivBarcode.setTorchOn()
            binding.ivFlash.isSelected = true
            binding.ivFlash.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.flash_on, theme
                )
            )
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        binding.ivBarcode.setTorchOff()
    }


    fun setErrorSound() {
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.bad_beep)
        if (this.mediaPlayer != null && !this.mediaPlayer!!.isPlaying) {
            this.mediaPlayer!!.setVolume(100f, 100f)
            this.mediaPlayer!!.start()
        }
        val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(500)
        }
    }


    override fun onPause() {
        super.onPause()
        if (device.equals(
                Constants._NEWLAND, ignoreCase = true
            ) || device.equals(
                Constants._NEW_NEWLAND, ignoreCase = true
            ) || device.equals(Constants._NEWLAND_T90, ignoreCase = true)
        ) {
        } else {
            binding.ivBarcode.pause()
            //  binding.ivFlash.isSelected
            switchFlashlight()
        }
    }

    private fun openNextActivity(scannedAwb: String) {
        val extras = Bundle().apply {
            putString(Constants.flyerRelatedAirwillNo, scannedAwb)
            putBoolean(Constants.Is_RTS, true)
        }
        startScreen(CameraxActivity(), extras)
    }
}







