package com.servicecenter.l2validation.app.ui.activity.udCalling.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.hideKeyboard
import com.servicecenter.l2validation.app.ui.activity.udCalling.BarcodeScannerActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDCallingActivity
import com.servicecenter.l2validation.app.ui.activity.udCalling.UDShipmentActivity
import com.servicecenter.l2validation.app.ui.adapters.UDListAdapter
import com.servicecenter.l2validation.app.ui.fragments.FilterBottomSheetFragment
import com.servicecenter.l2validation.app.ui.viewmodel.UDSharedViewModel
import com.servicecenter.l2validation.data.local.entities.UDShipment
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.FragmentPending2Binding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.CommonUtils.isInternetAvailable
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.PENDING
import com.servicecenter.l2validation.utils.Constants.request_flag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PendingFragment : BaseFragment<FragmentPending2Binding>() {
    private lateinit var adapter: UDListAdapter
    private val  viewModel:  UDSharedViewModel by viewModels()
    private var pendingList: ArrayList<UDShipment> = arrayListOf()
    private lateinit var layoutManager: LinearLayoutManager
    private var udStatus = PENDING
    private var inputData=HashMap<String,Any>()
    private var isManualSearch = false
    override fun getLayout(): Int {
        return R.layout.fragment_pending2
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize()
        if (isInternetAvailable(requireContext())) {

            inputData[request_flag]=udStatus
            viewModel.callUDShipmentListApi(inputData)
            viewModel.callFilterApi(udStatus)
            setupRecyclerView()
        } else {
            showToast(
                getString(R.string.no_internet), false
            )

        }
        fetchShipmentListApiState()
        callPaginationApi()
        checkLoadingBar()
        setupSearchView()
    }

    private fun initialize(){
        binding.searchBar.barcodeImage.setOnClickListener {
            val intent=Intent(requireContext(),BarcodeScannerActivity::class.java).apply {
                putExtra(Constants.UD_STATUS, udStatus)
            }
            startActivity(intent)
        }

        binding.searchBar.filter.setOnClickListener {
            val bottomSheetFragment = FilterBottomSheetFragment(viewModel,udStatus)
            bottomSheetFragment.show(
                requireActivity().supportFragmentManager, bottomSheetFragment.tag
            )
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = UDListAdapter(emptyList(),udStatus) { item ->
            // Handle item click
            val intent = Intent(requireContext(), UDShipmentActivity::class.java).apply {
                putExtra(Constants.UD_STATUS, udStatus)
                putExtra(Constants.AWB_NUMBER, item.awb_no)
                putExtra(Constants.DRS_ID, item.drs_id)
            }
            startActivity(intent)

        }
        binding.recyclerView.adapter = adapter
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

    private fun fetchShipmentListApiState() {
        lifecycleScope.launch (Dispatchers.Main) {
            viewModel.udShipmentList.collect {
                when (it) {
                    is APIResultState.Success -> {

                        val shipmentList = it.data as Response

                        if(shipmentList.ud_shipment.isEmpty()){
                            binding.recyclerConst.visibility=View.GONE
                            binding.tvNotFound.visibility=View.VISIBLE
                            binding.ivCross.visibility=View.VISIBLE
                            getPendingList(shipmentList)
                        }else {
                            binding.recyclerConst.visibility=View.VISIBLE
                            binding.tvNotFound.visibility=View.GONE
                            binding.ivCross.visibility=View.GONE
                            getPendingList(shipmentList)
                        }
                        progressDialog().dismiss()

                    }
                    is APIResultState.Loading -> {
                        if (viewModel.currentPage <= 1) {
                            progressDialog().show()

                        }
                    }
                    is APIResultState.Failure->{
                        progressDialog().dismiss()
                        (activity as UDCallingActivity).showErrorMsg(it.description)
                    }

                    else -> {
                        manageApiFlowStatus(it, false)
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getPendingList(allShipmentList: Response) {
        if (isManualSearch) {
            pendingList.clear()
        } else if (viewModel.filterDataList) {
            pendingList.clear()
            viewModel.filterDataList = false
        }
        pendingList.addAll(allShipmentList.ud_shipment)

        (activity as? UDCallingActivity)?.updateTabSize(0, allShipmentList.total_count)
        adapter.updateList(pendingList)
        adapter.notifyDataSetChanged()
    }

    private fun callPaginationApi() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                if (visibleItemCount + pastVisibleItems >= totalItemCount) {

                    if(!isManualSearch) {
                        viewModel.loadNextPage(udStatus)
                    }
                }
            }
        })

    }
    private fun setupSearchView() {
        binding.searchBar.searchView.setIconifiedByDefault(false)
        binding.searchBar.searchView.setQueryHint(getString(R.string.search))
        binding.searchBar.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.length in 9..12) {
                    isManualSearch = true //  flag for manual search
                    binding.searchBar.searchView.clearFocus()
                    binding.searchBar.searchView.setQuery(query, false)
                    (requireActivity() as UDCallingActivity).hideKeyboard(requireActivity())
                    val inputDataSearch=HashMap<String,Any>()
                    inputDataSearch[request_flag]=udStatus
                    inputDataSearch["awb_no"]=query
                    viewModel.resetTheCurrentPage(1)
                    viewModel.callUDShipmentListApi(inputDataSearch)

                } else {
                    showToast(
                        getString(R.string.please_enter_a_number_between_10_to_12_characters), false
                    )
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        val closeButton: View =
            binding.searchBar.searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setOnClickListener {

            binding.searchBar.searchView.setQuery("", false)
            binding.searchBar.searchView.clearFocus()
            cancelManualSearch()
        }
    }

    private fun cancelManualSearch() {
        pendingList.clear()
        isManualSearch = false
        viewModel.clearFilterchecked()
        viewModel.clearFilteredList()
        viewModel.resetTheCurrentPage(1)
        inputData[request_flag]=udStatus
        viewModel.callUDShipmentListApi(inputData)
         hideKeyboard()
    }
}