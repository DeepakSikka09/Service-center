package com.servicecenter.l2validation.app.ui.activity.salTally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseFragment
import com.servicecenter.l2validation.app.extensions.gone
import com.servicecenter.l2validation.app.extensions.visible
import com.servicecenter.l2validation.app.ui.viewmodel.TallyViewModel
import com.servicecenter.l2validation.databinding.FragmentMessageBinding
import com.servicecenter.l2validation.utils.Constants.BundleConstants.BTN_ACTION
import com.servicecenter.l2validation.utils.Constants.BundleConstants.BTN_TEXT
import com.servicecenter.l2validation.utils.Constants.BundleConstants.IS_SUB
import com.servicecenter.l2validation.utils.Constants.BundleConstants.MSG
import com.servicecenter.l2validation.utils.Constants.BundleConstants.MSG_BUNDLE
import com.servicecenter.l2validation.utils.Constants.BundleConstants.STATUS
import com.servicecenter.l2validation.utils.Constants.BundleConstants.SUB_MSG
import com.servicecenter.l2validation.utils.Constants.TALLY_NOT_STARTED
import com.servicecenter.l2validation.utils.Constants.TALLY_STARTED
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MessageFragment : BaseFragment<FragmentMessageBinding>() {
    private val sharedViewModel: TallyViewModel by activityViewModels()
    private lateinit var backPress: OnBackPressedCallback
    private var buttonAction = 0
    override fun getLayout(): Int {
        return R.layout.fragment_message
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPress = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                when (buttonAction) {
                    1->{
                        //scan next shipment
                        popBackStack(R.id.scanFragment,false)
                    }
                    2->{
                        //mark shortage or sal tally after recon
                        popBackStack(R.id.markCompleteFragment,false)
                    }
                    3->{
                        if (sharedViewModel.getTallyStatus() == TALLY_STARTED){
                            popBackStack(R.id.scanFragment,false)
                        }else{
                            popBackStack(R.id.markCompleteFragment,false)
                        }
                    }
                    4->{
                        sharedViewModel.updateTallyStatus(TALLY_NOT_STARTED)
                        requireActivity().finish()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().onBackPressedDispatcher.addCallback(this.viewLifecycleOwner, backPress)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = requireArguments().getBundle(MSG_BUNDLE)
        val isSub = bundle?.getBoolean(IS_SUB, true) ?: true
        val subTitleMsg = bundle?.getString(SUB_MSG, "") ?: ""
        if (isSub) {
            binding.subHeadingCl.visible()
            binding.subHeadingTv.text = subTitleMsg
        } else {
            binding.subHeadingCl.gone()
        }

        val msg = bundle?.getString(MSG, " ") ?: ""
        binding.messageTv.text = msg

        val status = bundle?.getBoolean(STATUS, true) ?: true
        if (status) {
            binding.iconIv.setImageResource(R.drawable.qc_passed)
        } else {
            binding.iconIv.setImageResource(R.drawable.iv_cross_red)
        }

        val buttonText = bundle?.getString(BTN_TEXT, "") ?: ""
        buttonAction = bundle?.getInt(BTN_ACTION, 0) ?: 0
        binding.submitBtn.text = buttonText
        binding.submitBtn.setOnClickListener{
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPress.remove()
    }
}