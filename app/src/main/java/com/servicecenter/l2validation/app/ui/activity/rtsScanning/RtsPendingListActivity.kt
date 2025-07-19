package com.servicecenter.l2validation.app.ui.activity.rtsScanning
// Code Reviewed
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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.nlscan.android.scan.ScanManager
import com.nlscan.android.scan.ScanSettings
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.hideKeyboard
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.ui.viewmodel.RtsPendingListViewModel
import com.servicecenter.l2validation.data.local.entities.PendingRtsData
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.ActivityRtsPendingListBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.cameraX.CameraxActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Arrays
import java.util.regex.Pattern


@AndroidEntryPoint
class RtsPendingListActivity : BaseActivity<ActivityRtsPendingListBinding, RtsPendingListViewModel>(), View.OnClickListener {

    private var device: String? = null
    private lateinit var mScanMgr: ScanManager
    private var mediaPlayer: MediaPlayer? = null
    private var pendingRtsShipmentList: ArrayList<PendingRtsData>? = ArrayList()
    private var manualSearchResults: ArrayList<PendingRtsData> = ArrayList()
    private var lastText: String? = ""
    private lateinit var adapter: RtsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    var isBottomSheetVisible = true
    lateinit var bottomSheet: RtsFilterBottomSheet
    private var isScannerVisible = false
    private var isManualSearch = false
    private var latestText:String?=""

    var came_from_search = false
    override fun getLayout(): Int {
        return R.layout.activity_rts_pending_list
    }

