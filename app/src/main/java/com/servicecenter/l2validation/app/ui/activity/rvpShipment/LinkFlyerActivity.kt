package com.servicecenter.l2validation.app.ui.activity.rvpShipment

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.nlscan.android.scan.ScanManager
import com.nlscan.android.scan.ScanSettings
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.R.string.scan_shipment_label
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.activity.DashboardActivity
import com.servicecenter.l2validation.app.ui.viewmodel.LinkFlyerViewModel
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.ActivityLinkFlyerBinding
import com.servicecenter.l2validation.databinding.CustomAlertMessageBinding
import com.servicecenter.l2validation.databinding.CustomDialogMessageBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.Failed
import com.servicecenter.l2validation.utils.Constants.pending
import com.servicecenter.l2validation.utils.cameraX.CameraxActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Arrays
import java.util.regex.Pattern


@AndroidEntryPoint
class LinkFlyerActivity : BaseActivity<ActivityLinkFlyerBinding, LinkFlyerViewModel>(), View.OnClickListener {

    lateinit var shipmentList: List<ShipmentDetail>
    private var device: String? = null
    private lateinit var mScanMgr: ScanManager
    private var scanFrom = ""
    var lastTextFlyerCode: String = ""
    private var flyerRelatedAirwayNo: Long = 0
    var lastText: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var pendingList: ArrayList<ShipmentDetail>
    private lateinit var failedList: ArrayList<ShipmentDetail>
    private var attempt: Int = 2
    private var isFeImageValidationRequired: Boolean = false
    private var l1QcValidationRequired: Boolean = false
    private lateinit var dialog: Dialog
    private var frontImageID: Long = 0L
    private var backImageID: Long = 0L

    override fun getLayout(): Int {
        return R.layout.activity_link_flyer
    }

    override fun getViewModels(): Class<LinkFlyerViewModel> {
        return LinkFlyerViewModel::class.java
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shipmentList=ArrayList()
        initlize()
        initializeScanner()

        if (isInternetAvailable(this)) {
            viewModel.callShipmentListApi()
        } else {
            showToast(getString(R.string.no_internet), false)
        }
        fetchShipmentListApiState()
        getFlyerCodeShipmentList()
        getAllShipmentList()
        fetchResponse()

        analyticsToAllScreen(Constants.FLYER_BARCODE_SCAN)
    }

