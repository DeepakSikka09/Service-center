package com.servicecenter.l2validation.app.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.servicecenter.l2validation.app.ui.activity.udCalling.fragment.NoResponseFragment
import com.servicecenter.l2validation.app.ui.activity.udCalling.fragment.PendingFragment
import com.servicecenter.l2validation.app.ui.activity.udCalling.fragment.ValidateFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity):FragmentStateAdapter(fragmentActivity) {

    private val fragments= listOf(
        PendingFragment(),
        ValidateFragment(),
        NoResponseFragment()
    )

    override fun getItemCount(): Int =fragments.size

    override fun createFragment(position: Int): Fragment=fragments[position]


}