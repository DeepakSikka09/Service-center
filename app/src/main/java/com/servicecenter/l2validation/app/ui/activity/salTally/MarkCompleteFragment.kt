package com.servicecenter.l2validation.app.ui.activity.salTally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.gone
import com.servicecenter.l2validation.app.extensions.visible
import com.servicecenter.l2validation.app.ui.adapters.MarkCompleteAdapter
import com.servicecenter.l2validation.app.ui.viewmodel.MarkCompleteViewModel
import com.servicecenter.l2validation.app.ui.viewmodel.TallyViewModel
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.data.remote.model.TallyShipmentDetail
import com.servicecenter.l2validation.databinding.FragmentMarkCompleteBinding
import com.servicecenter.l2validation.databinding.TallyFilterBottomsheetBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants.BundleConstants.AWB_NO
import com.servicecenter.l2validation.utils.Constants.BundleConstants.BTN_ACTION
import com.servicecenter.l2validation.utils.Constants.BundleConstants.BTN_TEXT
import com.servicecenter.l2validation.utils.Constants.BundleConstants.IS_SUB
import com.servicecenter.l2validation.utils.Constants.BundleConstants.MSG
import com.servicecenter.l2validation.utils.Constants.BundleConstants.MSG_BUNDLE
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
class MarkCompleteFragment : BaseFragment<FragmentMarkCompleteBinding>() {

    lateinit var pendingAdapter: MarkCompleteAdapter
    private lateinit var backPress: OnBackPressedCallback
    private lateinit var filterBottomBinding: TallyFilterBottomsheetBinding
    private val filterSet = mutableSetOf<String>()
    private var searchTxt = ""
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var pendingList = listOf<TallyShipmentDetail>()
    private lateinit var customScanner: CustomScanner
    private val sharedViewModel: TallyViewModel by activityViewModels()
    val viewModel: MarkCompleteViewModel by viewModels()
    override fun getLayout(): Int {
        return R.layout.fragment_mark_complete
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPress = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>("bundle")
                if (searchTxt.isNotBlank()) {
                    searchTxt = ""
                    binding.searchView.setQuery("", true)
                    searchAndFilter()
                } else if (sharedViewModel.selectedAwbSet.isNotEmpty())
                    pendingAdapter.hideSelectAll()
                else
                    showToast("Please Complete SAL Tally", false)
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
        binding.toolbar.tvHeadingName.text =
            ContextCompat.getString(requireContext(), R.string.mark_complete)
        viewModel.callPendingShipment()
        sharedViewModel.setShipmentDetails(null)
        setRecycleView()
        if (!viewModel.isFlowRegister) {
            observe()
            viewModel.isFlowRegister = true
        }
        registerClick()
        setupSearchView()
    }

    private fun setRecycleView() {
        pendingAdapter = MarkCompleteAdapter({
            binding.selectAllCl.visible()
            binding.scanBtn.text = getString(R.string.mark_shortage)
            binding.toolbar.tvHeadingName.text =
                ContextCompat.getString(requireContext(), R.string.mark_shipment_shortage)
        }, {
            binding.selectAllCl.gone()
            binding.scanBtn.text = getString(R.string.tally_scan_shipment)
            binding.toolbar.tvHeadingName.text =
                ContextCompat.getString(requireContext(), R.string.mark_complete)
        }, { state ->
            binding.selectAllCheck.isChecked = state
        }, sharedViewModel.selectedAwbSet
        )
        binding.listRecycler.adapter = pendingAdapter
        binding.listRecycler.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        if (sharedViewModel.selectedAwbSet.isNotEmpty()) {
            binding.selectAllCl.visible()
        } else {
            binding.selectAllCl.gone()
        }
    }

