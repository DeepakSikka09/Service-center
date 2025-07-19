package com.servicecenter.l2validation.data.datastore
//Code Reviewed
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface IPreferenceDataStoreAPI {
    suspend fun <T> getData(key: Preferences.Key<T>,defaultValue: T):Flow<T>
    suspend fun <T> setData(key: Preferences.Key<T>,value:T)
    suspend fun <T> removePreference(key: Preferences.Key<T>)
    suspend fun clearAllPreference()
}