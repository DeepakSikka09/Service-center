package com.servicecenter.l2validation.data.di
//Code Reviewed
import android.content.Context
import androidx.room.Room
import com.servicecenter.l2validation.data.local.ServiceCenterDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ServiceCenterDatabase {
        return Room.databaseBuilder(
            context,
            ServiceCenterDatabase::class.java,
            "service_center_database"
        ).fallbackToDestructiveMigration().build()
    }
}
