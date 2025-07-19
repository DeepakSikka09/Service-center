package com.servicecenter.l2validation.app.ui.activity.salTally

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.ui.viewmodel.TallyViewModel
import com.servicecenter.l2validation.databinding.ActivitySalTallyBinding
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SalTallyActivity : BaseActivity<ActivitySalTallyBinding, TallyViewModel>() {
    override fun getLayout(): Int {
        return R.layout.activity_sal_tally
    }

    override fun getViewModels(): Class<TallyViewModel> {
        return TallyViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.sal_tally_graph)
        if (viewModel.getTallyStatus() == Constants.TALLY_MARKED_COMPLETED){
            graph.setStartDestination(R.id.markCompleteFragment)
        }else {
            graph.setStartDestination(R.id.scanFragment)
        }

        val navController = navHostFragment.navController
        navController.setGraph(graph, intent.extras)
    }
}