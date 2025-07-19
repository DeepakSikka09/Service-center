package com.servicecenter.l2validation.app.ui.adapters


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.servicecenter.l2validation.app.ui.activity.rvpShipment.QcQuestionFragment
import com.servicecenter.l2validation.data.remote.model.QCQuestion

class QcQuestionPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val qcQuestions: List<QCQuestion>,
    private val flyerRltdAirwillNo: String,
    private val frontImageId: Long,
    private val backImageId: Long
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return qcQuestions.size
    }

    override fun createFragment(position: Int): Fragment {
        val qcQuestion = qcQuestions[position]
        return QcQuestionFragment.newInstance(flyerRltdAirwillNo,
            frontImageId,
            backImageId,
            ArrayList(qcQuestions),
            position)
    }
}

