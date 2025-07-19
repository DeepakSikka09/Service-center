package com.servicecenter.l2validation.data.local.dao
//Code Reviewed
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.servicecenter.l2validation.data.local.entities.PendingRtsData

@Dao
interface PendingRtsDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<PendingRtsData>)

   @Query("SELECT * From pendingrtsdata")
     fun getData():List<PendingRtsData>

    @Query("DELETE FROM pendingrtsdata")
    suspend fun deleteRtsShipment()

}