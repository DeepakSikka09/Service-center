package com.servicecenter.l2validation.app.ui.activity.udCalling.fragment

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.servicecenter.l2validation.BuildConfig
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.calculateDistance
import com.servicecenter.l2validation.app.extensions.formatTime
import com.servicecenter.l2validation.app.extensions.navigateToActivity
import com.servicecenter.l2validation.app.extensions.showToast
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.app.extensions.toFormattedDate
import com.servicecenter.l2validation.app.ui.activity.udCalling.MapActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDCallingActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDShipmentActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDShipmentActivity.Companion.correlationId
import com.servicecenter.l2validation.app.ui.viewmodel.UDShipmentViewModel
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.local.entities.AwbDetails
import com.servicecenter.l2validation.data.local.entities.CallBridgeDetails
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.CallBottomsheetBinding
import com.servicecenter.l2validation.databinding.FragmentUdShipmentDetailBinding
import com.servicecenter.l2validation.databinding.LocationDialogBoxBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.checkGpsStatus
import com.servicecenter.l2validation.utils.CommonUtils.isCallActive
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.NORESPONSE
import com.servicecenter.l2validation.utils.Constants.PENDING
import com.servicecenter.l2validation.utils.Constants.PERMISSION_REQUEST_CODE
import com.servicecenter.l2validation.utils.Constants.VALIDATED
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.roundToInt