    private fun registerClick() {
        binding.selectAllCheck.setOnClickListener {
            if (binding.selectAllCheck.isChecked) {
                pendingAdapter.selectAllItem()
            } else {
                pendingAdapter.hideSelectAll()
            }
        }

        binding.scanBtn.setOnClickListener {
            searchTxt = ""
            binding.searchView.setQuery("", true)
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>("bundle")
            if (sharedViewModel.selectedAwbSet.isNotEmpty()) {
                viewModel.callMarkShortage(sharedViewModel.selectedAwbSet.toList())
            } else {
                navigate(R.id.scanFragment)
            }
        }

        binding.filterIv.setOnClickListener {
            showFilterBottomSheet()
        }
        binding.toolbar.ivBackArrow.setOnClickListener {
            backPress.handleOnBackPressed()
        }
        binding.barcodeImage.setOnClickListener {
            if (CustomScanner.isNewLand()) {
                if (!(::customScanner.isInitialized)) {
                    customScanner = CustomScanner(null, { scannedText ->
                        searchTxt = scannedText
                        binding.searchView.setQuery(scannedText, true)
                        searchAndFilter()
                    }, {
                        showToast("invalid AWB", false)
                    }, null, requireActivity())
                    customScanner.initializeScanner()
                }
            } else {
                navigate(R.id.searchScanFragment)
            }
        }
    }

