package com.androiddevs.runningappyt.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.androiddevs.runningappyt.db.converters.BitmapConverter

@Database(
    entities = [Run::class],
    version = 1
)
@TypeConverters(BitmapConverter::class)
abstract class RunningDatabase : RoomDatabase() {

    abstract fun getRunDao(): RunDAO
}