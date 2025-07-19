package com.servicecenter.l2validation.app.ui.activity.udCalling

import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.ui.adapters.ViewPagerAdapter
import com.servicecenter.l2validation.app.ui.viewmodel.UDSharedViewModel
import com.servicecenter.l2validation.databinding.ActivityUdcallingBinding
import com.servicecenter.l2validation.utils.Constants.PENDING
import com.servicecenter.l2validation.utils.Constants.VALIDATED
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UDCallingActivity : BaseActivity<ActivityUdcallingBinding,UDSharedViewModel>(),View.OnClickListener{


    override fun getLayout(): Int {
        return R.layout.activity_udcalling
    }

    override fun getViewModels(): Class<UDSharedViewModel> {
        return  UDSharedViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initialize()
        binding.productDetails.tvHeadingName.text = getString(R.string.ud_calling)
        binding.viewpager.adapter=ViewPagerAdapter(this)
        binding.viewpager.setOffscreenPageLimit(2)
        initializeTabLayout(binding.tablayout, binding.viewpager)
    }

    private fun initialize() {
        binding.productDetails.ivBackArrow.setOnClickListener(this)
        binding.viewpager.isUserInputEnabled = true
    }

    private fun initializeTabLayout(tabLayout: TabLayout, viewPager: ViewPager2) {
        // Attach TabLayoutMediator
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> PENDING
                1 -> VALIDATED
                2 -> getString(R.string.no_response)
                else -> null
            }
        }.attach()

        // Handle page change events
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (tabLayout.selectedTabPosition != position) {
                    tabLayout.selectTab(tabLayout.getTabAt(position))
                }
                viewModel.resetTheCurrentPage(1)
            }
        })

        // Handle tab selection events
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    if (viewPager.currentItem != it.position) {
                        viewPager.currentItem = it.position
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.iv_back_arrow->finish()
        }
    }

     fun updateTabSize(tabPosition: Int,size:Int) {
        val tab=binding.tablayout.getTabAt(tabPosition)
        tab?.text=when (tabPosition) {

            0 -> "$PENDING ($size)"
            1 -> "$VALIDATED ($size)"
            2 -> "${getString(R.string.no_response)} ($size)"
            else -> null

        }
    }
    fun showErrorMsg(errorMdg:String) {
        binding.disableLocationLayout.clHeader.visibility=View.VISIBLE
        binding.disableLocationLayout.tvHeadingName.text=errorMdg
        binding.tablayout.visibility=View.GONE
        binding.view1.visibility=View.GONE
        binding.viewpager.visibility=View.GONE

    }
}