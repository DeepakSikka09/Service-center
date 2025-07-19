package com.servicecenter.l2validation.app.ui.activity.rvpShipment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.ui.adapters.ShipmentAdapter
import com.servicecenter.l2validation.app.ui.viewmodel.ShipmentListViewModel
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail
import com.servicecenter.l2validation.databinding.ActivityShipmentListBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShipmentListActivity :  BaseActivity<ActivityShipmentListBinding, ShipmentListViewModel>(), View.OnClickListener {


    private var came_from = ""
    override fun getLayout(): Int {
        return R.layout.activity_shipment_list
    }

    override fun getViewModels(): Class<ShipmentListViewModel> {
        return ShipmentListViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = intent.extras
        came_from = bundle?.getSerializable("came_from").toString()
        if (came_from == Constants.Failed) {
            binding.productDetails.tvHeadingName.text = getString(R.string.failed_shipments)
            analyticsToAllScreen(Constants.FAILED_LIST)
        } else {
            binding.productDetails.tvHeadingName.text = getString(R.string.pending_shipments)
            analyticsToAllScreen(Constants.PENDING_LIST)

        }
        binding.productDetails.ivBackArrow.setOnClickListener(this)
        viewModel.getShipmentList(came_from)
        fetchData()
    }

    private fun fetchData() {
        lifecycleScope.launch {
            // Collect the StateFlow in the appropriate lifecycle scope
            viewModel.shipmentList.collect { it ->
                when (it) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                      /*  showToast()*/
                        setAdapter(it.data as List<ShipmentDetail>)
                    }
                    else -> {
                        manageApiFlowStatus(it, true)
                    }
                }
            }
        }
    }

    private fun setAdapter(shipmentDetails: List<ShipmentDetail>) {
        if (shipmentDetails.size > 0) {
            binding.shipmentConst.visibility=View.VISIBLE
            binding.noDataTv.visibility=View.GONE
            binding.shipmentConst.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

            binding.shipmentConst.adapter = ShipmentAdapter(
                this,
                shipmentDetails as ArrayList<ShipmentDetail>
            )
        }else{
            binding.shipmentConst.visibility=View.GONE
            binding.noDataTv.visibility=View.VISIBLE
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.iv_back_arrow -> finish()
        }
    }
}