    override fun getViewModels(): Class<RtsPendingListViewModel> {
        return RtsPendingListViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.productDetails.tvHeadingName.text =
            resources.getString(R.string.pending_for_rts_creation)

        if (CommonUtils.isInternetAvailable(this)) {
            viewModel.getRtsPendingListData(1)
        } else {
            showToast(getString(R.string.no_internet), false)
        }
        initializeRecyclerView()
        initializeListeners()
        initializeScanner()
        fetchData()
        checkAwb()
        checkLoadingBar()
        setupSearchView()

        analyticsToAllScreen(Constants.RTS_LIST_EVENT_2)


        binding.shipmentConst.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                if (visibleItemCount + pastVisibleItems >= totalItemCount) {
                    if (!came_from_search) {
                        viewModel.loadNextPage()
                    }
                }
            }
        })
        bottomSheet = RtsFilterBottomSheet(viewModel, this)
    }

    private fun initializeRecyclerView() {
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.shipmentConst.layoutManager = layoutManager
        adapter = RtsAdapter(this, mutableListOf())
        binding.shipmentConst.adapter = adapter
    }

    private fun initializeListeners() {
        binding.productDetails.ivBackArrow.setOnClickListener(this)
        binding.filter.setOnClickListener(this)
        binding.barcodeImage.setOnClickListener(this)
        binding.ivScannerBack.setOnClickListener(this)
        binding.scanNextShipmentBtn.setOnClickListener(this)
        binding.ivFlash.setOnClickListener(this)
    }


    private fun fetchData() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.rtsPendingList.collect { it ->
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        if (viewModel.came_from.equals(Constants.FILTER)) {
                            viewModel.came_from = ""
                            pendingRtsShipmentList = ArrayList()
                            pendingRtsShipmentList?.addAll(it.data as ArrayList<PendingRtsData>)

                            if (pendingRtsShipmentList.isNullOrEmpty()) {
                                binding.tvNotFound.visibility = View.VISIBLE
                                binding.shipmentConst.visibility = View.GONE;
                                binding.tvNotFound.rotation = -30f
                            } else {
                                binding.tvNotFound.visibility = View.GONE
                                binding.shipmentConst.visibility = View.VISIBLE;
                                adapter.addItems(
                                    it.data as List<PendingRtsData>,
                                    Constants.FILTER_DATA
                                )
                            }
                        } else {
                            viewModel.came_from = ""
                            pendingRtsShipmentList?.addAll(it.data as ArrayList<PendingRtsData>)
                            adapter.addItems(it.data as List<PendingRtsData>, "")
                        }
                    }

                    is APIResultState.Loading -> {
                        if (viewModel.currentPage <= 1) {
                            progressDialog().show()
                        }
                    }

                    else -> {
                        manageApiFlowStatus(it, true)
                    }
                }
            }

        }
    }


    private fun checkLoadingBar() {
        lifecycleScope.launch {
            viewModel.loadingBar.collect {
                if (it) {
                    binding.progressBar.visibility = View.VISIBLE
                } else {
                    binding.progressBar.visibility = View.GONE
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
                        val response = it.data as Response
                        val dataList = response?.rts_list
                        came_from_search = true
                        if (isManualSearch) {
                            pendingRtsShipmentList?.clear()// api_count
                            manualSearchResults?.clear()//filter_count
                            if (!dataList.isNullOrEmpty()) {
                                manualSearchResults?.addAll(dataList)
                            }
                            adapter.addItems(
                                manualSearchResults ?: emptyList(),
                                Constants.MANUAL_SEARCH
                            )
                        } else {
                            openNextActivity("success", lastText!!)
                        }
                    }

                    is APIResultState.Failure -> {
                        progressDialog().dismiss()
                        showIncorrectAwbError(it.description, "Ok")
                    }

                    else -> {
                        manageApiFlowStatus(it, false)

                    }
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.iv_back_arrow -> finish()
            R.id.filter -> {
                if (isBottomSheetVisible) {
                    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                } else {
                    bottomSheet.dismiss()
                }
            }

            R.id.barcode_image -> {
                isScannerVisible = true
                binding.constAllData.visibility = View.GONE
                binding.constScannerBar.visibility = View.VISIBLE
                initializeScanner()
                startScanner()
            }

            R.id.iv_scanner_back -> {
                isScannerVisible = false
                binding.constScannerBar.visibility = View.GONE
                binding.constAllData.visibility = View.VISIBLE

            }

            R.id.scan_next_shipment_btn -> {
                binding.consInvalidAwb.visibility = View.GONE
                binding.btnConst.visibility = View.GONE
                binding.constAllData.visibility = View.VISIBLE
                binding.constInfo.visibility=View.GONE
                // binding.constScannerBar.visibility = View.VISIBLE
                //startScanner()
            }

            R.id.iv_flash -> {
                switchFlashlight()
            }
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


    private fun startScanner() {
        if (device.equals(
                Constants._NEWLAND, ignoreCase = true
            ) || device.equals(
                Constants._NEW_NEWLAND, ignoreCase = true
            ) || device.equals(Constants._NEWLAND_T90, ignoreCase = true)
        ) {
            binding.constScannerBar.visibility = View.GONE
            binding.constAllData.visibility = View.VISIBLE

            mScanMgr.setScanEnable(true)
        } else {
            binding.ivBarcode.resume()
        }
    }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            try {
                lastText = result.text
                val pattern = Pattern.compile(Constants.REGEX)
                val matcher = pattern.matcher(lastText)
                if (matcher.matches()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        checkAwbInRtsList(lastText!!)
                    }
                } else {
                    setErrorSound()
                    showIncorrectAwbError("Incorrect Awb Number", "Scan Again")
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

                        val pattern = Pattern.compile(Constants.REGEX)
                        val matcher = pattern.matcher(sValue)
                        if (matcher.matches()) {
                            checkAwbInRtsList(sValue)

                        } else {
                            setErrorSound()
                            showIncorrectAwbError("Incorrect Awb Number", "Scan Again")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showIncorrectAwbError(message: String, btnText: String) {
        if(btnText=="Scan Again"){
            binding.constInfo.visibility=View.VISIBLE
        }
        latestText=""
        binding.constScannerBar.visibility = View.GONE
        binding.ivBarcode.pause()
        binding.consInvalidAwb.visibility = View.VISIBLE
        binding.btnConst.visibility = View.VISIBLE
        binding.constAllData.visibility = View.GONE
        binding.tvInvalid.text = message
        binding.scanNextShipmentBtn.text = btnText
        binding.scanNextShipmentBtn.setBackgroundColor(
            ContextCompat.getColor(this, R.color.blue_93)
        )
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


    private fun checkAwbInRtsList(lastText: String) {
        var found = false
        pendingRtsShipmentList?.forEach {
            if (it.awb_number == lastText.toLong()) {
                found = true
                openNextActivity("success", lastText)
                return@forEach
            }
        }
        if (!found) {
            if(!latestText.equals(lastText)) {
                latestText=lastText
                isManualSearch = false
                viewModel.checkAwbApi(lastText)

            }
        }
    }

    private fun openNextActivity(status: String, sValue: String) {
        val extras = Bundle().apply {
            putString(
                Constants.flyerRelatedAirwillNo, sValue
            )
            putBoolean(Constants.Is_RTS, true)
        }
        startScreen(CameraxActivity(), extras)
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

    override fun onBackPressed() {
        if (isScannerVisible) {

            binding.constScannerBar.visibility = View.GONE
            binding.constAllData.visibility = View.VISIBLE
            isScannerVisible = false
        } else {
            super.onBackPressed()
        }
    }


    private fun setupSearchView() {
        binding.searchView.setIconifiedByDefault(false)
        binding.searchView.setQueryHint("Search AWB")
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.length in 9..12) {
                    isManualSearch = true //  flag for manual search
                    binding.searchView.clearFocus()
                    binding.searchView.setQuery(query, false)
                    hideKeyboard(this@RtsPendingListActivity)
                    viewModel.checkAwbApi(query)
                } else {

                    showToast(getString(R.string.please_enter_a_number_between_10_to_12_characters), false)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                if (isManualSearch && newText.isNullOrEmpty()) {
                    came_from_search = false
                    isManualSearch=false
                    adapter.clearItems()
                    viewModel.resetCurrentPage()
                    viewModel.getRtsPendingListData(1)
                    //hideKeyboard(this@RtsPendingListActivity)
                }
                return true
            }


        })

        val closeButton: View =
            binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setOnClickListener {


            binding.searchView.setQuery("", false)
            binding.searchView.clearFocus()
            cancelManualSearch()
        }
    }

    private fun cancelManualSearch() {
        manualSearchResults.clear()
        pendingRtsShipmentList?.clear()
        came_from_search = false
        isManualSearch=false
        adapter.clearItems()
        viewModel.resetCurrentPage()
        viewModel.getRtsPendingListData(1)
    }
}