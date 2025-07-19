package com.servicecenter.l2validation.app.ui.activity.salTally

import android.os.Bundle
import android.view.View
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.databinding.FragmentSearchScanBinding
import com.servicecenter.l2validation.utils.Constants.BundleConstants.AWB_NO
import com.servicecenter.l2validation.utils.CustomScanner

class SearchScanFragment : BaseFragment<FragmentSearchScanBinding>() {
    private lateinit var customScanner:CustomScanner
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.tvHeadingName.text = getText(R.string.tally_scan_shipment)
        if(!::customScanner.isInitialized) {
            customScanner = CustomScanner(
                binding.barcodeView,
                { scannedText ->
                    customScanner.pauseScanner()
                    val bundle=Bundle()
                    bundle.putString(AWB_NO,scannedText)
                    popBackWithData(bundle)
                },
                {
                    showToast("AWB Invalid",false)
                    popBackStack()
                },
                binding.flashIv,
                requireActivity()
            )
            customScanner.initializeScanner()
        }
        binding.toolbar.ivBackArrow.setOnClickListener {
            popBackStack()
        }

    }
    override fun getLayout(): Int {
        return R.layout.fragment_search_scan
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!::customScanner.isInitialized){
            customScanner.unRegisterScanner()
        }
    }

    override fun onResume() {
        super.onResume()
        if(::customScanner.isInitialized){
            customScanner.startScanner()
        }
    }
}