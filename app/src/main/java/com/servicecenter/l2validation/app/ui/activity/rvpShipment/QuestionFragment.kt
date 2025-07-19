package com.servicecenter.l2validation.app.ui.activity.rvpShipment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.analyticsToAllButton
import com.servicecenter.l2validation.app.extensions.analyticsToAllScreen
import com.servicecenter.l2validation.app.ui.adapters.ProductImagePagerAdapter
import com.servicecenter.l2validation.databinding.FragmentQuestionBinding
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.FE_IMAGE_CLEAR
import com.servicecenter.l2validation.utils.Constants.FE_IMAGE_MATCHED
import com.servicecenter.l2validation.utils.Constants.FLYER_PROPERLY_SEALED
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuestionFragment : BaseFragment<FragmentQuestionBinding>(), View.OnClickListener {

    private lateinit var feImagePagerAdapter: ProductImagePagerAdapter
    private lateinit var customerPagerAdapter: ProductImagePagerAdapter
    private lateinit var flyerImageAdapter: ProductImagePagerAdapter

    private var currentQuestionIndex = 0
    private val questions = mutableListOf<String>()
    private val answers = mutableMapOf<String, String>()
    private var isFeImagesEmpty =false
    private var  isFlyerImagesEmpty =false
    private var  isProductImagesEmpty =false

    override fun getLayout(): Int {
        return R.layout.fragment_question    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize()
        setupAdapters()
        isFeImagesEmpty=arguments?.getStringArrayList(Constants.FE_IMAGES)?.isEmpty()==true
        isFlyerImagesEmpty = arguments?.getStringArrayList(Constants.FLYER_IMAGES)?.isEmpty() == true
        isProductImagesEmpty = arguments?.getStringArrayList(Constants.PRODUCT_IMAGES)?.isEmpty() == true
        setUpQuestions(isFlyerImagesEmpty, isProductImagesEmpty,isFeImagesEmpty)
    }

    private fun initialize() {
        binding.noRadio.setOnClickListener(this)
        binding.yesRadio.setOnClickListener(this)
        binding.submitBtn.setOnClickListener(this)

        (activity as QcImageActivity).analyticsToAllScreen(Constants.QUESTION_FRAGMENT)
    }

    private fun setupAdapters() {
        feImagePagerAdapter = ProductImagePagerAdapter(requireContext(), arrayListOf())
        customerPagerAdapter = ProductImagePagerAdapter(requireContext(), arrayListOf())
        flyerImageAdapter = ProductImagePagerAdapter(requireContext(), arrayListOf())

        binding.viewPagerCi.adapter = customerPagerAdapter
        binding.dotsIndicatorCi.setViewPager(binding.viewPagerCi)

        binding.viewPagerFe.adapter = feImagePagerAdapter
        binding.dotsIndicatorFe.setViewPager(binding.viewPagerFe)

        binding.viewPagerFlyer.adapter = flyerImageAdapter
        binding.dotsIndicatorFlyer.setViewPager(binding.viewPagerFlyer)

        arguments?.getStringArrayList(Constants.FE_IMAGES)?.let {
            feImagePagerAdapter.updateProductImages(it)
        }
        arguments?.getStringArrayList(Constants.PRODUCT_IMAGES)?.let {
            customerPagerAdapter.updateProductImages(it)
        }
        arguments?.getStringArrayList(Constants.FLYER_IMAGES)?.let {
            flyerImageAdapter.updateProductImages(it)
        }
    }

    private fun setUpQuestions(isFlyerImagesEmpty: Boolean, isProductImagesEmpty: Boolean,isFeImagesEmpty: Boolean) {
        questions.clear()

        if (isProductImagesEmpty && isFeImagesEmpty) {
            // Both FE_IMAGES and PRODUCT_IMAGES are empty
            if (!isFlyerImagesEmpty) {
                questions.add(getString(R.string.is_the_flyer_properly_sealed))
            }
        } else if (isProductImagesEmpty) {
            // PRODUCT_IMAGES are empty
            questions.add(getString(R.string.image_capture_by_fe_is_clear)) // FE Image question
            if (!isFlyerImagesEmpty) {
                questions.add(getString(R.string.is_the_flyer_properly_sealed)) // Flyer-related question
            }
        } else if (isFeImagesEmpty) {
            // FE_IMAGES are empty
            questions.add(getString(R.string.does_the_image_match)) // Product match question
            if (!isFlyerImagesEmpty) {
                questions.add(getString(R.string.is_the_flyer_properly_sealed))
            }
        } else {
            //  None are empty Add all questions normally
            questions.add(getString(R.string.does_the_image_match))
            questions.add(getString(R.string.image_capture_by_fe_is_clear))
            if (!isFlyerImagesEmpty) {
                questions.add(getString(R.string.is_the_flyer_properly_sealed))
            }
        }
        displayQuestion(currentQuestionIndex)
    }

    private fun displayQuestion(index: Int) {
        if (index < questions.size) {
            binding.consigneeReasonTv.text = questions[index]
            binding.submitBtn.text = if (index == questions.size - 1) {
                getString(R.string.submit)
            } else {
                getString(R.string.next)
            }

            binding.submitBtn.isEnabled = false
            binding.submitBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.disable_btn))

            when (questions[index]) {
                getString(R.string.image_capture_by_fe_is_clear) -> {
                    binding.customerConstraint.visibility = View.GONE
                    binding.view2.visibility = View.GONE
                    binding.feConstraint.visibility = View.VISIBLE
                    binding.view3.visibility = View.VISIBLE
                    binding.viewPagerFe.layoutParams.height = resources.getDimensionPixelSize(R.dimen._450DP)
                    binding.viewPagerFe.requestLayout()
                }
                getString(R.string.does_the_image_match) -> {
                    if (isFeImagesEmpty) {
                        // Case: FE images are not available
                        binding.customerConstraint.visibility = View.VISIBLE
                        binding.feConstraint.visibility = View.GONE
                        binding.view3.visibility = View.GONE
                        binding.mainConstraint.visibility = View.VISIBLE
                        binding.viewPagerCi.layoutParams.height = resources.getDimensionPixelSize(R.dimen._450DP)
                        binding.viewPagerCi.requestLayout()
                    } else {
                        // Case: FE images are available
                        binding.customerConstraint.visibility = View.VISIBLE
                        binding.feConstraint.visibility = View.VISIBLE
                        binding.view3.visibility = View.VISIBLE
                        binding.mainConstraint.visibility = View.VISIBLE
                        binding.viewPagerCi.requestLayout()
                    }
                }
                getString(R.string.is_the_flyer_properly_sealed) -> {
                    binding.mainConstraint.visibility = View.GONE
                    binding.flyerConstraint.visibility = View.VISIBLE
                    binding.viewPagerFlyer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun markedRadioBtn() {
        if (binding.yesRadio.isChecked) {
            binding.yesRadio.setBackgroundResource(R.drawable.curved_rectangle_white_four_side)
            binding.noRadio.setBackgroundResource(0)
        } else if (binding.noRadio.isChecked) {
            binding.noRadio.setBackgroundResource(R.drawable.curved_rectangle_white_four_side)
            binding.yesRadio.setBackgroundResource(0)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.yes_radio -> {
                markedRadioBtn()
                (activity as QcImageActivity).analyticsToAllButton(
                    Constants.FE_IMAGE_VALIDATION_YES,
                    Constants.BUTTON_KEY,
                    Constants.FE_IMAGE_VALIDATION_YES
                )
                enableSubmitButton()
            }
            R.id.no_radio -> {
                markedRadioBtn()
                (activity as QcImageActivity).analyticsToAllButton(
                    Constants.FE_IMAGE_VALIDATION_NO,
                    Constants.BUTTON_KEY,
                    Constants.FE_IMAGE_VALIDATION_NO
                )
                enableSubmitButton()
            }
            R.id.submitBtn -> {
                submitAnswer()
            }
        }
    }

    private fun enableSubmitButton() {
        binding.submitBtn.isEnabled = true
        binding.submitBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_color))
    }

    private fun submitAnswer() {
        val selectedAnswer = if (binding.yesRadio.isChecked) getString(R.string.yes) else getString(R.string.no)

        val key = when (questions[currentQuestionIndex]) {
            getString(R.string.does_the_image_match) -> FE_IMAGE_MATCHED
            getString(R.string.image_capture_by_fe_is_clear) -> FE_IMAGE_CLEAR
            getString(R.string.is_the_flyer_properly_sealed) -> FLYER_PROPERLY_SEALED
            else -> "unknown_question"
        }

        answers[key] = selectedAnswer

        currentQuestionIndex++
        if (currentQuestionIndex < questions.size) {
            binding.radioGroup.clearCheck()
            resetRadioButtonBackgrounds()
            displayQuestion(currentQuestionIndex)
        } else {
            (activity as QcImageActivity).sendToCommitActivity(answers)
        }

        binding.submitBtn.isEnabled = false
        binding.submitBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.disable_btn))
    }

    private fun resetRadioButtonBackgrounds() {
        binding.yesRadio.setBackgroundResource(0)
        binding.noRadio.setBackgroundResource(0)
    }
}
/*fe_images->fe captured Image
product_images->customer Image
flyer_images->Flyer Image */