@AndroidEntryPoint
class UdShipmentDetailFragment : BaseFragment<FragmentUdShipmentDetailBinding>(),
    View.OnClickListener {

    private var tag = "MarkUDValidationFragment"
    override fun getLayout(): Int =R.layout.fragment_ud_shipment_detail

    private val viewModel: UDShipmentViewModel by activityViewModels()
    private var awbNumber: Long = 0L
    private var drsId: Long = 0L
    private var udStatus: String = ""
    private var orderId: String? = ""
    private var itemType: String? = ""
    private var actionableDays: Int? = 0
    private var feNumber: String? = ""
    private var timeMarked: String? = ""
    private var paymentType: String? = ""
    private var feLat: Double = 0.0
    private var feLong: Double = 0.0
    private var consigneeLat: Double = 0.0
    private var consigneeLong: Double = 0.0
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var callBottomSheet: CallBottomsheetBinding
    private var callStatusFlag: Int = 0
    private var clientCorrelationId: String = ""
    private lateinit var mediaPlayer: MediaPlayer
    private var isPlaying = false
    private var lastClickTime: Long = 0
    private var flowJob: Job? = null
    private var uLatitude: Double = 0.0
    private var uLongitude: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationDialogBinding: LocationDialogBoxBinding
    private var currentPosition: Int = 0


    private lateinit var backPress: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPress = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
//                requireActivity().finish()
                onBack()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().onBackPressedDispatcher.addCallback(this.viewLifecycleOwner, backPress)

        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemType = arguments?.getString(Constants.ITEM_TYPE)
        awbNumber = arguments?.getLong(Constants.AWB_NUMBER)!!
        drsId = arguments?.getLong(Constants.DRS_ID)!!
        udStatus = arguments?.getString(Constants.UD_STATUS).toString()
        mediaPlayer = MediaPlayer()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        (activity as? UDShipmentActivity)?.setFragmentCallback {
            // Handle callback in the fragment
            Log.d("CallReceiverFragment", "Call ended or idle, fragment handling callback.")
//            binding.callConsigneeBtn.isEnabled=false
            binding.callConsigneeBtn.text = getString(R.string.please_wait)
            binding.callConsigneeBtn.isEnabled = false
            binding.callConsigneeBtn.setBackgroundColor(Color.GRAY)
        }
        initialize()
        fetchShipmentDetails()


    }

   /* private fun requestAudioFocus() {
        // Request audio focus for playback
        val result = maudioManager.requestAudioFocus(
            null, // OnAudioFocusChangeListener can be passed here, if you need it
            AudioManager.STREAM_MUSIC, // Stream type: Music
            AudioManager.AUDIOFOCUS_GAIN // Focus request: gain focus
        )
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Successfully gained audio focus
        }
    }

    private fun abandonAudioFocus() {
        // Abandon audio focus when a call occurs
        maudioManager.abandonAudioFocus(null)
    }*/

    private val handler = Handler(Looper.getMainLooper())
    private val mRunnable = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                binding.audioSeekBar.progress = mediaPlayer.currentPosition
                binding.audioStartTime.text = formatTime(mediaPlayer.currentPosition)
                handler.postDelayed(this, 1000) // Update every second
            }
        }
    }
    private fun initialize() {
        if (isInternetAvailable(requireActivity())) {
            binding.udConst.visibility=View.VISIBLE
            awbNumber.let {
                viewModel.fetchShipmentByAwbNumber(awbNumber, drsId, udStatus)
            }
        } else {
            binding.udConst.visibility=View.GONE
            showToast(
                getString(R.string.no_internet),
                false
            )
        }
        if (itemType != null) {
            setLayoutBasedOnItemType(itemType!!)
        }
        binding.callConsigneeBtn.setOnClickListener(this)
        binding.productDetails.ivBackArrow.setOnClickListener(this)
        binding.locationConstraint.setOnClickListener(this)

        fetchUdCallingStatus()

    }
    private fun setLayoutBasedOnItemType(itemType: String) {
        when (itemType) {
            VALIDATED -> {
                binding.consigneeNotConstraint.visibility = View.VISIBLE
                binding.callRecordingConstraint.visibility = View.VISIBLE
                binding.callRecordingConstraint.visibility = View.VISIBLE
                binding.view5.visibility = View.VISIBLE
                binding.remarkLandmarkConstraint.visibility = View.VISIBLE
                binding.view2.visibility = View.GONE
                binding.viewconsignee.visibility = View.VISIBLE
                binding.view4.visibility = View.GONE
                binding.productConstraint.visibility = View.GONE
                binding.consigneeReasonConstraint.visibility = View.GONE
                binding.callConsigneeBtn.visibility = View.GONE
                binding.locationConstraint.visibility = View.GONE
            }

            NORESPONSE -> {
                binding.callConsigneeBtn.visibility = View.GONE
            }
        }
    }
    private fun showIncorrectAwbError(message: String) {
        binding.udConst.visibility = View.GONE
        binding.consInvalidAwb.visibility = View.VISIBLE
        binding.tvInvalid.text = message
    }

    private fun sendDetails(shipmentDetails: AwbDetails?) {
        "AWB : $awbNumber".also { binding.productDetails.tvHeadingName.text = it }
        orderId = shipmentDetails?.order_id
        val feNumberString = shipmentDetails?.fe_number?.toString() ?: ""

        binding.feMobileNo.text = String.format(getString(R.string._91_xxxxxx_s), feNumberString.takeLast(4))
        feNumber = feNumberString
        timeMarked = shipmentDetails?.ud_time_stamp
        (getString(R.string.ud_marked_on) + timeMarked).also { binding.locationAddressTv.text =it }
        actionableDays = shipmentDetails?.actionalble_days_max_limit
        paymentType = shipmentDetails?.payment_type
        feLat = shipmentDetails?.ud_location?.fe_lat ?: 0.0
        feLong = shipmentDetails?.ud_location?.fe_long ?: 0.0
        consigneeLat = shipmentDetails?.ud_location?.consignee_lat ?: 0.0
        consigneeLong = shipmentDetails?.ud_location?.consignee_long ?: 0.0

        if (shipmentDetails?.reschedule_remarks.isNullOrEmpty()) {
            binding.view5.visibility = View.GONE
            binding.remarkLandmarkConstraint.visibility = View.GONE
        }
        if (!shipmentDetails?.ud_call_remark.isNullOrEmpty()) {
            binding.alertTv.text = shipmentDetails?.disconnected_by
            binding.viewalert.visibility = View.VISIBLE
            binding.alertCl.visibility = View.VISIBLE
            binding.view2.visibility = View.VISIBLE
        }
        if(udStatus== NORESPONSE){
            binding.viewalert.visibility = View.VISIBLE
            binding.alertCl.visibility = View.VISIBLE
            binding.view2.visibility = View.VISIBLE
            binding.alertTv.visibility=View.GONE
            val lastCall=shipmentDetails?.last_call_start_time
            "Last Call Attempt: ${lastCall?.toFormattedDate()}".also { binding.alertRemark.text = it }
        }
        calculateDistanceFormDC(shipmentDetails)
    }

    //TODO
    /*    {
            RINGING("RINGING_CALLTO", "Call is Ringing", 1),
            PICKED_UP("ANSWER_CALLTO", "Call Picked by consignee", 2),
            DISCONNECTED_CALL_FROM("DIS_CALLFROM", "Call Disconnected by you", 3),
            DISCONNECTED_CALL_TO("DIS_CALLTO", "Call Disconnected by Consignee", 3),
            ANSWERED("ANSWERED", "Call was Answered by consignee", 4),
            NORESPONSE("MISSED", "No response from consignee", 5);

        }*/
  private  fun manageButton(callStatusFlag: Int) {
        //if call picked
        when (callStatusFlag) {
            2 -> {
                binding.callConsigneeBtn.text = getString(R.string.mark_ud_validation)
                binding.callConsigneeBtn.isEnabled = true
                binding.callConsigneeBtn.setBackgroundColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.primary_color
                    )
                )
            }
            1 -> {
                //ringing
                binding.callConsigneeBtn.text = getString(R.string.mark_ud_validation)
                binding.callConsigneeBtn.isEnabled = false
                binding.callConsigneeBtn.setBackgroundColor(Color.GRAY)

            }
            else -> {
                binding.callConsigneeBtn.text = getString(R.string.call_consignee)
                binding.callConsigneeBtn.isEnabled = true
                binding.callConsigneeBtn.setBackgroundColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.primary_color
                    )
                )
            }
        }
    }
    override fun onPause() {
        super.onPause()
        if (udStatus == VALIDATED) {
            pauseMediaPlayer()
        }
    }


    override fun onResume() {
        super.onResume()
        if(!checkGpsStatus(requireActivity())){
            showToast("Enable the GPS ",false)
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }else{
            checkLocationPermissionAndFetch()
        }



    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.call_consignee_btn -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 5000) {
                    showToast(getString(R.string.please_wait_for_5_seconds), false)
                    return
                }
                lastClickTime = currentTime

                if (binding.callConsigneeBtn.text == getString(R.string.mark_ud_validation)) {
                    viewModel.cancelJob()
                    flowJob?.cancel()
                    val markUdFragment = MarkUDValidationFragment().apply {
                        arguments = Bundle().apply {
                            putString(Constants.AWB_NUMBER, awbNumber.toString())
                            putString(Constants.DRS_ID, drsId.toString())
                            putString(Constants.ORDER_ID, orderId)
                            putString(Constants.FE_NUMBER, feNumber)
                            putString(Constants.PAYMENT_TYPE, paymentType)
                            putString(Constants.CLIENT_CORRELATION_ID, clientCorrelationId)
                            actionableDays?.let {
                                putInt(Constants.ACTIONALBLE_DAYS, it)

                            }
                        }
                    }

                    (requireActivity() as UDShipmentActivity).replaceFragment(markUdFragment, tag)
                } else {
                    if (isCallActive(requireContext())) {
                            showToast(getString(R.string.your_call_is_already_active), false)
                    } else {
                        val bridgeDetails = binding.responseData?.callbridge_details
                        if (bridgeDetails != null && bridgeDetails.size > 1) {
                            openBottomSheet(bridgeDetails)
                        } else {
                            val singlePrimaryNumber =
                                "${bridgeDetails?.get(0)?.callbridge_number},${bridgeDetails?.get(0)?.pin}#"
                            dialPhoneNumber(singlePrimaryNumber)
                        }
                    }
                }
            }

            R.id.iv_back_arrow -> {

                onBack()
            }


            R.id.location_constraint -> {
                val extras = Bundle().apply {
                    putDouble(Constants.FE_LAT, feLat)
                    putDouble(Constants.FE_LONG, feLong)
                    putDouble(Constants.CONSIGNEE_LAT, consigneeLat)
                    putDouble(Constants.CONSIGNEE_LONG, consigneeLong)
                }
                startScreen(MapActivity(), extras)
            }
        }
    }

    private fun openBottomSheet(shipmentDetails: List<CallBridgeDetails>) {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        callBottomSheet = CallBottomsheetBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(callBottomSheet.root)

        val primaryNumber = shipmentDetails[0].callbridge_number
        val primaryNumberPin = shipmentDetails[0].pin
        callBottomSheet.primaryContactNumber.text =
            String.format(getString(R.string._xxxxxx_s), primaryNumber.takeLast(4))

        val alternateNumber = shipmentDetails[1].callbridge_number
        val alternateNumberPin = shipmentDetails[1].pin
        callBottomSheet.alternateContactNumber.text =
            String.format(getString(R.string._xxxxxx_s), alternateNumber.takeLast(4))
        callBottomSheet.primaryCallCard.setOnClickListener {
            val primaryNumberWithPin = "${primaryNumber},${primaryNumberPin}#"
            dialPhoneNumber(primaryNumberWithPin)
            bottomSheetDialog.dismiss()
        }
        callBottomSheet.alternateCallCard.setOnClickListener {
            val alternateNumberWithPin = "${alternateNumber},${alternateNumberPin}#"
            dialPhoneNumber(alternateNumberWithPin)
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    private fun dialPhoneNumber(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
            ) {
            startActivity(intent)


        } else {
            // Handle permission request if not granted
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE,
                ),
                1
            )
        }
    }

    private fun fetchShipmentDetails() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.shipment.collect { result ->
                when (result) {
                    is APIResultState.Success -> {

                        if (udStatus == PENDING) {
                            viewModel.startJob(awbNumber, drsId)
                        }
                        val shipmentDetails = result.data
                        binding.responseData = shipmentDetails as AwbDetails?
                        if (shipmentDetails != null) {
                            sendDetails(shipmentDetails)
                            progressDialog().dismiss()
                            when (udStatus) {

                                VALIDATED -> {
                                    if (shipmentDetails.call_recording.isNullOrEmpty()) {
                                        binding.callRecordingConstraint.visibility = View.GONE
                                    } else {
                                        binding.callRecordingConstraint.visibility = View.VISIBLE
                                        playMedia(shipmentDetails.call_recording)

                                    }
                                }

                                PENDING -> {
                                    binding.callConsigneeBtn.visibility = View.VISIBLE
                                }
                            }


                        }

                    }

                    is APIResultState.Failure -> {
                        progressDialog().dismiss()

                        showIncorrectAwbError(result.description)
                    }

                    is APIResultState.Loading -> {
                        progressDialog().show()
                    }

                    else -> {
                        manageApiFlowStatus(
                            apiResultState = result,
                            true
                        )
                    }
                }
            }
        }
    }
    private fun fetchUdCallingStatus() {
        flowJob = lifecycleScope.launch {
            viewModel.udCallingStatus.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        val callStatusData = result.data as Response
                        callStatusFlag = callStatusData.call_details.call_status_flag
                        clientCorrelationId = callStatusData.call_details.client_correlation_id
                        correlationId = callStatusData.call_details.client_correlation_id
                        viewModel.preferenceDataStoreHelper.setData(
                            PreferenceDataStoreConstants.recentCorrelationId,
                            clientCorrelationId
                        )
                        if (callStatusData.is_marked_no_response) {
                            showToast(getString(R.string.max_reached), false, Toast.LENGTH_LONG)
                            navigateToActivity(
                                UDCallingActivity::class.java
                            )

                        } else {

                            manageButton(callStatusData.call_details.call_status_flag)
                        }
                    }

                    is APIResultState.Failure -> {
                        Log.d("check_data","ok")
                        manageButton(0)
                    }
                    else -> {
                        manageApiFlowStatus(apiResultState = result, true)
                    }
                }
            }
        }

    }


    private fun playMedia(callRecording: String?) {
        try {
            binding.audioSeekBar.isEnabled=false
            binding.mediaProgress.visibility=View.VISIBLE
            binding.playButton.visibility=View.GONE
            binding.fastForwardButton.isEnabled=false
            binding.rewindButton.isEnabled=false
            mediaPlayer.setDataSource(callRecording) // Set audio source
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {

                setupSeekBar()
                binding.audioSeekBar.isEnabled=true
                binding.mediaProgress.visibility=View.GONE
                binding.playButton.visibility=View.VISIBLE
                binding.fastForwardButton.isEnabled=true
                binding.rewindButton.isEnabled=true
            }



        } catch (e: IOException) {
            e.printStackTrace()
        }



        // Set up the play button
        binding.playButton.setOnClickListener {

            if (mediaPlayer.isPlaying) {
                // Pause media  and update UI
                pauseMediaPlayer()
            } else {
                startMedia()

            }
        }

        binding.fastForwardButton.setOnClickListener {
            val newPosition = mediaPlayer.currentPosition + 10000 // Forward by 10 seconds
            if (newPosition <= mediaPlayer.duration) {
                mediaPlayer.seekTo(newPosition)
                binding.audioSeekBar.progress = newPosition
                binding.audioStartTime.text = formatTime(newPosition)
            }
        }

        // Backward button logic
        binding.rewindButton.setOnClickListener {
            val newPosition = mediaPlayer.currentPosition - 10000 // Backward by 10 seconds
            if (newPosition >= 0) {
                mediaPlayer.seekTo(newPosition)
                binding.audioSeekBar.progress = newPosition
                binding.audioStartTime.text = formatTime(newPosition)
            }
        }

        // Handle completion
        mediaPlayer.setOnCompletionListener {
            binding.playButton.setImageResource(R.drawable.play_button) // Reset to play icon
            binding.audioSeekBar.progress = 0 // Reset SeekBar
            binding.audioStartTime.text = getString(R.string._00_00) // Reset start time
            isPlaying = false
            mediaPlayer.reset()
        }
    }



    private  fun pauseMediaPlayer(){
        if ( mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            currentPosition = mediaPlayer.currentPosition
            isPlaying = false
            updateButtonImage()  // Update the button image to "Play"
        }
    }

    private fun releasePlayer(){
        mRunnable.let { handler.removeCallbacks(it) }
        mediaPlayer.stop()
        mediaPlayer.release() // Release resources

    }

    private fun startMedia() {
        if (!mediaPlayer.isPlaying) {
            if (currentPosition > 0) {
                mediaPlayer.seekTo(currentPosition)// Resume from the last position

                mediaPlayer.start()
            } else {
                mediaPlayer.start() // Start from the beginning
            }
            isPlaying = true
            updateButtonImage()
            handler.postDelayed(mRunnable, 1000)
            // Show pause icon
            // Update the button image to "Pause"
        }
    }


    private fun setupSeekBar() {
        // Set total duration in SeekBar
        binding.audioSeekBar.max = mediaPlayer.duration
        binding.audioEndTime.text = formatTime(mediaPlayer.duration)


        // Start updating SeekBar when the media starts playing
//        handler.postDelayed(mRunnable as Runnable, 1000)
//        handler?.post(mRunnable as Runnable)


        // Handle manual SeekBar changes
        binding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    binding.audioStartTime.text = formatTime(progress)
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Optional: Pause updates while user is seeking
                handler.removeCallbacks(mRunnable as Runnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Optional: Resume updates after seeking
                handler.postDelayed(mRunnable as Runnable, 1000)
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    isPlaying = true
                    updateButtonImage()  // Update the button image when resuming
                }
            }
        })
    }

    private fun updateButtonImage() {
        if (isPlaying) {
            // Change image to "Pause" when playing
           binding. playButton.setImageResource(R.drawable.pause)
        } else {
            // Change image to "Play" when paused or stopped
            binding. playButton.setImageResource(R.drawable.play_button)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        flowJob?.cancel()
                if (udStatus == VALIDATED) {
            releasePlayer()
        }

    }

    private fun checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), PERMISSION_REQUEST_CODE
            )
            return
        }

        // Get last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                  uLatitude = location.latitude
                  uLongitude = location.longitude
                Log.d("LocationFragment", "Latitude: $uLatitude, Longitude: $uLongitude")
            } else {
                Log.d("LocationFragment", "Location not available")
            }
        }
     }
    private fun calculateDistanceFormDC(shipmentDetails: AwbDetails?) {
        val distanceInKm = shipmentDetails?.dc_location?.latitude?.let { dcLat ->
            shipmentDetails.dc_location.longitude?.let { dcLong ->
                calculateDistance(uLatitude, uLongitude, dcLat, dcLong)
            }
        }
        val distanceInMeters =  distanceInKm?.times(1000)?.roundToInt() // Convert to meters
        if (distanceInMeters != null && distanceInMeters >= shipmentDetails.dc_range && udStatus== PENDING && !BuildConfig.DEBUG) {
           showCustomDialogBox()
        }
    }

    private fun showCustomDialogBox() {
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        locationDialogBinding = DataBindingUtil.inflate(
            LayoutInflater.from(requireActivity()),
            R.layout.location_dialog_box,
            null,  // Use null to avoid casting issues
            false
        )

        dialog.setContentView(locationDialogBinding.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        locationDialogBinding.dialogBack.setOnClickListener {
            dialog.dismiss()

//            if (requireActivity() is UDShipmentActivity) {
//                (requireActivity() as UDShipmentActivity).backPress()
//            }
            onBack()
        }

        dialog.show()
    }

    fun onBack(){

        requireActivity().finish()
    }

}