package com.servicecenter.l2validation.app.ui.activity.rvpShipment

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.extensions.showToast
import com.servicecenter.l2validation.app.extensions.startScreen
import com.servicecenter.l2validation.databinding.ActivityQcImageBinding
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QcImageActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityQcImageBinding::inflate)
    private var flyerRltdAirwillNo: String = ""
    private var frontImageId: Long = 0L
    private var backImageId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val bundle = intent.extras
        flyerRltdAirwillNo = bundle?.getString(Constants.flyerRelatedAirwillNo).toString()
        frontImageId = bundle?.getLong(Constants.FRONT_IMAGE_ID) ?: 0L
        backImageId = bundle?.getLong(Constants.BACK_IMAGE_ID) ?: 0L

        val feImages = bundle?.getStringArrayList(Constants.FE_IMAGES) ?: arrayListOf()
        val productImages = bundle?.getStringArrayList(Constants.PRODUCT_IMAGES) ?: arrayListOf()
        val flyerImages = bundle?.getStringArrayList(Constants.FLYER_IMAGES) ?: arrayListOf()

        setupUI(feImages, productImages, flyerImages)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()
            }
        })
    }

    private fun setupUI(feImages: List<String>, productImages: List<String>, flyerImages: List<String>) {
        binding.productDetails.ivBackArrow.visibility = View.GONE
        binding.tvAwbNo.text = flyerRltdAirwillNo

        // Initialize the fragment and sending the images to fragment
        if (supportFragmentManager.findFragmentById(R.id.fragments_container) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragments_container, QuestionFragment().apply {
                    arguments = Bundle().apply {
                        putStringArrayList(Constants.FE_IMAGES, ArrayList(feImages))
                        putStringArrayList(Constants.PRODUCT_IMAGES, ArrayList(productImages))
                        putStringArrayList(Constants.FLYER_IMAGES, ArrayList(flyerImages))
                    }
                })
                .commitNow()
        }

        analyticsToAllScreen(Constants.FE_IMAGE_VALIDATION)
    }

    fun sendToCommitActivity(answers: Map<String, String>) {
        val extras = Bundle().apply {
            putString(Constants.flyerRelatedAirwillNo, flyerRltdAirwillNo)
            putLong(Constants.FRONT_IMAGE_ID, frontImageId)
            putLong(Constants.BACK_IMAGE_ID, backImageId)
            answers.forEach { (key, value) ->
                putString(key, value)
            }
        }
        startScreen(CommitActivity(), extras)
    }

     fun backPress() {
        showToast(getString(R.string.you_can_t_go_back), false)
    }
}