    private fun observe() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.pendingList.collect { response ->
                when (response) {
                    is APIResultState.Success -> {
                        response.data as Response
                        response.data.tally_shipment.let {tallyShipment->
                            if (tallyShipment!=null) {
                                pendingList = tallyShipment

                                "${response.data.scanned_shipments}/${response.data.total_shipments}".also { binding.completeValue.text = it }
                                val bundle =
                                    findNavController().currentBackStackEntry?.savedStateHandle?.get<Bundle>(
                                        "bundle"
                                    )
                                val awbSearched = bundle?.getString(AWB_NO) ?: ""
                                if (awbSearched.isNotBlank()) {
                                    searchTxt = awbSearched
                                    binding.searchView.setQuery(awbSearched, false)
                                    searchAndFilter()
                                } else {
                                    pendingAdapter.setList(tallyShipment)
                                }
                                response.data.recon_status?.let {status->
                                    viewModel.updateTallyStatus(status)
                                    when (status) {
                                        TALLY_NOT_STARTED -> {
                                            setMessageScreen(
                                                msg = response.data.description,
                                                btnAction = 4
                                            )
                                        }
                                        TALLY_STARTED -> {}
                                        TALLY_MARKED_COMPLETED -> {}
                                        TALLY_COMPLETED -> {
                                            setMessageScreen(
                                                msg = response.data.description,
                                                btnAction = 4
                                            )
                                            viewModel.updateTallyMsg(response.data.description)
                                        }
                                        else->{}
                                    }
                                }
                                progressDialog().dismiss()
                            }else{
                                progressDialog().dismiss()
                                viewModel.updateTallyStatus(TALLY_MARKED_COMPLETED)
                                viewModel.updateTallyMsg(response.data.description)
                                showToast(response.data.description,true)
                                requireActivity().finish()
                            }
                        }
                    }

                    is APIResultState.Failure -> {
                        progressDialog().dismiss()
                        setMessageScreen(
                            false,
                            response.description,
                            2
                        )
                    }

                    else -> {
                        manageApiFlowStatus(response, true)
                    }
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.scanShipmentFlow.collect { result ->
                when (result) {
                    is APIResultState.Success -> {
                        result.data as Response
                        progressDialog().dismiss()
                        result.data.recon_status?.let {status->
                            viewModel.updateTallyStatus(status)
                            when (status) {
                                TALLY_NOT_STARTED -> {
                                    setMessageScreen(
                                        msg = result.data.description,
                                        btnAction = 4
                                    )
                                }
                                TALLY_STARTED -> {}
                                TALLY_MARKED_COMPLETED -> {
                                    setMessageScreen()
                                }
                                TALLY_COMPLETED -> {
                                    setMessageScreen(
                                        msg = result.data.description,
                                        btnAction = 4
                                    )
                                    viewModel.updateTallyMsg(result.data.description)
                                }
                                else->{}
                            }
                        }
                    }

                    is APIResultState.Failure -> {
                        progressDialog().dismiss()
                        setMessageScreen(
                            false,
                            result.description
                        )
                    }

                    else -> {
                        manageApiFlowStatus(result, true)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPress.remove()
        if (::customScanner.isInitialized) {
            customScanner.unRegisterScanner()
        }
    }

    private fun showFilterBottomSheet() {
        if (!(::filterBottomBinding.isInitialized)) {
            filterBottomBinding = TallyFilterBottomsheetBinding.inflate(layoutInflater)
        }
        filterBottomBinding.close.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        filterBottomBinding.btnClear.setOnClickListener {
            pendingAdapter.setList(pendingList)

            filterBottomBinding.apply {
                fwdCheck.isChecked = false
                rtsCheck.isChecked = false
                rvpCheck.isChecked = false
            }
            filterSet.clear()
            searchAndFilter()
            bottomSheetDialog.dismiss()
        }
        filterBottomBinding.btnApply.setOnClickListener {
            searchAndFilter()
            bottomSheetDialog.dismiss()
        }
        filterBottomBinding.fwdCheck.setOnClickListener {
            if (filterBottomBinding.fwdCheck.isChecked) {
                filterSet.add("FWD")
            } else {
                filterSet.remove("FWD")
            }
        }
        filterBottomBinding.rtsCheck.setOnClickListener {
            if (filterBottomBinding.rtsCheck.isChecked) {
                filterSet.add("RTS")
            } else {
                filterSet.remove("RTS")
            }
        }
        filterBottomBinding.rvpCheck.setOnClickListener {
            if (filterBottomBinding.rvpCheck.isChecked) {
                filterSet.add("RVP")
            } else {
                filterSet.remove("RVP")
            }
        }
        if (!(::bottomSheetDialog.isInitialized)) {
            bottomSheetDialog = BottomSheetDialog(requireContext())
            bottomSheetDialog.setContentView(filterBottomBinding.root)
        }
        bottomSheetDialog.show()
    }

    private fun setupSearchView() {
        binding.searchView.setIconifiedByDefault(false)
        binding.searchView.setQueryHint("Search AWB")
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.length in 9..12) {
                    searchTxt = query
                    searchAndFilter()
                } else {
                    showToast(
                        getString(R.string.please_enter_a_number_between_10_to_12_characters),
                        false
                    )
                }
                hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        val closeButton: View =
            binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setOnClickListener {
            hideKeyboard()
            searchTxt = ""
            binding.searchView.setQuery("", true)
            searchAndFilter()
        }
    }

    fun searchAndFilter() {
        if (searchTxt.isBlank()) {
            if (filterSet.isNotEmpty()) {
                pendingAdapter.setList(pendingList.filter { item ->
                    filterSet.contains(item.product_type)
                })
            } else {
                pendingAdapter.setList(pendingList)
            }
        } else {
            if (filterSet.isNotEmpty()) {
                pendingAdapter.setList(pendingList.filter { item ->
                    filterSet.contains(item.product_type)
                }.filter {
                    it.awb_no.toString().contains(searchTxt)
                })
            } else {
                pendingAdapter.setList(pendingList.filter {
                    it.awb_no.toString().contains(searchTxt)
                })
            }
        }
    }

    private fun setMessageScreen(
        status: Boolean = true,
        msg: String = "${sharedViewModel.selectedAwbSet.size} Shipments Marked Shortage Successfully",
        btnAction: Int = 2
    ) {
        val bundle = Bundle()
        bundle.putBoolean(IS_SUB, false)
        bundle.putBoolean(STATUS, status)
        bundle.putString(SUB_MSG, "")
        bundle.putString(
            MSG,
            msg
        )
        bundle.putString(BTN_TEXT, "OK")
        bundle.putInt(BTN_ACTION, btnAction)
        navigate(
            R.id.messageFragment,
            bundleOf(MSG_BUNDLE to bundle)
        )
        sharedViewModel.selectedAwbSet.clear()
        searchTxt = ""
        binding.searchView.setQuery("", true)
        searchAndFilter()
    }

}