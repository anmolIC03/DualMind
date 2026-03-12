package com.example.dualmind.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MeetingEntity::class, AudioChunkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun meetingDao(): MeetingDao

}