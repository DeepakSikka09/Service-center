package com.servicecenter.l2validation.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.nlscan.android.scan.ScanManager
import com.nlscan.android.scan.ScanSettings
import com.servicecenter.l2validation.R
import java.util.Arrays
import java.util.regex.Pattern

class CustomScanner(
    val ivBarcode: DecoratedBarcodeView?,
    val success: (String) -> Unit,
    val fail: () -> Unit,
    private val ivFlash: ShapeableImageView?,
    val activity: FragmentActivity,
) {
    private lateinit var mScanMgr: ScanManager
    var lastText: String? = null
    private var mediaPlayer: MediaPlayer? = null
    var isFlashOn = false

    fun initializeScanner() {
        registerReceiver()
        if (isNewLand()) {
            ivBarcode?.visibility = View.GONE
            mScanMgr = ScanManager.getInstance()
            mScanMgr.startScan()
            mScanMgr.disableBeep()
            mScanMgr.setScanEnable(true)
            mScanMgr.setOutpuMode(ScanSettings.Global.VALUE_OUT_PUT_MODE_BROADCAST)
            val settings: Map<String, String> = mScanMgr.scanSettings
            val sOutputMode = settings[ScanSettings.Global.OUT_PUT_MODE] //Acquire
        } else {
            val formats: Collection<BarcodeFormat> =
                Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128)
            ivBarcode?.barcodeView?.decoderFactory = DefaultDecoderFactory(formats)
            ivBarcode?.initializeFromIntent(activity.intent)
            ivBarcode?.decodeContinuous(callback)
        }
        ivFlash?.setOnClickListener {
            if(isFlashOn){
                switchFlashOff()
            }else{
                switchFlashOn()
            }
        }
    }

    fun startScanner() {
        if (isNewLand()) {
            mScanMgr.setScanEnable(true)
        } else {
            ivBarcode?.resume()
        }
    }

    fun pauseScanner() {
        if(isFlashOn)
            switchFlashOff()
        if (isNewLand()) {
            mScanMgr.setScanEnable(false)
        } else {
            ivBarcode?.pause()
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
                    if (sValue !== "") {

                        val pattern = Pattern.compile(Constants.REGEX)
                        val matcher = pattern.matcher(sValue)
                        if (matcher.matches()) {
                            success(sValue)

                        } else {
                            setErrorSound()
                            fail()
                            startScanner()
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
                lastText = result.text
                val pattern = Pattern.compile(Constants.REGEX)
                val matcher = pattern.matcher(lastText)
                if (matcher.matches()) {
                        success(lastText!!)
                } else {
                    setErrorSound()
                    fail()
                    startScanner()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setErrorSound() {
        mediaPlayer = MediaPlayer.create(activity, R.raw.bad_beep)
        if (this.mediaPlayer != null && !this.mediaPlayer!!.isPlaying) {
            this.mediaPlayer!!.setVolume(100f, 100f)
            this.mediaPlayer!!.start()
        }
        val v = activity.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(500)
        }
    }

    private fun registerReceiver() {
        if (isNewLand()) {
            val intFilter = IntentFilter(ScanManager.ACTION_SEND_SCAN_RESULT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(
                    mResultReceiver, intFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                activity.registerReceiver(mResultReceiver, intFilter)
            }
        } else {
            ivBarcode?.isSelected = true
            ivBarcode?.resume()
        }
    }

    fun unRegisterScanner(){
        if (isNewLand()) {
            mScanMgr.stopScan()
            activity.unregisterReceiver(mResultReceiver)
        }
    }

    fun switchFlashOn(){
        isFlashOn = true
        ivBarcode?.setTorchOn()
        ivFlash?.setImageResource(R.drawable.flash_on)
    }

    fun switchFlashOff(){
        isFlashOn = false
        ivBarcode?.setTorchOff()
        ivFlash?.setImageResource(R.drawable.flash_off)
    }

    companion object {
        val device = (Build.MANUFACTURER + ":" + Build.MODEL).uppercase()
        fun isNewLand(): Boolean {
            return device.equals(
                Constants._NEWLAND, ignoreCase = true
            ) || device.equals(
                Constants._NEW_NEWLAND, ignoreCase = true
            ) || device.equals(Constants._NEWLAND_T90, ignoreCase = true)
        }
    }
}