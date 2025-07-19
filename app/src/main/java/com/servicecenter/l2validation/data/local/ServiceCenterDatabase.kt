package com.servicecenter.l2validation.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
import com.servicecenter.l2validation.data.local.dao.PendingRtsDao
import com.servicecenter.l2validation.data.local.dao.ShipmentDao
import com.servicecenter.l2validation.data.local.entities.PendingRtsData
import com.servicecenter.l2validation.data.local.entities.ShipmentDetail

@Database(entities = [ShipmentDetail::class,PendingRtsData::class] , version = 3, exportSchema = false)

abstract class ServiceCenterDatabase : RoomDatabase() {
    abstract fun shipmentDetailDao():ShipmentDao

    abstract fun rtsPendingDataDao():PendingRtsDao
}
