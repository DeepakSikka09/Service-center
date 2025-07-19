package com.servicecenter.l2validation.app.ui.activity.rvpShipment

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.app.ui.viewmodel.ImageQcViewModel
import com.servicecenter.l2validation.data.remote.model.QCQuestion
import com.servicecenter.l2validation.databinding.ActivityImageQcBinding
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImageQcActivity : BaseActivity<ActivityImageQcBinding, ImageQcViewModel>(), View.OnClickListener {
    private var flyerRltdAirwillNo: String = ""
    private var frontImageId: Long = 0L
    private var backImageId: Long = 0L

    override fun getLayout(): Int {
        return R.layout.activity_image_qc
    }

    override fun getViewModels(): Class<ImageQcViewModel> {
        return ImageQcViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = intent.extras
        flyerRltdAirwillNo = bundle?.getString(Constants.flyerRelatedAirwillNo).toString()
        frontImageId = bundle?.getLong(Constants.FRONT_IMAGE_ID) ?: 0L
        backImageId = bundle?.getLong(Constants.BACK_IMAGE_ID) ?: 0L
        setupUI()
        fetchQcQuestions()

        // Handle back press logic in the activity
        onBackPressedDispatcher.addCallback(this) {
            // Check if fragment can handle the back press (to navigate through questions)
            val fragment = supportFragmentManager.findFragmentByTag(QcQuestionFragment::class.java.simpleName)
            if (fragment is QcQuestionFragment && fragment.onBackPressed()) {
                // Back press handled by fragment
            } else {
                // Default behavior (if fragment doesn't handle it)
                super.onBackPressed()
            }
        }
    }

    private fun setupUI() {
        binding.productDetails.ivBackArrow.visibility = View.GONE
        binding.tvAwbNo.text = flyerRltdAirwillNo
        if (supportFragmentManager.findFragmentByTag(QcQuestionFragment::class.java.simpleName) == null) {
            val qcQuestionFragment = QcQuestionFragment.newInstance(flyerRltdAirwillNo, frontImageId, backImageId, arrayListOf(),
                0)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, qcQuestionFragment, QcQuestionFragment::class.java.simpleName)
                .commit()
        }
    }


    private fun fetchQcQuestions() {
        viewModel.callImageListApi(flyerRltdAirwillNo)

        // Observe the result and update the UI accordingly
        lifecycleScope.launch {
            viewModel.ImageListApiflow.collect { apiState ->
                when (apiState) {
                    is APIResultState.Success -> {
                        progressDialog().dismiss()
                        val qcQuestions = apiState.data as List<QCQuestion>

                        // Update the fragment with the fetched QC questions
                        val qcQuestionFragment = supportFragmentManager.findFragmentByTag(QcQuestionFragment::class.java.simpleName)
                        if (qcQuestionFragment is QcQuestionFragment) {
                            // Update the fragment with the new questions list and current index (e.g., 0 for the first question)
                            qcQuestionFragment.updateQuestions(qcQuestions, 0)
                        }
                    }
                    else -> {
                        manageApiFlowStatus(apiState, false)
                    }
                }
            }
        }
    }

    override fun onClick(p0: View?) {
        // Handle your button clicks here if needed
    }
}
