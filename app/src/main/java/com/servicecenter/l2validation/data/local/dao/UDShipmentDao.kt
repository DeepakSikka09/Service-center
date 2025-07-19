package com.servicecenter.l2validation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail
import com.servicecenter.l2validation.data.local.entities.UDShipment
@Dao
interface UDShipmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanData(UDShipment: ArrayList<UDShipment>)

    @Query("DELETE FROM UDShipment")
    suspend fun deleteShipmentDetail()

    @Query("SELECT * FROM UDShipment")
    fun getAllShipment(): List<UDShipment>

    @Query("SELECT * FROM udshipment WHERE awb_no = :awbNumber")
    suspend fun getShipmentByAwbNumber(awbNumber: String):UDShipment
}