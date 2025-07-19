package com.servicecenter.l2validation.app.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.ui.adapters.CategoryAdapter
import com.servicecenter.l2validation.app.ui.adapters.SubCategoryAdapter
import com.servicecenter.l2validation.app.ui.viewmodel.UDSharedViewModel
import com.servicecenter.l2validation.application.L2Application
import com.servicecenter.l2validation.data.local.entities.FilterResponse
import com.servicecenter.l2validation.data.remote.model.Response
import com.servicecenter.l2validation.databinding.FragmentFilterBottomSheetBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants.request_flag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class FilterBottomSheetFragment(val  viewModel:  UDSharedViewModel, val udStatus: String) : BottomSheetDialogFragment(),
    OnClickListener {
    private lateinit var binding: FragmentFilterBottomSheetBinding
    private lateinit var subCategoryAdapter: SubCategoryAdapter
    private val requestData=HashMap<String,Any>()


    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
       //k setStyle(STYLE_NORMAL, R.style.bottomSheetBackground);

        inIt()
        fetchFilterListApiState()
        return binding.root

    }
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                val layoutParams = it.layoutParams
                layoutParams.height = (resources.displayMetrics.heightPixels * 0.7).toInt()
                it.layoutParams = layoutParams
                BottomSheetBehavior.from(it).isDraggable=false
                BottomSheetBehavior.from(it).expandedOffset=70

                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }



    private fun inIt() {

        binding.close.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
        binding.btnApply.setOnClickListener(this)

    }

    private fun fetchFilterListApiState() {

        CoroutineScope(Dispatchers.Main).launch {
            viewModel.filterListApiFlow.collect {
                when (it) {
                    is APIResultState.Success -> {


                        viewModel.filterData = (it.data as Response).filters

                        setupRecyclerViews(viewModel.filterData)

                    }

                    else -> {
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupRecyclerViews(filterData: List<FilterResponse>) {

        if(filterData.size>0) {
            binding.tvSortBy.text = getString(R.string.by) + " " + filterData[0].key
            val categoryAdapter = CategoryAdapter(filterData) { category, pos ->
                // category item click
                subCategoryAdapter.updateData(filterData[pos].value, pos)
                binding.tvSortBy.text = getString(R.string.by) + " " + category.key
            }

            binding.rvCate.adapter = categoryAdapter
            binding.rvCate.layoutManager = LinearLayoutManager(context)

            subCategoryAdapter =
                SubCategoryAdapter(0, filterData[0].value) { subData, status, catPosition ->
                    subData.filter_checked = status

                }
            binding.rvSub.layoutManager = LinearLayoutManager(context, VERTICAL, false)
            binding.rvSub.adapter = subCategoryAdapter


        }

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.close -> {
                dismiss()
            }

            R.id.btnApply -> {
                Log.d("check_data", viewModel.filterData.size.toString())
                viewModel.addFilterData(viewModel.filterData)


                for(data in viewModel.filteredData){
                    requestData.put(data.key,data.value)
                }
                requestData[request_flag]=udStatus
                viewModel.filterDataList=true
                viewModel.resetTheCurrentPage(1)
                viewModel.callUDShipmentListApi(requestData)
                dismiss()
            }

            R.id.btnCancel -> {
                viewModel.clearFilterchecked()
                viewModel.filterDataList=true
                requestData[request_flag]=udStatus
                viewModel.resetTheCurrentPage(1)
                viewModel.callUDShipmentListApi(requestData)

                dismiss()
            }
        }
    }
}
