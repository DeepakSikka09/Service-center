package com.servicecenter.l2validation.data.local.dao
// Code Reviewed
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail
import androidx.room.Query

@Dao
interface ShipmentDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScanData(shipmentDetail: ArrayList<ShipmentDetail>)

    @Query("SELECT * FROM ShipmentDetail")
    fun getAllShipment(): List<ShipmentDetail>

    @Query("select  * from shipmentdetail WHERE flyer_code = :flyerCode And status in('Pending','Failed' )")
    fun checkflyerCodeExistance(flyerCode: String): List<ShipmentDetail>

    @Query("select  * from shipmentdetail WHERE status = :status")
    fun getStatusWiseShipmentList(status: String): List<ShipmentDetail>

    @Query("DELETE FROM ShipmentDetail")
    suspend fun deleteShipmentDetail()

    @Query("update shipmentdetail SET  status = :status WHERE AWB_No =:AwbNo ")
    fun updateStatusWiseShipmentList(AwbNo: Long, status: String)

    @Query("select is_fe_image_validation_required from shipmentdetail where AWB_No=:awb")
    fun  getQcStatus(awb:String):Boolean


}