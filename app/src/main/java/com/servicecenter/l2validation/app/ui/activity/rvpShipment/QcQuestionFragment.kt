package com.servicecenter.l2validation.app.ui.activity.rvpShipment


import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.ui.adapters.ProductImagePagerAdapter
import com.servicecenter.l2validation.data.remote.model.QCQuestion
import com.servicecenter.l2validation.databinding.FragmentQcQuestionBinding
import com.servicecenter.l2validation.utils.Constants

class QcQuestionFragment : BaseFragment<FragmentQcQuestionBinding>(), View.OnClickListener {

    private var flyerRltdAirwillNo: String? = null
    private var frontImageId: Long = 0L
    private var backImageId: Long = 0L
    private var isAnswered = false
    private val handler = Handler(Looper.getMainLooper())
    private var questions: List<QCQuestion> = emptyList()
    private var currentIndex: Int = 0

    companion object {
        // Modify newInstance() to accept the additional parameters
        fun newInstance(
            flyerRltdAirwillNo: String,
            frontImageId: Long,
            backImageId: Long,
            questions: ArrayList<QCQuestion>,
            index: Int): QcQuestionFragment {
            val fragment = QcQuestionFragment()
            val bundle = Bundle()
            bundle.putString("flyerRltdAirwillNo", flyerRltdAirwillNo)  // Pass the flyerRltdAirwillNo
            bundle.putLong("frontImageId", frontImageId)  // Pass the frontImageId
            bundle.putLong("backImageId", backImageId)  // Pass the backImageId
            bundle.putSerializable("questions", questions)  // Use putSerializable for the list of questions
            bundle.putInt("index", index)  // Pass the current index
            fragment.arguments = bundle
            return fragment
        }
    }

    // Override getLayout() to use the layout provided by BaseFragment
    override fun getLayout(): Int = R.layout.fragment_qc_question

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the additional parameters from the arguments
        flyerRltdAirwillNo = arguments?.getString("flyerRltdAirwillNo")
        frontImageId = arguments?.getLong("frontImageId") ?: 0L
        backImageId = arguments?.getLong("backImageId") ?: 0L

        // Retrieve the questions list and the current index
        val questions = arguments?.getSerializable("questions") as? ArrayList<QCQuestion> ?: emptyList()
        val index = arguments?.getInt("index") ?: 0

        // Update the fragment's questions and current index
        updateQuestions(questions, index)
    }

    fun updateQuestions(qcQuestions: List<QCQuestion>, index: Int) {
        this.questions = qcQuestions
        this.currentIndex = index

        // Check if the question list is empty
        if (questions.isNullOrEmpty()) {
            return
        }

        // Get the current question
        val currentQuestion = questions[currentIndex]

        // Show instructions for the current question
        binding.tvInstructions.text = currentQuestion.instructions

        // Load the images
        val feImage = currentQuestion.fe_image
        val imageUrls = feImage.split(",").map { it.trim() }
        val adapter = ProductImagePagerAdapter(requireContext(), ArrayList(imageUrls))
        binding.viewPagerImages.adapter = adapter
        binding.dotsIndicatorCi.setViewPager(binding.viewPagerImages)

        // Initially disable submit button
        binding.submitBtn.isEnabled = false

        // Set button text based on the question index
        binding.submitBtn.text = if (currentIndex == questions.size - 1)
            getString(R.string.submit)  // If it's the last question, show "Submit"
        else
            getString(R.string.next)  // Otherwise, show "Next"

        // Set up the RadioGroup listener to detect the selection of Yes or No
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = view?.findViewById<RadioButton>(checkedId)
            if (selectedRadioButton != null) {
                val answer = if (selectedRadioButton.text == "Yes") "Yes" else "No"
                val currentQuestion = questions[currentIndex]
                //val qcParameterId = currentQuestion.qc_parameter_id
                currentQuestion.answer = answer

                handler.postDelayed({
                    binding.submitBtn.isEnabled = true
                    binding.submitBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_color))
                }, 2000)
            }
        }



        // Handle button click for Next or Submit
        binding.submitBtn.setOnClickListener {
            handleNextQuestion()
        }
    }

    private fun handleNextQuestion() {
        if (currentIndex < questions.size - 1) {
            // Move to the next question
            val nextFragment = QcQuestionFragment.newInstance(flyerRltdAirwillNo ?: "", frontImageId, backImageId, ArrayList(questions), currentIndex + 1)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null) // Add to back stack
                .commit()
        } else {
            // All questions are done, submit the answers
            submitAnswers()
        }
    }

    private fun submitAnswers() {
        // Create a list to hold only the questions with answers
        val answersList = questions.filter { it.answer != null }
            .map { question ->
                mapOf(
                    Constants.qc_parameter_id to question.qc_parameter_id,
                    Constants.answer to question.answer // Only include the ones with non-null answers
                )
            }

        if (answersList.isEmpty()) {
            Toast.makeText(requireContext(), "Please answer all questions before submitting.", Toast.LENGTH_SHORT).show()
            return
        }

        // Now pass all the data to CommitActivity
        val intent = Intent(requireContext(), CommitActivity::class.java)
        intent.putExtra("answers", ArrayList(answersList))
        Log.d("AnswersList", "Answers: " + answersList.toString())

        intent.putExtra(Constants.flyerRelatedAirwillNo, flyerRltdAirwillNo)
        intent.putExtra(Constants.FRONT_IMAGE_ID, frontImageId)
        intent.putExtra(Constants.BACK_IMAGE_ID, backImageId)

        startActivity(intent)
    }


    override fun onClick(p0: View?) {
        // Handle additional click events here
    }

    // Handle back press logic
    fun onBackPressed(): Boolean {
        if (currentIndex == 0) {
            // Prevent going back from the first question
            showToast(getString(R.string.you_can_t_go_back), false)
            return true // Consume the back press event
        } else {
            // Otherwise, navigate back to the previous question
            val prevFragment = QcQuestionFragment.newInstance(flyerRltdAirwillNo ?: "", frontImageId, backImageId, ArrayList(questions), currentIndex - 1)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, prevFragment)
                .commit() // No need for addToBackStack here, since we're already navigating back
            return true
        }
    }
}