    private fun fetchShipmentListApiState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.ShipmentListApiflow.collect {
                when (it) {
                    is APIResultState.Success -> {
                        startScanner()
                        viewModel.getAllPendingShipment()
                        progressDialog().dismiss()

                    }

                    else -> {
                        manageApiFlowStatus(it, false)
                     //  pauseScanner()
                    }
                }
            }
        }
    }


    private fun getAllShipmentList() {
        lifecycleScope.launch {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.allShipmentList.collect { allShipmentList ->
                when (allShipmentList) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        getPendingList(allShipmentList.data as List<ShipmentDetail>)
                    }

                    else -> {
                        manageApiFlowStatus(allShipmentList, true)
                    }
                }
            }
        }
    }

    private fun getPendingList(allShipmentList: List<ShipmentDetail>) {
        pendingList = ArrayList<ShipmentDetail>()
        failedList = ArrayList<ShipmentDetail>()
        for (i in allShipmentList.indices) {
            if (allShipmentList[i].status.equals("pending", ignoreCase = true)) {
                pendingList.add(allShipmentList[i])
            } else if (allShipmentList[i].status.equals("failed", ignoreCase = true)) {
                failedList.add(allShipmentList[i])
            }
        }
        binding.tvPendingNo.text = pendingList.size.toString()
        binding.tvFailedNo.text = failedList.size.toString()
    }

    private fun getFlyerCodeShipmentList() {
        lifecycleScope.launch {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.ShipmentList.collect { shipmentList1 ->
                when (shipmentList1) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        shipmentList = shipmentList1.data as List<ShipmentDetail>
                        if(::dialog.isInitialized && dialog.isShowing && dialog!=null){
                            dismissDialog(dialog)
                        }
                        if((shipmentList1.data as List<ShipmentDetail>).size>0) {
                            checkFeValidation(shipmentList1.data as List<ShipmentDetail>)

                        }else{
                            if(::dialog.isInitialized && dialog.isShowing && dialog!=null){
                                dismissDialog(dialog)
                            }
                            shipmentList.let {
                                showCustomDialog(
                                    getString(R.string.flyer_code_not_in_the_pending_list), false,
                                    it
                                )
                            }
                        }

                    }

                    else -> {
                        manageApiFlowStatus(shipmentList1, true)
                    }
                }
            }
        }
    }

    private fun checkFeValidation(shipmentDetails: List<ShipmentDetail>) {
        val awb = shipmentDetails[0].AWB_No.toString()
        isFeImageValidationRequired = shipmentDetails[0].is_fe_image_validation_required
        l1QcValidationRequired = shipmentDetails[0].l1_qc_validation_required
        if (isFeImageValidationRequired) {
            flyerCodeExist(shipmentDetails)

            lifecycleScope.launch(Dispatchers.Main) {
                delay(1000)
                dismissDialog(dialog)
                val intent = Intent(this@LinkFlyerActivity, CameraxActivity::class.java).apply {
                    putExtra(Constants.LINK_FLYER_ACTIVITY, true)
                    putExtra(Constants.flyerRelatedAirwillNo, awb)
                }
                cameraActivityResultLauncher.launch(intent)
            }
        } else {
            flyerCodeExist(shipmentDetails)
        }
    }

    private fun flyerCodeExist(shipmentList: List<ShipmentDetail>) {
        if (shipmentList.size > 0) {
            setBeepSound()
            pauseScanner()
            if (!this.isFinishing) {
                analyticsToAllButton(Constants.FLYER_BARCODE_SCAN,Constants.SCAN_KEY, shipmentList[0].flyer_code)

                showCustomDialog(
                    getString(R.string.flyer_code_scanned_now_scan_awb_no), true, shipmentList
                )
            }

        } else {
            setErrorSound()
            pauseScanner()
            if (!this.isFinishing) {
                showCustomDialog(
                    getString(R.string.flyer_code_not_in_the_pending_list), false, shipmentList
                )
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
            Log.d("dsgh", "sdhgjakl")
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

    private fun GoForShipmentScan(shipmentList: List<ShipmentDetail>) {
        binding.tvScanShipment.text = getString(scan_shipment_label)
        binding.clPending.visibility = View.GONE
        binding.constrntFailed.visibility = View.GONE
        binding.scanNextShipmentBtn.visibility = View.VISIBLE
        binding.btnConst.visibility = View.VISIBLE
        binding.constInfo.visibility = View.VISIBLE
        binding.rvpLabelCb.visibility = View.VISIBLE
        binding.tvInfoDetails.text = getString(R.string.shipment_label_not_readable)
        flyerRelatedAirwayNo = shipmentList[0].AWB_No
        scanFrom = Constants.SHIPMENT_LABEL

        analyticsToAllButton(Constants.SHIPMENT_LABEL_SCAN,Constants.SCAN_KEY,""+flyerRelatedAirwayNo)


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
            val settings: Map<String, String> = mScanMgr.getScanSettings()
            val sOutputMode = settings[ScanSettings.Global.OUT_PUT_MODE] //Acquire
        } else {
            val formats: Collection<BarcodeFormat> =
                Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128)
            binding.ivBarcode.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
            binding.ivBarcode.initializeFromIntent(intent)
            binding.ivBarcode.decodeContinuous(callback)
        }
    }

    private fun initlize() {
        binding.ivFlash.setOnClickListener(this)
        binding.clPending.setOnClickListener(this)
        binding.btnConst.setOnClickListener(this)
        binding.rvpLabelCb.setOnClickListener(this)
        binding.constrntFailed.setOnClickListener(this)
        binding.scanNextShipmentBtn.setOnClickListener(this)
        binding.customHeader.ivBackArrow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.iv_flash -> {
                switchFlashlight()
            }

            R.id.iv_back_arrow -> {
                finish()
            }

            R.id.btn_const -> {
            }

            R.id.cl_pending -> {
                analyticsToAllButton(Constants.FLYER_BARCODE_SCAN,Constants.BUTTON_KEY,Constants.PENDING_SHIPMENTS)

                val extras = Bundle().apply {
                    putString("came_from", pending)
                }
                startScreen(ShipmentListActivity(), extras)
            }

            R.id.constrnt_failed -> {
                analyticsToAllButton(Constants.FLYER_BARCODE_SCAN,Constants.BUTTON_KEY,Constants.FAILED_SHIPMENTS)
                val extras = Bundle().apply {
                    putString("came_from", Failed)
                }
                startScreen(ShipmentListActivity(), extras)
            }

            R.id.rvp_label_cb -> {
                if (binding.rvpLabelCb.isChecked) {
                    pauseScanner()
                    binding.scanNextShipmentBtn.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.blue_93)
                    )
                    binding.scanNextShipmentBtn.isEnabled = true
                } else {
                    startScanner()
                    binding.scanNextShipmentBtn.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.disable_btn)
                    )
                    binding.scanNextShipmentBtn.isEnabled = false
                }
            }

            R.id.scan_next_shipment_btn -> {
                analyticsToAllButton(Constants.RVP_LABEL_NOT_READABLE,Constants.CHECKBOX_KEY,Constants.RVP_LABEL_NOT_READABLE)
                analyticsToAllButton(Constants.SCAN_NEXT_SHIPMENT_LABEL,Constants.BUTTON_KEY,Constants.SCAN_NEXT_SHIPMENT_LABEL)
                val serverRequest = CommonRequest(
                    airwaybill_number = flyerRelatedAirwayNo.toString(), status = "failure"
                )
                viewModel.sendDataToServer(serverRequest, flyerRelatedAirwayNo)
            }
        }
    }


    private val mResultReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ScanManager.ACTION_SEND_SCAN_RESULT == action) {
                val bvalue = intent.getByteArrayExtra(ScanManager.EXTRA_SCAN_RESULT_ONE_BYTES)
                var sValue = intent.getStringExtra("SCAN_BARCODE1")
                try {
                    if (sValue == null && bvalue != null) sValue = String(bvalue, charset("GBK"))
                    sValue = sValue ?: ""
                    if (sValue !== "" && sValue != null) {
                        Log.d("check_data", sValue)
                        Log.d("check_data", String(bvalue!!))


                        if (scanFrom.equals(Constants.SHIPMENT_LABEL, ignoreCase = true)) {
                            val pattern = Pattern.compile(Constants.REGEX)
                            val matcher = pattern.matcher(sValue)
                            if (matcher.matches()) {
                                //DB CALL--for matching airwaybill no
                                if (sValue.equals(flyerRelatedAirwayNo.toString(), true)) {
                                    scanFrom = ""
                                    setBeepSound()
                                    openNextActivity("success", sValue)
                                } else {
                                    setErrorSound()
                                    showAlertDialog(
                                        getString(R.string.scan_label), attempt, "", sValue
                                    )
                                    --attempt
                                }
                            } else {
                                //show error msg
                                setErrorSound()
                                showAlertDialog(
                                    getString(R.string.regex_vaidation),
                                    attempt,
                                    "wrong_airwaybill",
                                    ""
                                )
                                //showToast(getString(R.string.regex_vaidation), false)
                            }
                        } else {
                            lastTextFlyerCode = sValue
                            //DB WORK ---Check for flyer code
                            viewModel.checkForFlyerCode(lastTextFlyerCode)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            try {
                if (scanFrom.equals(Constants.SHIPMENT_LABEL, ignoreCase = true)) {
                    if (result.text == null || result.text == lastText || result.text == lastTextFlyerCode) {
                        return
                    } else {
                        lastText = result.text
                        val pattern = Pattern.compile(Constants.REGEX)
                        val matcher = pattern.matcher(lastText)
                        if (matcher.matches()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                //DB CALL--for matching airwaybill no
                                if (lastText.equals(flyerRelatedAirwayNo.toString(), true)) {
                                    scanFrom = ""
                                    setBeepSound()
                                    openNextActivity("success", lastText.toString())
                                } else {
                                    setErrorSound()
                                    showAlertDialog(
                                        getString(R.string.scan_label),
                                        attempt,
                                        "",
                                        lastText.toString()
                                    )
                                    --attempt
                                }
                            } else {
                                //deprecated in API 26
                                //DB CALL--for matching airwaybill no
                                if (lastText.equals(flyerRelatedAirwayNo.toString(), true)) {
                                    scanFrom = ""
                                    openNextActivity("success", lastText.toString())
                                } else {
                                    showAlertDialog(
                                        getString(R.string.scan_label),
                                        attempt,
                                        "",
                                        lastText.toString()
                                    )
                                    --attempt
                                }
                            }
                        } else {
                            setErrorSound()
                            // show Dialog box same as newland device
                            showAlertDialog(
                                getString(R.string.regex_vaidation), attempt, "wrong_airwaybill", ""
                            )
                            //showToast(getString(R.string.regex_vaidation), false)
                        }
                    }
                } else {
                    if (result.text == null || result.text == lastTextFlyerCode || result.text.equals(
                            lastText, ignoreCase = true
                        )
                    ) {
                        return
                    } else {
                        lastTextFlyerCode = result.text
                        Log.d("check_scan", lastTextFlyerCode);
                        //DB WORK ---Check for flyer code
                        viewModel.checkForFlyerCode(lastTextFlyerCode)
                        //viewModel.checkForFlyerCode()
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }


    override fun onResume() {
        super.onResume()
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

        if (binding.rvpLabelCb.isChecked) {
            pauseScanner()
        }
        if(binding.tvPendingNo.text=="0" &&binding.tvFailedNo.text=="0"){
            pauseScanner()
        }else{
            startScanner()
        }
        if (isFeImageValidationRequired) {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(500)
                startScanner()
                GoForShipmentScan(shipmentList)
                analyticsToAllScreen(Constants.SHIPMENT_LABEL_SCAN)
            }
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
        } else {
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

    private fun showCustomDialog(message: String, b: Boolean, shipmentList: List<ShipmentDetail>) {
        dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val dialogbinding: CustomDialogMessageBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this),
            R.layout.custom_dialog_message,
            binding.getRoot() as ViewGroup,
            false
        )
        dialog.setContentView(dialogbinding.getRoot())
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogbinding.tvStatus.setText(message)
        if (!b) {
            dialogbinding.tvStatus.setTextColor(getColor(R.color.red))
        } else {
        }
        if (!dialog.isShowing) {
            dialog.show()
        }
        if (!isFeImageValidationRequired) {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(2000)
                startScanner()
                if (b) {
                    GoForShipmentScan(shipmentList)
                    analyticsToAllScreen(Constants.SHIPMENT_LABEL_SCAN)
                }
                dismissDialog(dialog)

            }
        }

    }


    private fun showAlertDialog(message: String, count: Int, come_from: String, text: String) {
        pauseScanner()
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val dialogbinding: CustomAlertMessageBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this),
            R.layout.custom_alert_message,
            binding.getRoot() as ViewGroup,
            false
        )
        dialog.setContentView(dialogbinding.getRoot())
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogbinding.tvMsg.setText(message)

        dialogbinding.tvMsg.setTextColor(getColor(R.color.red))

        if (!dialog.isShowing) {
            dialog.show()
        }
        if (come_from == "wrong_airwaybill") {
            dialogbinding.tvAttempt.visibility = View.GONE
        } else {
            if (count.toString().equals("0", ignoreCase = true)) {
                dialogbinding.tvAttempt.setText(
                    String.format(
                        "Attempt:- %s Left", count.toString()
                    )
                )


            } else {
                dialogbinding.tvAttempt.setText(
                    String.format(
                        "Attempt:- %s Left", count.toString()
                    )
                )
            }
        }


        dialogbinding.btOkay.setOnClickListener {
            startScanner()
            if (come_from != "wrong_airwaybill") {
                if (count.toString().equals("0", ignoreCase = true)) {
                    openNextActivity("failure", text)
                } else {
                }
            } else {

            }
            dialog.dismiss()
        }
    }


    private fun dismissDialog(dialog: Dialog) {
        dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressedDispatcher
        binding.ivBarcode.setTorchOff()
        startScreen(DashboardActivity())
       // finishAffinity()
    }

    fun setBeepSound() {
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep)
        if (this.mediaPlayer != null && !this.mediaPlayer!!.isPlaying()) {
            this.mediaPlayer!!.setVolume(100f, 100f)
            this.mediaPlayer!!.start()
        }
    }

    fun setErrorSound() {
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.bad_beep)
        if (this.mediaPlayer != null && !this.mediaPlayer!!.isPlaying()) {
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

    private fun fetchResponse() {
        lifecycleScope.launch {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.serverResponse.collect { allShipmentList ->
                when (allShipmentList) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        val data = allShipmentList.data as Response
                        showToast(data.description, false)
                       /* finish()
                        startActivity(getIntent())*/
                        val intent = Intent(this@LinkFlyerActivity, LinkFlyerActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }

                    else -> {
                        startScanner()
                        manageApiFlowStatus(allShipmentList, true)
                    }
                }
            }
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

  var  cameraActivityResultLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.let {
                frontImageID=it.getLongExtra(Constants.FRONT_IMAGE_ID, 0L)
                backImageID=it.getLongExtra(Constants.BACK_IMAGE_ID, 0L)
            }
        }
    }

    fun openNextActivity(status: String, sValue: String) {
        val extras = Bundle().apply {
            putString(
                Constants.flyerRelatedAirwillNo, flyerRelatedAirwayNo.toString()
            )
            putString(
                Constants.shiment_label_status, status
            )
            putString(
                Constants.scanned_AWB, sValue
            )
            if (isFeImageValidationRequired) {
                putLong(Constants.FRONT_IMAGE_ID, frontImageID)
                putLong(Constants.BACK_IMAGE_ID, backImageID)
                putBoolean(Constants.SUCCESS_LINK_FLYER_ACTIVITY, true)
                putBoolean(Constants.l1_qc_validation_required,l1QcValidationRequired)
            }
          //  putLong(Constants.SHIPMENT_LABEL_IMAGE_ID, shipmentLabelImageID)
        }
        startScreen(QCPassedActivity(), extras)
    }
}







