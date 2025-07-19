package com.servicecenter.l2validation.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreConstants
import com.servicecenter.l2validation.data.datastore.PreferenceDataStoreHelper
import com.servicecenter.l2validation.data.local.entities.ImagePath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TallyViewModel @Inject constructor(
    val preferenceDataStoreHelper: PreferenceDataStoreHelper,
):ViewModel() {


    private val _imageFlow:MutableStateFlow<ImagePath> = MutableStateFlow(ImagePath())
    val imageFlow:StateFlow<ImagePath> get() = _imageFlow

    val selectedAwbSet = mutableSetOf<Long>()
    var shipmentDetail: Pair<Long, String>? = null

    fun getTallyStatus():Int{
        var tallyStatus = 0
        viewModelScope.launch{
            tallyStatus = withContext(Dispatchers.Main) {
                preferenceDataStoreHelper.getData(
                    PreferenceDataStoreConstants.TALLY_STATUS,
                    0
                ).first()
            }
        }
        return tallyStatus
    }

    fun updateFrontImage(imagePath: String, imageUrl: String){
        viewModelScope.launch {
            _imageFlow.value = _imageFlow.value.copy(
                frontImage = imagePath,
                frontImageUrl = imageUrl
            )
        }
    }
    fun updateBackImage(imagePath: String, imageUrl: String){
        viewModelScope.launch {
            _imageFlow.value = _imageFlow.value.copy(
                backImage = imagePath,
                backImageUrl = imageUrl
            )
        }
    }

    fun setImageEmpty(){
        viewModelScope.launch {
            _imageFlow.value = ImagePath()
        }
    }

    fun setShipmentDetails(pair: Pair<Long, String>?) {
        shipmentDetail = pair
    }

    fun updateTallyStatus(status: Int) {
        viewModelScope.launch {
            preferenceDataStoreHelper.setData(
                PreferenceDataStoreConstants.TALLY_STATUS,
                status
            )
        }
    }
}