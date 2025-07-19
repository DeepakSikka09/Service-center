package com.servicecenter.l2validation.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.domain.usecases.RVPScanUseCase
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.data.remote.model.CommonRequest
import com.servicecenter.l2validation.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImageQcViewModel @Inject constructor(
    private val rvpScanUseCase: RVPScanUseCase,
    val serviceCenterDatabase: ServiceCenterDatabase,
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
) : ViewModel() {

    private val _imageListApiflow = MutableStateFlow<APIResultState>(APIResultState.Empty)
    val ImageListApiflow: StateFlow<APIResultState> get() = _imageListApiflow





    fun callImageListApi(awb: String) {
        viewModelScope.launch {
            try {
                _imageListApiflow.value = APIResultState.Loading
                val imageListResult = withContext(Dispatchers.IO) {
                    rvpScanUseCase.executeProductImage(awb)
                }

                if (imageListResult.status) {
                    val qcQuestions =imageListResult.response?.qc_questions ?: emptyList()
                    qcQuestions.forEachIndexed { index, question ->
                        Log.d("QCQuestionChecksss", "Question $index: qc_parameter_id = ${question.qc_parameter_id}")
                    }

                    _imageListApiflow.value = APIResultState.Success(qcQuestions) // Set the result as a list of QC questions
                } else {
                    _imageListApiflow.value =
                        APIResultState.Failure(imageListResult.response?.description ?: "No description")
                }
            } catch (e: Exception) {
                _imageListApiflow.value = APIResultState.Failure(e.localizedMessage ?: "Error occurred")
            }
        }
    }

}