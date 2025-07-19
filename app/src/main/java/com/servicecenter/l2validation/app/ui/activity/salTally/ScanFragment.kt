package com.servicecenter.l2validation.app.ui.activity.salTally

import android.app.Dialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.gone
import com.servicecenter.l2validation.app.extensions.visible
import com.servicecenter.l2validation.app.ui.viewmodel.TallyScanViewModel
import com.servicecenter.l2validation.app.ui.viewmodel.TallyViewModel
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.CancelTallyDialogBinding
import com.servicecenter.l2validation.databinding.FragmentScanBinding
import com.servicecenter.l2validation.databinding.ProgressbarLayoutBinding
import com.servicecenter.l2validation.databinding.TallyErrorDialogBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants.BundleConstants.AWB_NO
import com.servicecenter.l2validation.utils.Constants.BundleConstants.BTN_ACTION
import com.servicecenter.l2validation.utils.Constants.BundleConstants.BTN_TEXT
import com.servicecenter.l2validation.utils.Constants.BundleConstants.IS_SUB
import com.servicecenter.l2validation.utils.Constants.BundleConstants.MSG
import com.servicecenter.l2validation.utils.Constants.BundleConstants.MSG_BUNDLE
import com.servicecenter.l2validation.utils.Constants.BundleConstants.ORDER_TYPE
import com.servicecenter.l2validation.utils.Constants.BundleConstants.STATUS
import com.servicecenter.l2validation.utils.Constants.BundleConstants.SUB_MSG
import com.servicecenter.l2validation.utils.Constants.TALLY_COMPLETED
import com.servicecenter.l2validation.utils.Constants.TALLY_MARKED_COMPLETED
import com.servicecenter.l2validation.utils.Constants.TALLY_NOT_STARTED
import com.servicecenter.l2validation.utils.Constants.TALLY_STARTED
import com.servicecenter.l2validation.utils.CustomScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScanFragment : BaseFragment<FragmentScanBinding>(), View.OnClickListener {
    private lateinit var cancelDialog: Dialog
    val viewModel: TallyScanViewModel by viewModels()
    private val sharedViewModel: TallyViewModel by activityViewModels()
    private lateinit var dBinding: CancelTallyDialogBinding
    private var lastText = ""
    private lateinit var errorDialog: Dialog
    private lateinit var errorBinding: TallyErrorDialogBinding
    private lateinit var backPress: OnBackPressedCallback
    private lateinit var customScanner: CustomScanner
    private lateinit var progressBarDialog :Dialog
    override fun getLayout(): Int {
        return R.layout.fragment_scan
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPress = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                if (sharedViewModel.getTallyStatus() == TALLY_STARTED) {
                    customScanner.pauseScanner()
                    openCancelTallyDialog()
                } else
                    popBackStack()
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
        setUpToolbar()
        binding.lifecycleOwner = this@ScanFragment
        progressBarDialog  = Dialog(requireContext())
        val params = progressBarDialog.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT // set as full width
        params.height = WindowManager.LayoutParams.MATCH_PARENT//full height
        progressBarDialog.window?.setGravity(Gravity.CENTER_HORIZONTAL)
        val pBinding: ProgressbarLayoutBinding = ProgressbarLayoutBinding.inflate(
            layoutInflater
        )
        progressBarDialog.setContentView(pBinding.root, params)
        progressBarDialog.window!!.setBackgroundDrawableResource(
            R.color.transparent
        )
        progressBarDialog.setCancelable(false)
        sharedViewModel.shipmentDetail?.let { shipment ->
            binding.awbTallySuccess.visible()
            shipment.first.toString().also { binding.awbValue.text = it }
            binding.messageTv.text = getString(R.string.reconciliation_successful)
        }

        if (sharedViewModel.getTallyStatus() == TALLY_NOT_STARTED)
            viewModel.startRecon()
        else
            viewModel.shipmentDetailApi()
        sharedViewModel.setImageEmpty()
        if (!viewModel.isFlowRegister) {
            observe()
            viewModel.isFlowRegister = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (::customScanner.isInitialized) {
            customScanner.startScanner()
        }
    }

    private fun observe() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.shipmentDetailApiFlow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        result.data as Response
                        customScanner = CustomScanner(
                            binding.ivBarcode,
                            { sValue ->
                                lastText = sValue
                                checkAwbInPendingList(sValue)
                            },
                            {
                                setMessageScreen(
                                    isSub = true,
                                    subMsg = "If AWB Barcode is not readable, take Re-Print from Track Me Screen",
                                    msg = "Incorrect AWB Scanned",
                                    btnText = "Scan Again",
                                    btnAction = 1
                                )
                            },
                            binding.ivFlash,
                            requireActivity()
                        )
                        customScanner.initializeScanner()
                        progressBarDialog.dismiss()
                        result.data.scanned_shipments.let { shipmentCount ->
                            if (shipmentCount != null) {
                                viewModel.setShipmentCount(
                                    shipmentCount,
                                    result.data.total_shipments ?: 0
                                )
                            } else {
                                showToast(result.data.description, true)
                            }
                        }
                        result.data.recon_status?.let {status->
                            viewModel.updateTallyStatus(status)
                            when (status) {
                                TALLY_NOT_STARTED -> {
                                    setMessageScreen(
                                        isSub = false,
                                        subMsg = "",
                                        msg = result.data.description,
                                        btnText = "OK",
                                        btnAction = 4
                                    )
                                }
                                TALLY_STARTED -> {}
                                TALLY_MARKED_COMPLETED -> {}
                                TALLY_COMPLETED -> {
                                    setMessageScreen(
                                        isSub = false,
                                        subMsg = "",
                                        msg = result.data.description,
                                        btnText = "OK",
                                        btnAction = 4
                                    )
                                    viewModel.updateTallyMsg(result.data.description)
                                }
                                else->{}
                            }
                        }
                    }

                    is APIResultState.Failure -> {
                        progressBarDialog.dismiss()
                        setMessageScreen(
                            isSub = false,
                            subMsg = "",
                            msg = result.description,
                            btnText = "OK",
                            btnAction = 1
                        )
                    }

                    is APIResultState.Loading -> {
                        progressBarDialog.show()
                    }

                    else -> {
                        manageApiFlowStatus(result, true)
                    }
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.startReconFlow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        viewModel.shipmentDetailApi()
                        viewModel.updateTallyStatus(TALLY_STARTED)
                        result.data as Response
                        result.data.recon_status?.let {status->
                            viewModel.updateTallyStatus(status)
                            when (status) {
                                TALLY_NOT_STARTED -> {
                                    setMessageScreen(
                                        isSub = false,
                                        subMsg = "",
                                        msg = result.data.description,
                                        btnText = "OK",
                                        btnAction = 4
                                    )
                                }
                                TALLY_STARTED -> {}
                                TALLY_MARKED_COMPLETED -> {
                                    navigate(R.id.markCompleteFragment)
                                }
                                TALLY_COMPLETED -> {
                                    setMessageScreen(
                                        isSub = false,
                                        subMsg = "",
                                        msg = result.data.description,
                                        btnText = "OK",
                                        btnAction = 4
                                    )
                                    viewModel.updateTallyMsg(result.data.description)
                                }
                                else->{}
                            }
                        }
                    }

                    is APIResultState.Failure -> {
                        progressBarDialog.dismiss()
                        setMessageScreen(
                            isSub = false,
                            subMsg = "",
                            msg = result.description,
                            btnText = "OK",
                            btnAction = 4
                        )
                    }

                    is APIResultState.Loading -> {
                        progressBarDialog.show()
                    }

                    else -> {
                        manageApiFlowStatus(result, true)
                    }
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.scanShipmentFlow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        result.data as Response
                        result.data.let { shipment ->
                            shipment.is_sort_code?.let { isSortCode ->
                                if (isSortCode) {
                                    binding.shortCodeCl.visible()
                                    binding.codeValue.text = shipment.sort_code
                                } else {
                                    binding.shortCodeCl.gone()
                                }
                            }
                            shipment.is_priority_shipment?.let { isPriority ->
                                if (isPriority) {
                                    binding.priorityShipmentTv.visible()
                                } else {
                                    binding.priorityShipmentTv.gone()
                                }
                            }
                            shipment.is_rto?.let { isRto ->
                                if (isRto) {
                                    binding.rtoLock.visible()
                                } else {
                                    binding.rtoLock.gone()
                                }
                            }
                            shipment.is_dmg?.let { isDmg ->
                                if (isDmg) {
                                    binding.dmgLock.visible()
                                } else {
                                    binding.dmgLock.gone()
                                }
                            }
                            shipment.is_ofd?.let { isOfd ->
                                if (isOfd) {
                                    binding.ofdLock.visible()
                                } else {
                                    binding.ofdLock.gone()
                                }
                            }

                            if (shipment.image_required == true) {
                                navigate(
                                    R.id.captureImageFragment, bundleOf(
                                        AWB_NO to result.data.awb_no,
                                        ORDER_TYPE to result.data.product_type
                                    )
                                )
                            } else {
                                viewModel.setShipmentCount(
                                    viewModel.shipmentCount.value.first + 1,
                                    viewModel.shipmentCount.value.second
                                )
                                sharedViewModel.setShipmentDetails(
                                    Pair(
                                        result.data.awb_no ?: 0,
                                        result.data.product_type ?: ""
                                    )
                                )
                                customScanner.startScanner()
                            }
                            binding.awbTallySuccess.visible()
                            binding.awbValue.text = result.data.awb_no.toString()
                            binding.messageTv.text = result.data.description
                            result.data.recon_status?.let {status->
                                viewModel.updateTallyStatus(status)
                                when (status) {
                                    TALLY_NOT_STARTED -> {
                                        setMessageScreen(
                                            isSub = false,
                                            subMsg = "",
                                            msg = result.data.description,
                                            btnText = "OK",
                                            btnAction = 4
                                        )
                                    }
                                    TALLY_STARTED -> {}
                                    TALLY_MARKED_COMPLETED -> {
                                        navigate(R.id.markCompleteFragment)
                                    }
                                    TALLY_COMPLETED -> {
                                        setMessageScreen(
                                            isSub = false,
                                            subMsg = "",
                                            msg = result.data.description,
                                            btnText = "OK",
                                            btnAction = 4
                                        )
                                        viewModel.updateTallyMsg(result.data.description)
                                    }
                                    else->{}
                                }
                            }
                        }
                        progressBarDialog.dismiss()

                    }

                    is APIResultState.Loading -> {
                        binding.awbTallySuccess.gone()
                        binding.priorityShipmentTv.gone()
                        binding.shortCodeCl.gone()
                        binding.dmgLock.gone()
                        binding.ofdLock.gone()
                        binding.rtoLock.gone()
                        progressBarDialog.show()
                    }

                    is APIResultState.Failure -> {
                        progressBarDialog.dismiss()
                        if(result.description.isNotBlank()){
                            showErrorDialog(
                                "Unexpected Error Occurred",
                                result.description
                            )
                        }else {
                            result.data?.let { response ->
                                response as Response
                                sharedViewModel.setShipmentDetails(null)
                                showErrorDialog(
                                    response.description,
                                    response.awb_no.toString()
                                )
                            }
                        }
                    }

                    else -> {
                        progressBarDialog.dismiss()
                    }
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.cancelReconFlow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        result.data as Response
                        viewModel.updateTallyStatus(TALLY_NOT_STARTED)
                        requireActivity().finish()
                        progressBarDialog.dismiss()
                    }

                    is APIResultState.Failure -> {
                        progressBarDialog.dismiss()
                        setMessageScreen(
                            isSub = false,
                            subMsg = "",
                            msg = result.description,
                            btnAction = 1,
                            btnText = "OK"
                        )
                    }
                    is APIResultState.Loading->{
                        progressBarDialog.show()
                    }
                    else -> {
                        manageApiFlowStatus(result, true)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.shipmentCount.collect {
                "${it.first}/${it.second}".also { pending -> binding.tvPendingNo.text = pending }
            }
        }

        lifecycleScope.launch {
            viewModel.doneReconFlow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        progressBarDialog.dismiss()
                        viewModel.updateTallyStatus(TALLY_MARKED_COMPLETED)
                        navigate(R.id.markCompleteFragment)
                    }

                    is APIResultState.Failure -> {
                        progressBarDialog.dismiss()
                        setMessageScreen(
                            isSub = false,
                            subMsg = "",
                            msg = result.description,
                            btnAction = 1,
                            btnText = "OK"
                        )
                    }
                    is APIResultState.Loading->{
                        progressBarDialog.show()
                    }

                    else -> {
                        manageApiFlowStatus(result, true)
                    }
                }
            }
        }
    }

    private fun checkAwbInPendingList(scannedAwb: String) {
        customScanner.pauseScanner()
        try {
            scannedAwb.toLong().let { awbNo ->
                viewModel.scanShipment(awbNo)
            }
        } catch (ex: Exception) {
            customScanner.startScanner()

        }
    }

    private fun setMessageScreen(
        isSub: Boolean,
        btnText: String,
        msg: String,
        subMsg: String,
        btnAction: Int
    ) {
        val bundle = Bundle()
        bundle.putBoolean(IS_SUB, isSub)
        bundle.putBoolean(STATUS, false)
        bundle.putString(
            SUB_MSG,
            subMsg
        )
        bundle.putString(MSG, msg)
        bundle.putString(BTN_TEXT, btnText)
        bundle.putInt(BTN_ACTION, btnAction)

        navigate(R.id.messageFragment, bundleOf(MSG_BUNDLE to bundle))
    }

    override fun onPause() {
        super.onPause()
        if (::customScanner.isInitialized) {
            customScanner.pauseScanner()
        }
    }

    private fun setUpToolbar() {
        binding.toolbar.tvHeadingName.text =
            ContextCompat.getString(requireContext(), R.string.tally_scan_shipment)
        binding.toolbar.popUpMenuBtn.visible()
        val popWrapper = ContextThemeWrapper(requireContext(), R.style.menuItemStyle)
        val popup = PopupMenu(popWrapper, binding.toolbar.popUpMenuBtn)
        popup.apply {
            inflate(R.menu.tally_scan_menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.markExceptionItem -> {
                        dismiss()
                        if (sharedViewModel.shipmentDetail != null) {
                            sharedViewModel.shipmentDetail?.let { shipment ->
                                navigate(
                                    R.id.exceptionFragment, bundleOf(
                                        AWB_NO to shipment.first,
                                        ORDER_TYPE to shipment.second
                                    )
                                )
                            }
                        } else {
                            showToast("Scan shipment first", false)
                        }
                        return@setOnMenuItemClickListener true
                    }

                    R.id.markCompleteItem -> {
                        dismiss()
                        customScanner.pauseScanner()
                        viewModel.callCompleteRecon()
                        return@setOnMenuItemClickListener true
                    }

                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
            }
        }
        if (!viewModel.isExceptionMarkAvailable()) {
            popup.menu.removeGroup(R.id.markException)
        }
        binding.toolbar.popUpMenuBtn.setOnClickListener {
            popup.show()
        }
        binding.toolbar.ivBackArrow.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.toolbar.ivBackArrow.id -> {
                backPress.handleOnBackPressed()
            }
        }
    }

    private fun openCancelTallyDialog() {
        dBinding = CancelTallyDialogBinding.inflate(layoutInflater)
        cancelDialog = Dialog(requireContext())
        cancelDialog.apply {
            setContentView(dBinding.root)
            window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
            }
            setCancelable(false)
        }
        cancelDialog.show()
        dBinding.noBtn.setOnClickListener {
            customScanner.startScanner()
            cancelDialog.dismiss()
        }

        dBinding.yesBtn.setOnClickListener {
            viewModel.callCancelRecon()
            cancelDialog.dismiss()
        }
    }

    private fun showErrorDialog(description: String, awb: String) {
        customScanner.pauseScanner()
        errorBinding = TallyErrorDialogBinding.inflate(layoutInflater)
        errorDialog = Dialog(requireContext())
        errorDialog.apply {
            setContentView(errorBinding.root)
            window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
            }
            setCancelable(false)
        }
        errorDialog.show()
        if (description.isNotBlank()) {
            errorBinding.headingTv.text = description
        }
        errorBinding.awbTv.text = awb
        errorBinding.scanBtn.setOnClickListener {
            customScanner.startScanner()
            errorDialog.dismiss()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPress.remove()
        if (::customScanner.isInitialized)
            customScanner.unRegisterScanner()
    }
}