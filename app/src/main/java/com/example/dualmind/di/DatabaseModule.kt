package com.example.dualmind.di

import android.content.Context
import androidx.room.Room
import com.example.dualmind.data.local.AppDatabase
import com.example.dualmind.data.local.MeetingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Tells Hilt how to create the Room Database.
    // @Singleton ensures only ONE instance of the database exists in the whole app.
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "dualmind_database"
        ).build()
    }

    // Tells Hilt how to provide the DAO to your Repositories or ViewModels.
    @Provides
    fun provideMeetingDao(database: AppDatabase): MeetingDao {
        return database.meetingDao()
    }
}